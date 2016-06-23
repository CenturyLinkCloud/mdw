/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.phase;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.jms.JMSException;

import org.apache.myfaces.shared_tomahawk.renderkit.RendererUtils;
import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.JMSServices;
import com.centurylink.mdw.common.utilities.form.CallURL;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.task.EngineAccess;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWDataAccess;
import com.centurylink.mdw.web.ui.UIDocument;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.qwest.mbeng.MbengException;

public class EngineCallbackPhaseListener implements PhaseListener
{
  private static final long serialVersionUID = 1L;
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public PhaseId getPhaseId()
  {
    return PhaseId.INVOKE_APPLICATION;
  }

  public void beforePhase(PhaseEvent event)
  {
    FacesContext facesContext = event.getFacesContext();
    ExternalContext externalContext = facesContext.getExternalContext();
    Map<String,String> parameters = externalContext.getRequestParameterMap();

    if (parameters.get("mainDetailForm:actionForm:taskAction_go") != null
        || parameters.get("mainDetailForm:taskDetailSave") != null
        || parameters.get("mainDetailForm:taskComplete") != null)
      return; // normal task action processing or save

    // bypass callback for designated param naming convention (ending with "bypassCallback")
    for (String paramName : parameters.keySet())
    {
      if (paramName.endsWith("bypassCallback") && parameters.get(paramName) != null)
        return;
    }
        
    String actionSequence = getActionSequence(externalContext);

    if (actionSequence != null)
    {
      FormDataDocument formDataDoc = (FormDataDocument) FacesVariableUtil.getValue("formDataDocument");
      try
      {
    	  CallURL parser = new CallURL(actionSequence);
    	  Map<String,String> actionParams = parser.getParameters();

          String procInstId = formDataDoc.getMetaValue(FormDataDocument.META_PROCESS_INSTANCE_ID);
            
          formDataDoc.setAttribute(FormDataDocument.ATTR_ACTION, actionSequence);
          formDataDoc.setMetaValue(FormDataDocument.META_AUTOBIND, "true");
          for (String param : actionParams.keySet()) {
        	  if (param.endsWith(FormConstants.URLARG_TIMEOUT)) continue;
        	  if (param.endsWith("timeoutMessage")) continue;
        	  formDataDoc.setMetaValue(param, actionParams.get(param));
          }
          formDataDoc.clearErrors();          
          populateDocWithFormValues(formDataDoc);
          String timeoutString = actionParams.get(FormConstants.URLARG_TIMEOUT);
          int timeoutSeconds = timeoutString == null ? 120 : Integer.parseInt(timeoutString);

          FormDataDocument responseDataDoc = null;
          try
          {
            responseDataDoc = (new EngineAccess()).callEngine(formDataDoc, timeoutSeconds, null);
          }
          catch (JMSException ex)
          {
            logger.severeException(ex.getMessage(), ex);
            if (JMSServices.ERROR_CODE_TIMEOUT.equals(ex.getErrorCode()))
            {
              String timeoutMessage = actionParams.get("timeoutMessage");
              if (timeoutMessage == null)
                timeoutMessage = "Request has timed out after " + timeoutSeconds + " seconds.  Please retry.";
              FacesVariableUtil.addMessage(timeoutMessage);
            }
          }

          // refresh the process instance managed bean
          if (procInstId != null)
            updateProcessInstanceManagedBean(new Long(procInstId));          
          
          if (responseDataDoc != null)
          {
            List<String> errors = responseDataDoc.getErrors();
            if (!errors.isEmpty())
            {
              for (String error : errors)
              {
                FacesVariableUtil.addMessage(error);
              }
            }
            Long taskInstId = responseDataDoc.getTaskInstanceId();
            if (taskInstId != null && errors.isEmpty())
            {
            	TaskManager taskManager = RemoteLocator.getTaskManager();
                TaskInstanceVO taskInst = taskManager.getTaskInstanceVO(taskInstId);
                EventManager eventManager = RemoteLocator.getEventManager();
                eventManager.updateDocumentContent(taskInst.getSecondaryOwnerId(),
                		responseDataDoc, FormDataDocument.class.getName());
            }

            if (errors.isEmpty())
            	FacesVariableUtil.setValue("formDataDocument", responseDataDoc);  // includes new form name if set
          }
      }
      catch (Exception ex)
      {
        FacesVariableUtil.addMessage(ex.getMessage());
        logger.severeException(ex.getMessage(), ex);
      }
    }
  }

  public void afterPhase(PhaseEvent event)
  {
    // do nothing
  }

  private String getActionSequence(ExternalContext externalContext)
  {
    List<?> actionForList = (List<?>) externalContext.getRequestMap().get(RendererUtils.ACTION_FOR_LIST);
    Map<String,String> parameters = externalContext.getRequestParameterMap();

    String bestMatch = null;
    for (String paramName : parameters.keySet())
    {
      if (paramName.equals("hiddenAction"))
        return parameters.get(paramName);
      else if (paramName.endsWith("hiddenAction"))
      {
        if (actionForList == null)
          return parameters.get(paramName);
        else
        {
          for (Object actionFor : actionForList)
          {
            if (actionFor.equals("mainDetailForm:detailForm") && paramName.indexOf(":detailForm:") == -1)
              bestMatch = parameters.get(paramName); // buttons outside the detail form take precedence when submitted with actionFor=detailForm
            else if (paramName.indexOf(actionFor.toString()) >= 0 && bestMatch == null)
              bestMatch = parameters.get(paramName);
          }
        }
      }
    }
    return bestMatch;
  }

  public void populateDocWithFormValues(FormDataDocument formDataDoc) throws MbengException, UIException, XmlException
  {
    MDWProcessInstance processInstance = (MDWProcessInstance) FacesVariableUtil.getValue("process");
    for (String var : processInstance.getDirtyVariables())
    {
      Object objValue = processInstance.getVariables().get(var);
      String strValue = objValue == null ? "<EMPTY>" : objValue.toString();
      if (objValue instanceof DocumentReference)
      {
        UIDocument uiDoc = processInstance.getDocument((DocumentReference)objValue);
        strValue = uiDoc.getDocumentVO().getContent();
      }
      formDataDoc.setValue(var, strValue);
    }
  }

  public void updateProcessInstanceManagedBean(Long processInstanceId) throws SQLException, DataAccessException
  {
    // set the process instance managed bean
    MDWProcessInstance processInstance = (MDWProcessInstance) FacesVariableUtil.getValue("process");
    RuntimeDataAccess runtimeDataAccess = ((MDWDataAccess)FacesVariableUtil.getValue("dataAccess")).getRuntimeDataAccess();
    ProcessInstanceVO processInstanceVO = runtimeDataAccess.getProcessInstanceAll(processInstanceId);
    if (processInstanceVO.isNewEmbedded() || ProcessVOCache.getProcessVO(processInstanceVO.getProcessId()).isEmbeddedProcess())
      processInstanceVO = runtimeDataAccess.getProcessInstanceAll(processInstanceVO.getOwnerId());
    
    processInstance.setProcessInstanceVO(processInstanceVO);
  }

}
