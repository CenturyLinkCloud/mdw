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
package com.centurylink.mdw.service.rest;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction.Entity;
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
        response=User.class)
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        // we trust this header value because only MDW set it
        String authUser = (String)headers.get(Listener.AUTHENTICATED_USER_HEADER);
        try {
            if (authUser == null)
                throw new ServiceException("Missing parameter: " + Listener.AUTHENTICATED_USER_HEADER);
            UserServices userServices = ServiceLocator.getUserServices();
            User userVO = userServices.getUser(authUser);
            return userVO.getJsonWithRoles();
        }
        catch (DataAccessException ex) {
                throw new ServiceException(ex.getCode(), ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new ServiceException("Error getting authenticated user: " + authUser, ex);
        }
    }
}
