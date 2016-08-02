/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.bpm.WorkTypeDocument.WorkType;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.ActivityResultCodeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.designer.DesignerCompatibility;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;

public class Server {

    public static final int DEFAULT_CONNECT_TIMEOUT = 10000;
    public static final int DEFAULT_READ_TIMEOUT = 30000;

    private String name;
    private String applicationName;
    private String databaseUrl;
    private VersionControl versionControl;
    private File rootDirectory;
    private String engineUrl;
    private String environment; // dev, test, prod, etc
    private String taskManagerUrl;
    private String mdwWebUrl;
    private String user;

    public Server() {
    }

    public Server(String databaseUrl, String user) {
        this.databaseUrl = databaseUrl;
        this.user = user;
    }
    public Server(Server copy) {
        name = copy.name;
        applicationName = copy.applicationName;
        databaseUrl = copy.databaseUrl;
        rootDirectory = copy.rootDirectory;
        versionControl = copy.versionControl;
        engineUrl = copy.engineUrl;
        environment = copy.environment;
        taskManagerUrl = copy.taskManagerUrl;
        mdwWebUrl = copy.mdwWebUrl;
        user = copy.user;
    }

    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    public int getConnectTimeout() { return this.connectTimeout; }
    public void setConnectTimeout(int ms) {
        this.connectTimeout = ms;
    }

    private int readTimeout = DEFAULT_READ_TIMEOUT;
    public int getReadTimeout() { return this.readTimeout; }
    public void setReadTimeout(int ms) {
        this.readTimeout = ms;
    }

    public String getApplicationName() {
        return applicationName;
    }
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    public String getDatabaseUrl() {
        return databaseUrl;
    }
    public void setDatabaseUrl(String databaseUrl) {
        if (databaseUrl==null||databaseUrl.length()==0) this.databaseUrl = null;
        else this.databaseUrl = databaseUrl;
    }

    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public VersionControl getVersionControl() {
        return versionControl;
    }
    public void setVersionControl(VersionControl versionControl) {
        this.versionControl = versionControl;
    }

    public String getEngineUrl() {
        return engineUrl;
    }
    public void setEngineUrl(String engineUrl) {
        if (engineUrl==null||engineUrl.length()==0) this.engineUrl = null;
        else this.engineUrl = engineUrl;
    }
    public String getEnvironment() {
        return environment;
    }
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    public String getTaskManagerUrl() {
        return taskManagerUrl;
    }
    public void setTaskManagerUrl(String taskManagerUrl) {
        this.taskManagerUrl = taskManagerUrl;
    }
    public String getMdwWebUrl() {
        return mdwWebUrl;
    }
    public void setMdwWebUrl(String mdwWebUrl) {
        this.mdwWebUrl = mdwWebUrl;
    }
    public String getServiceUrl() {
        return getMdwWebUrl() + "/Services";
    }
    public String getName() {
        if (name!=null) {
            return name;
        } else if (engineUrl!=null) {
             return engineUrl.toLowerCase();
        } else if (mdwWebUrl!=null) {
             int k = mdwWebUrl.lastIndexOf('/');
            return k>0?mdwWebUrl.substring(0,k).toLowerCase():mdwWebUrl.toLowerCase();
        } else if (databaseUrl!=null) {
             int k = databaseUrl.lastIndexOf(':');
             return k>0?databaseUrl.substring(k+1).toUpperCase():databaseUrl;
        } else return null;
    }
    public String getRealName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setServerUrl(String serverUrl) {
        if (serverUrl==null||serverUrl.length()==0) {
            engineUrl = null;
            mdwWebUrl = null;
        } else if (serverUrl.startsWith("http")) {
            engineUrl = null;
            mdwWebUrl = serverUrl;
        } else {
            engineUrl = serverUrl;
            mdwWebUrl = null;
        }
    }
    public String getServerUrl() {
        if (engineUrl!=null) return engineUrl;
        else return mdwWebUrl;
    }

    public String buildRetryActivityInstanceRequest(Long activityId, Long activityInstanceId, boolean oldFormat)
    throws XmlException {
        return buildActionOnActivityInstanceRequest(activityId, activityInstanceId, ActivityResultCodeConstant.RESULT_RETRY, null, oldFormat);
    }

    public String buildSkipActivityInstanceRequest(Long activityId, Long activityInstanceId, String completionCode, boolean oldFormat)
    throws XmlException {
        return buildActionOnActivityInstanceRequest(activityId, activityInstanceId, ActivityResultCodeConstant.RESULT_SKIP, completionCode, oldFormat);
    }

    private String buildActionOnActivityInstanceRequest(Long activityId, Long activityInstanceId, String actionType, String completionCode, boolean oldFormat)
    throws XmlException
    {
        ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
        ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
        Action action = actionRequest.addNewAction();
        action.setName("PerformInstanceLevelAction");

        Parameter parameter = action.addNewParameter();
        parameter.setName("mdw.WorkType");
        parameter.setStringValue(WorkType.ACTIVITY.toString());

        parameter = action.addNewParameter();
        parameter.setName("mdw.Action");
        parameter.setStringValue(actionType);

        parameter = action.addNewParameter();
        parameter.setName("mdw.DefinitionId");
        parameter.setStringValue(activityId.toString());

        parameter = action.addNewParameter();
        parameter.setName("mdw.InstanceId");
        parameter.setStringValue(activityInstanceId.toString());

        if (completionCode != null) {
            parameter = action.addNewParameter();
            parameter.setName("mdw.CompletionCode");
            parameter.setStringValue(completionCode);
        }

        String request;
        if (oldFormat)
            request = DesignerCompatibility.getInstance().getOldActionRequest(actionRequestDoc);
        else
            request = actionRequestDoc.xmlText(getXmlOptions());

        return request;
    }

    public String getErrorMessageFromResponse(String response) throws XmlException {
        MDWStatusMessageDocument statusMessageDoc = MDWStatusMessageDocument.Factory.parse(response, Compatibility.namespaceOptions());
        MDWStatusMessage statusMessage = statusMessageDoc.getMDWStatusMessage();
        if (statusMessage.getStatusCode() == 0) return "";    // indicate success
        else return statusMessage.getStatusMessage();
    }

    protected ActionRequestDocument getLaunchProcessBaseDoc(Long processId, String masterRequestId, String owner, Long ownerId) {
        ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
        ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
        Action action = actionRequest.addNewAction();
        action.setName("PerformInstanceLevelAction");

        Parameter parameter = action.addNewParameter();
        parameter.setName("mdw.WorkType");
        parameter.setStringValue(WorkType.PROCESS.toString());

        parameter = action.addNewParameter();
        parameter.setName("mdw.Action");
        parameter.setStringValue("Launch");

        parameter = action.addNewParameter();
        parameter.setName("mdw.DefinitionId");
        parameter.setStringValue(processId.toString());

        parameter = action.addNewParameter();
        parameter.setName("mdw.MasterRequestId");
        parameter.setStringValue(masterRequestId);

        parameter = action.addNewParameter();
        parameter.setName("mdw.Owner");
        parameter.setStringValue(owner);

        parameter = action.addNewParameter();
        parameter.setName("mdw.OwnerId");
        if (ownerId == null)
            ownerId = new Date().getTime();
        parameter.setStringValue(ownerId.toString());
        return actionRequestDoc;
    }
    public String buildLaunchProcessRequest(ProcessVO procdef, String masterRequestId,
            Long activityId, Map<String,String> parameters, boolean isServiceProcess, boolean oldFormat)
    throws DataAccessException, RemoteException, XmlException {
        ActionRequestDocument actionRequestDoc = getLaunchProcessBaseDoc(procdef.getProcessId(), masterRequestId, "Designer", new Date().getTime());
        Action action = actionRequestDoc.getActionRequest().getAction();
        if (activityId!=null) {
            Parameter parameter = action.addNewParameter();
            parameter.setName("mdw.ActivityId");
            parameter.setStringValue(activityId.toString());
        }
        if (isServiceProcess) {
             Parameter syncParam = actionRequestDoc.getActionRequest().getAction().addNewParameter();
             syncParam.setName("mdw.Synchronous");
             syncParam.setStringValue("true");
//             Parameter responseVarParam = actionRequestDoc.getActionRequest().getAction().addNewParameter();
//             responseVarParam.setName("mdw.ResponseVariableName");
//             responseVarParam.setStringValue("response");
        }
        for (String processParam : parameters.keySet()) {
            Parameter parameter = action.addNewParameter();
            parameter.setName(processParam);
            if (procdef != null)
            {
              VariableVO varVO = procdef.getVariable(processParam);
              if (varVO != null)
                parameter.setType(varVO.getVariableType());
            }
            parameter.setStringValue(parameters.get(processParam));
        }
        if (oldFormat)
            return DesignerCompatibility.getInstance().getOldActionRequest(actionRequestDoc);
        else
            return actionRequestDoc.xmlText(getXmlOptions());
    }

    protected XmlOptions getXmlOptions() {
        return new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
    }

    private DataAccessOfflineException serverOfflineException;
    public DataAccessOfflineException getServerOfflineException() { return serverOfflineException; }
    public void setServerOfflineException(DataAccessOfflineException ex) { serverOfflineException = ex; }

    /**
     * Override this in any extending class.
     */
    public boolean isOnline() throws DataAccessException {
        return false;
    }
}
