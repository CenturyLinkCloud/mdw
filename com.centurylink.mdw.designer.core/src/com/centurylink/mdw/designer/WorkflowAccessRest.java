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
package com.centurylink.mdw.designer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.ActionRequest;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.dataaccess.ProcessImporter;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

public class WorkflowAccessRest extends ServerAccessRest {

    public WorkflowAccessRest(RestfulServer server) {
        super(server);
    }

    public ProcessVO getProcess(long processId, boolean json) throws DataAccessException {
        try {
            if (json) {
                String processJson = getServer().invokeResourceService("Workflow?id=" + processId);
                JSONObject jsonObj = new JSONObject(processJson);
                ProcessVO process = new ProcessVO(jsonObj);
                process.setPackageName(jsonObj.getString("package"));
                process.setPackageVersion(jsonObj.getString("packageVersion"));
                process.setId(jsonObj.getLong("id"));
                return process;
            }
            else {
                String pkgXml = getServer().invokeResourceService("Processes?id=" + processId);
                ProcessImporter importer = DataAccess.getProcessImporter(DataAccess.currentSchemaVersion);
                PackageVO pkg = importer.importPackage(pkgXml);
                ProcessVO process = pkg.getProcesses().get(0);
                process.setPackageName(pkg.getName());
                process.setPackageVersion(pkg.getVersionString());
                process.setId(processId);
                return process;
            }
        }
        catch (Exception ex) {
            throw new DataAccessException("Error retrieving process: " + processId, ex);
        }
    }

    public List<VariableTypeVO> getVariableTypes() throws DataAccessException {
        try {
            String variableTypesJson = getServer().invokeResourceService("BaseData/VariableTypes?format=json");
            JSONArray jsonArray = new JSONArray(variableTypesJson);
            List<VariableTypeVO> variableTypes = new ArrayList<VariableTypeVO>();
            for (int i = 0; i < jsonArray.length(); i++)
                variableTypes.add(new VariableTypeVO(jsonArray.getJSONObject(i)));
            return variableTypes;
        }
        catch (Exception ex) {
            throw new DataAccessException("Error retrieving variable types", ex);
        }
    }

    public List<TaskCategory> getTaskCategories() throws DataAccessException {
        try {
            String taskCategoriesJson = getServer().invokeResourceService("BaseData/TaskCategories?format=json");
            JSONArray jsonArray = new JSONArray(taskCategoriesJson);
            List<TaskCategory> taskCategories = new ArrayList<TaskCategory>();
            for (int i = 0; i < jsonArray.length(); i++)
                taskCategories.add(new TaskCategory(jsonArray.getJSONObject(i)));
            return taskCategories;
        }
        catch (Exception ex) {
            throw new DataAccessException("Error retrieving task categories", ex);
        }
    }

    public Map<String,String> getAttributes(String ownerType, long ownerId) throws DataAccessException, IOException {
        try {
            String pathWithArgs = "Attributes?format=json&ownerType=" + ownerType + "&ownerId=" + ownerId;
            String attrsJson = getServer().invokeResourceService(pathWithArgs);
            JSONObject attrs = new JSONObject(attrsJson);
            String[] names = JSONObject.getNames(attrs);
            if (names == null)
                return null;
            Map<String,String> attributes = new HashMap<String,String>();
            for (String name : names)
                attributes.put(name, attrs.getString(name));
            return attributes;
        }
        catch (JSONException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public void updateAttributes(String ownerType, long ownerId, Map<String,String> attributes) throws DataAccessException {
        try {
            JSONObject attrsJson = new JSONObject();
            for (String name : attributes.keySet())
                attrsJson.put(name, attributes.get(name));

            if (getServer().getSchemaVersion() >= 6000) {
                try {
                    getServer().post("Attributes/" + ownerType + "/" + ownerId, attrsJson.toString(2));
                }
                catch (IOException ex) {
                    throw new DataAccessOfflineException("Unable to connect to " + getServer().getServiceUrl(), ex);
                }
            }
            else
            {
                Map<String,String> params = getStandardParams();
                params.put("ownerType", ownerType);
                params.put("ownerId", String.valueOf(ownerId));
                ActionRequest actionRequest = new ActionRequest("UpdateAttributes", params);
                actionRequest.addJson("attributes", attrsJson);
                invokeActionService(actionRequest.getJson().toString(2));
            }
        }
        catch (JSONException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * TODO: use new service and delete the old one
     */
    public void importGitAssets(String user, String encryptedPassword, String branch) throws DataAccessException {
        try {
            Map<String,String> params = getStandardParams();
            params.put("branch", branch);
            ActionRequest actionRequest = new ActionRequest("ImportGitAssets", params);
            invokeActionService(actionRequest.getJson().toString(2), user, encryptedPassword);
        }
        catch (JSONException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }
}