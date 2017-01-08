/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class Json extends WorkflowAsset
{
  public Json()
  {
    super();
  }

  public Json(RuleSetVO ruleSetVO, WorkflowPackage packageVersion)
  {
    super(ruleSetVO, packageVersion);
  }

  public Json(Json cloneFrom)
  {
    super(cloneFrom);
  }

  @Override
  public String getTitle()
  {
    return "JSON";
  }

  @Override
  public String getDefaultExtension()
  {
    return RuleSetVO.getFileExtension(RuleSetVO.JSON);
  }

  @Override
  public String getIcon()
  {
    return "javascript.gif";
  }

  private static List<String> languages;
  @Override
  public List<String> getLanguages()
  {
    if (languages == null)
    {
      languages = new ArrayList<String>();
      languages.add(RuleSetVO.JSON);
    }
    return languages;
  }
}