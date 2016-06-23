/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Ant task for performing XPath queries.
 *
 *  Examples:
 *  <pre>
 *  &lt;xpath file="./plugin.xml" exp="/plugin/&#64;version" prop="version"&gt;
 *  </pre> Extracts the value of the version attribute of plugin element of
 *  plugin.xml and assigns the value to the version property. If multiple xpath
 *  matches exist, the results are delimited by commas.
 *
 *  <pre>
 *  &lt;xpath url="http://somehost/something.xml" exp="/plugin/&#64;version" prop="version"&gt;
 *  </pre> Extracts the value of the version attribute of plugin element of
 *  plugin.xml and assigns the value to the version property. If multiple xpath
 *  matches exist, the results are delimited by commas.
 */
public class XPath extends Task
{
  private File file = null;
  private URL url = null;
  private String exp = null;
  private String prop = null;
  private Path classpath = null;

  /**
   *  The filename of the xml document to run the xpath expression against.
   *
   *  @param  file
   */
  public void setFile(File file)
  {
    this.file = file;
  }

  /**
   * A url pointing to the xml document to run the xpath expression against.
   * @param url
   */
  public void setUrl(URL url)
  {
    this.url = url;
  }

  /**
   *  The xpath expression to apply against the xml document.
   *
   *  @param  exp
   */
  public void setExp(String exp)
  {
    this.exp = exp;
  }

  /**
   *  The property to assign the result to.
   *
   *  @param  prop
   */
  public void setProp(String prop)
  {
    this.prop = prop;
  }

  /**
   *  Run the task.
   *
   *  @exception  BuildException (if file, exp or prop is not set)
   */
  public void execute() throws BuildException
  {
    if (file == null && url == null || exp == null || prop == null)
      throw new BuildException("Null file (or url), exp or prop.");

    try
    {
      InputSource in = null;
      if (file != null)
        in = new InputSource(new FileInputStream(file));
      else
        in = new InputSource(url.openStream());

      // set up a DOM tree to query
      setDocumentBuilderFactory();
      DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
      log("DocumentBuilderFactory: " + domFactory.getClass().getName(), Project.MSG_INFO);
      domFactory.setNamespaceAware(true);
      Document doc = domFactory.newDocumentBuilder().parse(in);

      // set up an identity transformer to use as serializer
      TransformerFactory txFactory = TransformerFactory.newInstance();
      Transformer serializer = txFactory.newTransformer();
      serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

      // use the simple XPath API to select a nodeList
      Class<?> clazz = loadClass("org.apache.xpath.XPathAPI");
      Method method = clazz.getMethod("selectNodeList", new Class<?>[]{org.w3c.dom.Node.class, java.lang.String.class});
      NodeList nodeList = (NodeList) method.invoke(null, new Object[]{doc, exp});
      //NodeList nodeList = XPathAPI.selectNodeList(doc, exp);

      String result = "";
      for (int i = 0; i < nodeList.getLength(); i++)
      {
        Node node = nodeList.item(i);
        if (isTextNode(node))
        {
          // dom may have more than one node corresponding to a single xpath text
          // node -- coalesce all contiguous text nodes at this level
          result = node.getNodeValue();
          for (Node next = node.getNextSibling(); isTextNode(next);
               next = next.getNextSibling())
          {
            result += next.getNodeValue();
          }
        }
        else
        {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          serializer.transform(new DOMSource(node), new StreamResult(baos));
          result += new String(baos.toByteArray());
        }
        if (i > 0)
          result += ",";
      }
      getProject().setProperty(prop, result.substring(0, result.length()));
    }
    catch(Exception e)
    {
      e.printStackTrace();
      throw new BuildException("Can't process XPath.", e);
    }
  }

  /**
   * Explicitly set the DOM document builder factory so that the WebLogic
   * non-DOM Level 2 parser won't be used (can happen when running BEA's
   * version of Ant).
   */
  void setDocumentBuilderFactory()
  {
    if (System.getProperty("java.version").startsWith("1.4"))
    {
      System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
          "org.apache.crimson.jaxp.DocumentBuilderFactoryImpl");
    }
    else
    {
      System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
          "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }
  }

  /**
   * Load named class either via the system classloader or the custom classloader
   *
   * @param classname the name of the class to load.
   * @return the requested class.
   * @exception Exception
   *              if the class could not be loaded.
   */
  @SuppressWarnings("deprecation")
  private Class<?> loadClass(String classname) throws Exception
  {
    if (classpath == null)
    {
      return Class.forName(classname);
    }
    else
    {
      AntClassLoader al = new AntClassLoader(getProject(), classpath);
      Class<?> c = al.findClass(classname);
      AntClassLoader.initializeClass(c);
      return c;
    }
  }

  /**
   * Set the optional classpath for the Xalan XPathAPI
   *
   * @param classpath
   *          the classpath to use when loading the XSL processor
   */
  public void setClasspath(Path classpath)
  {
    createClasspath().append(classpath);
  }

  /**
   * Create optional classpath for the Xalan XPathAPI
   *
   * @return a path instance to be configured by the Ant core.
   */
  public Path createClasspath()
  {
    if (classpath == null)
    {
      classpath = new Path(getProject());
    }
    return classpath.createPath();
  }


  /**
   *  Is this a text node?
   */
  static boolean isTextNode(Node node)
  {
    if (node == null)
      return false;
    short nodeType = node.getNodeType();
    return nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.TEXT_NODE || nodeType == Node.ATTRIBUTE_NODE;
  }
}
