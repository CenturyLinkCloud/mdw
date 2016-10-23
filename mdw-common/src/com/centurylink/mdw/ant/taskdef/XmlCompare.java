/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.File;

import junit.framework.AssertionFailedError;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.xml.XmlBeanAssert;

/**
 * Ant task for comparing two XML documents.  Ignores whitespace as well as
 * ordering of element attributes in determining equivalence.
 *
 *  Examples:
 *  <pre>
 *  &lt;xmlcompare file1="./file_one.xml" file2="./file_two.xml" &gt;
 *  </pre> Performs the comparison on file_one.xml versus file_two.xml.  Fails
 *  the build with a message identifying the discrepancy if the two documents
 *  are not equivalent.
 */
public class XmlCompare extends Task
{
  private File file1;
  public File getFile1() { return file1; }
  public void setFile1(File f) { file1 = f; }

  private File file2;
  public File getFile2() { return file2; }
  public void setFile2(File f) { file2 = f; }

  /**
   *  Run the task.
   *
   *  @exception  BuildException (if properties are not set)
   */
  public void execute() throws BuildException
  {
    if (file1 == null || file2 == null)
      throw new BuildException("Null file1 or file2 property.");

    // parse xml beans out of the two files
    try
    {
      XmlObject xmlBean1 = XmlObject.Factory.parse(file1);
      XmlObject xmlBean2 = XmlObject.Factory.parse(file2);

      XmlBeanAssert.assertEquals(xmlBean1, xmlBean2);
    }
    catch (Exception ex)
    {
      throw new BuildException(ex);
    }
    catch (AssertionFailedError er)
    {
      throw new BuildException(er.getMessage());
    }
  }
}
