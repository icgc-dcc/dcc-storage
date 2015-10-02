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
package org.icgc.dcc.storage.client.command;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.icgc.dcc.storage.client.fs.StorageFileSystemProvider;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameters;
import com.google.common.base.Throwables;

import co.paralleluniverse.javafs.JavaFS;
import lombok.SneakyThrows;
import lombok.val;

@Component
@Parameters(separators = "=", commandDescription = "Mounts file system")
public class MountCommand extends AbstractClientCommand {

  @Override
  @SneakyThrows
  public int execute() {
    val fileSystem = createFileSystem();
    val mountPoint = Paths.get("/tmp/mnt");
    checkState(Files.exists(mountPoint), "Mount point %s does not exist. Exiting...", mountPoint);

    println("\rMounting file system to '%s'...", mountPoint);
    try {
      mount(fileSystem, mountPoint);
    } catch (Throwable t) {
      t.printStackTrace();
      Throwables.propagate(t);
    }

    return SUCCESS_STATUS;
  }

  @SneakyThrows
  private FileSystem createFileSystem() {
    return new StorageFileSystemProvider().newFileSystem(new URI("icgc://storage"), emptyMap());
  }

  private void mount(FileSystem fileSystem, Path mountPoint) throws IOException, InterruptedException {
    // To force unmount: diskutil unmount
    val readOnly = false;
    val logging = true;
    JavaFS.mount(fileSystem, mountPoint, readOnly, logging);
    Thread.sleep(Long.MAX_VALUE);
  }

}
