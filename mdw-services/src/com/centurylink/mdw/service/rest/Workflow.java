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

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetPackageList;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.asset.CustomPageLookup;
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
        @ApiImplicitParam(name="version", paramType="query", dataType="string"),
        @ApiImplicitParam(name="summary", paramType="query", dataType="boolean")})
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        Query query = getQuery(path, headers);
        try {
            // (for previous versions) process by id
            long processId = query.getLongFilter("id");
            if (processId > 0) {
                Process p = ProcessCache.getProcess(processId);
                if (p == null) {
                    throw new ServiceException(ServiceException.NOT_FOUND, "Process ID not found: " + processId);
                }
                else {
                    JSONObject json = query.getBooleanFilter("summary") ? new JsonObject() : p.getJson();
                    json.put("id", p.getId());
                    json.put("name", p.getName());
                    json.put("package", p.getPackageName());
                    json.put("version", p.getVersionString());
                    return json;
                }
            }

            String[] segments = getSegments(path);

            if (segments.length == 1) {
                // definitions -- with start pages
                Query procQuery = new Query();
                procQuery.setFilter("extension", "proc");
                AssetPackageList procPkgList = ServiceLocator.getAssetServices().getAssetPackageList(procQuery);
                return procPkgList.getJson();
            }

            if (segments.length < 3)
                throw new ServiceException(ServiceException.BAD_REQUEST, "Path segments {packageName}/{processName} are required");
            if (query.getBooleanFilter("summary")) {
                Process process;
                if (segments.length == 4)
                    process = ProcessCache.getProcess(segments[1] + "/" + segments[2], Asset.parseVersion(segments[3]));
                else
                    process = ProcessCache.getProcess(segments[1] + "/" + segments[2], 0);
                JSONObject json = new JsonObject();
                json.put("id", process.getId());
                json.put("name", process.getName());
                json.put("package", process.getPackageName());
                json.put("version", process.getVersionString());
                return json;
            }
            else {
                String assetPath = segments[1] + "/" + segments[2];
                if (segments.length == 4)
                    query.setFilter("version", Asset.parseVersion(segments[3]));
                Process process = workflowServices.getProcessDefinition(assetPath, query);
                JSONObject json = process.getJson(); // does not include name, package or id
                json.put("name", process.getName());
                json.put("id", process.getId());
                json.put("packageName", process.getPackageName());
                String startPage = process.getAttribute(WorkAttributeConstant.PROCESS_START_PAGE);
                if (startPage != null) {
                    String assetSpec = startPage;
                    String ver = process.getAttribute(WorkAttributeConstant.PROCESS_START_PAGE_ASSET_VERSION);
                    if (ver != null)
                        assetSpec += " v" + ver;
                    AssetVersionSpec startPageSpec = AssetVersionSpec.parse(assetSpec);
                    json.put("startPageUrl", new CustomPageLookup(startPageSpec, null).getUrl());
                }

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

    @Override
    @Path("//{packageName}/{processName}")
    @ApiOperation(value="Update a process definition", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Process", paramType="body", dataType="com.centurylink.mdw.model.workflow.Process")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        // TODO implement update
        return super.put(path, content, headers);
    }
}
