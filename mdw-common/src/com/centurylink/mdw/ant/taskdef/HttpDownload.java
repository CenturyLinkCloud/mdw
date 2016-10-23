/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *  Connects via HTTP to a URL and downloads the specified file.
 *
 *  Examples: <pre>
 *  &lt;httpdownload url="http://www.centurylink.com/index.html" file="./index.html"&gt;
 *  </pre> Downloads the index.html file from qwest.com and places it into
 *  the current directory.
 */
public class HttpDownload extends Task
{
  private File file = null;
  public File getFile() { return file; }

  private File dir = null;
  public File getDir() { return dir; }

  private URL url = null;
  public URL getUrl() { return url; }

  private boolean overwrite = false;
  public boolean isOverwrite() { return overwrite; }

  private boolean failOnError=true;
  public boolean shouldFailOnError() { return failOnError; }

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
   *  The local directory to write the downloaded file to.
   *
   *  @param  directory
   */
  public void setDir(File dir)
  {
    this.dir = dir;
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
   * Specifies whether or not the build should fail on an exception.
   * If the URL is unreachable the file will not
   * be downloaded, but this option tells whether the build should fail at
   * that point.
   *
   * @param failOnError
   */
  public void setFailOnError(boolean failOnError)
  {
    this.failOnError = failOnError;
  }

  /**
   *  Run the task.
   *
   *  @exception  BuildException (if url or file is not set)
   */
  public void execute() throws BuildException
  {
    if (url == null || (file == null && dir == null))
      throw new BuildException("URL either File or Dir is required.");

    if (file == null)
      download(url, new File(dir + "/" + url.getFile().substring(url.getFile().lastIndexOf("/") + 1)));
    else
      download(url, file);
  }

  protected void download(URL srcURL, File file)
  {
    try
    {
      log("Downloading ... " + srcURL.toString());
      doDownload(srcURL, file);
      log("  Downloaded ... " + file);
    }
    catch (IOException e)
    {
      String msg = "Unable to connect to URL: " + srcURL + " - Unable to download file: " + file;
      throw new BuildException(msg+e.getMessage());
    }
  }

  public void doDownload(URL url, File dest) throws IOException
  {
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    HttpURLConnection.setFollowRedirects(true);
    FileOutputStream fos = new FileOutputStream(dest);
    byte[] buffer = new byte[2048];
    InputStream is = conn.getInputStream();
    while (true)
    {
     int bytesRead = is.read(buffer);
     if (bytesRead == -1)
       break;
     fos.write(buffer, 0, bytesRead);
    }
    fos.close();
    is.close();
    conn.disconnect();
  }
}
