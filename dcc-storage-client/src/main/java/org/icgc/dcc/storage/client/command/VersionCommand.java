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

import static com.google.common.base.Objects.firstNonNull;
import static org.icgc.dcc.common.core.util.VersionUtils.getScmInfo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameters;

@Component
@Parameters(separators = "=", commandDescription = "Display application version information")
public class VersionCommand extends AbstractClientCommand {

  @Value("${storage.url}")
  private String storageUrl;

  @Override
  public int execute() throws Exception {
    printTitle();
    version();
    return SUCCESS_STATUS;
  }

  private void version() {
    terminal.println(terminal.label("  Version: ") + getVersion());
    terminal.println(terminal.label("  Built:   ") + getScmInfo().get("git.build.time"));
    terminal.println(terminal.label("  Contact: ") + terminal.email("dcc-support@icgc.org"));
    terminal.println("");
    terminal.println(terminal.label("  Active Configuration: "));
    terminal.println("    Storage Endpoint: " + storageUrl);
  }

  private String getVersion() {
    return firstNonNull(getClass().getPackage().getImplementationVersion(), "[unknown version]");
  }

}
