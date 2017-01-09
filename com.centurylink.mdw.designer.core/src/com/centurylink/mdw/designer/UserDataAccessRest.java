/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.types.ActionRequestMessage;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.dataaccess.UserDataAccess;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.model.value.user.RoleList;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.model.value.user.WorkgroupList;

public class UserDataAccessRest extends ServerAccessRest implements UserDataAccess {

    public UserDataAccessRest(RestfulServer server) {
        super(server);
    }

    public List<UserGroupVO> getAllGroups(boolean includeDeleted) throws DataAccessException {
        try {
            String pathWithArgs = "Workgroups?format=json";
            if (includeDeleted)
                pathWithArgs += "&includeDeleted=true";
            String response = invokeResourceService(pathWithArgs);
            return new WorkgroupList(response).getItems();
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public UserVO getUser(String cuid) throws DataAccessException {
        try {
            String pathWithArgs = "User?format=json&cuid=" + cuid + "&withRoles=true";
            String response = invokeResourceService(pathWithArgs);
            JSONObject jsonObj = new JSONObject(response);
            if (jsonObj.has("status"))
                return new UserVO(cuid);  // user not found -- no privileges
            else
                return new UserVO(jsonObj);
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public List<String> getRoleNames() throws DataAccessException {
        try {
            String pathWithArgs = "Roles?format=json";
            String response = invokeResourceService(pathWithArgs);
            RoleList roleList = new RoleList(response);
            List<String> roleNames = new ArrayList<String>();
            for (UserRoleVO role : roleList.getItems()) {
                roleNames.add(role.getName());
            }
            return roleNames;
        }
        catch (IOException ex) {
            throw new DataAccessOfflineException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public void auditLogUserAction(final UserActionVO userAction) throws DataAccessException {

        // certain actions need to be logged on the server
        // TODO: this should actually be done on the server side, but currently many services don't require username
        Action action = userAction.getAction();
        Entity entity = userAction.getEntity();
        if (action == Action.Run || action == Action.Send || action == Action.Retry || action == Action.Proceed || action == Action.Import
                || entity == Entity.ProcessInstance || entity == Entity.ActivityInstance || entity == Entity.VariableInstance) {
            if (isOnline()) {
                // run in background thread
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            ActionRequestMessage actionRequest = new ActionRequestMessage();
                            actionRequest.setAction("AuditLog");
                            actionRequest.addParameter("appName", "MDW Designer");
                            JSONObject msgJson = actionRequest.getJson();
                            msgJson.put(userAction.getJsonName(), userAction.getJson());
                            invokeActionService(msgJson.toString(2));
                        }
                        catch (Exception ex) {
                            ex.printStackTrace(); // silent
                        }
                    }
                }).start();
            }
        }
    }

    /**
     * No longer used as of MDW 5.2
     */
    public Set<String> getPrivilegesForUser(String userName) throws DataAccessException {
        return null;
    }
}
