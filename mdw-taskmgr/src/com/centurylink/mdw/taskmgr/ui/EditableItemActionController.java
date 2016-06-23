/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui;

import java.util.List;

import javax.faces.event.ActionEvent;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.status.GlobalApplicationStatus;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIError;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.list.ListItem;

public abstract class EditableItemActionController implements ListActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public static final String ACTION_LIST = "list";
  public static final String ACTION_EDIT = "edit";
  public static final String ACTION_DELETE = "delete";
  public static final String ACTION_ADD = "add";
  public static final String ACTION_SAVE = "save";
  public static final String ACTION_CONFIRM_DELETE = "confirmDelete";
  public static final String ACTION_CANCEL = "cancel";
  public static final String ACTION_ERROR = "error";

  private EditableItem _item;
  public EditableItem getItem() { return _item; }
  public void setItem(EditableItem ei) { _item = ei; }

  private String _action = ACTION_LIST;
  public String getAction() { return _action; }
  public void setAction(String s) { _action = s; }

  /**
   * Called from command links in the list.
   *
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem) throws UIException
  {
    setAction(action);
    setItem((EditableItem)listItem);
    return "";
  }

  /**
   * Called from the command buttons for editing.
   *
   * @return the nav destination
   */
  public String performAction() throws UIException
  {
    try
    {
      if (getAction().equals(ACTION_SAVE))
      {
        getItem().save();
        setAction(ACTION_LIST);
      }
      else if (getAction().equals(ACTION_ADD))
      {
        getItem().add();
        setAction(ACTION_LIST);
      }
      else if (getAction().equals(ACTION_CONFIRM_DELETE))
      {
        getItem().delete();
        setAction(ACTION_LIST);
      }
      else if (getAction().equals(ACTION_CANCEL))
      {
        setAction(ACTION_LIST);
      }
      else if (getAction().equals(ACTION_DELETE))
      {
        setAction(ACTION_CONFIRM_DELETE);
      }
      else if (getAction().equals(ACTION_ERROR))
      {
        setAction(ACTION_LIST);
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.setValue("error", new UIError(ex));
      setAction(ACTION_ERROR);
    }

    return "";
  }

  public String add() throws UIException
  {
    setAction(ACTION_ADD);
    return performAction();
  }

  public String cancel() throws UIException
  {
    setAction(ACTION_CANCEL);
    ListManager.getInstance().clearCurrentRows();
    return performAction();
  }

  public String confirmDelete() throws UIException
  {
    setAction(ACTION_CONFIRM_DELETE);
    return performAction();
  }

  public String save() throws UIException
  {
    setAction(ACTION_SAVE);
    return performAction();
  }

  public void resetView(ActionEvent event) throws UIException
  {
    resetView();
  }

  public void resetView() throws UIException
  {
    ListManager.getInstance().clearCurrentRows();
    setAction(ACTION_LIST);
    performAction();
  }

  protected void notifyDetailTaskManagers(String action, Jsonable jsonable) throws UIException
  {
    try
    {
      if (GlobalApplicationStatus.getInstance().getDetailTaskManagersStatus())
      {
        List<StatusMessage> statuses = TaskManagerAccess.getInstance().notifyDetailTaskManagers(action, jsonable);
        getFinalStatusToUpdateSummaryTaskManager(statuses);
      }
      else
      {
        throw new UIException(GlobalApplicationStatus.getInstance().getOfflineSystemMsg());
      }
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  /*
   *  Update Summary task manager when more than half of detail managers got updated successfully
   */
  private void getFinalStatusToUpdateSummaryTaskManager(List<StatusMessage> statuses) throws UIException
  {
    StringBuffer errorMsg = new StringBuffer();
    int successCount = 0;
    int taskMgrCount = statuses.size();
    for (StatusMessage status : statuses)
    {
      if (status.isSuccess())
      {
        successCount++;
      }
      else
      {
        if (errorMsg.toString().isEmpty())
        {
          errorMsg.append("Notification error: Unable to update ");
        }
        errorMsg.append(status.getMessage()).append(",");
      }
    }
    if (successCount == taskMgrCount)
    {
    }
    else if (successCount > taskMgrCount / 2)
    {
      FacesVariableUtil.addMessage(errorMsg.toString().substring(0,errorMsg.toString().lastIndexOf(",")));
    }
    else
    {
      throw new UIException(errorMsg.toString().substring(0,errorMsg.toString().lastIndexOf(",")));
    }
  }

}
