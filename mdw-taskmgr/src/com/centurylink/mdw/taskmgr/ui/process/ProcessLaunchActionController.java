/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.servlet.http.HttpServletRequest;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.bpm.WorkTypeDocument.WorkType;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIDocument;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;
import com.centurylink.mdw.web.util.RemoteLocator;

public class ProcessLaunchActionController implements ActionListener
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void processAction(ActionEvent actionEvent) throws AbortProcessingException
  {
    FacesVariableUtil.setValue("processLaunchActionController", this);
    FullProcessInstance processInstance = (FullProcessInstance) FacesVariableUtil.getValue("newProcessInstance");

    launchProcess(processInstance);
  }

  protected void launchProcess(FullProcessInstance processInstance)
  {
    if (processInstance.getMasterRequestId() == null || processInstance.getMasterRequestId().trim().length() == 0)
    {
      SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmss");
      processInstance.setMasterRequestId(FacesVariableUtil.getCurrentUser().getCuid() + "~" + sdf.format(new Date()));
     }
    try
    {
      // process variables
      Map<String, Object> processVars = new HashMap<String, Object>();
      for (VariableDataItem variableDataItem : processInstance.getVariableDataItems())
      {
        if (variableDataItem.isInputParam() && variableDataItem.getVariableData() != null
              && variableDataItem.getVariableData().toString().length() != 0)
        {
          if (variableDataItem.isDocument())
            processVars.put(variableDataItem.getName(), createDocument(variableDataItem.getDataType(), variableDataItem.getVariableData()));
          else
            processVars.put(variableDataItem.getName(), variableDataItem.getVariableData());
        }
      }

      // launch the process
      EventManager eventMgr = RemoteLocator.getEventManager();
      if (logger.isInfoEnabled())
        logger.info("Launching Process: " + processInstance.getName());

      eventMgr.launchProcess(processInstance.getProcessId(), processInstance.getMasterRequestId(), OwnerType.DOCUMENT, getOwningDocRef(processInstance, processVars).getDocumentId(), null, new Long(0), processVars);
      FacesVariableUtil.addMessage("Process \"" + processInstance.getName() + "\" successfully launched.");
      auditLogProcessLaunch(processInstance);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex.getMessage());
    }
  }

  /**
   * If the process instance does not already have a Master Request Id,
   * this method will attempt read it from a request parameter
   * (eg: a form element whose id is 'masterRequestId').
   */
  protected void launchProcess(MDWProcessInstance mdwProcessInstance) throws UIException
  {
    if (mdwProcessInstance.getMasterRequestId() == null || mdwProcessInstance.getMasterRequestId().trim().length() == 0)
    {
      String mrIdParam = (String) FacesVariableUtil.getRequestParamValue("masterRequestId");
      if (mrIdParam == null)
      {
        throw new UIException("Master Request ID is required.");
      }
      else
      {
        mdwProcessInstance.setMasterRequestId(mrIdParam);
      }
    }

    try
    {
      // process variables
      Map<String, Object> processVars = mdwProcessInstance.getVariables();

      // update documents
      EventManager eventMgr = RemoteLocator.getEventManager();
      for (UIDocument uiDoc : mdwProcessInstance.getDocuments().values())
      {
        eventMgr.updateDocumentContent(uiDoc.getDocumentVO().getDocumentId(), uiDoc.getObject(), uiDoc.getType());
      }

      // launch the process
      if (logger.isInfoEnabled())
        logger.info("Launching Process: " + mdwProcessInstance.getProcessName());

      eventMgr.launchProcess(mdwProcessInstance.getProcessId(), mdwProcessInstance.getMasterRequestId(), OwnerType.DOCUMENT, getOwningDocRef(mdwProcessInstance, processVars).getDocumentId(), null, new Long(0), processVars);

      mdwProcessInstance.setStarted(true);
      auditLogProcessLaunch(mdwProcessInstance);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException(ex.getMessage(), ex);
    }
  }

  protected DocumentReference createDocument(String docType, Object document)
  {
    try
    {
      // TODO owner id for portlet
      HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
      Long ownerId = new Long(request.getSession().getCreationTime());
      EventManager eventManager = RemoteLocator.getEventManager();
      Long docid = eventManager.createDocument(docType, new Long(0), "Process Launch", ownerId, null, null, document);
      return new DocumentReference(docid, null);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex.getMessage());
      return null;
    }
  }

  protected DocumentReference getOwningDocRef(MDWProcessInstance processInstance, Map<String,Object> processVars)
  {
    ActionRequestDocument launchDoc = getLaunchProcessDoc(processInstance, processVars);
    return createDocument(XmlObject.class.getName(), launchDoc);
  }

  protected ActionRequestDocument getLaunchProcessDoc(MDWProcessInstance processInstance, Map<String,Object> processVars)
  {
    ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
    ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
    com.centurylink.mdw.service.Action action = actionRequest.addNewAction();
    action.setName("PerformInstanceLevelAction");

    Parameter parameter = action.addNewParameter();
    parameter.setName("mdw.WorkType");
    parameter.setStringValue(WorkType.PROCESS.toString());

    parameter = action.addNewParameter();
    parameter.setName("mdw.Action");
    parameter.setStringValue("Launch");

    parameter = action.addNewParameter();
    parameter.setName("mdw.DefinitionId");
    parameter.setStringValue(processInstance.getProcessId().toString());

    parameter = action.addNewParameter();
    parameter.setName("mdw.MasterRequestId");
    parameter.setStringValue(processInstance.getMasterRequestId());

    parameter = action.addNewParameter();
    parameter.setName("mdw.User");
    parameter.setStringValue(FacesVariableUtil.getCurrentUser().getCuid());

    for (String name : processVars.keySet())
    {
      parameter = actionRequestDoc.getActionRequest().getAction().addNewParameter();
      parameter.setName(name);
      Object value = processVars.get(name);
      if (value != null)
        parameter.setStringValue(value.toString());
    }

    return actionRequestDoc;
  }

  public void auditLogProcessLaunch(MDWProcessInstance processInstance)
  {
    try
    {
      String user = FacesVariableUtil.getCurrentUser().getCuid();
      UserActionVO userAction = new UserActionVO(user, Action.Run, Entity.Process, processInstance.getProcessId(), processInstance.getProcessInstanceVO().getProcessName());
      userAction.setSource("Task Manager");
      EventManager eventMgr = RemoteLocator.getEventManager();
      eventMgr.createAuditLog(userAction);
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
  }

}
