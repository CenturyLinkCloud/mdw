/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class JarFile extends WorkflowAsset
{
  public JarFile()
  {
    super();
  }

  public JarFile(RuleSetVO ruleSetVO, WorkflowPackage packageVersion)
  {
    super(ruleSetVO, packageVersion);
  }

  public JarFile(JarFile cloneFrom)
  {
    super(cloneFrom);
  }

  @Override
  public String getTitle()
  {
    return "JAR File";
  }

  @Override
  public String getIcon()
  {
    return "jar.gif";
  }

  @Override
  public String getDefaultExtension()
  {
    return ".jar";
  }

  private static List<String> jarFileLanguages;
  @Override
  public List<String> getLanguages()
  {
    if (jarFileLanguages == null)
    {
      jarFileLanguages = new ArrayList<String>();
      jarFileLanguages.add("JAR");
    }
    return jarFileLanguages;
  }
}