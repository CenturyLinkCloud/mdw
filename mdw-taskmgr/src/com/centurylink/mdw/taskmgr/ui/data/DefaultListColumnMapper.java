/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.list.ListColumnMapper;

public class DefaultListColumnMapper implements ListColumnMapper
{
  private Map<String,String> columnMap;

  public DefaultListColumnMapper(String listId) throws IOException
  {
    columnMap = mapProperties(openMapFile(listId + "-columns.map"));
  }

  public String getDatabaseColumn(String listColumn)
  {
    return columnMap.get(listColumn);
  }

  private Map<String,String> mapProperties(InputStream mapFileStream) throws IOException
  {
    if (mapFileStream == null)
      return null;
    Properties props = new Properties();
    props.load(mapFileStream);
    Map<String, String> map = new HashMap<String, String>();
    for (Object key : props.keySet())
      map.put(key.toString(), props.getProperty(key.toString()));
    return map;
  }

  private InputStream openMapFile(String filename) throws IOException
  {
    RuleSetVO ruleSet = null;
    PackageVO packageVO = FacesVariableUtil.getCurrentPackage();
    String hubViewDefFromGlobalCfg = PropertyManager.getProperty(PropertyNames.MDW_HUB_VIEW_DEF);
    // in Current package
    if (packageVO != null)
      ruleSet = packageVO.getRuleSet(filename);
    if (ruleSet != null && !ruleSet.isLoaded()) {
      ruleSet = RuleSetCache.getRuleSet(ruleSet.getId());
    }
    // based on global hub.view.definition property - package
    if (ruleSet == null && hubViewDefFromGlobalCfg != null && hubViewDefFromGlobalCfg.indexOf("/") > 0)
      ruleSet = RuleSetCache.getRuleSet(hubViewDefFromGlobalCfg.substring(0, hubViewDefFromGlobalCfg.indexOf("/")) + "/" + filename);
    // user override of standard hub
    if (ruleSet == null)
    {
      String userHubPkgName = ApplicationContext.getHubOverridePackage();
      PackageVO userHubPkg = PackageVOCache.getPackage(userHubPkgName);
      if (userHubPkg != null && userHubPkg.getRuleSet(filename) != null)
        ruleSet = RuleSetCache.getRuleSet(userHubPkg.getName() + "/" + filename);
    }
    // hub package
    if (ruleSet == null)
      ruleSet = RuleSetCache.getRuleSet(PackageVO.MDW_HUB + "/" + filename);

    if (ruleSet == null)
      return FileHelper.readFile(filename, DefaultListColumnMapper.class.getClassLoader());
    else
      return new ByteArrayInputStream(ruleSet.getRuleSet().getBytes());
  }
}
