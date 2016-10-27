/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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

import com.centurylink.mdw.common.service.JsonArray;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/BaseData")
@Api("Custom baseline data")
public class BaseData extends JsonRestService {

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.BaseData;
    }

    /**
     * Retrieve variableTypes or taskCategories.
     */
    @Override
    @Path("/{dataType}")
    @ApiOperation(value="Retrieve app-specific baseline data",
        notes="Supported dataTypes include VariableTypes and TaskCategories",
        responseContainer="Array")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        String dataType = getSegment(path, 1);
        if (dataType == null)
            throw new ServiceException("Missing path segment: {dataType}");
        try {
            BaselineData baselineData = DataAccess.getBaselineData();
            if (dataType.equals("VariableTypes")) {
                List<VariableType> variableTypes = baselineData.getVariableTypes();
                JSONArray jsonArray = new JSONArray();
                for (VariableType variableType : variableTypes)
                    jsonArray.put(variableType.getJson());
                return new JsonArray(jsonArray).getJson();
            }
            else if (dataType.equals("TaskCategories")) {
                List<TaskCategory> taskCats = new ArrayList<TaskCategory>();
                taskCats.addAll(baselineData.getTaskCategories().values());
                Collections.sort(taskCats);
                JSONArray jsonArray = new JSONArray();
                for (TaskCategory taskCat : taskCats)
                    jsonArray.put(taskCat.getJson());
                return new JsonArray(jsonArray).getJson();
            }
            else {
                throw new ServiceException("Unsupported dataType: " + dataType);
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }

    }
}

