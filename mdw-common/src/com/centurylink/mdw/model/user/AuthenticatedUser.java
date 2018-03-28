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
package com.centurylink.mdw.model.user;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.model.task.TaskAction;

/**
 * Contains the information provided by the Portal.
 */
public class AuthenticatedUser extends User {
    public static final String NOTIFICATION_PREF = "notificationPref";

    public long getUserId() {
        return getId().longValue();
    }

    public AuthenticatedUser() {
    }

    public AuthenticatedUser(User user) {
        this(user, null);
    }

    public AuthenticatedUser(User user, Map<String,String> attributes) {
        setCuid(user.getCuid());
        setId(user.getId());
        setName(user.getName());
        setWorkgroups(user.getWorkgroups());
        setAttributes(attributes);
    }

    /**
     * Constructor for an ad-hoc user.
     */
    public AuthenticatedUser(String cuid) {
        setId(0L);
        setCuid(cuid);
        setWorkgroups(new Workgroup[0]);
        setAllowableActions(new TaskAction[0]);
        setAttributes(new HashMap<String, String>());
    }

    public boolean emailOptIn;
    public boolean isEmailOptIn() {
        return emailOptIn;
    }
    public void setEmailOptIn(boolean optIn) {
        this.emailOptIn = optIn;
    }

    private TaskAction[] _allowableActions;

    public TaskAction[] getAllowableActions() {
        return _allowableActions;
    }

    public void setAllowableActions(TaskAction[] actions) {
        _allowableActions = actions;
    }

    public boolean isHasWorkgroups() {
        return getWorkgroups() != null && getWorkgroups().length > 0;
    }

    public boolean isHasWorkgroupsOtherThanCommon() {
        if (getWorkgroups() != null) {
            String[] groupNames = getWorkgroupNames();
            if (groupNames.length == 1 && groupNames[0].equals(Workgroup.COMMON_GROUP)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Whether the user belongs to the specified MDW role.
     */
    public boolean isInRoleForAnyGroup(String role) {
        if (getWorkgroups() == null)
            return false;
        // special handling for "Site Admin" as a role
        if (Workgroup.SITE_ADMIN_GROUP.equals(role))
            return isInGroup(Workgroup.SITE_ADMIN_GROUP);
        for (Workgroup group : getWorkgroups()) {
            if (group.hasRole(role))
                return true;
        }
        return false;
    }

    public boolean isInRole(String group, String role) {
        return super.hasRole(group, role);
        // potential todo - this may need to check for inherited roles
    }

    public boolean isInGroup(String group) {
        return super.belongsToGroup(group);
    }

    /**
     * @return the names of all the user's workgroups
     */
    public String[] getWorkgroupNames() {
        String[] names = new String[getWorkgroups().length];
        for (int i = 0; i < getWorkgroups().length; i++) {
            names[i] = getWorkgroups()[i].getName();
        }
        return names;
    }

    public String[] getGroupNameAndRoles() {
        String[] nameAndRoles = new String[getWorkgroups().length];
        for (int i = 0; i < getWorkgroups().length; i++) {
            nameAndRoles[i] = getWorkgroups()[i].getNameAndRolesAsString();
        }
        return nameAndRoles;
    }

    /**
     * Returns a string concatenation of the user's workgroups.
     *
     * @return
     */
    public String getWorkgroupsAsString() {
        StringBuffer sb = new StringBuffer(512);
        for (int i = 0; i < getWorkgroups().length; i++) {
            Workgroup group = getWorkgroups()[i];
            if (!Workgroup.COMMON_GROUP.equals(group.getName())) {
                sb.append(group.getName());
                if (i != getWorkgroups().length - 1)
                    sb.append(", ");
            }
        }
        return sb.toString();
    }

    public boolean isUserBelongsToAdminGrp() {
        return super.belongsToGroup(Workgroup.SITE_ADMIN_GROUP);
    }

    public String toString() {
        String ret = "cuid: " + getCuid() + ",\n" + "firstName: " + getFirstName() + ",\n"
                + "lastName: " + getLastName() + ",\n" + "email: " + getEmail() + ",\n"
                + "phone: " + getPhone() + ",\n";

        ret += "allowable actions:\n";
        if (_allowableActions != null) {
            for (int i = 0; i < _allowableActions.length; i++)
                ret += "  " + _allowableActions[i].getTaskActionName() + ",\n";
        }

        ret += "workgroups\n";
        if (getWorkgroups() != null) {
            for (int i = 0; i < getWorkgroups().length; i++)
                ret += "  " + getWorkgroups()[i].getName() + ",\n";
        }
        return ret;
    }

    /**
     * Convenience method for checking user roles in expression language
     * statements.
     *
     * @return a Map whose key values are the roles a user belongs to.
     *
     *         Note: checks whether a user has the specified role for ANY
     *         groups.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map getRoles() {
        Map roles = new HashMap();

        if (getWorkgroups() != null) {
            for (Workgroup group : getWorkgroups()) {
                if (group.getRoles() != null) {
                    for (String role : group.getRoles()) {
                        roles.put(role, new Boolean(true));
                    }
                }
                // special treatment of Site Admin like a role
                if (group.getName().equals(Workgroup.SITE_ADMIN_GROUP))
                    roles.put(Workgroup.SITE_ADMIN_GROUP, new Boolean(true));
            }
        }
        return roles;
    }

    public boolean isLoaded() {
        return getCuid() != null && getName() != null;
    }

    public boolean equals(Object other) {
        if (!(other instanceof AuthenticatedUser))
            return false;

        AuthenticatedUser otherUser = (AuthenticatedUser) other;
        if (otherUser.getCuid() == null)
            return getCuid() == null;
        return otherUser.getCuid().equals(getCuid());
    }

    // these are only used for the samples demo
    private String _firstName;

    public String getFirstName() {
        return _firstName;
    }

    public void setFirstName(String s) {
        _firstName = s;
    }

    private String _lastName;

    public String getLastName() {
        return _lastName;
    }

    public void setLastName(String s) {
        _lastName = s;
    }
}
