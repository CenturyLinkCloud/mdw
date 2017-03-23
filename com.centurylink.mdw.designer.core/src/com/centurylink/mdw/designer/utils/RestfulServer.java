/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.designer.utils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.designer.DesignerCompatibility;
import com.centurylink.mdw.designer.display.DesignerDataModel;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.ApplicationSummaryDocument;
import com.centurylink.mdw.service.Parameter;

/**
 * For communicating between Designer and the MDW Server.
 */
public class RestfulServer extends Server {

    public RestfulServer(String database, String user, String mdwWebUrl) {
        super(database, user);
        setMdwWebUrl(mdwWebUrl);
    }

    public RestfulServer(String database, String user, String mdwWebUrl, int schemaVersion) {
        super(database, user, schemaVersion);
        setMdwWebUrl(mdwWebUrl);
    }

    public RestfulServer(RestfulServer cloneFrom) {
        super(cloneFrom);
    }

    private DesignerDataModel dataModel;
    public DesignerDataModel getDataModel() { return dataModel; }
    public void setDataModel(DesignerDataModel dm) { this.dataModel = dm; }

    public MDWStatusMessageDocument launchProcess(Long processId, String masterRequestId, String owner, Long ownerId, Map<VariableVO,String> variables, Long activityId, boolean oldFormat)
    throws DataAccessException, RemoteException, XmlException {
        ActionRequestDocument actionRequestDoc = getLaunchProcessBaseDoc(processId, masterRequestId, owner, ownerId);

        for (VariableVO variableVO : variables.keySet()) {
            Parameter parameter = actionRequestDoc.getActionRequest().getAction().addNewParameter();
            parameter.setName(variableVO.getVariableName());
            parameter.setType(variableVO.getVariableType());
            String stringValue = variables.get(variableVO);
            parameter.setStringValue(stringValue);
        }

        if (activityId != null) {
            Parameter parameter = actionRequestDoc.getActionRequest().getAction().addNewParameter();
            parameter.setName("mdw.ActivityId");
            parameter.setStringValue(activityId.toString());
        }

        String request;
        if (oldFormat)
            request = DesignerCompatibility.getInstance().getOldActionRequest(actionRequestDoc);
        else
            request = actionRequestDoc.xmlText(getXmlOptions());

        return invokeService(request);
    }

    public String launchSynchronousProcess(Long processId, String masterRequestId, String owner, Long ownerId, Map<VariableVO,String> variables, String responseVarName, boolean oldFormat)
    throws DataAccessException, RemoteException, XmlException {
        ActionRequestDocument actionRequestDoc = getLaunchProcessBaseDoc(processId, masterRequestId, owner, ownerId);
        Parameter syncParam = actionRequestDoc.getActionRequest().getAction().addNewParameter();
        syncParam.setName("mdw.Synchronous");
        syncParam.setStringValue("true");
        Parameter responseVarParam = actionRequestDoc.getActionRequest().getAction().addNewParameter();
        responseVarParam.setName("mdw.ResponseVariableName");
        responseVarParam.setStringValue(responseVarName);

        for (VariableVO variableVO : variables.keySet()) {
          Parameter parameter = actionRequestDoc.getActionRequest().getAction().addNewParameter();
          parameter.setName(variableVO.getVariableName());
          parameter.setType(variableVO.getVariableType());
          String stringValue = variables.get(variableVO);
          parameter.setStringValue(stringValue);
        }

        try {
            HttpHelper httpHelper = new HttpHelper(new URL(getMdwWebUrl() + "/Services/REST"));
            httpHelper.setConnectTimeout(getConnectTimeout());
            httpHelper.setReadTimeout(getReadTimeout());
            String request;
            if (oldFormat)
                request = DesignerCompatibility.getInstance().getOldActionRequest(actionRequestDoc);
            else
                request = actionRequestDoc.xmlText(getXmlOptions());
            return httpHelper.post(request);
        }
        catch (SocketTimeoutException ex) {
            throw new RemoteException("Timeout after " + getReadTimeout() + " ms", ex);
        }
        catch (IOException ex) {
            throw new RemoteException("Unable to connect to " + getMdwWebUrl(), ex);
        }
    }

    public String retryActivityInstance(Long activityId, Long activityInstanceId, boolean oldFormat)
    throws DataAccessException, RemoteException, XmlException {
    	String request = buildRetryActivityInstanceRequest(activityId, activityInstanceId, oldFormat);
        MDWStatusMessageDocument statusMsgDoc = invokeService(request);
        if (statusMsgDoc.getMDWStatusMessage().getStatusCode() == 0)
            return "";  // indicates success
        else
            return statusMsgDoc.getMDWStatusMessage().getStatusMessage();
    }

    public String skipActivityInstance(Long activityId, Long activityInstanceId, String completionCode, boolean oldFormat)
    throws DataAccessException, RemoteException, XmlException {
    	String request = buildSkipActivityInstanceRequest(activityId, activityInstanceId, completionCode, oldFormat);
        MDWStatusMessageDocument statusMsgDoc = invokeService(request);
        if (statusMsgDoc.getMDWStatusMessage().getStatusCode() == 0)
            return "";  // indicates success
        else
            return statusMsgDoc.getMDWStatusMessage().getStatusMessage();
    }

    /**
     * Refreshes both properties and caches.
     */
    public MDWStatusMessageDocument refreshCache(boolean global, boolean oldFormat, boolean includeDynamicJava)
    throws DataAccessException, RemoteException, XmlException {
        return refreshCache(null, null, global, oldFormat, includeDynamicJava);
    }

    public MDWStatusMessageDocument refreshCache(String type, String cacheName, boolean global, boolean oldFormat, boolean includeDynamicJava)
    throws DataAccessException, RemoteException, XmlException {
        ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
        ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
        Action action = actionRequest.addNewAction();
        action.setName("RefreshCache");
        if (type != null) {
            Parameter typeParam = action.addNewParameter();
            typeParam.setName("RefreshType");
            typeParam.setStringValue(type);
        }
        if (cacheName != null) {
            Parameter cacheNameParam = action.addNewParameter();
            cacheNameParam.setName("CacheName");
            cacheNameParam.setStringValue(cacheName);
        }
        if (global) {
            Parameter parameter = action.addNewParameter();
            parameter.setName("GlobalRefresh");
            parameter.setStringValue("true");
        }
        if (!includeDynamicJava) {
            Parameter parameter = action.addNewParameter();
            parameter.setName("ExcludedFormats");
            parameter.setStringValue(RuleSetVO.JAVA);
        }

        String request;
        if (oldFormat)
            request = DesignerCompatibility.getInstance().getOldActionRequest(actionRequestDoc);
        else
            request = actionRequestDoc.xmlText(getXmlOptions());
        return invokeService(request);
    }

    public MDWStatusMessageDocument stubServer(String host, int port, boolean on, boolean oldFormat)
    throws DataAccessException, RemoteException, XmlException {
        ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
        ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
        Action action = actionRequest.addNewAction();
        action.setName("RegressionTest");
        Parameter param = action.addNewParameter();
        param.setName("Maintenance");
        param.setStringValue("Stubbing");
        param = action.addNewParameter();
        param.setName("Server");
        param.setStringValue(host + ":" + port);
        param = action.addNewParameter();
        param.setName("Mode");
        param.setStringValue(on ? "on" : "off");

        String request;
        if (oldFormat)
            request = DesignerCompatibility.getInstance().getOldActionRequest(actionRequestDoc);
        else
            request = actionRequestDoc.xmlText(getXmlOptions());
        return invokeService(request);
    }

    public MDWStatusMessageDocument invokeService(String request) throws DataAccessException, RemoteException {
        return invokeService(request, null, null);
    }

    public String post(String path, String request) throws IOException {
        return post(path, request, null, null);
    }

    public String post(String path, String request, String user, String password) throws IOException {
        String url = getServiceUrl();
        if (path != null)
            url += "/" + path;
        HttpHelper httpHelper = new HttpHelper(new URL(url), user, password);
        httpHelper.setConnectTimeout(getConnectTimeout());
        httpHelper.setReadTimeout(getReadTimeout());
        return httpHelper.post(request);
    }

    public String delete(String path, String request) throws IOException {
        return delete(path, request, null, null);
    }

    public String delete(String path, String request, String user, String password) throws IOException {
        String url = getServiceUrl();
        if (path != null)
            url += "/" + path;
        HttpHelper httpHelper = new HttpHelper(new URL(url), user, password);
        httpHelper.setConnectTimeout(getConnectTimeout());
        httpHelper.setReadTimeout(getReadTimeout());
        return httpHelper.delete(request);
    }

    public MDWStatusMessageDocument invokeService(String request, String user, String password)
    throws DataAccessException, RemoteException {
        String response = null;
        try {
            // append to Services context root since sometimes only Services/* are excluded from CT auth
            HttpHelper httpHelper = new HttpHelper(new URL(getMdwWebUrl() + "/Services/REST"), user, password);
            httpHelper.setConnectTimeout(getConnectTimeout());
            httpHelper.setReadTimeout(getReadTimeout());
            response = httpHelper.post(request);
            MDWStatusMessageDocument statusMessageDoc;
            if (response.startsWith("{")) {
                StatusMessage statusMessage = new StatusMessage(new JSONObject(response));
                statusMessageDoc = statusMessage.getStatusDocument();
            }
            else {
                statusMessageDoc = MDWStatusMessageDocument.Factory.parse(response, Compatibility.namespaceOptions());
            }
            MDWStatusMessage statusMessage = statusMessageDoc.getMDWStatusMessage();
            if (statusMessage.getStatusCode() == -3) {
                // event handler not registered
                throw new RemoteException("No event handler is registered for instance-level actions on: " + getMdwWebUrl());
            }
            else if (statusMessage.getStatusCode() != 0) {
                throw new RemoteException("Error response from server: " + statusMessage.getStatusMessage());
            }
            return statusMessageDoc;
        }
        catch (RemoteException ex) {
            throw ex;  // don't fall through to IOException catch block
        }
        catch (SocketTimeoutException ex) {
            throw new DataAccessOfflineException("Timeout after " + getReadTimeout() + " ms", ex);
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException("Unable to connect to " + getMdwWebUrl(), ex);
        }
        catch (JSONException ex) {
            throw new DataAccessException("Unparsable JSON response:\n" + response);
        }
        catch (XmlException ex) {
            throw new DataAccessException("Unparsable XML response:\n" + response);
        }
    }

    public ApplicationSummaryDocument getAppSummary()
    throws IOException, XmlException {
        String url = getMdwWebUrl() + "/Services/GetAppSummary?format=xml";
        HttpHelper httpHelper = new HttpHelper(new URL(url));
        httpHelper.setConnectTimeout(getConnectTimeout());
        httpHelper.setReadTimeout(getReadTimeout());
        String response = httpHelper.get();
        if (response != null && (response.trim().startsWith("<xs:MDWStatusMessage") || response.trim().startsWith("<bpm:MDWStatusMessage"))) {
          MDWStatusMessageDocument msgDoc = MDWStatusMessageDocument.Factory.parse(response, Compatibility.namespaceOptions());
          throw new IOException("Server error: " + msgDoc.getMDWStatusMessage().getStatusMessage());
        }
        return ApplicationSummaryDocument.Factory.parse(response, Compatibility.namespaceOptions());
    }

    public String invokeResourceService(String path) throws DataAccessException, IOException {
        String url = getMdwWebUrl() + (path.startsWith("/") ? "Services/" + path : "/Services/" + path);
        String response = null;
        try {
            HttpHelper httpHelper = new HttpHelper(new URL(url));
            httpHelper.setConnectTimeout(getConnectTimeout());
            httpHelper.setReadTimeout(getReadTimeout());
            response = httpHelper.get();
        }
        catch (SocketTimeoutException ex) {
            throw new IOException("Timeout after " + getReadTimeout() + " ms", ex);
        }
        catch (IOException ex) {
            throw new IOException("Unable to connect to " + getMdwWebUrl(), ex);
        }
        if (response != null && (response.trim().startsWith("<xs:MDWStatusMessage") || response.trim().startsWith("<bpm:MDWStatusMessage"))) {
            try {
              MDWStatusMessageDocument msgDoc = MDWStatusMessageDocument.Factory.parse(response, Compatibility.namespaceOptions());
              throw new DataAccessException(msgDoc.getMDWStatusMessage().getStatusMessage());
            }
            catch (Exception ex) {
                throw new DataAccessException(-1, response, ex);
            }
        }
        return response;
    }

    /**
     * Used for retrieving document values (does not interpret response at all).
     */
    public String invokeResourceServiceRaw(String path) throws DataAccessException, IOException {
        String url = getMdwWebUrl() + (path.startsWith("/") ? "Services/" + path : "/Services/" + path);
        try {
            HttpHelper httpHelper = new HttpHelper(new URL(url));
            httpHelper.setConnectTimeout(getConnectTimeout());
            httpHelper.setReadTimeout(getReadTimeout());
            return httpHelper.get();
        }
        catch (SocketTimeoutException ex) {
            throw new IOException("Timeout after " + getReadTimeout() + " ms", ex);
        }
        catch (IOException ex) {
            throw new IOException("Unable to connect to " + getMdwWebUrl(), ex);
        }
    }
}
