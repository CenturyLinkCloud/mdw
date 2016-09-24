/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.utilities.JsonUtil;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.Pagelet;
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
            Query query = new Query(path, headers);
            String implClassName = getSegment(path, 1);
            if (implClassName == null) {
                List<ActivityImplementorVO> impls = workflowServices.getImplementors();
                // temporary option until pagelets are saved as json
                if (query.getBooleanFilter("pageletAsJson")) {
                    for (ActivityImplementorVO impl : impls) {
                        String pagelet = impl.getAttributeDescription();
                        if (pagelet != null && !pagelet.isEmpty() && !pagelet.trim().startsWith("{"))
                            impl.setAttributeDescription(new Pagelet(pagelet).getJson().toString(2));
                    }
                }
                return JsonUtil.getJsonArray(impls).getJson();
            }
            else {
                ActivityImplementorVO impl = workflowServices.getImplementor(implClassName);
                if (impl == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Implementor not found: " + implClassName);
                if (query.getBooleanFilter("pageletAsJson")) {
                    String pagelet = impl.getAttributeDescription();
                    if (pagelet != null && !pagelet.isEmpty() && !pagelet.trim().startsWith("{"))
                        impl.setAttributeDescription(new Pagelet(pagelet).getJson().toString(2));
                }
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
