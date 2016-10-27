/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.startup.StartupException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/WorkflowCache")
@Api("Workflow cache refresh")
public class WorkflowCache extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_DESIGN);
        return roles;
    }

    @Override
    protected Action getAction(String path, Object content, Map<String, String> headers) {
        if ("POST".equals(headers.get(Listener.METAINFO_HTTP_METHOD)))
            return Action.Refresh;
        else
            return super.getAction(path, content, headers);
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String, String> headers) {
        return Entity.Cache;
    }

    /**
     * TODO: Handle content values "excludes" and "include" for intelligent refresh.
     * Then this can be used by Designer.
     */
    @Override
    @Path("/{cacheName}")
    @ApiOperation(value="Refresh the entire MDW workflow cache, or an individual cache",
        notes="If {cacheName} is present a single cache is refresh, otherwise all caches are refreshed", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="distributed", paramType="body", dataType="boolean")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {

        try {
            String singleCacheName = getSegment(path, 1);

            if (singleCacheName == null) {
                // TODO excludes and includes
                CacheRegistration.getInstance().refreshCaches(null);
            }
            else {
                new CacheRegistration().refreshCache(singleCacheName);
            }

            if (content.has("distributed") && content.getBoolean("distributed"))
                propagatePost(content, headers);
        }
        catch (StartupException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        catch (IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }

        return null;
    }
}
