/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.detail;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.data.task.TaskStatus;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.task.TaskManagerBean;
import com.centurylink.mdw.taskmgr.ui.detail.Detail;
import com.centurylink.mdw.taskmgr.ui.detail.InstanceData;
import com.centurylink.mdw.taskmgr.ui.detail.InstanceDataItem;
import com.centurylink.mdw.taskmgr.ui.layout.DetailUI;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.Tasks;
import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActions;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWBase;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Represents a Detail for the user interface (Task, Jeopardy, Fallout or Alert),
 * with an associated FullTaskInstance populated with data values for display.
 */
public class TaskDetail extends Detail implements InstanceData
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public TaskDetail(DetailUI detailUI)
  {
    super(detailUI);
  }

  private List<InstanceDataItem> _instanceDataItems = new ArrayList<InstanceDataItem>();
  public List<InstanceDataItem> getInstanceDataItems() { return _instanceDataItems; }
  public boolean isHasInstanceDataItems() { return _instanceDataItems.size() > 0; }


  /**
   * Retrieve the task detail and instance data.
   */
  protected void retrieveInstance(String instanceId) throws UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      TaskInstanceVO taskInstance = taskMgr.getTaskInstanceVO(new Long(instanceId));
      FullTaskInstance fullTaskInst = new FullTaskInstance(taskInstance);
      List<FullTaskInstance> subTaskInsts = new ArrayList<FullTaskInstance>();
      for (TaskInstanceVO subTaskInst : taskMgr.getSubTaskInstances(taskInstance.getTaskInstanceId()))
        subTaskInsts.add(new FullTaskInstance(subTaskInst));
      fullTaskInst.setSubTaskInstances(subTaskInsts);
      setModelWrapper(fullTaskInst);

      MDWBase mdw = (MDWBase) FacesVariableUtil.getValue("mdw");

      if (getFullTaskInstance().isAutoformTask())
      {
        mdw.setTaskInstance(null, null);
        retrieveInstanceData();
      }
      else if (taskInstance.isGeneralTask())
      {
        Long docid = taskInstance.getSecondaryOwnerId();
        datadoc = new FormDataDocument();
        mdw.loadDocument(datadoc, docid);
        mdw.setTaskInstance(taskInstance, datadoc);
        getTaskActions(datadoc);
      }
      else
      {
        mdw.setTaskInstance(null, null);
        if (!getFullTaskInstance().isHasCustomPage())
          retrieveInstanceData();
      }
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException("Error retrieving Task Detail.", ex);
    }
  }


  /**
   * Reads the property setting for loading a workflow snapshot.
   * @return the URL for a workflow gif image
   */
  public String getWorkflowSnapshotUrl()
  {
    String url = null;
    try
    {
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      url = propMgr.getStringProperty("MDWFramework.TaskManagerWeb", "workflow.snapshot.image.url");
      url += "?IMAGE_TYPE=TASK_INSTANCE&amp;TaskInstanceId=" + getModelWrapper().getWrappedId();
    }
    catch(PropertyException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    return url;
  }

  public void retrieveInstanceData() throws UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      _instanceDataItems.clear();
      Long taskInstId = new Long(getModelWrapper().getWrappedId());
      List<VariableInstanceVO> instanceData;
      if (getFullTaskInstance().isAutoformTask())
      {
        // TODO: support value expressions by doing away with FormDataDocument
        DocumentVO docvo = taskMgr.getTaskInstanceData(getFullTaskInstance().getTaskInstance());
        FormDataDocument datadoc = new FormDataDocument();
        datadoc.load(docvo.getContent());
        instanceData = taskMgr.constructVariableInstancesFromFormDataDocument(getTaskTemplate(), getProcessInstanceId(), datadoc, taskInstId);
      }
      else
      {
        instanceData = taskMgr.getVariableInstanceVOsForTaskInstance(taskInstId);
      }
      String varstring = getTaskTemplate().getAttribute(TaskAttributeConstant.VARIABLES);
	  List<String[]> parsed = StringHelper.parseTable(varstring, ',', ';', 5);
	  for (String[] one : parsed) {
		  String varname = one[0];
		  String displayOption = one[2];
		  String sequence = one[3];
          for (VariableInstanceVO var : instanceData)
          {
              if (varname.equals(var.getName()) && ! displayOption.equals(TaskActivity.VARIABLE_DISPLAY_HIDDEN)) {
                  InstanceDataItem item = new TaskInstanceDataItem(var, this, Integer.parseInt(sequence));
                  _instanceDataItems.add(item);
                  break;
              }
          }
	  }

	  // Sort Instance list as per sequence
	  Collections.sort(_instanceDataItems, new Comparator<InstanceDataItem>() {

		@Override
		public int compare(InstanceDataItem item1, InstanceDataItem item2) {
			int result = 0;
			if (item1.getSequence() == item2.getSequence()) {
				result = item1.getName().compareTo(item2.getName());
			} else if (item1.getSequence() > item2.getSequence()) {
				result = 1;
			} else {
				result = -1;
			}
			return result;
		}
	  });
    }
    catch(Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

  public boolean isApplicableForCurrentUsersWorkgroups() throws UIException
  {
    List<String> taskGroups = getFullTaskInstance().getTaskInstance().getGroups();
	if (taskGroups != null && taskGroups.size() > 0)
	{
      String[] userGroups = FacesVariableUtil.getCurrentUser().getWorkgroupNames();
      for (String g : userGroups)
      {
        if (taskGroups.contains(g))
          return true;
      }
      return false;
    }
    else
    {
      TaskVO[] tasks = ((Tasks) FacesVariableUtil.getValue("tasks")).getTasksForUserWorkgroups();
      for (int i = 0; i < tasks.length; i++)
      {
        if (tasks[i].getTaskId().longValue() == getFullTaskInstance().getTaskId())
          return true;
      }
      return false;
    }
  }

  public FullTaskInstance getFullTaskInstance()
  {
    return (FullTaskInstance) getModelWrapper();
  }

  public Long getProcessInstanceId()
  {
    if (getModelWrapper() == null)
      return null;

    return getFullTaskInstance().getProcessInstanceId();
  }

  public boolean isInFinalState()
  {
    if (getModelWrapper() == null)
      return false;
    try
    {
      TaskManager taskManager = RemoteLocator.getTaskManager();
      return taskManager.isInFinalStatus(getFullTaskInstance().getTaskInstance());
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return false;
    }
  }

  public String getStatus()
  {
    if (getModelWrapper() == null)
      return null;

    return getFullTaskInstance().getStatus();
  }

  public String getMasterRequestId()
  {
    if (getModelWrapper() == null)
      return null;

    return getFullTaskInstance().getOrderId();
  }

  public String getTaskName()
  {
    if (getModelWrapper() == null)
      return null;

    return getFullTaskInstance().getName();
  }

  public String getComments()
  {
    if (getModelWrapper() == null)
      return null;

    return getFullTaskInstance().getComments();
  }

  public boolean isEditable()
  {
    return super.isEditable() && isTaskAssignedToCurrentUser();
  }

  public boolean isInstanceDataEditable()
  {
    // if 'Work' action is applicable, can't edit unless 'In Progress' status
    if (isWorkTaskActionApplicableForStatus()
        && !getStatus().equals(TaskStatus.STATUS_IN_PROGRESS))
    {
      return false;
    }
    return isTaskAssignedToCurrentUser() && !isInFinalState();
  }

  public boolean isTaskAssignedToCurrentUser()
  {
    if (!"Assigned".equals(getFullTaskInstance().getStatus())
        && !"In Progress".equals(getFullTaskInstance().getStatus()))
    {
      return false;
    }
    else
    {
      return FacesVariableUtil.getCurrentUser().getCuid().equals(getFullTaskInstance().getAssignee());
    }
  }

  public boolean isWorkTaskActionApplicableForStatus()
  {
    for (TaskAction taskAction : TaskActions.getStandardTaskActionsForStatus(getTaskName(), getStatus()))
    {
      if (taskAction.getTaskActionName().equals(TaskAction.WORK))
        return true;
    }
    return false;
  }

  public boolean isAnyValidTaskActions() throws UIException
  {
    return getValidTaskActions().size() > 0;
  }

  private List<TaskAction> validTaskActions;
  public List<TaskAction> getValidTaskActions() throws UIException
  {
    // cache the valid task actions since the dynamic lookup is expensive
    if (validTaskActions != null)
      return validTaskActions;

    validTaskActions = getStandardTaskActions();

    // add any applicable dynamic actions
    List<TaskAction> dynamicTaskActions = getDynamicTaskActions();
    if (dynamicTaskActions != null && !isInFinalState())
    {
      for (TaskAction dynamicTaskAction : dynamicTaskActions)
      {
        if (!validTaskActions.contains(dynamicTaskAction))
        {
          dynamicTaskAction.setDynamic(true);
          validTaskActions.add(dynamicTaskAction);
        }
      }
    }

    Collections.sort(validTaskActions);
    return validTaskActions;
  }

  /**
   * Gets the custom task actions as defined by the outgoing work transitions
   * for the associated activity.
   */
  private List<TaskAction> getDynamicTaskActions()
  {
    if (!isInstanceDataEditable())
      return null;

    try
    {
      TaskManager taskManager = RemoteLocator.getTaskManager();
      return taskManager.getDynamicTaskActions(new Long(getInstanceId()));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Gets the standard task actions, filtered according to what's applicable
   * depending on the context of the task activity instance.
   */
  protected List<TaskAction> filterStandardTaskActions(List<TaskAction> standardTaskActions)
  {
    // if the task is not assigned to the current user, the only possible actions are Assign, Claim and Release
    if (!isTaskAssignedToCurrentUser())
    {
      List<TaskAction> filtered = new ArrayList<TaskAction>();
      for (TaskAction action : standardTaskActions)
      {
        if (action.getTaskActionName().equalsIgnoreCase(TaskAction.ASSIGN)
            || action.getTaskActionName().equals(TaskAction.CLAIM)
            || action.getTaskActionName().equals(TaskAction.RELEASE))
        {
          filtered.add(action);
        }
      }
      return filtered;
    }

    // otherwise find
    try
    {
      TaskManager taskManager = RemoteLocator.getTaskManager();
      return taskManager.filterStandardTaskActions(new Long(getInstanceId()), standardTaskActions);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Returns a sub-list of selectItems appropriate for the status
   * of the current TaskDetail.
   *
   * @return SelectItems for the valid actions
   */
  public List<SelectItem> getTaskActionSelectItems() throws UIException
  {
    return TaskActions.buildSelectItemList(getValidTaskActions());
  }

  public UIComponent getTaskActionMenu() throws UIException, CachingException
  {
    // avoid classloader dependency on RichFaces 3.3
    try
    {
      Class<?> toInstantiate = Class.forName("com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActionMenu");
      Constructor<?> ctor = toInstantiate.getConstructor(new Class[] {String.class, List.class} );
      return (UIComponent) ctor.newInstance(new Object[] { getId(), getValidTaskActions() } );
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void setTaskActionMenu(UIComponent menu)
  {
    // does nothing; required by JSF binding
  }

  public UIComponent getFormTaskActionMenu() throws UIException
  {
    // avoid classloader dependency on RichFaces 3.3
    try
    {
      Class<?> toInstantiate = Class.forName("com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActionMenu");
      Constructor<?> ctor = toInstantiate.getConstructor(new Class[] {String.class, List.class} );
      return (UIComponent) ctor.newInstance(new Object[] { getId() + "_form", getValidTaskActions() } );
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public void setFormTaskActionMenu(UIComponent menu)
  {
    // does nothing; required by JSF binding
  }

  /**
   * Returns a select item list that includes only the standard task actions,
   * and not any dynamic ones.
   */
  public List<SelectItem> getStandardTaskActionSelectItems() throws UIException
  {
    return TaskActions.buildSelectItemList(getStandardTaskActions());
  }

  public List<TaskAction> getStandardTaskActions()
  {
    List<TaskAction> standardTaskActions = TaskActions.getStandardTaskActionsForStatus(getTaskName(), getStatus());
    return filterStandardTaskActions(standardTaskActions);
  }

  // this supports one-click task actioning
  private String action;
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }

  private void getTaskActions(FormDataDocument datadoc)
  {
    String v = datadoc.getMetaValue(FormDataDocument.META_TASK_CUSTOM_ACTIONS);
    String[] custom_actions = v == null ? null : v.split("#");
    validTaskActions = TaskActions.getStandardTaskActionsForStatus(getTaskName(), getStatus());
    // call isInstanceDataEditable must be after validTaskActions are assigned
    boolean addDynamicActions = isInstanceDataEditable() && !isInFinalState();
    TaskAction retryAction = null, completeAction = null, cancelAction = null;
    for (TaskAction action : validTaskActions)
    {
      if (action.getTaskActionName().equalsIgnoreCase("Retry"))
        retryAction = action;
      else if (action.getTaskActionName().equalsIgnoreCase("Complete"))
        completeAction = action;
      else if (action.getTaskActionName().equalsIgnoreCase("Cancel"))
        cancelAction = action;
    }
    if (custom_actions != null)
    {
      for (String custom_action : custom_actions)
      {
        if (custom_action.equalsIgnoreCase("Retry"))
          retryAction = null;
        else if (custom_action.equalsIgnoreCase("Complete"))
          completeAction = null;
        else if (custom_action.equalsIgnoreCase("Cancel"))
          cancelAction = null;
        else if (addDynamicActions)
        {
          TaskAction action = new TaskAction();
          action.setTaskActionName(custom_action);
          action.setDynamic(true);
          if (!validTaskActions.contains(action))
            validTaskActions.add(action);
        }
      }
    }
    if (retryAction != null)
      validTaskActions.remove(retryAction);
    if (completeAction != null)
      validTaskActions.remove(completeAction);
    if (cancelAction != null)
      validTaskActions.remove(cancelAction);
    Collections.sort(validTaskActions);
  }

  private FormDataDocument datadoc;

  public FormDataDocument getDataDocument()
  {
    return datadoc;
  }

  @Override
  public void populate(String instanceId) throws UIException
  {
    try
    {
      retrieveInstance(instanceId);

      if (getDetailUI() != null)
        buildDetail();
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException("Error retrieving Task Detail.", ex);
    }
  }

  public boolean isInstanceDataSavable()
  {
    if (!isHasInstanceDataItems() || !isInstanceDataEditable())
      return false;

    for (InstanceDataItem item : getInstanceDataItems())
    {
      if (item.isValueEditable())
        return true;
    }
    return false;
  }


  private String navOutcome;
  public String getNavOutcome() { return navOutcome; }
  public void setNavOutcome(String outcome) { this.navOutcome = outcome; }

  public TaskVO getTaskVO()
  {
    return getFullTaskInstance().getTaskVO();
  }

  public TaskVO getTaskTemplate()
  {
    return getFullTaskInstance().getTaskVO();
  }

  public boolean isSubmitWithoutValidation()
  {
    return true;
  }

  public String getTaskForm()
  {
    return getTaskTemplate().getFormName();
  }

  public boolean isCustomForm()
  {
    return !getTaskTemplate().isAutoformTask() && getTaskForm() != null;
  }

}
