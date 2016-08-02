/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.FrameworkUpdateDialog;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class Script extends WorkflowAsset
{
  public Script()
  {
    super();
  }

  public Script(RuleSetVO ruleSetVO, WorkflowPackage packageVersion)
  {
    super(ruleSetVO, packageVersion);
  }

  public Script(Script cloneFrom)
  {
    super(cloneFrom);
  }

  @Override
  public String getTitle()
  {
    return "Script";
  }

  @Override
  public String getIcon()
  {
    if (isGroovy())
      return "groovy.gif";
    else if (isJavaScript())
      return "javascript.gif";
    else
      return "script.gif";
  }

  @Override
  public String getDefaultExtension()
  {
    return ".script";
  }

  @Override
  public IFolder getTempFolder()
  {
    if (isGroovy())
    {
      IFolder tempFolder = getProject().getSourceProject().getFolder(MdwPlugin.getSettings().getTempResourceLocation());
      if (!isInDefaultPackage())
      {
        StringTokenizer st = new StringTokenizer(getGroovyPackageName(), ".");
        while (st.hasMoreTokens())
          tempFolder = tempFolder.getFolder(st.nextToken());
      }

      return tempFolder;
    }
    else
    {
      return super.getTempFolder();
    }
  }

  @Override
  public String getTempFileName()
  {
    if (isGroovy())
      return getGroovyClassName() + getExtension();
    else
      return super.getTempFileName();
  }

  @Override
  public String getDefaultContent()
  {
    if (getPackage().isDefaultPackage() || !isGroovy())
      return " ";
    else
      return "package " + getGroovyPackageName() + ";\n";
  }

  @Override
  public String validate()
  {
    if (isGroovy() && !isInDefaultPackage() && !"true".equals(System.getProperty("mdw.allow.nonstandard.naming")))
    {
      String goodPkgName = getGroovyPackageName();
      if (!goodPkgName.equals(getPackage().getName()))
        return "Packages with Groovy scripts must comply with Java package naming restrictions.";
    }
    return super.validate();
  }

  @Override
  public void beforeFileOpened()
  {
    if (isGroovy())
    {
      ProjectConfigurator projConf = new ProjectConfigurator(getProject(), MdwPlugin.getSettings());
      projConf.setGroovy(new NullProgressMonitor());
      try
      {
        if (getProject().isRemote() && projConf.isJavaCapable() && !projConf.hasFrameworkJars())
        {
          FrameworkUpdateDialog updateDlg = new FrameworkUpdateDialog(MdwPlugin.getShell(), MdwPlugin.getSettings(), getProject());
          if (updateDlg.open() == Dialog.OK)
          {
            String origVer = getProject().getMdwVersion();  // as reported by server or db
            getProject().setMdwVersion(updateDlg.getMdwVersion());  // for downloading
            projConf.initializeFrameworkJars();
            getProject().setMdwVersion(origVer);
          }
        }
      }
      catch (CoreException ex)
      {
        PluginMessages.uiError(ex, "Framework Jars", getProject());
      }
    }
  }

  public boolean isGroovy()
  {
    return RuleSetVO.GROOVY.equalsIgnoreCase(getLanguage());
  }

  public boolean isJavaScript()
  {
    return RuleSetVO.JAVASCRIPT.equalsIgnoreCase(getLanguage());
  }

  private static List<String> scriptLanguages;
  @Override
  public List<String> getLanguages()
  {
    if (scriptLanguages == null)
    {
      scriptLanguages = new ArrayList<String>();
      scriptLanguages.add("Groovy");
      scriptLanguages.add("JavaScript");
      scriptLanguages.add("MagicBox");
    }
    return scriptLanguages;
  }

  protected String getGroovyPackageName()
  {
    return JavaNaming.getValidPackageName(getPackage().getName());
  }

  protected String getGroovyClassName()
  {
    return JavaNaming.getValidClassName(getName());
  }

}