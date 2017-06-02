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
package com.centurylink.mdw.microservice;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.workflow.adapter.rest.RestServiceAdapter;

/**
 * REST adapter overridden to support microservices and
 * populate headers, response and serviceSummary (if it exists)
 */
public class MicroserviceRestAdapter extends RestServiceAdapter {

    public static final String JSON_RESPONSE_VARIABLE = "JSON Response Variable";
    public static final String LOCAL_TUNNEL_PROXY_PORT = "Local Tunnel Proxy Port";

    /**
     * Overridden to support local tunnel proxying.
     */
    @Override
    public Object openConnection() throws ConnectionException {
        try {
            String proxyPortAttr = getAttributeValueSmart(LOCAL_TUNNEL_PROXY_PORT);
            if (proxyPortAttr != null && !proxyPortAttr.isEmpty()) {
                try {
                    int port = Integer.parseInt(proxyPortAttr);
                    loginfo("Using local tunnel proxy port: " + port);
                    return super.openConnection("127.0.0.1", port);
                }
                catch (NumberFormatException ex) {
                    throw new PropertyException("Invalid integer for " + LOCAL_TUNNEL_PROXY_PORT
                            + ": " + proxyPortAttr);
                }
            }
        }
        catch (PropertyException ex) {
            throw new ConnectionException(-1, ex.getMessage(), ex);
        }

        return super.openConnection();
    }

    /**
     * Overridden to build JSON request headers.
     */
    @Override
    public Map<String, String> getRequestHeaders() {
        Map<String, String> requestHeaders = super.getRequestHeaders();
        if (requestHeaders == null)
            requestHeaders = new HashMap<String, String>();
        try {
            requestHeaders.put(Request.REQUEST_ID, getMasterRequestId());
            String httpMethod = getHttpMethod();
            if ("GET".equals(httpMethod))
                requestHeaders.put("Accept", "application/json");
            else
                requestHeaders.put("Content-Type", "application/json");
        }
        catch (ActivityException ex) {
            logexception(ex.getMessage(), ex);
        }
        return requestHeaders;
    }

    protected void populateResponseVariable(StatusResponse response)
            throws ActivityException, JSONException {
        Variable responseVariable = null;
        String responseVarName = getAttributeValue(JSON_RESPONSE_VARIABLE);
        if (responseVarName != null) {
            responseVariable = getProcessDefinition().getVariable(responseVarName);
            if (responseVariable == null)
                throw new ActivityException("No variable defined: " + responseVarName);
        }
        else {
            // default response variable name
            responseVariable = getProcessDefinition().getVariable("response");
        }

        if (responseVariable != null && VariableTranslator.getTranslator(getPackage(),
                responseVariable.getVariableType()) instanceof JsonTranslator) {
            if (Jsonable.class.getName().equals(responseVariable.getVariableType()))
                setVariableValue(responseVariable.getName(), response);
            else if (JSONObject.class.getName().equals(responseVariable.getVariableType()))
                setVariableValue(responseVariable.getName(), response.getJson());
            else
                throw new JSONException(
                        "Unrecognized JSON variable type: " + responseVariable.getVariableType());
        }
    }

    /**
     * Add request-id header
     *
     * @throws ActivityException
     */
    protected void populateResponseHeaders() throws AdapterException, ActivityException {
        Variable responseHeadersVar = getProcessDefinition().getVariable("responseHeaders");
        if (responseHeadersVar != null) {
            try {
                Map<String, String> responseHeaders = super.getResponseHeaders();
                if (responseHeaders == null)
                    responseHeaders = new HashMap<String, String>();
                responseHeaders.put(Request.REQUEST_ID, getMasterRequestId());
                setVariableValue("responseHeaders", responseHeaders);
            }
            catch (ActivityException ex) {
                throw new AdapterException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Populate response variable and serviceSummary
     * @return response Id
     */
    @Override
    protected Long logResponse(Response response) {
        Long responseId = super.logResponse(response);
        int code = response.getStatusCode() == null ? 0 : response.getStatusCode();
        Status status = new Status(code, response.getStatusMessage());
        // Ensure that we get the most up-to-date serviceSummary
        String[] outDocs = new String[1];
        outDocs[0] = ServiceSummary.SERVICE_SUMMARY;
        setOutputDocuments(outDocs);
        //
        try {
            populateResponseVariable(new StatusResponse(status));
            populateResponseHeaders();
            updateServiceSummary(status, responseId);
        }
        catch (Exception ex) {
            logexception(ex.getMessage(), ex);
        }

        return responseId;
    }

    @Override
    protected Long logRequest(String message) {
        Long requestId = super.logRequest(message);
        try {
            Variable requestIdVar = getProcessDefinition().getVariable("requestId");
            if (requestIdVar != null && Long.class.getName().equals(requestIdVar.getVariableType()))
                setParameterValue("requestId", requestId);
        }
        catch (ActivityException ex) {
            logexception(ex.getMessage(), ex);
        }
        return requestId;
    }

    public void updateServiceSummary(Status status, Long responseId)
            throws ActivityException, ServiceException, DataAccessException {

        ServiceSummary serviceSummary = getServiceSummary();
        if (serviceSummary != null) {
            String microservice = getMicroservice();
            List<Invocation> invocations = serviceSummary.getInvocations(microservice);
            if (invocations == null)
                throw new ActivityException("No invocations for: " + microservice);

            // Default to now, update real time from the request
            Date sentTime = Calendar.getInstance().getTime();

            // if last invocation does not have a response, add it there
            if (invocations.size() > 0
                    && invocations.get(invocations.size() - 1).getStatus() == null) {
                invocations.get(invocations.size() - 1).setStatus(status);
            }
            else {
                Invocation invocation = new Invocation(getRequestId(), status, sentTime,
                        responseId);
                serviceSummary.addInvocation(microservice, invocation);
            }

            setVariableValue(ServiceSummary.SERVICE_SUMMARY, serviceSummary);
            // Do any notifications
            notifyServiceSummaryUpdate(serviceSummary);
        }
    }

    /**
     * <p>
     * This is left to the implementor if any kind of notification
     * needs to be sent out whenever the service summary is updated
     * </p>
     * @param serviceSummary
     * @throws ServiceException
     * @throws DataAccessException
     */
    public void notifyServiceSummaryUpdate(ServiceSummary serviceSummary) {
    }

    /**
     * This returns the microservice that is to be updated in the serviceSummary
     * It defaults to the value of "packageName/processName".
     * In the case where this won't work (i.e we are in a deep subprocess)
     * it can be overriden by defining the value in the activity for "Microservice name"
     * @return the microservice to be updated in the serviceSummary
     */
    public String getMicroservice() {
        String microservice = getAttributeValue(ServiceSummary.MICROSERVICE);
        if (StringHelper.isEmpty(microservice))
            microservice = getPackage().getName() + "/" + getProcessDefinition().getName();
        microservice = getMicroservicePath(microservice);
        return microservice;
    }

    /**
     * Looks up and returns the ServiceSummary variable value
     * @return ServiceSummary object
     * @throws ActivityException
     */
    public ServiceSummary getServiceSummary() throws ActivityException {
        ServiceSummary serviceSummary = (ServiceSummary) getVariableValue(
                ServiceSummary.SERVICE_SUMMARY);
        if (serviceSummary == null) {
            logger.severe(ServiceSummary.SERVICE_SUMMARY + " not found");
            return null;
        }
        else {
            return serviceSummary;
        }
    }

    /**
     * Returns the requestId that we will use to populate the serviceSummary
     * @return requestId used to populate the serviceSummary
     * @throws ActivityException
     */
    public Long getRequestId() throws ActivityException {

        String requestIdVarName = getAttributeValue(ServiceSummary.REQUEST_ID_VAR);
        if (requestIdVarName == null)
            requestIdVarName = ServiceSummary.DEFAULT_REQUEST_ID_VAR;

        Variable requestIdVar = getProcessDefinition().getVariable(requestIdVarName);
        if (requestIdVar == null && !"GET".equals(getHttpMethod()))
            throw new ActivityException("Request ID variable not defined: " + requestIdVarName);

        Object requestIdObj = getVariableValue(requestIdVarName);
        if (requestIdObj == null)
            return null;

        if (requestIdObj instanceof Long) {
            return (Long) requestIdObj;
        }
        else {
            try {
                return Long.valueOf(requestIdObj.toString());
            }
            catch (NumberFormatException ex) {
                throw new ActivityException(
                        "Invalid value for " + requestIdVarName + ": " + requestIdObj);
            }
        }
    }

    /**
     * Overrides the standard functionality to update the serviceSummary
     * with a 500 response for any errors
     *
     */
    @Override
    public String invoke(Object conn, String request, int timeout, Map<String, String> headers)
            throws ConnectionException, AdapterException {
        HttpHelper httpHelper = null;
        String httpMethod = null;
        try {
            httpHelper = getHttpHelper(conn);

            int connectTimeout = getConnectTimeout();
            if (connectTimeout > 0)
                httpHelper.setConnectTimeout(connectTimeout);

            int readTimeout = getReadTimeout();
            if (readTimeout > 0)
                httpHelper.setReadTimeout(readTimeout);

            if (headers != null)
                httpHelper.setHeaders(headers);

            httpMethod = getHttpMethod();
            if (httpMethod.equals("GET"))
                return httpHelper.get();
            else if (httpMethod.equals("POST"))
                return httpHelper.post(request);
            else if (httpMethod.equals("PUT"))
                return httpHelper.put(request);
            else if (httpMethod.equals("PATCH"))
                return httpHelper.patch(request);
            else if (httpMethod.equals("DELETE"))
                return httpHelper.delete();
            else
                throw new AdapterException("Unsupported HTTP Method: " + httpMethod);
        }
        catch (IOException ex) {
            int responseCode = httpHelper.getResponseCode();
            String responseMessage = httpHelper.getResponseMessage();
            if (httpHelper != null) {
                if (httpHelper.getResponse() != null) {
                    logResponse(httpHelper.getResponse());
                }
                else {
                    // Make sure that we log a 500 and a message in the service
                    // summary
                    Response response = new Response();
                    response.setStatusCode(Status.INTERNAL_ERROR.getCode());
                    response.setStatusMessage(
                            getAdapterInvocationErrorMessage() + ":" + ex.getMessage());
                    response.setContent("{}");
                    logResponse(response);
                }
            }
            // Deal with 500 response
            if (responseCode >= Status.INTERNAL_ERROR.getCode() || responseCode <= 0) {
                /**
                 * Plugs into automatic retrying
                 */
                logexception(ex.getMessage(), ex);
                throw new ConnectionException(-1, ex.getMessage(), ex);
            }
            else if (responseCode >= Status.INTERNAL_ERROR.getCode()) {
                // between 200-> 500, log it and continue
                logdebug(httpMethod + "-Received code:" + responseCode + " message:"
                        + responseMessage + "...continuing");
            }
            return httpHelper.getResponse();
        }
        catch (Exception ex) {
            int responseCode = -1;
            if (httpHelper != null)
                responseCode = httpHelper.getResponseCode();
            throw new AdapterException(responseCode, ex.getMessage(), ex);
        }
        finally {
            if (httpHelper != null)
                setResponseHeaders(httpHelper.getHeaders());
        }
    }
    /**
     * Trim microservice subprocess per naming convention
     */
    public String getMicroservicePath(String assetPath) {
        AssetVersionSpec spec = AssetVersionSpec.parse(assetPath);
        return spec.getPackageName().substring(spec.getPackageName().lastIndexOf('.') + 1) + "/" + spec.getName();
    }

}
