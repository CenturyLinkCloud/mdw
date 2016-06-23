/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.web.util.RemoteLocator;

public class EventSources implements Lister{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  private static String[] mEventSources;


  /**
   * Retrieve the event sources.
   */
  public static void refresh()
  {
    try
    {
      EventManager eventMgr = RemoteLocator.getEventManager();
      mEventSources = eventMgr.getDistinctEventLogEventSources();
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }
  /**
   * Get a list of SelectItems populated from an array of event sources.
   *
   * @param eventSources the event sources to include in the list
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public static List<SelectItem> getEventSourceSelectItems(String[] eventSources, String firstItem)
  {

    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (eventSources.length > 0)
    {
      if (firstItem != null)
      {
        selectItems.add(new SelectItem(firstItem));
      }
      for (int i = 0; i < eventSources.length; i++)
      {
        selectItems.add(new SelectItem(eventSources[i]));
      }
    }

    return selectItems;
  }

  public List<SelectItem> list()
  {
    if (mEventSources == null || mEventSources.length == 0)
    {
      refresh();
    }
    return getEventSourceSelectItems(mEventSources, "");
  }
}
