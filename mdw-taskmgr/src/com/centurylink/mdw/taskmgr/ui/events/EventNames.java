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

public class EventNames implements Lister
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  private static String[] mEventNames;



  /**
   * Retrieve the event names.
   */
  public static void refresh()
  {
    try
    {
      EventManager eventMgr = RemoteLocator.getEventManager();
      mEventNames = eventMgr.getDistinctEventLogEventNames();
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }
  /**
   * Get a list of SelectItems populated from an array of event names.
   *
   * @param eventNames the event names to include in the list
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public static List<SelectItem> getEventNameSelectItems(String[] eventNames, String firstItem) {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();

    if (eventNames.length > 0)
    {
      if (firstItem != null)
      {
        selectItems.add(new SelectItem(firstItem));
      }
      for (int i = 0; i < eventNames.length; i++)
      {
        selectItems.add(new SelectItem(eventNames[i]));
      }
    }

    return selectItems;
  }

  public List<SelectItem> list()
  {
    if (mEventNames == null || mEventNames.length == 0)
    {
      refresh();
    }
    return getEventNameSelectItems(mEventNames, "");
  }

}
