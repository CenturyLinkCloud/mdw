/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.detail;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.taskmgr.ui.events.detail.ExternalEventDetail;
import com.centurylink.mdw.taskmgr.ui.layout.DetailUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.orders.detail.OrderDetail;
import com.centurylink.mdw.taskmgr.ui.process.ProcessDetail;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskAction;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.CustomPageDetailHandler;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.GeneralTaskDetailHandler;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskData;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWDataAccess;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Provides a central, instantiable entity for accessing cached detail data.
 * The instance is kept at session scope, so that user detail information is
 * maintained for the duration of the session unless invalidated.
 */
public class DetailManager
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private Map<String,Detail> _details = new HashMap<String,Detail>();

  public DetailManager()
  {
  }

  public TaskDetail getTaskDetail() throws UIException
  {
    // handle case where taskInstanceId is request param
    String taskInstanceId = (String)FacesVariableUtil.getRequestParamValue(TaskAttributeConstant.TASK_INSTANCE_ID_PARAM);
    if (taskInstanceId == null)
      taskInstanceId = (String) FacesVariableUtil.getRequestAttrValue(TaskAttributeConstant.TASK_INSTANCE_ID_PARAM); // forwarding
    TaskDetail taskDetail = (TaskDetail) getDetail("taskDetail");

    String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
    Boolean redirectPendingAttr = (Boolean)FacesVariableUtil.getRequestAttrValue("taskDetailRedirectPending");
    boolean redirectPending = redirectPendingAttr != null && redirectPendingAttr.booleanValue();

    if (taskInstanceId != null && taskInstanceId.trim().length() > 0 && !taskInstanceId.equals(taskDetail.getInstanceId()))
    {
      setTaskDetail(taskInstanceId);
      taskDetail = (TaskDetail) getDetail("taskDetail");

      String taskAction = null;
      String user = null;

      try
      {
        // identify the action and user
        taskAction = (String)FacesVariableUtil.getRequestParamValue("mdw.Action");
        String userIdentifier = (String)FacesVariableUtil.getRequestParamValue("mdw.UserId");
        if (userIdentifier != null)
          user = CryptUtil.decrypt(userIdentifier);
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new UIException(ex.getMessage(), ex);
      }

      if (taskDetail.getFullTaskInstance().isAutoformTask())
      {
        // fall to default code for classic task below
      }
      else if (taskDetail.getFullTaskInstance().isGeneralTask())
      {
        String navOutcome = new GeneralTaskDetailHandler(taskDetail.getFullTaskInstance()).go();
        if (navOutcome == null)
        {
          taskDetail.setNavOutcome("");
          FacesVariableUtil.setRequestAttrValue("taskDetailRedirectPending", Boolean.TRUE);
        }
        else
        {
          taskDetail.setNavOutcome(navOutcome);
          FacesVariableUtil.navigate(navOutcome + "_redirect");
          FacesVariableUtil.setRequestAttrValue("taskDetailRedirectPending", Boolean.TRUE);
        }
        return taskDetail;
      }
      else if (TaskTemplateCache.getTaskTemplate(taskDetail.getFullTaskInstance().getTaskId()).isHasCustomPage())
      {
        //Avoid navigation for TaskActions (POST requests)
        if (!((javax.servlet.http.HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getMethod().equalsIgnoreCase("POST"))
        {
          //Navigation required to open taskDetail from Designer (for all GET requests)
          String navOutcome = getCustomPageDetailHandler(taskDetail.getFullTaskInstance()).go();
          taskDetail.setNavOutcome(navOutcome);
          FacesVariableUtil.setRequestAttrValue("taskDetailRedirectPending", Boolean.TRUE);
          return taskDetail;
        }
      }

      if (taskAction != null && user != null)
      {
        for (DetailItem detailItem : ((TaskDetail)getDetail("taskDetail")).getItems())
          detailItem.setLinkAction(null);
        try
        {
          AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
          if (!user.equals(authUser.getCuid()))
          {
            UserManager userManager = RemoteLocator.getUserManager();
            authUser = userManager.loadUser(user);
            FacesVariableUtil.setValue("authenticatedUser", authUser);
          }

          if (!taskDetail.getFullTaskInstance().getStatus().equals("Completed"))
          {
              Long instanceId = new Long(taskInstanceId);
              Long userId = authUser.getUserId();

              TaskManager taskManager = RemoteLocator.getTaskManager();
              // FIXME need to call TaskManagerAccess methods here
              // FIXME also need to notify engine separately
              taskManager.performActionOnTaskInstance("Claim", instanceId, userId, userId, null, null, false);
              taskManager.performActionOnTaskInstance(taskAction, instanceId, userId, null, null, null, true);
              setTaskDetail(taskInstanceId);
              taskDetail = (TaskDetail)getDetail("taskDetail");
              taskDetail.setAction(taskAction);
              for (DetailItem detailItem : taskDetail.getItems())
                detailItem.setLinkAction(null);
          }
        }
        catch (Exception ex)
        {
          logger.severeException(ex.getMessage(), ex);
          throw new UIException(ex.getMessage(), ex);
        }
      }
    }
    else if (taskDetail.getNavOutcome() != null && viewId.endsWith("taskDetail.xhtml"))
    {
      if (!redirectPending)
      {
        FacesContext.getCurrentInstance().responseComplete();
        FacesVariableUtil.setRequestAttrValue("taskDetailRedirectPending", Boolean.TRUE);
        if (taskDetail.getNavOutcome().length() == 0)
        {
          try
          {
            FacesVariableUtil.navigate(new URL(ApplicationContext.getTaskManagerUrl() + "/MDWHTTPListener/task?name=" + taskDetail.getInstanceId()));
          }
          catch (IOException ex)
          {
            logger.severeException(ex.getMessage(), ex);
          }
        }
        else if (taskDetail.getNavOutcome().equals("customPage"))
        {
          getCustomPageDetailHandler(taskDetail.getFullTaskInstance()).go();
        }
        else
        {
          FacesVariableUtil.navigate(taskDetail.getNavOutcome() + "_redirect");
        }
      }
    }

    return (TaskDetail) getDetail("taskDetail");
  }

  protected CustomPageDetailHandler getCustomPageDetailHandler(FullTaskInstance taskInstance)
  {
    return new CustomPageDetailHandler(taskInstance);
  }

  public ExternalEventDetail getExternalEventDetail() throws UIException
  {
    return (ExternalEventDetail) getDetail("externalEventDetail");
  }

  public void setTaskDetail(String taskInstanceId) throws UIException
  {
    setDetail("taskDetail", taskInstanceId);
    // reset the action for any array data item controller
    ArrayDataItemActionController arrayDataItemActionController
        = (ArrayDataItemActionController) FacesVariableUtil.getValue("arrayDataItemActionController");
    if (arrayDataItemActionController != null)
      arrayDataItemActionController.setAction(arrayDataItemActionController.getDefaultAction());

    // set the task managed bean for new-style pages
    FacesVariableUtil.setValue("task", getTaskDetail().getFullTaskInstance());

    // initialize the taskAction comment
    ((TaskAction)FacesVariableUtil.getValue("taskAction")).setComment(getTaskDetail().getComments());

    if (!OwnerType.USER.equals(getTaskDetail().getFullTaskInstance().getOwnerType())
        && !OwnerType.EXTERNAL.equals(getTaskDetail().getFullTaskInstance().getOwnerType())
        && getTaskDetail().getFullTaskInstance().getTaskInstance().isLocal())
    {
      try
      {
        // set the process instance managed bean
        MDWProcessInstance processInstance = (MDWProcessInstance) FacesVariableUtil.getValue("process");
        RuntimeDataAccess runtimeDataAccess = ((MDWDataAccess)FacesVariableUtil.getValue("dataAccess")).getRuntimeDataAccess();
        ProcessInstanceVO processInstanceVO = runtimeDataAccess.getProcessInstanceAll(getTaskDetail().getProcessInstanceId());
        if (processInstanceVO == null)
          throw new UIException("Process instance not found: " + getTaskDetail().getProcessInstanceId());

        ProcessVO processDef = ProcessVOCache.getProcessVO(processInstanceVO.getProcessId());
        if (processDef == null)
          throw new UIException("Process definition not found (id=" + processInstanceVO.getProcessId() + "): " + processInstanceVO.getComment());

        if (processInstanceVO.isNewEmbedded() || processDef.isEmbeddedProcess())
          processInstanceVO = runtimeDataAccess.getProcessInstanceAll(processInstanceVO.getOwnerId());
        processInstance.setProcessInstanceVO(processInstanceVO);

        // set the task instance data
        FacesVariableUtil.setValue("taskData", new TaskData(getTaskDetail(), processInstance));
      }
      catch (UIException ex)
      {
        throw ex;
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new UIException(ex.getMessage(), ex);
      }
    }
  }

  public void setOrderDetail(String orderId) throws UIException
  {
    setDetail("orderDetail", orderId);
  }

  public void setExternalEventDetail(String ownerType, String instanceId) throws UIException
  {
    ExternalEventDetail detail = (ExternalEventDetail) getInstance().createDetail("externalEventDetail");
    detail.setOwnerType(ownerType);
    detail.populate(instanceId);
    setDetail("externalEventDetail", detail);
  }

  /**
   * Sets the mapped detail instance based on the detail UI definition id and
   * the instance id to be used to retrieve the detail information.
   *
   * @param detailId the UI view definition detail ID
   * @param instanceId instance ID used to populate the model wrapper
   */
  public void setDetail(String detailId, String instanceId) throws UIException
  {
    Detail detail = getInstance().createDetail(detailId, instanceId);
    setDetail(detailId, detail);
  }

  /**
   * Sets the mapped detail instance based on the detail UI definition id and
   * the instance id to be used to retrieve the detail information.  The criterion
   * represented by the instanceId is specified rather than defaulted.
   *
   * @param detailId the UI view definition detail ID
   * @param instanceId instance ID used to populate the model wrapper
   * @param instanceName identifies which criterion is represented by instanceId
   */
  public void setDetail(String detailId, String instanceId, String instanceName) throws UIException
  {
    Detail detail = getInstance().createDetail(detailId, instanceId, instanceName);
    setDetail(detailId, detail);
  }

  public OrderDetail getOrderDetail() throws UIException
  {
    // handle case where orderId is request param
    String orderId = (String)FacesVariableUtil.getRequestParamValue("orderId");
    OrderDetail orderDetail = (OrderDetail) getDetail("orderDetail");
    if (!StringHelper.isEmpty(orderId) && !orderId.equals(orderDetail.getOrderId()))
    {
      setOrderDetail(orderId);
    }

    return (OrderDetail) getDetail("orderDetail");
  }

  /**
   * Employs the factory pattern to return a DetailManager instance
   * dictated by the property "detail.manager" (default is detailManager).
   * @return a handle to a DetailManager instance
   */
  public static DetailManager getInstance()
  {
    String detailManagerBean = getDetailManagerBeanName();
    DetailManager dm = (DetailManager)FacesVariableUtil.getValue(detailManagerBean);
    if (dm == null)
    {
      logger.info("No managed bean found for name '" + detailManagerBean + "'");
      dm = createInstance();  // default
      FacesVariableUtil.setValue(detailManagerBean, dm);
    }
    return dm;
  }

  public ProcessDetail getProcessDetail() throws UIException
  {
    // handle case where instanceId is request param
    ProcessDetail processDetail = (ProcessDetail) getDetail("processDetail");
    String instanceId = (String)FacesVariableUtil.getRequestParamValue("instanceId");
    if (!StringHelper.isEmpty(instanceId) && !instanceId.equals(processDetail.getInstanceId()))
      setProcessDetail(instanceId);

    return (ProcessDetail) getDetail("processDetail");
  }

  public void setProcessDetail(String instanceId) throws UIException
  {
    setDetail("processDetail", instanceId);
  }

  public static String getDetailManagerBeanName()
  {
    String detailManagerBean = null;
    try
    {
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      detailManagerBean = propMgr.getStringProperty("MDWFramework.TaskManagerWeb", "detail.manager");
    }
    catch (PropertyException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    if (detailManagerBean == null)
    {
      detailManagerBean = "detailManager";
    }

    return detailManagerBean;
  }

  protected static DetailManager createInstance()
  {
    return new DetailManager();
  }

  /**
   * Looks up and returns an instance of a detail based on its id in TaskView.xml.
   *
   * @param detailId identifies the detail
   * @return an instance of the detail associated with the user's session
   */
  public Detail getDetail(String detailId) throws UIException
  {
    return findDetail(detailId);
  }

  public void setDetail(String detailId, Detail detail)
  {
    _details.put(detailId, detail);
  }

  private Detail findDetail(String detailId) throws UIException
  {
    Detail detail = (Detail) _details.get(detailId);
    if (detail == null)
    {
      detail = createDetail(detailId);
      _details.put(detailId, detail);
    }

    return detail;
  }

  /**
   * Create a detail instance without populating.
   * @param detailId the UI id
   * @return the newly created detail instance
   */
  public Detail createDetail(String detailId) throws UIException
  {
    try
    {
      DetailUI detailUI = ViewUI.getInstance().getDetailUI(detailId);
      String modelClass = null;
      if (detailUI == null)
      {
        if (detailId.equals("taskDetail"))
          modelClass = "com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail";
        else if (detailId.equals("orderDetail"))
          modelClass = "com.centurylink.mdw.taskmgr.ui.orders.detail.OrderDetail";
        else if (detailId.equals("externalEventDetail"))
          modelClass = "com.centurylink.mdw.taskmgr.ui.events.detail.ExternalEventDetail";
        else if (detailId.equals("processDetail"))
          modelClass = "com.centurylink.mdw.taskmgr.ui.process.ProcessDetail";
      }
      else
      {
        modelClass = detailUI.getModel();
      }
      if (modelClass == null) // Dynamic Detail class
      {
        Object detailObj = FacesVariableUtil.getValue(detailId);
        if (detailObj != null) {
          return (Detail)detailObj;
        }
      }
      Class<?> toInstantiate = Class.forName(modelClass);
      Constructor<?> detailCtor = toInstantiate.getConstructor(new Class[] {DetailUI.class} );
      Detail detail = (Detail) detailCtor.newInstance(new Object[] { detailUI } );
      return detail;
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException("Cannot create detail: " + detailId, ex);
    }
  }

  /**
   * Create a detail instance and populate using the model wrapper rather
   * than relying on retrieveInstance() to populate (used in cases where the
   * model data is retrieved separately from detail creation).
   * @param detailId the UI id
   * @param modelWrapper the modelWrapper holding the detail data
   * @return the newly created detail instance
   */
  public Detail createDetail(String detailId, ModelWrapper modelWrapper) throws UIException
  {
    Detail detail = createDetail(detailId);
    detail.setModelWrapper(modelWrapper);
    detail.populate(null);
    return detail;
  }

  /**
   * Creates and populates a detail instance based on the unique id (relies on the
   * retrieveInstance() method in the detail subclass to populate the model wrapper).
   *
   * @param detailId the UI id
   * @param instanceId the unique id for populating
   * @return the detail instance
   */
  public Detail createDetail(String detailId, String instanceId) throws UIException
  {
      Detail detail = createDetail(detailId);
      detail.populate(instanceId);
      return detail;
  }

  /**
   * Creates and populates a detail instance based on the unique id (relies on the
   * retrieveInstance() method in the detail subclass to populate the model wrapper).
   * This overload allows specifying the what the instanceId represents via instanceName.
   *
   * @param detailId the UI id
   * @param instanceId the unique id for populating
   * @param instanceName indicates the name of the instance retrieval criterion
   * @return the detail instance
   */
  public Detail createDetail(String detailId, String instanceId, String instanceName) throws UIException
  {
      Detail detail = createDetail(detailId);
      detail.populate(instanceId, instanceName);
      return detail;
  }

  public void invalidate()
  {
    _details = new HashMap<String,Detail>();
  }
}
