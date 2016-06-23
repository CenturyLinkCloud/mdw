/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

/**
 * Ant task for adding or removing the BIRT options in a faces-config deployment descriptor.
 *
 *  Examples:
 *  <pre>
 *  &lt;birtFaces file="web/WEB-INF/faces-config.xml" operation="remove"&gt;
 *  </pre>
 *  Strips the BIRT elements from the filefaces-config.xml.
 */
public class BirtFaces extends Task
{
  public static final String NAMESPACE = "http://java.sun.com/xml/ns/javaee";
  public static final String REPORT_COMPONENT = "com.centurylink.mdw.web.jsf.components.ReportComponent";
  public static final String REPORT_RENDERER = "com.centurylink.mdw.web.jsf.renderers.BirtReportRenderer";

  private File file;
  public File getFile() { return file; }
  public void setFile(File f) { file = f; }

  private String phaseListener;
  public String getPhaseListener() { return phaseListener; }
  public void setPhaseListener(String s) { phaseListener = s; }

  private String operation;
  public String getOperation() { return operation; }
  public void setOperation(String s) { operation = s; }

  private boolean includeComponent;
  public boolean isIncludeComponent() { return includeComponent; }
  public void setIncludeComponent(boolean b) { includeComponent = b; }

  /**
   *  Run the task.
   *
   *  @exception  BuildException (if file or op is not set)
   */
  public void execute() throws BuildException
  {
    if (file == null || operation == null || phaseListener == null)
      throw new BuildException("Missing file, operation or phaseListener.");
    if (!operation.equalsIgnoreCase("add") && !operation.equalsIgnoreCase("remove"))
      throw new BuildException("Supported operations: 'add' and 'remove'.");

    try
    {
      FileInputStream fis = null;
      String xmlIn = null;
      try
      {
        fis = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read = 0;
        while ((read = fis.read(buf)) != -1)
          baos.write(buf, 0, read);
        xmlIn = baos.toString();
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
        throw new BuildException(ex.getMessage(), ex);
      }
      finally
      {
        if (fis != null)
          fis.close();
      }

      FileOutputStream fos = null;
      try
      {
        String xmlOut;
        if (operation.equalsIgnoreCase("add"))
          xmlOut = add(xmlIn);
        else
          xmlOut = remove(xmlIn);
        fos = new FileOutputStream(file);
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlOut.getBytes());
        byte[] buf = new byte[1024];
        int read = 0;
        while ((read = bais.read(buf)) != -1)
          fos.write(buf, 0, read);
        log("BIRT elements " + (operation.equalsIgnoreCase("add") ? "added to " : "removed from ") + file);
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
        throw new BuildException(ex.getMessage(), ex);
      }
      finally
      {
        if (fos != null)
          fos.close();
      }
    }
    catch (IOException ex)
    {
      throw new BuildException(ex.getMessage(), ex);
    }
  }

  public String add(String xml) throws IOException
  {
    XmlCursor xmlCursor = null;
    xml = remove(xml);  // in case already present
    try
    {
      XmlObject xmlBean = XmlObject.Factory.parse(xml);
      xmlCursor = xmlBean.newCursor();
      xmlCursor.toStartDoc();
      xmlCursor.toFirstChild();  // document

      // add the reports phase listener
      xmlCursor.toChild(NAMESPACE, "lifecycle");
      xmlCursor.toLastChild();
      xmlCursor.toEndToken();
      xmlCursor.toNextToken();
      xmlCursor.insertChars("\n    ");
      xmlCursor.insertElementWithText(new QName(NAMESPACE, "phase-listener"), phaseListener);
      xmlCursor.toStartDoc();
      xmlCursor.toFirstChild();  // document

      if (includeComponent)
      {
        if (xmlCursor.toChild(NAMESPACE, "render-kit"))
        {
          if (xmlCursor.toNextSibling(NAMESPACE, "render-kit"))
          {
            // separate render-kit
            xmlCursor.toParent();
            xmlCursor.toChild(NAMESPACE, "managed-bean");
            xmlCursor.toPrevSibling();
            xmlCursor.toEndToken();
            xmlCursor.toNextToken();
            xmlCursor.insertChars("\n\n  ");
            xmlCursor.insertComment("report component");

            insertReportComponent(xmlCursor);

            xmlCursor.insertElement(new QName(NAMESPACE, "render-kit"));
            xmlCursor.toPrevToken();

            insertReportRenderer(xmlCursor);
          }
          else
          {
            // combined render-kit
            xmlCursor.toPrevSibling();
            xmlCursor.toEndToken();
            xmlCursor.toNextToken();

            insertReportComponent(xmlCursor);

            xmlCursor.toParent();
            xmlCursor.toChild(NAMESPACE, "render-kit");
            xmlCursor.toLastChild();
            xmlCursor.toEndToken();
            xmlCursor.toNextToken();

            insertReportRenderer(xmlCursor);
          }
        }
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      xmlBean.save(baos);
      return baos.toString();
    }
    catch (XmlException ex)
    {
      throw new IOException("Update failed for " + file.getAbsolutePath(), ex);
    }
    finally
    {
      if (xmlCursor != null)
        xmlCursor.dispose();
    }
  }

  public String remove(String xml) throws IOException
  {
    XmlCursor xmlCursor = null;
    try
    {
      XmlObject xmlBean = XmlObject.Factory.parse(xml);
      xmlCursor = xmlBean.newCursor();
      xmlCursor.toStartDoc();
      xmlCursor.toFirstChild();  // document

      // remove the reports phase listener
      xmlCursor.toChild(NAMESPACE, "lifecycle");
      if (xmlCursor.toChild(NAMESPACE, "phase-listener"))
      {
        do
        {
          if (phaseListener.equals(xmlCursor.getTextValue()))
          {
            xmlCursor.removeXml();
            if (xmlCursor.isText())
              xmlCursor.removeXml();  // remove newline
            fixIndent(xmlCursor);
          }
        }
        while (xmlCursor.toNextSibling(NAMESPACE, "phase-listener"));
      }
      xmlCursor.toStartDoc();
      xmlCursor.toFirstChild();

      if (includeComponent)
      {
        // report component
        if (xmlCursor.toChild(NAMESPACE, "component"))
        {
          do
          {
            if (xmlCursor.toChild(NAMESPACE, "component-type"))
            {
              if (REPORT_COMPONENT.equals(xmlCursor.getTextValue()))
              {
                xmlCursor.toParent();
                xmlCursor.removeXml();
                if (xmlCursor.isText())
                  xmlCursor.removeXml();  // remove whitespace
              }
              else
              {
                xmlCursor.toParent();
              }
            }
          }
          while (xmlCursor.toNextSibling(NAMESPACE, "component"));
        }
        xmlCursor.toStartDoc();
        xmlCursor.toFirstChild();

        // report renderer (separate render-kit)
        if (xmlCursor.toChild(NAMESPACE, "render-kit"))
        {
          do
          {
            if (xmlCursor.toChild(NAMESPACE, "renderer"))
            {
              if (xmlCursor.toChild(NAMESPACE, "renderer-type"))
              {
                if (REPORT_RENDERER.equals(xmlCursor.getTextValue()))
                {
                  xmlCursor.toParent();
                  xmlCursor.toParent();
                  xmlCursor.removeXml();
                  xmlCursor.toPrevToken();
                  if (xmlCursor.isText())
                    xmlCursor.removeXml();  // remove whitespace
                  if (xmlCursor.isComment() && xmlCursor.getTextValue().trim().equals("report component"))
                    xmlCursor.removeXml();  // remove comment
                  if (xmlCursor.isText())
                    xmlCursor.removeXml();
                }
                else
                {
                  xmlCursor.toParent();
                }
                xmlCursor.toParent();
              }
            }
          }
          while (xmlCursor.toNextSibling(NAMESPACE, "render-kit"));
        }

        // report renderer (combined render-kit)
        xmlCursor.toParent();
        if (xmlCursor.toChild(NAMESPACE, "render-kit"))
        {
          if (xmlCursor.toChild(NAMESPACE, "renderer"))
          {
            do
            {
              if (xmlCursor.toChild(NAMESPACE, "renderer-type"))
              {
                if (REPORT_RENDERER.equals(xmlCursor.getTextValue()))
                {
                  xmlCursor.toParent();
                  xmlCursor.removeXml();
                  if (xmlCursor.isText())
                    xmlCursor.removeXml();  // remove newline
                  fixIndent(xmlCursor);
                }
                else
                {
                  xmlCursor.toParent();
                }
              }
            }
            while (xmlCursor.toNextSibling(NAMESPACE, "renderer"));
          }
        }
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      xmlBean.save(baos);
      return baos.toString();
    }
    catch (XmlException ex)
    {
      throw new IOException("Update failed for " + file.getAbsolutePath(), ex);
    }
    finally
    {
      if (xmlCursor != null)
        xmlCursor.dispose();
    }
  }

  private void insertReportComponent(XmlCursor xmlCursor)
  {
    xmlCursor.insertChars("\n  ");
    xmlCursor.insertElement(new QName(NAMESPACE, "component"));
    xmlCursor.toPrevToken();
    xmlCursor.insertChars("\n    ");
    xmlCursor.insertElementWithText(new QName(NAMESPACE, "component-type"), REPORT_COMPONENT);
    xmlCursor.insertChars("\n    ");
    xmlCursor.insertElementWithText(new QName(NAMESPACE, "component-class"), REPORT_COMPONENT);
    xmlCursor.insertChars("\n  ");
    xmlCursor.toParent();
    xmlCursor.toEndToken();
    xmlCursor.toNextToken();
    xmlCursor.insertChars("\n  ");
  }

  private void insertReportRenderer(XmlCursor xmlCursor)
  {
    xmlCursor.insertChars("\n  ");
    xmlCursor.insertElement(new QName(NAMESPACE, "renderer"));
    xmlCursor.toPrevToken();
    xmlCursor.insertChars("\n      ");
    xmlCursor.insertElementWithText(new QName(NAMESPACE, "component-family"), "javax.faces.Panel");
    xmlCursor.insertChars("\n      ");
    xmlCursor.insertElementWithText(new QName(NAMESPACE, "renderer-type"), REPORT_RENDERER);
    xmlCursor.insertChars("\n      ");
    xmlCursor.insertElementWithText(new QName(NAMESPACE, "renderer-class"), REPORT_RENDERER);
    xmlCursor.insertChars("\n    ");
    xmlCursor.toParent();
    xmlCursor.toEndToken();
    xmlCursor.toNextToken();
    xmlCursor.insertChars("\n  ");
  }

  private void fixIndent(XmlCursor xmlCursor)
  {
    xmlCursor.toPrevToken();
    if (xmlCursor.isText())
    {
      // fix indent
      xmlCursor.toNextChar(2);  // newline
      xmlCursor.removeChars(2);
    }
  }
}
