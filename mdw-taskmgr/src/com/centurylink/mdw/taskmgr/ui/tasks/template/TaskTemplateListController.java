/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.template;

import java.io.IOException;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.task.Attribute;
import com.centurylink.mdw.task.TaskTemplate;
import com.centurylink.mdw.task.TaskTemplatesDocument;
import com.centurylink.mdw.task.TaskTemplatesDocument.TaskTemplates;
import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.taskmgr.ui.tasks.TaskItem;
import com.centurylink.mdw.taskmgr.ui.tasks.Tasks;
import com.centurylink.mdw.taskmgr.ui.workgroups.WorkgroupTree;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

public class TaskTemplateListController extends EditableItemActionController
{
  public static final String CONTROLLER_BEAN = "taskTemplateListController";
  public static final String ACTION_USER_GROUPS = "taskUserGroups";
  public static final String ACTION_VARIABLES = "taskVariables";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  /**
   * Called from command links in the task list.
   *
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem) throws UIException
  {
    super.performAction(action, listItem);
    FacesVariableUtil.setValue(CONTROLLER_BEAN, this);
    TaskItem taskItem = (TaskItem) listItem;
    // added for retrieving all attributes for use in Task Admin
    taskItem.setTask(TaskTemplateCache.getTaskTemplate(taskItem.getTask().getTaskId()));
    if (ACTION_EDIT.equals(action))
      WorkgroupTree.getInstance().setTask(taskItem.getTask());
    else if (ACTION_DELETE.equals(action))
      WorkgroupTree.getInstance().setDeletePending(taskItem.getTask());

    return null;
  }

  /**
   * Called from the command buttons for editing.
   *
   * @return the nav destination
   */
  public String performAction() throws UIException
  {
    // all actions are covered in the base type
    super.performAction();

    if (getAction().equals(ACTION_LIST))
    {
      FacesVariableUtil.setValue("taskItem", new TaskItem());
    }
    else if (getAction().equals(ACTION_ERROR))
    {
      return "go_error";
    }

    return "go_tasks";
  }

  /**
   * @return the appropriate display label for the current user action
   */
  public String getActionTitle()
  {
    TaskItem item = (TaskItem) FacesVariableUtil.getValue("taskItem");
    if (getAction().equals(ACTION_USER_GROUPS))
      return "User Groups For Task " + item.getName();
    if (getAction().equals(ACTION_VARIABLES))
      return "Variables For Task " + item.getName();
    return "Task";
  }

  private UploadedFile _importFile;
  public UploadedFile getImportFile() { return _importFile; }
  public void setImportFile(UploadedFile importFile)
  {
    this._importFile = importFile;
  }

  public String doImport() throws IOException, UIException
  {
    if (_importFile == null)
    {
      return null;
    }

    FacesContext facesContext = FacesContext.getCurrentInstance();
    facesContext.getExternalContext().getApplicationMap().put("fileupload_bytes", _importFile.getBytes());
    facesContext.getExternalContext().getApplicationMap().put("fileupload_type", _importFile.getContentType());
    facesContext.getExternalContext().getApplicationMap().put("fileupload_name", _importFile.getName());
    String fileName = _importFile.getName();
    // remove path info from the file name
    int slash = fileName.lastIndexOf('/');
    if (slash >= 0)
      fileName = fileName.substring(slash + 1);
    int backSlash = fileName.lastIndexOf('\\');
    if (backSlash >= 0)
      fileName = fileName.substring(backSlash + 1);

    String xml = new String(_importFile.getBytes());


    try
    {
      TaskTemplatesDocument templatesDoc = TaskTemplatesDocument.Factory.parse(xml, Compatibility.namespaceOptions());

      TaskManager taskMgr = RemoteLocator.getTaskManager();
      TaskTemplates templates = templatesDoc.getTaskTemplates();
      if (templates.getTaskList() != null)
      {
        for (TaskTemplate template : templatesDoc.getTaskTemplates().getTaskList())
        {
          TaskVO taskVO = new TaskVO(template);

          TaskVO existingTaskVO = TaskTemplateCache.getTaskTemplate(null, template.getLogicalId());
          if (existingTaskVO != null)
          {
            logger.info("Overwriting task template with id=" + existingTaskVO.getTaskId() + " and logicalId=" + template.getLogicalId());
            taskVO.setTaskId(existingTaskVO.getTaskId());
            taskMgr.updateTask(taskVO, true);
          }
          else
          {
            Long taskId = taskMgr.createTask(taskVO, true);
            taskVO.setTaskId(taskId);
          }

          EventManager eventMgr = RemoteLocator.getEventManager();
          UserActionVO action = new UserActionVO(FacesVariableUtil.getCurrentUser().getCuid(), Action.Import, Entity.TaskTemplate, taskVO.getTaskId(), taskVO.getTaskName());
          action.setSource("TaskManager");
          eventMgr.createAuditLog(action);
        }
      }


      Tasks.syncList("taskTemplateList");

      return null;
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(),  ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  private List<ListItem> _exportList = null;
  public List<ListItem> getExportList() { return _exportList; }
  public void setExportList(List<ListItem> list) { this._exportList = list; }

  public String doExport() throws UIException
  {
    TaskTemplatesDocument templatesDoc = TaskTemplatesDocument.Factory.newInstance();
    TaskTemplates templates = templatesDoc.addNewTaskTemplates();

    for (ListItem item : _exportList)
    {
      TaskItem taskItem = (TaskItem) item;
      TaskTemplate template = templates.addNewTask();
      if (taskItem.getLogicalId() == null || taskItem.getLogicalId().isEmpty())
        throw new UIException("Task (ID=" + taskItem.getId() + ") is missing logicalId");
      template.setLogicalId(taskItem.getLogicalId());
      template.setName(taskItem.getName());
      if (taskItem.getTaskCategory() != null)
        template.setCategory(taskItem.getTaskCategoryCode());
      if (taskItem.getComment() != null)
        template.setDescription(taskItem.getComment());

      TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskItem.getId());
      if (taskVO.getAttributes() != null)
      {
        for (AttributeVO attrVO : taskVO.getAttributes())
        {
          Attribute attr = template.addNewAttribute();
          attr.setName(attrVO.getAttributeName());
          attr.setStringValue(attrVO.getAttributeValue());
        }
      }
    }
    return templatesDoc.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2));
  }

}
