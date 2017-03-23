/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.File;
import java.net.URL;

import org.apache.tools.ant.BuildException;

/**
 *  Connects via HTTP to a URL and uploads the specified file.
 *
 *  Examples: <pre>
 *  &lt;httpupload file="./index.html" url="http://www.centurylink.com/index.html"/&gt;
 *  </pre> Uploads index.html from the current directory to centurylink.com.
 */
public class HttpUpload extends HttpTransfer
{
  private File file = null;
  private URL url = null;
  private boolean overwrite = true;
  private String user;
  private String password;

  /**
   *  The local filename to write the downloaded file to.
   *
   *  @param  file
   */
  public void setFile(File file)
  {
    this.file = file;
  }

  /**
   *  The URL to download from.
   *
   *  @param  url
   */
  public void setUrl(URL url)
  {
    this.url = url;
  }

  public void setUser(String user)
  {
    this.user = user;
  }

  public void setPassword(String password)
  {
    this.password = password;
  }

  /**
   *  Overwrite existing files even if the destination files are newer.
   *  Defaults to false.
   *
   *  @param  overwrite
   */
  public void setOverwrite(boolean overwrite)
  {
    this.overwrite = overwrite;
  }

  /**
   *  Run the task.
   *
   *  @exception  BuildException (if url or file is not set)
   */
  public void execute() throws BuildException
  {
    if (url == null || file == null)
      throw new BuildException("Null URL or File.");
    try {
              uploadFile(url,file,user,password,overwrite);
    } catch (Exception ex){
        log("Exception: " + ex);
        ex.printStackTrace();
        if (isFailOnError())
            throw new BuildException("Exception: " + ex);
    }

  }
}
