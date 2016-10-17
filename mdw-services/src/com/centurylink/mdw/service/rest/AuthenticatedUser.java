/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Important: This service does not actually authenticate the user.  The authentication is assumed to have already taken
 * place in the Security Servlet Filter or in AuthUtils, depending on the auth type.
 */
@Path("/AuthenticatedUser")
@Api("MDWHub authenticated user")
public class AuthenticatedUser extends JsonRestService {

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.User;
    }

    /**
     * Retrieve the current authenticated user.
     */
    @Override
    @ApiOperation(value="Retrieves the user as authenticated by MDW",
        response=UserVO.class)
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        // we trust this header value because only MDW set it
        String authUser = (String)headers.get(Listener.AUTHENTICATED_USER_HEADER);
        try {
            if (authUser == null)
                throw new ServiceException("Missing parameter: " + Listener.AUTHENTICATED_USER_HEADER);
            UserServices userServices = ServiceLocator.getUserServices();
            UserVO userVO = userServices.getUser(authUser);
            return userVO.getJsonWithRoles();
        }
        catch (Exception ex) {
            throw new ServiceException("Error getting authenticated user: " + authUser, ex);
        }
    }
}
