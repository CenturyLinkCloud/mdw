/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.taskmgr.ui.Decoder;
import com.centurylink.mdw.taskmgr.ui.Lister;

/**
 * Caches the refrence data for task statuses.
 *
 */
public class ProcessStatuses implements Lister, Decoder
{

  /**
   * Retrieve the list of TaskStatuses from the workflow.
   */
  public static void refresh()
  {
  }

  /**
   * Get a list of SelectItems populated from the statuses.
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public List<SelectItem> getStatusSelectItems(String firstItem)
  {

    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (firstItem != null)
    {
      selectItems.add(new SelectItem("0", firstItem));
    }
    for (int i = 0; i < WorkStatus.allStatusCodes.length; i++)
    {
      // TODO: check user role if supplied
      selectItems.add(new SelectItem(WorkStatus.allStatusCodes[i].toString(), WorkStatus.allStatusNames[i]));
    }
    return selectItems;
  }

  public String decode(Long id) {
    for (int i = 0; i < WorkStatus.allStatusCodes.length; i++)
    {
      if (WorkStatus.allStatusCodes[i].intValue()==id.intValue())
        return WorkStatus.allStatusNames[i];
    }
    return null;
  }

  public static String decodeProcessStatus(Integer id) {
    for (int i = 0; i < WorkStatus.allStatusCodes.length; i++)
    {
        if (WorkStatus.allStatusCodes[i].intValue()==id.intValue())
            return WorkStatus.allStatusNames[i];
    }
    return null;
  }

  public List<SelectItem> list()
  {
    return getStatusSelectItems("");
  }

}
