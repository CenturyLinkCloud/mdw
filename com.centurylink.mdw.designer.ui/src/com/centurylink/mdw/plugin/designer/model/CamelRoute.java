/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class CamelRoute extends WorkflowAsset
{
  public CamelRoute()
  {
    super();
  }

  public CamelRoute(RuleSetVO ruleSetVO, WorkflowPackage packageVersion)
  {
    super(ruleSetVO, packageVersion);
  }

  public CamelRoute(CamelRoute cloneFrom)
  {
    super(cloneFrom);
  }

  @Override
  public String getTitle()
  {
    return "Camel Route";
  }

  public boolean isSpringCamelRoute()
  {
    return RuleSetVO.CAMEL_ROUTE.equals(getRuleSetVO().getLanguage());
  }

  @Override
  public String getIcon()
  {
    if (RuleSetVO.SPRING.equals(getLanguage()))
      return "spring.gif";
    else
      return "camel.gif";
  }

  public void setLanguageFriendly(String friendly)
  {
    if (FRIENDLY_SPRING.equals(friendly))
      setLanguage(RuleSetVO.SPRING);
    else if (FRIENDLY_CAMEL.equals(friendly))
      setLanguage(RuleSetVO.CAMEL_ROUTE);
    else
      super.setLanguageFriendly(friendly);
  }

  @Override
  public String getDefaultExtension()
  {
    return ".camel";
  }

  private static final String FRIENDLY_CAMEL = "Standalone Route";
  private static final String FRIENDLY_SPRING = "Full Spring Config";

  private static List<String> languages;
  @Override
  public List<String> getLanguages()
  {
    if (languages == null)
    {
      languages = new ArrayList<String>();
      languages.add(FRIENDLY_CAMEL);
      languages.add(FRIENDLY_SPRING);
    }
    return languages;
  }
}