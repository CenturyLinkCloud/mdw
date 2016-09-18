/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.utilities.JsonUtil;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/Implementors")
@Api("Activity implementor definitions")
public class Implementors extends JsonRestService {

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.ActivityImplementor;
    }

    @Override
    @Path("/{className}")
    @ApiOperation(value="Retrieve activity implementor(s) JSON.",
        notes="If {className} is provided, a specific implementor is returned; otherwise all implementors.",
        response=ActivityImplementorVO.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        try {
            String implClassName = getSegment(path, 1);
            if (implClassName == null) {
                return JsonUtil.getJsonArray(workflowServices.getImplementors()).getJson();
            }
            else {
                ActivityImplementorVO impl = workflowServices.getImplementor(implClassName);
                if (impl == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Implementor not found: " + implClassName);
                return impl.getJson();
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
