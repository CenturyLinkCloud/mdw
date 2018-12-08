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

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.app.Templates;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.Pagelet;
import com.centurylink.mdw.model.asset.Pagelet.Widget;
import com.centurylink.mdw.model.asset.PrePostWidgetProvider;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.monitor.*;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Path("/Implementors")
@Api("Activity implementor definitions")
public class Implementors extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    protected List<String> getRoles(String path, String method) {
        if (method.equals("GET")) {
            List<String> roles = new ArrayList<>();
            if (UserGroupCache.getRole(Role.ASSET_VIEW) != null) {
                roles.add(Role.ASSET_VIEW);
                roles.add(Role.ASSET_DESIGN);
                roles.add(Workgroup.SITE_ADMIN_GROUP);
            }
            return roles;
        }
        else {
            return super.getRoles(path, method);
        }
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.ActivityImplementor;
    }

    @Override
    @Path("/{className}")
    @ApiOperation(value="Retrieve activity implementor(s) JSON.",
        notes="If {className} is provided, a specific implementor is returned; otherwise all implementors.",
        response=ActivityImplementor.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        try {
            String implClassName = getSegment(path, 1);
            if (implClassName == null) {
                List<ActivityImplementor> impls = workflowServices.getImplementors();
                Collections.sort(impls);
                return JsonUtil.getJsonArray(impls).getJson();
            }
            else {
                ActivityImplementor impl = workflowServices.getImplementor(implClassName);
                if (impl == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Implementor not found: " + implClassName);
                String pageletStr = impl.getPagelet();
                if (pageletStr != null && !pageletStr.isEmpty()) {
                    AssetServices assetServices = ServiceLocator.getAssetServices();
                    if (!pageletStr.startsWith("{") && !pageletStr.trim().startsWith("<")) {
                        // references a pagelet asset
                        String pageletAssetPath = pageletStr;
                        if (pageletAssetPath.indexOf("/") < 1) { // qualify asset path
                            pageletAssetPath = implClassName.substring(0, implClassName.lastIndexOf(".")) + "/" + pageletStr;
                        }
                        Asset pageletAsset = AssetCache.getAsset(pageletAssetPath);
                        if (pageletAsset == null)
                            throw new FileNotFoundException("No pagelet asset: " + pageletAssetPath);
                        pageletStr = pageletAsset.getStringContent();
                    }
                    impl.setPagelet(null);
                    Pagelet pagelet = new Pagelet(impl.getCategory(), pageletStr);
                    pagelet.addWidgetProvider(new PrePostWidgetProvider());
                    pagelet.addWidgetProvider(implCategory -> {
                        List<Widget> widgets = new ArrayList<>();
                        try {
                            Widget monitoringWidget = new Widget(new JSONObject(Templates.get("configurator/monitors.json")));
                            widgets.add(monitoringWidget);
                            JSONArray rows = new JSONArray();
                            for (ActivityMonitor activityMonitor : MonitorRegistry.getInstance().getActivityMonitors()) {
                                AssetInfo implAsset = assetServices.getImplAsset(activityMonitor.getClass().getName());
                                JSONArray row = MonitorAttributes.getRowDefault(implAsset, activityMonitor.getClass());
                                if (row != null) {
                                    rows.put(row);
                                }
                            }
                            if (AdapterActivity.class.getName().equals(implCategory)) {
                                for (AdapterMonitor adapterMonitor : MonitorRegistry.getInstance().getAdapterMonitors()) {
                                    AssetInfo implAsset = assetServices.getImplAsset(adapterMonitor.getClass().getName());
                                    JSONArray row = MonitorAttributes.getRowDefault(implAsset, adapterMonitor.getClass());
                                    if (row != null)
                                        rows.put(row);
                                }
                            }
                            else if (TaskActivity.class.getName().equals(implCategory)) {
                                for (TaskMonitor taskMonitor : MonitorRegistry.getInstance().getTaskMonitors()) {
                                    AssetInfo implAsset = assetServices.getImplAsset(taskMonitor.getClass().getName());
                                    JSONArray row = MonitorAttributes.getRowDefault(implAsset, taskMonitor.getClass());
                                    if (row != null)
                                        rows.put(row);
                                }
                            }
                            if (rows.length() > 0)
                                monitoringWidget.setAttribute("default", rows.toString());
                        }
                        catch (IOException ex) {
                            logger.severeException("Error loading monitor widgets for: " + implClassName, ex);
                        }
                        return widgets;
                    });

                    JSONObject implJson = impl.getJson();
                    JSONObject pageletJson = pagelet.getJson();
                    implJson.put("pagelet", pageletJson);
                    return implJson;
                }
                else {
                    return impl.getJson();
                }
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
