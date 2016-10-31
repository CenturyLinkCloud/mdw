/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.rest;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.JsonExport;
import com.centurylink.mdw.common.service.JsonExportable;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.User;

import io.swagger.annotations.Api;

@Api(hidden=true)
public abstract class JsonRestService extends RestService implements JsonService {

    public String getJson(JSONObject json, Map<String,String> headers) throws ServiceException {
        String path = headers.get(Listener.METAINFO_REQUEST_PATH);
        try {
            JSONObject response;
            if ("GET".equals(headers.get(Listener.METAINFO_HTTP_METHOD))) {
                // TODO separate auth for GET requests
                // But BE CAREFUL because Designer uses these services
                // and we have to figure out the auth credentials strategy.
                response = service(path, null, headers);
            }
            else {
                User user = authorize(path, json, headers);
                response = service(path, json, headers);
                auditLog(getUserAction(user, path, json, headers));
            }
            if (response == null)
                return null;
            else if (response.has(Jsonable.GENERIC_ARRAY))
                return response.getJSONArray(Jsonable.GENERIC_ARRAY).toString(2);
            else
                return response.toString(2);
        }
        catch (JSONException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    protected JSONObject service(String path, JSONObject content, Map<String,String> headers) throws ServiceException, JSONException {
        String method = headers.get(Listener.METAINFO_HTTP_METHOD);
        if ("GET".equals(method))
            return get(path, headers);
        if ("POST".equals(method))
            return post(path, content, headers);
        else if ("PUT".equals(method))
            return put(path, content, headers);
        else if ("DELETE".equals(method))
            return delete(path, content, headers);
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

    public String getText(Object requestObj, Map<String,String> metaInfo) throws ServiceException {
        throw new ServiceException(ServiceException.BAD_REQUEST, metaInfo.get(Listener.METAINFO_REQUEST_PATH) + " requires JSON content type");
    }

    protected void propagatePost(JSONObject content, Map<String,String> headers)
    throws ServiceException, IOException, JSONException {
        if (content.has("distributed"))
            content.remove("distributed");
        super.propagatePost(content.toString(2), headers);
    }

    @Override
    protected void validateResponse(String response) throws ServiceException {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject status = jsonObject.getJSONObject("status");
            int code = status.getInt("code");
            String message = status.getString("message");
            if (code != 0)
                throw new ServiceException(HTTP_500_INTERNAL_ERROR, "Propagation error: " + message);
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
}
