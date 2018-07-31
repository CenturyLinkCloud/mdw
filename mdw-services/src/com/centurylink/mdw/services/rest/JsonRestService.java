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
package com.centurylink.mdw.services.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.JsonExport;
import com.centurylink.mdw.model.JsonExportable;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.util.JsonUtil;

import io.swagger.annotations.Api;

public abstract class JsonRestService extends RestService implements JsonService {

    public String getJson(JSONObject json, Map<String,String> headers) throws ServiceException {
        String path = headers.get(Listener.METAINFO_REQUEST_PATH);
        if (path.startsWith("/api/") && !isApi())
            throw new ServiceException(Status.NOT_FOUND);
        try {
            JSONObject response;
            User user = authorize(path, json, headers);
            response = service(path, json, headers);
            if (user != null)
                auditLog(getUserAction(user, path, json, headers));
            if (response == null) {
                return getDefaultResponse(headers).toString(2);
            }
            else if (response.has(JsonArray.GENERIC_ARRAY)) {
                return response.getJSONArray(JsonArray.GENERIC_ARRAY).toString(2);
            }
            else {
                if (response.has("status")) {
                    String code = headers.get(Listener.METAINFO_HTTP_STATUS_CODE);
                    if (code == null || code.equals("0")) {
                        JSONObject status = response.optJSONObject("status");
                        if (status != null && status.has("code")) {
                            int setCode = status.optInt("code");
                            if (setCode != 0)
                                headers.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(setCode));
                        }

                    }
                }
                return response.toString(2);
            }
        }
        catch (JSONException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    protected JSONObject service(String path, JSONObject content, Map<String,String> headers) throws ServiceException, JSONException {
        String method = headers.get(Listener.METAINFO_HTTP_METHOD);
        if ("GET".equals(method))
            return get(path, headers);
        else if ("DELETE".equals(method))
            return delete(path, content, headers);
        else if (content == null) // content couldn't be parsed
            throw new ServiceException(ServiceException.BAD_REQUEST, "Malformed JSON in request body");

        if ("POST".equals(method))
            return post(path, content, headers);
        else if ("PUT".equals(method))
            return put(path, content, headers);
        else if ("PATCH".equals(method))
            return patch(path, content, headers);
        else
            throw new ServiceException(ServiceException.NOT_ALLOWED, method + " not implemented");
    }

    /**
     * Retrieve an existing entity or relationship.
     */
    @GET
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        throw new ServiceException(ServiceException.NOT_ALLOWED, "GET not implemented");
    }

    /**
     * Create a new entity or relationship.
     * Or perform other action requests that cannot be categorized into put or delete.
     */
    @POST
    public JSONObject post(String path, JSONObject content, Map<String,String> headers) throws ServiceException, JSONException {
        throw new ServiceException(ServiceException.NOT_ALLOWED, "POST not implemented");
    }

    /**
     * Update an existing entity with different data.
     */
    @PUT
    public JSONObject put(String path, JSONObject content, Map<String,String> headers) throws ServiceException, JSONException {
        throw new ServiceException(ServiceException.NOT_ALLOWED, "PUT not implemented");
    }

    /**
     * Delete an existing entity or relationship.
     */
    @DELETE
    public JSONObject delete(String path, JSONObject content, Map<String,String> headers) throws ServiceException, JSONException {
        throw new ServiceException(ServiceException.NOT_ALLOWED, "DELETE not implemented");
    }

    /**
     * Incrementally update an existing entity.
     */
    @PUT
    public JSONObject patch(String path, JSONObject content, Map<String,String> headers) throws ServiceException, JSONException {
        throw new ServiceException(ServiceException.NOT_ALLOWED, "PATCH not implemented");
    }

    public String getText(Object requestObj, Map<String,String> metaInfo) throws ServiceException {
        throw new ServiceException(ServiceException.BAD_REQUEST, metaInfo.get(Listener.METAINFO_REQUEST_PATH) + " requires JSON content type");
    }

    @Override
    protected void validateResponse(String response) throws ServiceException {
        try {
            JSONObject jsonObject = new JsonObject(response);
            JSONObject status = jsonObject.getJSONObject("status");
            int code = status.getInt("code");
            String message = status.getString("message");
            if (code >= 400)
                throw new ServiceException(code, "Propagation error: " + message);
        }
        catch (JSONException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    protected JsonExport getExporter(Map<String,String> headers) throws JSONException, ServiceException {
        String method = headers.get(Listener.METAINFO_HTTP_METHOD);
        if ("GET".equals(method)) {
            if (this instanceof JsonExportable) {
                authorizeExport(headers);
                // TODO headers.put("max", String.valueOf(Query.MAX_ALL));
                String path = headers.get(Listener.METAINFO_REQUEST_PATH);
                JSONObject response = service(path, null, headers);
                Jsonable jsonable = ((JsonExportable)this).toJsonable(getQuery(path, headers), response);
                return new JsonExport(jsonable);
            }
            else {
                throw new ServiceException(HTTP_404_NOT_FOUND, "Service not exportable");
            }
        }
        else {
            throw new ServiceException(HTTP_405_METHOD_NOT_ALLOWED, "Unsupported method: " + method);
        }
    }

    /**
     * Binary content must be Base64 encoded for API compatibility.
     */
    public String export(String downloadFormat, Map<String,String> headers) throws ServiceException {
        try {
            JsonExport exporter = getExporter(headers);
            if (Listener.DOWNLOAD_FORMAT_EXCEL.equals(downloadFormat))
                return exporter.exportXlsxBase64();
            else if (Listener.DOWNLOAD_FORMAT_ZIP.equals(downloadFormat))
                return exporter.exportZipBase64();
            else
                throw new ServiceException(HTTP_400_BAD_REQUEST, "Unsupported download format: " + downloadFormat);
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    /**
     * Helper method for invoking a service process.  Populates response headers from variable value.
     */
    protected JSONObject invokeServiceProcess(String name, Object request, String requestId,
            Map<String,Object> parameters, Map<String,String> headers) throws ServiceException {
        JSONObject responseJson;
        Map<String,String> responseHeaders = new HashMap<>();
        Object responseObject = ServiceLocator.getWorkflowServices().invokeServiceProcess(name,
                request, requestId, parameters, headers, responseHeaders);
        if (responseObject instanceof JSONObject)
            responseJson = (JSONObject) responseObject;
        else if (responseObject instanceof Jsonable)
            responseJson = ((Jsonable) responseObject).getJson();
        else
            throw new ServiceException(HTTP_500_INTERNAL_ERROR,
                    "Unsupported response type: " + (responseObject == null ? null : responseObject.getClass()));
        for (String key : responseHeaders.keySet())
            headers.put(key, responseHeaders.get(key));
        return responseJson;
    }

    protected void launchProcess(String name, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers) throws ServiceException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        long documentId = Long.parseLong(headers.get(Listener.METAINFO_DOCUMENT_ID));
        workflowServices.launchProcess(name, masterRequestId, OwnerType.DOCUMENT, documentId, parameters);
    }

    protected int notifyProcess(String packageName, String eventId, Map<String,String> headers) throws ServiceException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        return workflowServices.notify(PackageCache.getPackage(packageName), eventId, JsonUtil.getJson(headers));
    }

    protected int notifyProcess(String packageName, String eventId, String eventMessage) throws ServiceException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        return workflowServices.notify(PackageCache.getPackage(packageName), eventId, eventMessage);
    }

    /**
     * True if should expose via the /api/* path.  This path indicates public consumption,
     * versus /services/*, which is not included in swaggers and meant for internal use.
     */
    public boolean isApi() {
        return this.getClass().getAnnotation(Api.class) != null;
    }

    protected JSONObject getDefaultResponse(Map<String,String> headers) {
        String code = headers.get(Listener.METAINFO_HTTP_STATUS_CODE);
        if (code != null) {
            return StatusResponse.forCode(Integer.parseInt(code)).getJson();
        }
        else {
            return new StatusResponse(Status.OK).getJson();
        }
    }
}
