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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *  Connects via HTTP to a URL and downloads the specified xsd.
 *
 *  Examples: <pre>
 *  &lt;xsddownload url="http://www.qwest.com/myschema.xsd" dir="./"&gt;
 *  </pre> Downloads myschema.xsd from qwest.com and places it into
 *  the current directory.
 */
public class XsdDownload extends HttpDownload
{
  private File dir;
  public File getDir() { return dir; }

  private boolean dependencies = false;
  public boolean isDependencies() { return dependencies; }

  /**
   * The local directory to write the downloaded xsd file(s) to.
   *
   * @param  dir
   */
  public void setDir(File dir)
  {
    this.dir = dir;
  }

  /**
   * Whether to recursively download included and imported xsds.
   *
   * @param b true if dependencies should be downloaded
   */
  public void setDependencies(boolean b)
  {
    this.dependencies = b;
  }

  /**
   *  Run the task.
   *
   *  @exception  BuildException (if url or file is not set)
   */
  public void execute() throws BuildException
  {
    if (getUrl() == null || getDir() == null)
      throw new BuildException("Null URL or Dir.");

    if (!getDir().isDirectory())
      throw new BuildException("Dir: " + getDir() + " does not exist.");

    File file = new File(getDir().getPath() + getFileName(getUrl().getPath()));

    if (!isDependencies())
    {
      download(getUrl(), file);
      return;
    }

    // recursively search XSDs for dependencies
    recurse(getFileName(getUrl().getPath()),
                        getBaseUrlPath(getUrl()), getDir().getPath());
  }

  private void recurse(String relativePathToFile,
                       String baseUrlPath, String baseFilePath)
  {
    // download the file
    URL url = absoluteUrl(relativePathToFile, baseUrlPath);
    File file = absoluteFile(relativePathToFile, baseFilePath);
    File dir = new File(file.getParent());

    if (!dir.exists())
    {
      dir.mkdir();
    }

    download(url, file);

    try
    {
      // parse the file looking for imports and includes
      InputStream inStream = url.openStream();
      InputSource src = new InputSource(inStream);
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setNamespaceAware(true);
      SAXParser parser = factory.newSAXParser();
      SchemaParser sp = new SchemaParser(relativePathToFile);
      parser.parse(src, sp);
      inStream.close();
      for (int i = 0; i < sp.getIncludesAndImports().size(); i++)
      {
        String impOrIncl = sp.getIncludesAndImports().get(i);
        String newBaseUrlPath = getBaseUrlPath(url);
        String newBaseFilePath = getBaseFilePath(file);
        recurse(impOrIncl, newBaseUrlPath, newBaseFilePath);
      }
    }
    catch (Exception e)
    {
      throw new BuildException(e);
    }
  }



  private class SchemaParser extends DefaultHandler
  {
    private String relativePathToFile;
    private int elementDepth = 0;
    private List<String> includesAndImports = new ArrayList<String>();
    public List<String> getIncludesAndImports() { return includesAndImports; }

    public SchemaParser(String relativePathToFile)
    {
      this.relativePathToFile = relativePathToFile;
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes attrs)
      throws SAXException
    {
      if (elementDepth == 0) // the root 'schema' element
      {
        if (!localName.equals("schema"))
        {
          throw new SAXException("Can't parse schema at: " + getUrl());
        }
      }
      else if (elementDepth == 1
               && (localName.equals("include") || localName.equals("import")))
      {
        String schemaLoc = attrs.getValue("schemaLocation");
        if (schemaLoc == null || schemaLoc.indexOf(':') >= 0)
        {
          throw new BuildException(relativePathToFile
              + ":\nschemaLocation of dependent schema must be relative");
        }
        else
        {
          includesAndImports.add(attrs.getValue("schemaLocation"));
        }
      }

      elementDepth++;
    }

    public void endElement(String uri, String localName, String qName)
    {
      elementDepth--;
    }

  }

  private String getFileName(String path)
  {
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private URL absoluteUrl(String relPathToFile, String basePath)
  {
    try
    {
      return new URL(basePath + "/" + relPathToFile);
    }
    catch (MalformedURLException e)
    {
      throw new BuildException(e);
    }
  }

  private File absoluteFile(String relPathToFile, String basePath)
  {
    return new File(basePath + File.separator + relPathToFile);
  }

  private String getBaseUrlPath(URL url)
  {
    return url.toString().substring(0, url.toString().lastIndexOf('/'));
  }

  private String getBaseFilePath(File file)
  {
    return file.getPath()
       .substring(0, file.getPath().lastIndexOf(File.separator));
  }

}
