/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Node;


/**
 * Ant task for setting a value in an XML file using XPath syntax.
 *
 *  Examples:
 *  <pre>
 *  &lt;xpoke file="./build.xml" exp="/build/&#64;id" value="${build.id}"&gt;
 *  </pre>
 *  Updates the id attribute of the build element with the value of the build.id
 *  property. The XPath expression must match only a single node.  NOTE: FORMATTING
 *  MAY BE LOST WHEN THE NEW FILE IS SAVED.
 *
 *  <pre>
 *  &lt;xpath url="http://somehost/something.xml" exp="/build/&#64;id" value="${build.id}"&gt;
 *  </pre>
 *  Updates the id attribute of the build element with the value of the build.id
 *  property.  The XPath expression must match only a single node.  The host location
 *  of the XML URL must allow HTTP PUTs.  NOTE: FORMATTING MAY BE LOST WHEN THE NEW
 *  FILE IS UPLOADED.
 */
public class XPoke extends Task
{
  private File file;
  public File getFile() { return file; }
  public void setFile(File f) { file = f; }

  private URL url;
  public URL getUrl() { return url; }
  public void setUrl(URL u) { url = u; }

  private String exp;
  public String getExp() { return exp; }
  public void setExp(String s) { exp = s; }

  private String value;
  public String getValue() { return value; }
  public void setValue(String s) { value = s; }


  /**
   *  Run the task.
   *
   *  @exception  BuildException (if file, exp or prop is not set)
   */
  public void execute() throws BuildException
  {
    if (file == null && url == null || exp == null || value == null)
      throw new BuildException("Null file (or url), exp or value.");
    if (file != null && url != null)
      throw new BuildException("Don't specify both file and url.");

    try
    {
      // uses XmlBeans instead of straight Xalan (as in XPath task)
      XmlObject xo = null;
      if (getFile() == null)
      {
        log("parsing: " + getUrl());
        xo = XmlObject.Factory.parse(getUrl());
      }
      else
      {
        log("parsing: " + getFile());
        xo = XmlObject.Factory.parse(getFile());
      }

      XmlObject[] matches = xo.selectPath(getExp());

      if (matches.length == 0)
        throw new BuildException("No matches found for XPath expression.");
      if (matches.length > 1)
        throw new BuildException("Multiple matches found for XPath expression.");

      log("substituting...");
      XmlCursor xc = matches[0].newCursor();
      xc.setTextValue(getValue());

      if (getFile() == null)
        putXml(xo);
      else
        writeXml(xo);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      throw new BuildException(ex);
    }
  }


  private void putXml(XmlObject xo) throws IOException
  {
    log("uploading...");
    HttpURLConnection conn = (HttpURLConnection) getUrl().openConnection();
    conn.setRequestProperty("Content-Type", "application/octet-stream");
    conn.setRequestMethod("PUT");
    conn.setDoOutput(true);
    xo.save(conn.getOutputStream(), new XmlOptions().setSavePrettyPrint());
    conn.getOutputStream().close();
    conn.disconnect();

    int code = conn.getResponseCode();
    if (code < 200 || code >= 300)
    {
      String response = conn.getResponseMessage();
      throw new BuildException("Error uploading file: " + code + " -- " + response);
    }
  }

  private void writeXml(XmlObject xo) throws IOException
  {
    log("saving...");
    xo.save(getFile(), new XmlOptions().setSavePrettyPrint());
  }

  /**
   *  Is this a text node?
   */
  static boolean isTextNode(Node node)
  {
    if (node == null)
      return false;
    short nodeType = node.getNodeType();
    return nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.TEXT_NODE;
  }
}
