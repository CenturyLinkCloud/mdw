/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;

public abstract class RestService {

    public static final int HTTP_400_BAD_REQUEST = 400;
    public static final int HTTP_401_UNAUTHORIZED = 401;
    public static final int HTTP_403_FORBIDDEN = 403;
    public static final int HTTP_404_NOT_FOUND = 404;
    public static final int HTTP_405_METHOD_NOT_ALLOWED = 405;
    public static final int HTTP_409_CONFLICT = 409;
    public static final int HTTP_500_INTERNAL_ERROR = 500;
    public static final int HTTP_501_NOT_IMPLEMENTED = 501;

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * The user identified in the AuthenticatedUser headers must be authorized to
     * perform this action.  For HTTP, MDW removes this header (if it exists) from
     * every request and then populates based on session authentication or HTTP Basic.
     * @param content
     *
     * @return authorized user if successful
     */
    protected UserVO authorize(String path, JSONObject content, Map<String,String> headers) throws AuthorizationException {
        String userId = headers.get(Listener.AUTHENTICATED_USER_HEADER);
        if (userId == null)
            throw new AuthorizationException(HTTP_401_UNAUTHORIZED, "Service " + this.getClass().getSimpleName() + " requires authenticated user");
        try {
            UserVO user = UserGroupCache.getUser(userId);
            if (user == null)
                throw new AuthorizationException(HTTP_401_UNAUTHORIZED, "Cannot find user: " + userId);
            String workgroup = headers.get(Listener.AUTHORIZATION_WORKGROUP);
            List<String> roles = getRoles(path);
            if (roles != null) {
                for (String role : roles) {
                    if ((workgroup == null && user.hasRole(role)) || (workgroup != null && user.hasRole(workgroup, role))) {
                        List<UserGroupVO> workgroups = getRequiredWorkgroups(content);
                        // Passed Role Authorization, make sure the user is part of the necessary workgroups
                        // Mainly used for Task services
                        // Only fail if workgroups are defined and user isn't in a workgroup
                        if (workgroups != null && !userInGroups(user, workgroups)) {
                            // Put a decent message if it's a workgroups issue
                            throw new AuthorizationException(HTTP_401_UNAUTHORIZED,
                                        "User: " + userId + " not authorized for groups " + workgroups + " for " + this.getClass().getSimpleName() + "/" + path);
                        }
                        return user;
                    }

                }
            }
            throw new AuthorizationException(HTTP_401_UNAUTHORIZED,
                    "User: " + userId + " not authorized for: " + this.getClass().getSimpleName() + "/" + path);
        }
        catch (CachingException ex) {
            throw new AuthorizationException(ex.getMessage(), ex);
        }
        catch (DataAccessException ex) {
            throw new AuthorizationException(ex.getMessage(), ex);
        }
        catch (JSONException ex) {
            throw new AuthorizationException(ex.getMessage(), ex);
        }
    }

    /**
     * Figure out if a user belongs to any of a list of groups
     * @param user
     * @param workgroups
     * @return
     */
    private boolean userInGroups(UserVO user, List<UserGroupVO> workgroups) {
         for (UserGroupVO workgroup: workgroups) {
            if (user.belongsToGroup(workgroup.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param content
     * @return
     * @throws JSONException
     * @throws DataAccessException
     */
    protected List<UserGroupVO> getRequiredWorkgroups(JSONObject content) throws JSONException, DataAccessException {
        return null;
    }


    /**
     * A user who belongs to any role in the returned list is allowed
     * to perform this action/method.  Override to open access beyond Site Admin.
     * @param path
     * @return list of role names or null if authorization not required
     */
    public List<String> getRoles(String path) {
        List<String> defaultRoles = new ArrayList<String>();
        defaultRoles.add(UserGroupVO.SITE_ADMIN_GROUP);
        return defaultRoles;
    }

    protected String getRequiredProperty(String propName) throws ServiceException {
        String value = PropertyManager.getProperty(propName);
        if (value == null)
            throw new ServiceException(HTTP_404_NOT_FOUND, "Missing property: " + propName);
        return value;
    }

    protected void propagatePost(String request, Map<String,String> headers)
    throws ServiceException, IOException {
        String requestUrl = headers.get(Listener.METAINFO_REQUEST_URL);
        if (requestUrl == null)
            throw new ServiceException("Missing header: " + Listener.METAINFO_REQUEST_URL);
        for (URL serviceUrl : getOtherServerUrls(requestUrl)) {
            HttpHelper httpHelper = new HttpHelper(serviceUrl);
            httpHelper.setHeaders(headers);
            validateResponse(httpHelper.post(request));
        }
    }

    protected List<URL> getOtherServerUrls(String requestUrl) throws IOException {

        List<URL> serverUrls = new ArrayList<URL>();
        List<String> serverList = ApplicationContext.getCompleteServerList();
        for (String serverHost : serverList) {
            String serviceUrl = "http://" + serverHost;
            URL thisUrl = new URL(requestUrl);
            URL otherUrl = new URL(serviceUrl);
            if (!(thisUrl.getHost().equals(otherUrl.getHost())) || thisUrl.getPort() != otherUrl.getPort())
                serverUrls.add(new URL(serviceUrl + thisUrl.getPath()));
        }
        return serverUrls;
    }

    protected void auditLog(UserActionVO userAction) throws DataAccessException {
        try {
            ServiceLocator.getUserServices().auditLog(userAction);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

    /**
     * For audit logging.
     */
    protected UserActionVO getUserAction(UserVO user, String path, Object content, Map<String,String> headers) {
        Action action = getAction(path, content, headers);
        Entity entity = getEntity(path, content, headers);
        Long entityId = getEntityId(path, content, headers);
        String descrip = getEntityDescription(path, content, headers);
        if (descrip.length() > 1000)
            descrip = descrip.substring(0, 999);
        UserActionVO userAction = new UserActionVO(user.getName(), action, entity, entityId, descrip);
        userAction.setSource(getSource());
        return userAction;
    }

    protected String getSource() {
        return getClass().getSimpleName() + " Action Service";
    }

    /**
     * Override if entity has a meaningful ID.
     */
    protected Long getEntityId(String path, Object content, Map<String,String> headers) {
        return 0L;
    }

    /**
     * Override if toString() on content is not meaningful.
     */
    protected String getEntityDescription(String path, Object content, Map<String,String> headers) {
        return content.toString();
    }

    protected Action getAction(String path, Object content, Map<String,String> headers) {
        String method = headers.get(Listener.METAINFO_HTTP_METHOD);
        if ("POST".equals(method))
            return Action.Create;
        else if ("PUT".equals(method))
            return Action.Change;
        else if ("DELETE".equals(method))
            return Action.Delete;
        else
            return Action.Other;
    }

    protected Map<String,String> getParameters(Map<String,String> headers) {
        Map<String,String> params = new HashMap<String,String>();
        String query = headers.get(Listener.METAINFO_REQUEST_QUERY_STRING);
        if (query != null && !query.isEmpty()) {
            for (String pair : query.split("&")) {
                int idx = pair.indexOf("=");
                try {
                    params.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
                catch (UnsupportedEncodingException ex) { // as if UTF-8 is going to be unsupported
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
        return params;
    }

    protected Query getQuery(String path, Map<String,String> headers) {
        return new Query(path, getParameters(headers));
    }

    protected String getSegment(String path, int segment) {
        String[] segments = getSegments(path);
        if (segments.length < segment + 1)
            return null;
        else
            return segments[segment];
    }

    /**
     * Minus the base path that triggered this service.
     */
    protected String getSub(String path) {
        int slash = path.indexOf('/');
        if (slash > 0 && slash < path.length() - 1)  // the first part of the path is what got us here
            return path.substring(slash + 1);
        else
            return null;
    }

    protected String[] getSegments(String path) {
        return path.split("/");
    }

    /**
     * Should be overridden.  Avoid using Entity.Other.
     */
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Other;
    }

    protected abstract void validateResponse(String response) throws ServiceException;
}
