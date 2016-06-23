/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="User", description="MDW user")
public class UserVO implements Serializable, Comparable<UserVO>, Jsonable {

    // new friendly attribute names that can be displayed as labels
    public static final String EMAIL_ADDRESS = "Email Address";
    public static final String PHONE_NUMBER = "Phone Number";
    // old unfriendly attribute names for compatibility
    public static final String OLD_EMAIL_ADDRESS = "emailAddress";
    public static final String OLD_PHONE_NUMBER = "phoneNumber";

    public UserVO() {
    }

    public UserVO(String cuid) {
        this.cuid = cuid;
    }

    public UserVO(String cuid, String priv_string) {
        this.id = null;
        this.cuid = cuid;
        String[] privs = priv_string.split(",");
        workgroups = new UserGroupVO[privs.length];
        for (int i=0; i<privs.length; i++) {
            workgroups[i] = new UserGroupVO(privs[i]);
        }
    }

    public UserVO(JSONObject json) throws JSONException {
        if (json.has("cuid"))
            cuid = json.getString("cuid");
        else
            cuid = json.getString("id");
        if (json.has("name"))
            name = json.getString("name");
        JSONArray grps = null;
        if (json.has("workgroups"))
            grps = json.getJSONArray("workgroups");
        else if (json.has("groups")) // compatibility
            grps = json.getJSONArray("groups");
        if (grps != null) {
            workgroups = new UserGroupVO[grps.length()];
            for (int i = 0; i < grps.length(); i++) {
                workgroups[i] = new UserGroupVO();
                String grp = grps.getString(i);
                if (json.has(grp)) {
                    // roles for group
                    List<String> roles = new ArrayList<String>();
                    JSONArray rls = json.getJSONArray(grp);
                    for (int j = 0; j < rls.length(); j++) {
                        roles.add(rls.getString(j));
                    }
                    workgroups[i].setRoles(roles);
                }
                workgroups[i].setName(grp);
            }
            UserGroupVO commonGroup = getCommonGroup();
            if (commonGroup == null) {
                UserGroupVO[] newGroups = new UserGroupVO[workgroups.length + 1];
                newGroups[0] = UserGroupVO.getCommonGroup();
                for (int i = 1; i < newGroups.length; i++)
                    newGroups[i] = workgroups[i - 1];
                workgroups = newGroups;
            }
        }
        else {
            workgroups = new UserGroupVO[] { UserGroupVO.getCommonGroup() };
        }
        if (json.has("roles")) {
            JSONArray roles = json.getJSONArray("roles");
            UserGroupVO commonGroup = getCommonGroup();
            List<String> commonRoles = new ArrayList<String>();
            for (int i = 0; i < roles.length(); i++)
                commonRoles.add(roles.getString(i));
            commonGroup.setRoles(commonRoles);
        }

        if (json.has("attributes")) {
            JSONObject attrs = json.getJSONObject("attributes");
            attributes = new HashMap<String,String>();
            String[] attrNames = JSONObject.getNames(attrs);
            if (attrNames != null) {
                for (int i = 0; i < attrNames.length; i++) {
                    attributes.put(attrNames[i], attrs.getString(attrNames[i]));
                }
            }
        }
    }

    private Long id;
    @ApiModelProperty(hidden=true)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String name;
    @ApiModelProperty(value="User's full name", required=true)
    public String getName() { return this.name; }
    public void setName(String fullName) { this.name = fullName; }

    private String cuid;
    @ApiModelProperty(value="User's workstation id", required=true)
    public String getCuid() { return this.cuid; }
    public void setCuid(String cuid) { this.cuid = cuid; }

    private String endDate;
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    private UserGroupVO[] workgroups;
    @ApiModelProperty(value="User's workgroups")
    public UserGroupVO[] getWorkgroups() { return workgroups; }
    public void setWorkgroups(UserGroupVO[] workgroups) { this.workgroups = workgroups; }

    public void setGroups(List<UserGroupVO> groups) {
        this.workgroups = groups.toArray(new UserGroupVO[groups.size()]);
    }

    /**
     * Only used by Designer.
     * TODO: remove when possible
     */
    @ApiModelProperty(hidden=true)
    public List<String> getRoles(String group) {
        if (workgroups==null) return null;
        for (UserGroupVO g : workgroups) {
            if (g.getName().equals(group)) {
                return g.getRoles();
            }
        }
        return null;
    }

    @ApiModelProperty(hidden=true)
    public String[] getGroupNames() {
        if (workgroups == null)
            return new String[0];
        List<String> groupList = new ArrayList<String>();
        for (UserGroupVO group : workgroups) {
            if (!group.getName().equals(UserGroupVO.COMMON_GROUP))
              groupList.add(group.getName());
        }
        return groupList.toArray(new String[0]);
    }

    @ApiModelProperty(hidden=true)
    public UserGroupVO getCommonGroup() {
        if (workgroups == null)
            return null;
        for (UserGroupVO group : workgroups) {
            if (group.getName().equals(UserGroupVO.COMMON_GROUP))
              return group;
        }
        return null;
    }

    @ApiModelProperty(hidden=true)
    public String getGroupsAndRolesAsString() {
        StringBuffer buffer = new StringBuffer();
        if (workgroups != null) {
            for (UserGroupVO g : workgroups) {
                 if (buffer.length()>0) buffer.append(",");
                 buffer.append(g.getNameAndRolesAsString());
            }
        }
        return buffer.toString();
    }

    public UserGroupVO getUserGroup(String groupName) {
        if (workgroups == null)
            return null;
        for (UserGroupVO group : workgroups) {
            if (group.getName().equals(groupName))
                return group;
        }
        return null;
    }

    /**
     * Check whether user has the specified role in any of the groups
     */
    public boolean hasRole(String roleName) {
        return hasRole(UserGroupVO.COMMON_GROUP, roleName);
    }

    public boolean hasRole(String groupName, String roleName) {
        if (workgroups == null)
            return false;
        // Site Admin treated as role for some purposes
        if (UserGroupVO.SITE_ADMIN_GROUP.equals(roleName))
            return belongsToGroup(UserGroupVO.SITE_ADMIN_GROUP);
        for (UserGroupVO group : workgroups) {
            if (group.getName().equals(groupName))
                return group.hasRole(roleName);
        }
        // when user has the role in super group, detected by caller
        return false;
    }

    private Map<String,String> attributes;
    public Map<String,String> getAttributes() { return attributes; }
    @ApiModelProperty(value="User personalization attributes, such as email and phone number")
    public void setAttributes(Map<String,String> attributes) { this.attributes = attributes; }
    public String getAttribute(String name) {
      if (attributes == null)
        return null;
      return attributes.get(name);
    }
    public void setAttribute(String name, String value) {
      if (attributes == null)
        attributes = new HashMap<String,String>();
      attributes.put(name, value);
    }

    /**
     * Check whether user belongs to the specified group
     */
    public boolean belongsToGroup(String groupName) {
        if (workgroups == null || workgroups.length == 0) {
            return false;
        }
        for (UserGroupVO g : workgroups) {
            if (g.getName().equals(groupName)) return true;
        }
        return false;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("UserVO[");
        buffer.append("userGroups = ").append(getGroupsAndRolesAsString());
        buffer.append(" userId = ").append(id);
        buffer.append(" userName = ").append(cuid);
        buffer.append("]");
        return buffer.toString();
    }

    public int compareTo(UserVO other) {
        if (this.name == null && this.cuid == null)
            return 0;
        else {
            if (this.name != null)
            {
              if (other.getName() == null)
                return this.name.compareToIgnoreCase(other.getCuid());
              else
                return this.name.compareToIgnoreCase(other.getName());
            }
            else
            {
              return this.cuid.compareToIgnoreCase(other.getCuid());
            }
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof UserVO))
          return false;
        return getId().equals(((UserVO)o).getId());
    }

    /**
     * This is only used when UserVO is a member of UserGroupVO.
     * Only that group is populated as a substructure to store roles.
     * @param groupId
     * @param roleName
     */
    public void addRoleForGroup(String groupName, String roleName) {
        if (workgroups==null) {
            workgroups = new UserGroupVO[1];
            workgroups[0] = new UserGroupVO(null, groupName, null);
        }
        List<String> roles = workgroups[0].getRoles();
        if (roles==null) {
            roles = new ArrayList<String>();
            workgroups[0].setRoles(roles);
        }
        roles.add(roleName);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        // json.put("id", getCuid());
        json.put("cuid", getCuid());
        json.put("name", getName());
        if (workgroups != null) {
            JSONArray grpsJson = new JSONArray();
            for (UserGroupVO group : workgroups) {
                grpsJson.put(group.getName());
            }
            json.put("workgroups", grpsJson);
        }
        if (attributes != null) {
            JSONObject attrsJson = new JSONObject();
            for (String attr : attributes.keySet()) {
                String value = attributes.get(attr);
                attrsJson.put(attr, value == null ? "" : value);
            }
            json.put("attributes", attrsJson);
        }

        return json;
    }

    @ApiModelProperty(hidden=true)
    public JSONObject getJsonWithRoles() throws JSONException {
        return getJsonWithRoles(false);
    }

    @ApiModelProperty(hidden=true)
    public JSONObject getJsonWithRoles(boolean oldStyle) throws JSONException {
        JSONObject json = getJson();
        // groups with roles
        if (workgroups != null) {
            for (UserGroupVO group : workgroups) {
                if (group.getRoles() != null) {
                    // show common roles at top level
                    if (UserGroupVO.COMMON_GROUP.equals(group.getName()) && !oldStyle) {
                        JSONArray rolesJson = new JSONArray();
                        for (String role : group.getRoles())
                            rolesJson.put(role);
                        json.put("roles", rolesJson);
                    }
                    else {
                        JSONArray groupRoles = new JSONArray();
                        for (String role : group.getRoles())
                            groupRoles.put(role);
                        json.put(group.getName(), groupRoles);
                    }
                }
            }
        }
        if (oldStyle && json.has("workgroups")) {
            JSONArray groups = (JSONArray)json.remove("workgroups");
            json.put("groups", groups);
        }
        return json;
    }

    public String getJsonName() { return "User"; }

    @ApiModelProperty(hidden=true)
    public String getEmail() {
        String email = getAttribute(EMAIL_ADDRESS);
        if (email == null)
            email = getAttribute(EMAIL_ADDRESS);
        return email;
    }
    public void setEmail(String s) {
        setAttribute(OLD_EMAIL_ADDRESS, s);
    }

    @ApiModelProperty(hidden=true)
    public String getPhone() {
        String phone = getAttribute(PHONE_NUMBER);
        if (phone == null)
            phone = getAttribute(OLD_PHONE_NUMBER);
        return phone;
    }
    public void setPhone(String s) {
        setAttribute(PHONE_NUMBER, s);
    }

    /**
     * first and last names are parsed from name in UserGroupCache
     * (initially for typeahead suggestions)
     */
    private String first;
    @ApiModelProperty(hidden=true)
    public String getFirst() { return first; }
    public void setFirst(String first) { this.first = first; }

    private String last;
    @ApiModelProperty(hidden=true)
    public String getLast() { return last; }
    public void setLast(String last) { this.last = last; }

    // compatibility
    /**
     * @deprecated use {@link #getName()}
     */
    @Deprecated
    @ApiModelProperty(hidden=true)
    public String getUserName() { return getCuid(); }
    /**
     * @deprecated use {@link #setName()}
     */
    @Deprecated
    public void setUserName(String name) { setCuid(name); }
    /**
     * @deprecated use {@link #getWorkgroups()}
     */
    @Deprecated
    @ApiModelProperty(hidden=true)
    public UserGroupVO[] getUserGroups() { return getWorkgroups(); }
    /**
     * @deprecated use {@link #setWorkgroups()}
     */
    @Deprecated
    public void setUserGroups(UserGroupVO[] groups) { setWorkgroups(groups); }
    /**
     * @deprecated use {@link #setGroups()}
     */
    @Deprecated
    public void setUserGroups(List<UserGroupVO> groups) { setGroups(groups); }
    /**
     * @deprecated use {@link #getEmail()}
     */
    @Deprecated
    @ApiModelProperty(hidden=true)
    public String getEmailAddress() { return getEmail(); }
    /**
     * @deprecated use {@link #setEmail()}
     */
    @Deprecated
    public void setEmailAddress(String email) { setEmail(email); }
    /**
     * @deprecated use {@link #getPhone()}
     */
    @Deprecated
    @ApiModelProperty(hidden=true)
    public String getPhoneNumber() { return getPhone(); }
    /**
     * @deprecated use {@link #setPhone()}
     */
    @Deprecated
    public void setPhoneNumber(String phone) { setPhone(phone); }

}
