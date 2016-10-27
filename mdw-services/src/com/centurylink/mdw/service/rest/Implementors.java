/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Pagelet;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
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
                return JsonUtil.getJsonArray(impls).getJson();
            }
            else {
                ActivityImplementor impl = workflowServices.getImplementor(implClassName);
                if (impl == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Implementor not found: " + implClassName);
                String pagelet = impl.getAttributeDescription();
                if (pagelet != null && !pagelet.isEmpty() && !pagelet.trim().startsWith("{")) {
                    JSONObject pageletJson = new Pagelet(pagelet).getJson();
                    impl.setAttributeDescription(null);
                    JSONObject implJson = impl.getJson();
                    implJson.put("pagelet", pageletJson);
                    return impl.getJson();
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
