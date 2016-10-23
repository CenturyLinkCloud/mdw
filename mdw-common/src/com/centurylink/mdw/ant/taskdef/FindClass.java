/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.centurylink.mdw.util.ClasspathUtil;

/**
 *  Locates a class in the Java classpath at build time.
 *
 *  Examples: <pre>
 *  &lt;findclass classname="com.qwest.pkg.classname"&gt;
 *  </pre> Shows where on the classpath the fully-qualified class is found.
 */
public class FindClass extends Task
{
  private String mClassname;
  public String getClassname() { return mClassname; }
  public void setClassname(String s) { mClassname = s; }

  /**
   *  Run the task.
   *
   *  @exception  BuildException (if url or file is not set)
   */
  public void execute() throws BuildException
  {
    if (mClassname == null)
      throw new BuildException("Null classname.");

    getProject().log("class: " + getClassname());
    getProject().log("location: " + ClasspathUtil.locate(getClassname()), Project.MSG_ERR);
  }
}
