/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.taskmgr.ui.Decoder;
import com.centurylink.mdw.taskmgr.ui.Lister;

public class Processes implements Lister, Decoder
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private Map<String,ProcessVO> _processes;

  /**
   * Get a list of SelectItems populated from the public processes.
   * @return list of SelectItems
   */
  public List<SelectItem> getProcessSelectItems()
  {
    return getProcessSelectItems("");
  }

  /**
   * Get a list of SelectItems populated from the public processes.
   * @param firstItem the message for an optional first item (null for none)
   * @return list of SelectItems
   */
  public List<SelectItem> getProcessSelectItems(String firstItem)
  {
    Map<String,ProcessVO> processes = getProcesses();

    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    if (firstItem != null)
    {
      selectItems.add(new SelectItem("0", firstItem));
    }
    for (ProcessVO process : processes.values())
    {
      String processId = process.getProcessId().toString();
      selectItems.add(new SelectItem(processId, process.getLabel()));
    }
    return selectItems;
  }

  public Map<String,ProcessVO> getProcesses()
  {
    refresh();  // caching is causing too much user confusion
    return _processes;
  }

  /**
   * Retrieve the list of Processes.
   */
  public void refresh()
  {
    _processes = new LinkedHashMap<String,ProcessVO>();  // preserve order

    try
    {
      ProcessLoader loader = DataAccess.getProcessLoader();
      List<ProcessVO> procs = loader.getProcessList();
      for (ProcessVO proc : procs)
      {
        if (!_processes.containsKey(proc.getName())) // only latest versions
          _processes.put(proc.getName(), proc);
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public List<SelectItem> list()
  {
    return getProcessSelectItems("");
  }

  public String decode(Long id)
  {
    for (ProcessVO process : _processes.values())
    {
      if (process.getProcessId().equals(id))
        return process.getProcessName();
    }
    return null; // not found
  }

}
