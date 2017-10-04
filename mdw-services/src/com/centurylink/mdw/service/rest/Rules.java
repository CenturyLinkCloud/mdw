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

import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.RuntimeContext;
import com.centurylink.mdw.model.workflow.RuntimeContextAdapter;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.services.rules.RulesServicesImpl;
import com.centurylink.mdw.model.workflow.Package;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Rules")
@Api("Asset-driven rules service")
public class Rules extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Rule;
    }

    /**
     * Apply rules designated in asset path against incoming JSONObject.
     */
    @Override
    @Path("/{assetPath}")
    @ApiOperation(value="Apply rules identified by assetPath.", notes="Query may contain runtime values")
    @ApiImplicitParams({
        @ApiImplicitParam(name="FactsObject", paramType="body", required=true, value="Input to apply rules against"),
        @ApiImplicitParam(name="assetPath", paramType="path", required=true, value="Identifies a .drl or .xlsx asset"),
        @ApiImplicitParam(name="rules-executor", paramType="header", required=false, value="Default is Drools")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {

        String[] pathSegments = getSegments(path);
        if (pathSegments.length != 3)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        String assetPath = pathSegments[1] + "/" + pathSegments[2];
        Asset asset = null;
        if (assetPath.endsWith(".drl") || assetPath.endsWith(".xlsx")) {
            asset = AssetCache.getAsset(assetPath);
        }
        else {
            asset = AssetCache.getAsset(assetPath + ".drl");
            if (asset == null)
                asset = AssetCache.getAsset(assetPath + ".xlsx");
        }

        if (asset == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Rules asset not found: " + assetPath);

        Query query = getQuery(path, headers);
        final Map<String,String> filters = query.getFilters();
        final Package assetPackage = PackageCache.getPackage(asset.getPackageName());
        RuntimeContext context = new RuntimeContextAdapter() {
            public Map<String,Object> getVariables() {
                Map<String,Object> values = new HashMap<>();
                if (filters != null && !filters.isEmpty()) {
                    for (String name : filters.keySet())
                        values.put(name, filters.get(name));
                }
                return values;
            }
            @Override
            public Package getPackage() {
                return assetPackage;
            }
        };

        String executor = headers.get("rules-executor");
        if (executor == null)
            executor = RulesServicesImpl.DROOLS_EXECUTOR;

        return ServiceLocator.getRulesServices().applyRules(asset, content, context, executor);
    }
}
