/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class TextResource extends WorkflowAsset
{
  public TextResource()
  {
    super();
  }

  public TextResource(RuleSetVO ruleSetVO, WorkflowPackage packageVersion)
  {
    super(ruleSetVO, packageVersion);
  }

  public TextResource(Script cloneFrom)
  {
    super(cloneFrom);
  }

  @Override
  public String getTitle()
  {
    return "Text Resource";
  }

  @Override
  public String getIcon()
  {
    return "doc.gif";
  }

  @Override
  public String getDefaultExtension()
  {
    return ".txt";
  }

  @Override
  public List<String> getLanguages()
  {
    return Arrays.asList(new String[]{"Text", "Other"});
  }

  /**
   * Allow general filename extension for
   */
  public String validate()
  {
    if (getProject().isRequireAssetExtension())
    {
      int lastDot = getName().lastIndexOf('.');
      if (lastDot == -1)
        return "Assets require a filename extension";
      if (!"OTHER".equals(getLanguage()))
      {
        String ext = RuleSetVO.getFileExtension(getLanguage());
        if (!getName().substring(lastDot).equals(ext))
          return getLanguage() + " assets must have extension " + ext;
      }
    }
    return null;
  }
}