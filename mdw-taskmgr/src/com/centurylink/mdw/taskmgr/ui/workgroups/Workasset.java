/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.workgroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import org.apache.commons.lang.StringUtils;

import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * UI List class for workgroups admin. Also maintains a static list of Assets reference data loaded
 * from the workflow.
 */
public class Workasset extends SortableList
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private static List<RuleSetVO> _assets;

  /**
   * Needs a no-arg constructor since Users is a dropdown lister in addition to being a SortableList
   * implementation.
   */
  public Workasset() throws UIException
  {
    super(ViewUI.getInstance().getListUI("workassetsList"));
  }

  public static synchronized List<RuleSetVO> getAllAssets()
  {

    if (_assets == null)
      load();
    return _assets;
  }

  public Workasset(ListUI listUI)
  {
    super(listUI);
  }

  private static synchronized void load()
  {
    try
    {
      _assets = new ArrayList<RuleSetVO>();
      for (RuleSetVO rs : RuleSetCache.getAllRuleSets())
      {
        if (StringUtils.isNotBlank(rs.getPackageName()))
          _assets.add(rs);
      }
      Collections.sort(_assets);
      syncList("workassetsList");

    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public static void remove(RuleSetVO ruleSet)
  {
    _assets.remove(ruleSet);
    syncList("workassetsList");
  }

  public DataModel<ListItem> retrieveItems()
  {

    try
    {
      return new ListDataModel<ListItem>(convertRuleSetVOs(getAllAssets()));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }

  }

  protected List<ListItem> convertRuleSetVOs(List<RuleSetVO> assets)
  {
    List<ListItem> rowList = new ArrayList<ListItem>();
    for (RuleSetVO asset : assets)
    {
      AssetsItem item = new AssetsItem(asset);
      rowList.add(item);
    }
    return rowList;
  }

  public static RuleSetVO getAsset(Long id)
  {
    for (RuleSetVO group : getAllAssets())
    {
      if (group.getId().equals(id))
        return group;
    }
    return null;
  }


  @Override
  public boolean isSortable()
  {
    return _assets != null && _assets.size() > 0;
  }

}
