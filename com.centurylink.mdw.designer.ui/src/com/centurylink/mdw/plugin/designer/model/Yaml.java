/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class Yaml extends WorkflowAsset
{
  public Yaml()
  {
    super();
  }

  public Yaml(RuleSetVO ruleSetVO, WorkflowPackage packageVersion)
  {
    super(ruleSetVO, packageVersion);
  }

  public Yaml(Yaml cloneFrom)
  {
    super(cloneFrom);
  }

  @Override
  public String getTitle()
  {
    return "YAML";
  }

  @Override
  public String getDefaultExtension()
  {
    return RuleSetVO.getFileExtension(RuleSetVO.YAML);
  }

  @Override
  public String getIcon()
  {
    return "yaml.gif";
  }



  private static List<String> languages;
  @Override
  public List<String> getLanguages()
  {
    if (languages == null)
    {
      languages = new ArrayList<String>();
      languages.add(RuleSetVO.YAML);
    }
    return languages;
  }
}
