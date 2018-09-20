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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.MonitoringWidget;
import com.centurylink.mdw.model.asset.Pagelet;
import com.centurylink.mdw.model.asset.Pagelet.Widget;
import com.centurylink.mdw.model.asset.PrePostWidgetProvider;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.centurylink.mdw.monitor.AdapterMonitor;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.monitor.TaskMonitor;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.JsonUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/Implementors")
@Api("Activity implementor definitions")
public class Implementors extends JsonRestService {

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
                    impl.setPagelet(null);
                    JSONObject implJson = impl.getJson();
                    JSONObject pageletJson;
                    if (pageletStr.trim().startsWith("{")) {
                        pageletJson = new JSONObject(pageletStr);
                    }
                    else {
                        Pagelet pagelet = new Pagelet(impl.getCategory(), pageletStr);
                        pagelet.addWidgetProvider(new PrePostWidgetProvider());
                        pagelet.addWidgetProvider(implCategory -> {
                            List<Widget> widgets = new ArrayList<>();
                            Widget monitoringWidget = new MonitoringWidget("Monitors");
                            widgets.add(monitoringWidget);
                            JSONArray rows = new JSONArray();
                            for (ActivityMonitor activityMonitor : MonitorRegistry.getInstance().getActivityMonitors()) {
                                JSONArray row = getRowDefault(activityMonitor.getClass());
                                if (row != null) {
                                    rows.put(row);
                                }
                            }

                            if (AdapterActivity.class.getName().equals(implCategory)) {
                                for (AdapterMonitor adapterMonitor : MonitorRegistry.getInstance().getAdapterMonitors()) {
                                    JSONArray row = getRowDefault(adapterMonitor.getClass());
                                    if (row != null)
                                        rows.put(row);
                                }
                            }
                            else if (TaskActivity.class.getName().equals(implCategory)) {
                                for (TaskMonitor taskMonitor : MonitorRegistry.getInstance().getTaskMonitors()) {
                                    JSONArray row = getRowDefault(taskMonitor.getClass());
                                    if (row != null)
                                        rows.put(row);
                                }
                            }
                            if (rows.length() > 0)
                                monitoringWidget.setAttribute("default", rows.toString());
                            return widgets;
                        });
                        pageletJson = pagelet.getJson();
                    }
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

    public JSONArray getRowDefault(Class<? extends com.centurylink.mdw.monitor.Monitor> monitorClass) {
        Monitor monitorAnnotation = monitorClass.getAnnotation(Monitor.class);
        if (monitorAnnotation == null)
            return null;
        JSONArray cols = new JSONArray();
        cols.put(String.valueOf(monitorAnnotation.global()));
        cols.put(monitorAnnotation.value());
        String className = monitorClass.getName();
        AssetInfo implAsset = ServiceLocator.getAssetServices().getImplAsset(className);
        if (implAsset == null)
            cols.put("");
        else
            cols.put(className.substring(0, className.lastIndexOf(".")) + "/" + implAsset.getName());
        cols.put(monitorAnnotation.defaultOptions());
        return cols;
    }
}
