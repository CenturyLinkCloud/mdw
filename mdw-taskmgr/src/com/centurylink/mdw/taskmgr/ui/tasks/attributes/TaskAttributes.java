/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.tasks.TaskItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

public class TaskAttributes extends SortableList implements Lister
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public TaskAttributes(ListUI listUI)
  {
    super(listUI);
  }

  /**
   *  Needs a no-arg constructor since TaskAttributes is a dropdown lister
   *  in addition to being a SortableList implementation.
   */
  public TaskAttributes() throws UIException
  {
    super(ViewUI.getInstance().getListUI("taskAttributeList"));
  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
   */
  protected DataModel<ListItem> retrieveItems()
  {
    try
    {
      TaskItem selItem = ((TaskItem) FacesVariableUtil.getValue("taskItem"));
      List<ListItem> l = new ArrayList<ListItem>();
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      Collection<?> attribs = taskMgr.getTaskAttributes(selItem.getId());
      Iterator<?> it = attribs.iterator();
      while (it.hasNext())
      {
        AttributeVO att = (AttributeVO) it.next();
        TaskAttributeItem item = new TaskAttributeItem(att);
        l.add(item);
      }

      ListDataModel<ListItem> retModel = new ListDataModel<ListItem>(l);
      return retModel;
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }

  }

  /**
   * @see com.centurylink.mdw.taskmgr.ui.Lister#list(java.lang.String)
   */
  public List<SelectItem> list()
  {
    return new ArrayList<SelectItem>();
  }

}
