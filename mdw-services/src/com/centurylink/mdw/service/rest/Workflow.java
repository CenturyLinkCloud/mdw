/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Workflow")
@Api("MDW process definitions")
public class Workflow extends JsonRestService {

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Process;
    }

    @Override
    @Path("/{packageName}/{processName}/{processVersion}")
    @ApiOperation(value="Retrieve a process definition JSON.",
        notes="Path segments {packageName} and {processName} are required, while {processVersion} is optional.",
        response=Process.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="version", paramType="query")})
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        try {
            String[] segments = getSegments(path);
            if (segments.length < 3)
                throw new ServiceException(ServiceException.BAD_REQUEST, "Path segments {packageName}/{processName} are required");
            if (getQuery(path, headers).getBooleanFilter("summary")) {
                Process process;
                if (segments.length == 4)
                    process = ProcessCache.getProcess(segments[1] + "/" + segments[2], Asset.parseVersion(segments[3]));
                else
                    process = ProcessCache.getProcess(segments[1] + "/" + segments[2], 0);
                JSONObject json = new JSONObject();
                json.put("id", process.getId());
                json.put("name", process.getName());
                json.put("package", process.getPackageName());
                json.put("version", process.getVersionString());
                return json;
            }
            else {
                String assetPath = segments[1] + "/" + segments[2];
                Process process = workflowServices.getProcessDefinition(assetPath, getQuery(path, headers));
                JSONObject json = process.getJson(); // does not include name or package
                json.put("name", process.getName());
                json.put("packageName", process.getPackageName());
                return json;
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }
}
