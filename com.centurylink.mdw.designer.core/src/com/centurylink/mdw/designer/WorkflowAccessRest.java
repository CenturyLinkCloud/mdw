/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
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

    public ProcessVO getProcess(long processId) throws DataAccessException {
        try {
            String pkgXml = getServer().invokeResourceService("Processes?id=" + processId);
            ProcessImporter importer = DataAccess.getProcessImporter(DataAccess.currentSchemaVersion);
            PackageVO pkg = importer.importPackage(pkgXml);
            ProcessVO process = pkg.getProcesses().get(0);
            process.setPackageName(pkg.getName());
            process.setPackageVersion(pkg.getVersionString());
            process.setId(processId);
            return process;
        }
        catch (IOException ex) {
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
            Map<String,String> params = getStandardParams();
            params.put("ownerType", ownerType);
            params.put("ownerId", String.valueOf(ownerId));
            ActionRequest actionRequest = new ActionRequest("UpdateAttributes", params);
            JSONObject attrsJson = new JSONObject();
            for (String name : attributes.keySet())
                attrsJson.put(name, attributes.get(name));
            actionRequest.addJson("attributes", attrsJson);
            invokeActionService(actionRequest.getJson().toString(2));
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