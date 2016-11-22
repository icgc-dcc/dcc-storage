/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.storage.client.util;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
public class AzurePresignedUrlValidator extends PresignedUrlValidator {

  /**
   * Return expiry date of presigned URL in the local time zone (system default). Distinguished between V2 and V4 AWS
   * signatures
   * @param args - URL query arguments
   * @return expiry date translated to specified time zone
   */
  @Override
  LocalDateTime extractExpiryDate(Map<String, String> args, ZoneId effectiveTimeZone) {
    if (args.containsKey("se")) {
      val se = args.get("se");

      // This is the expected date format from Azure
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
      val zdt = ZonedDateTime.parse(se, formatter);

      return zdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    } else {
      log.error("Could not identify expected date parameters in request: {}", flattenMap(args));
      // missing expected query arguments
      throw new IllegalArgumentException(
          String.format("Could not parse presigned URL - missing expected expiry date parameters"));
    }
  }
}
