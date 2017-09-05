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

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public abstract class RestService {

    public static final int HTTP_200_OK = 200;
    public static final int HTTP_201_CREATED = 201;
    public static final int HTTP_202_ACCEPTED = 202;

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
    protected User authorize(String path, JSONObject content, Map<String,String> headers) throws AuthorizationException {
        String userId = headers.get(Listener.AUTHENTICATED_USER_HEADER);
        if (userId == null)
            throw new AuthorizationException(HTTP_401_UNAUTHORIZED, "Service " + this.getClass().getSimpleName() + " requires authenticated user");
        try {
            User user = UserGroupCache.getUser(userId);
            if (user == null)
                throw new AuthorizationException(HTTP_401_UNAUTHORIZED, "Cannot find user: " + userId);
            String workgroup = headers.get(Listener.AUTHORIZATION_WORKGROUP);
            List<String> roles = getRoles(path);
            if (roles != null) {
                if (roles.contains(Role.ANY))
                    return user;
                for (String role : roles) {
                    if ((workgroup == null && user.hasRole(role)) || (workgroup != null && user.hasRole(workgroup, role))) {
                        List<Workgroup> workgroups = getRequiredWorkgroups(content);
                        // Passed Role Authorization, make sure the user is part of the necessary workgroups
                        // Only fail if workgroups are defined and user isn't in a workgroup
                        if (workgroups != null && !userInGroups(user, workgroups)) {
                            String wgs = "";
                            for (int i = 0; i < workgroups.size(); i++) {
                                wgs += workgroups.get(i).getName();
                                if (i < workgroups.size() - 1)
                                    wgs += ", ";
                            }
                            throw new AuthorizationException(HTTP_401_UNAUTHORIZED,
                                        "User: " + userId + " not authorized for groups " + wgs + " for: " + path);
                        }
                        return user;
                    }

                }
            }
            throw new AuthorizationException(HTTP_401_UNAUTHORIZED,
                    "User: " + userId + " not authorized for: " + path);
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
    private boolean userInGroups(User user, List<Workgroup> workgroups) {
         for (Workgroup workgroup: workgroups) {
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
    protected List<Workgroup> getRequiredWorkgroups(JSONObject content) throws JSONException, DataAccessException {
        return null;
    }


    /**
     * TODO: pass HTTP Method
     *
     * A user who belongs to any role in the returned list is allowed
     * to perform this action/method.  Override to open access beyond Site Admin.
     * @param path
     * @return list of role names or null if authorization not required
     */
    public List<String> getRoles(String path) {
        List<String> defaultRoles = new ArrayList<String>();
        defaultRoles.add(Workgroup.SITE_ADMIN_GROUP);
        return defaultRoles;
    }

    protected String getRequiredProperty(String propName) throws ServiceException {
        String value = PropertyManager.getProperty(propName);
        if (value == null)
            throw new ServiceException(HTTP_404_NOT_FOUND, "Missing property: " + propName);
        return value;
    }

    protected void propagate(String method, String request, Map<String,String> headers)
    throws ServiceException, IOException {
        String requestUrl = headers.get(Listener.METAINFO_REQUEST_URL);
        String queryStr = "";
        if (!StringHelper.isEmpty(headers.get(Listener.METAINFO_REQUEST_QUERY_STRING)))
            queryStr = "?" + headers.get(Listener.METAINFO_REQUEST_QUERY_STRING);
        if (requestUrl == null)
            throw new ServiceException("Missing header: " + Listener.METAINFO_REQUEST_URL);
        for (URL serviceUrl : getOtherServerUrls(requestUrl)) {
            HttpHelper httpHelper = new HttpHelper(new URL(serviceUrl + queryStr));
            httpHelper.setHeaders(headers);
            if (method.equals("post"))
                validateResponse(httpHelper.post(request));
            else if (method.equals("put"))
                validateResponse(httpHelper.put(request));
        }
    }

    protected List<URL> getOtherServerUrls(String requestUrl) throws IOException {
        return ApplicationContext.getOtherServerUrls(new URL(requestUrl));
    }

    protected void auditLog(UserAction userAction) {
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
    protected UserAction getUserAction(User user, String path, Object content, Map<String,String> headers) {
        Action action = getAction(path, content, headers);
        Entity entity = getEntity(path, content, headers);
        Long entityId = getEntityId(path, content, headers);
        String descrip = getEntityDescription(path, content, headers);
        if (descrip.length() > 1000)
            descrip = descrip.substring(0, 999);
        UserAction userAction = new UserAction(user.getCuid(), action, entity, entityId, descrip);
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

    protected String getEntityDescription(String path, Object content, Map<String,String> headers) {
        return path;
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

    /**
     * returns authenticated user cuid
     */
    protected String getAuthUser(Map<String,String> headers) {
        return headers.get(Listener.AUTHENTICATED_USER_HEADER);
    }

    /**
     * Also audit logs.
     */
    protected void authorizeExport(Map<String,String> headers) throws AuthorizationException {
        String path = headers.get(Listener.METAINFO_REQUEST_PATH);
        User user = authorize(path, new JsonObject(), headers);
        Action action = Action.Export;
        Entity entity = getEntity(path, null, headers);
        Long entityId = new Long(0);
        String descrip = path;
        if (descrip.length() > 1000)
            descrip = descrip.substring(0, 999);
        UserAction exportAction = new UserAction(user.getName(), action, entity, entityId, descrip);
        exportAction.setSource(getSource());
        auditLog(exportAction);
    }
}
