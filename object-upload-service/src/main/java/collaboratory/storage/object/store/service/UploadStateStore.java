/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package collaboratory.storage.object.store.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import collaboratory.storage.object.store.core.model.CompletedPart;
import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadSpecification;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

@Slf4j
public class UploadStateStore {

  private static final String UPLOAD_SEPARATOR = "_";
  private static final String DIRECTORY_SEPARATOR = "/";
  private static final String META = ".meta";
  private static final String PART = ".part";

  @Autowired
  private AmazonS3 s3Client;

  @Value("${collaboratory.bucket.name}")
  private String bucketName;

  @Value("${collaboratory.upload.directory}")
  private String upload;

  public void create(UploadSpecification spec) throws IOException {
    log.debug("Upload Specification : {}", spec);
    ObjectMapper mapper = new ObjectMapper();
    try {
      s3Client.putObject(bucketName, getUploadStateKey(spec.getObjectId(), spec.getUploadId(), META),
          new ByteArrayInputStream(mapper.writeValueAsBytes(spec)), null);
    } catch (IOException e) {
      log.error("Fail to create upload temporary directory", e);
      throw e;
    }
  }

  private String getUploadStateKey(String objectId, String uploadId, String filename) {
    return new StringBuilder(upload)
        .append(DIRECTORY_SEPARATOR)
        .append(objectId)
        .append(UPLOAD_SEPARATOR)
        .append(uploadId)
        .append(DIRECTORY_SEPARATOR)
        .append(filename)
        .toString();
  }

  private String getLexicographicalOrderUploadPartName(int partNumber) {
    return String.format("%04x", (0xFFFF & partNumber));
  }

  public UploadSpecification loadUploadSpecification(String objectId, String uploadId) throws IOException {
    GetObjectRequest req = new GetObjectRequest(bucketName, getUploadStateKey(objectId, uploadId, META));
    S3Object obj = s3Client.getObject(req);
    ObjectMapper mapper = new ObjectMapper();

    return mapper.readValue(obj.getObjectContent(), UploadSpecification.class);

  }

  private boolean isUploadPartCompleted(String objectId, String uploadId, int partNumber) {
    try {
      s3Client.getObjectMetadata(bucketName,
          getUploadStateKey(objectId, uploadId, getLexicographicalOrderUploadPartName(partNumber)));
    } catch (AmazonS3Exception ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) return false;
      throw ex;
    }
    return true;
  }

  @SneakyThrows
  public List<Part> retrieveIncompletedPart(String objectId, String uploadId) {
    UploadSpecification spec = loadUploadSpecification(objectId, uploadId);
    Builder<Part> incompletedParts = ImmutableList.builder();
    for (Part part : spec.getParts()) {
      if (isUploadPartCompleted(objectId, uploadId, part.getPartNumber())) {
        incompletedParts.add(part);
      }
    }
    return incompletedParts.build();
  }

  public boolean isCompleted(String objectId, String uploadId) throws IOException {
    UploadSpecification spec = loadUploadSpecification(objectId, uploadId);
    for (Part part : spec.getParts()) {
      if (!isUploadPartCompleted(objectId, uploadId, part.getPartNumber())) {
        return false;
      }
    }
    return true;
  }

  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String eTag)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    try {
      s3Client.putObject(bucketName, getUploadStateKey(objectId, uploadId, PART),
          new ByteArrayInputStream(mapper.writeValueAsBytes(new CompletedPart(partNumber, md5, eTag))), null);
    } catch (IOException e) {
      log.error("Fail to create upload temporary directory", e);
      throw e;
    }

  }

  @SneakyThrows
  public List<PartETag> getUploadStatePartEtags(String objectId, String uploadId) {
    ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withPrefix(getUploadStateKey(objectId, uploadId, PART));
    ObjectMapper mapper = new ObjectMapper();
    ObjectListing objectListing;
    Builder<PartETag> etags = ImmutableList.builder();
    do {
      objectListing = s3Client.listObjects(listObjectsRequest);
      for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
        S3Object obj = s3Client.getObject(objectSummary.getBucketName(), objectSummary.getKey());
        CompletedPart part = mapper.readValue(obj.getObjectContent(), CompletedPart.class);
        etags.add(new PartETag(part.getPartNumber(), part.getEtag()));
      }
      listObjectsRequest.setMarker(objectListing.getNextMarker());
    } while (objectListing.isTruncated());

    return etags.build();
  }

  public void delete(String objectId, String uploadId) {
    ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withPrefix(getUploadStateKey(objectId, uploadId, PART));
    ObjectListing objectListing;
    do {
      objectListing = s3Client.listObjects(listObjectsRequest);
      for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
        s3Client.deleteObject(objectSummary.getBucketName(), objectSummary.getKey());
      }
      listObjectsRequest.setMarker(objectListing.getNextMarker());
    } while (objectListing.isTruncated());
  }
}
