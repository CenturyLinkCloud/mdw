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

/**
 * Important: This service does not actually authenticate the user.  The authentication is assumed to have already taken
 * place in the Security Servlet Filter or in AuthUtils, depending on the auth type.
 */
@Path("/AuthenticatedUser")
public class AuthenticatedUser extends JsonRestService {

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.User;
    }

    /**
     * Retrieve the current authenticated user.
     */
    @Override
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        // we trust this header value because only MDW can set it
        String authUser = (String)headers.get(Listener.AUTHENTICATED_USER_HEADER);
        try {
            if (authUser == null)
                throw new ServiceException("Missing parameter: " + Listener.AUTHENTICATED_USER_HEADER);
            UserServices userServices = ServiceLocator.getUserServices();
            User userVO = userServices.getUser(authUser);
            if (userVO == null)
                throw new ServiceException(HTTP_404_NOT_FOUND, "User not found: " + authUser);
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
