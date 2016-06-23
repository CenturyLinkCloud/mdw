/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskTemplateServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/TaskTemplates")
@Api("Task design-time templates")
public class TaskTemplates extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(UserRoleVO.USER_ADMIN);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Role;
    }


    @Override
    @Path("/{category}")
    @ApiOperation(value="Retrieve task templates or task categories",
        notes="If 'category' (literal) is present, returns task categories; otherwise returns task templates.",
        response=TaskVO.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        try {
            Map<String,String> parameters = getParameters(headers);
            String category = (String) parameters.get("category");
            if (category != null) {
                JSONArray array = new JSONArray();
                JSONObject json = new JSONObject();
                TaskTemplateServices taskTemplates = ServiceLocator.getTaskTemplateServices();
                List<TaskCategory> taskCategories = taskTemplates.getTaskCategories();
                for (TaskCategory aResult : taskCategories) {
                    array.put(aResult.getJson());
                }
                json.put("categories", array);
                return json;
            }
            else {
                TaskTemplateServices taskTemplates = ServiceLocator.getTaskTemplateServices();
                return taskTemplates.getTaskTemplates().getJson();
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * For update.
     */
    @Override
    @Path("/{taskTemplateId}")
    @ApiOperation(value="Update a task template", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="TaskTemplate", paramType="body", required=true, dataType="com.centurylink.mdw.model.value.task.TaskVO")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String taskId = getSegment(path, 1);
        if (taskId == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing segment: {taskId}");
        TaskTemplateServices taskTemplateServices = ServiceLocator.getTaskTemplateServices();
        try {
            if (content == null)
                throw new ServiceException(HTTP_404_NOT_FOUND,
                        "Taks Templates not found: " + taskId);
            TaskVO taskTemplate = taskTemplateServices.getTaskTemplate(taskId);
            taskTemplate.setAttribute("name", content.getString("name"));
            taskTemplate.setAttribute("TaskDescription", content.getString("TaskDescription"));
            taskTemplate.setTaskCategory(content.getString("category"));
            int days = convertToInteger(content.getString("alertIntervalDays"));
            int hours = convertToInteger(content.getString("alertIntervalHours"));
            taskTemplate.setAlertIntervalSeconds(days * 86400 + hours * 3600);
            days = convertToInteger(content.getString("slaDays"));
            hours = convertToInteger(content.getString("slaHours"));
            taskTemplate.setSlaSeconds(days * 86400 + hours * 3600);
            taskTemplate.setAttribute("PriorityStrategy", content.getString("PriorityStrategy"));
            taskTemplate.setAttribute("SubTaskStrategy", content.getString("SubTaskStrategy"));
            taskTemplate.setAttribute("RoutingStrategy", content.getString("RoutingStrategy"));
            taskTemplate.setAttribute("AutoAssign", content.getString("AutoAssign"));
            taskTemplate.setAttribute("Groups", content.getString("Groups"));
            taskTemplate.setAttribute("NoticeGroups", content.getString("NoticeGroups"));
            taskTemplate.setAttribute("Notices", content.getString("Notices"));
            taskTemplateServices.updateTaskTemplate(taskTemplate);
            return null;
        }
        catch (Exception ex) {
            throw new ServiceException(HTTP_500_INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public int convertToInteger(String s) {
        int number;
        try {
            number = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            return 0;
        }
        catch (NullPointerException e) {
            return 0;
        }
        return number;
    }

    /**
     * TODO: Delete REST Service
     */
    @Path("/{taskTemplateId}")
    @ApiOperation(value="Delete a task template", response=StatusMessage.class)
    public JSONObject delete(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        return super.delete(path, content, headers);
    }
}
