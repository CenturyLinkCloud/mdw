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
package com.centurylink.mdw.service.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.user.Role;
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
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

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
                Query query = getQuery(path, headers);
                if (segments.length == 4)
                    query.setFilter("version", Asset.parseVersion(segments[3]));
                Process process = workflowServices.getProcessDefinition(assetPath, query);
                JSONObject json = process.getJson(); // does not include name, package or id
                json.put("name", process.getName());
                json.put("id", process.getId());
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

    /**
     * Update workflow values is currently supported.
     */
    @Override
    @Path("/{instanceId}/{values}")
    @ApiOperation(value="Update values for an ownerType and ownerId", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Values", paramType="body")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length == 3 && segments[2].equals("values")) {
            try {
                Long instanceId = Long.parseLong(segments[1]);
                Map<String,Object> values = new HashMap<>();
                for (String name : JSONObject.getNames(content)) {
                    Value value = new Value(name, content.getJSONObject(name));
                    values.put(name, value.getValue());
                }
                ServiceLocator.getWorkflowServices().setVariables(instanceId, values);
            }
            catch (NumberFormatException ex) {
                throw new ServiceException(ServiceException.BAD_REQUEST, "Bad instanceId: " + segments[1]);
            }
        }
        else {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Unexpected path: " + path);
        }

        return null;
    }
}
