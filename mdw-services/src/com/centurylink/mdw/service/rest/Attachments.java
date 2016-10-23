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
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;

@Path("/Attachments")
@Api("Instance attachments")
public class Attachments extends JsonRestService {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Role;
    }

    /**
     * For creating a new attachment
     */
    @Override
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        try {
            String user = content.getString("user");
            User userVO = ServiceLocator.getUserManager().getUser(user);
            if (userVO == null)
                throw new ServiceException("User not found: " + user);

            TaskManager taskMgr = ServiceLocator.getTaskManager();
            Long id = taskMgr.addAttachment(content.getString("name"), content.getString("location"), content.getString("contentType"),
                    user,content.getString("owner"),content.getLong("ownerId"));
            if (logger.isDebugEnabled())
                logger.debug("Added attachment id: " + id);

            return null;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

}