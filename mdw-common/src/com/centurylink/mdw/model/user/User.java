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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Size;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="User", description="MDW user")
public class User implements Serializable, Comparable<User>, Jsonable {

    // attribute names are displayed as labels
    public static final String EMAIL = "Email";
    public static final String PHONE = "Phone";
    // old unfriendly attribute names for compatibility
    @Deprecated
    public static final String OLD_EMAIL = "Email Address";
    @Deprecated
    public static final String OLD_PHONE = "Phone Number";

    public User() {
    }

    public User(String cuid) {
        this.cuid = cuid;
    }

    public User(JSONObject json) throws JSONException {
        if (json.has("cuid"))
            cuid = json.getString("cuid");
        else
            cuid = json.optString("id");
        if (json.has("name"))
            name = json.getString("name");
        JSONArray grps = null;
        if (json.has("workgroups"))
            grps = json.getJSONArray("workgroups");
        else if (json.has("groups")) // compatibility
            grps = json.getJSONArray("groups");
        if (grps != null) {
            workgroups = new Workgroup[grps.length()];
            for (int i = 0; i < grps.length(); i++) {
                workgroups[i] = new Workgroup();
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
            Workgroup commonGroup = getCommonGroup();
            if (commonGroup == null) {
                Workgroup[] newGroups = new Workgroup[workgroups.length + 1];
                newGroups[0] = Workgroup.getCommonGroup();
                for (int i = 1; i < newGroups.length; i++)
                    newGroups[i] = workgroups[i - 1];
                workgroups = newGroups;
            }
        }
        else {
            workgroups = new Workgroup[] { Workgroup.getCommonGroup() };
        }
        if (json.has("roles")) {
            JSONArray roles = json.getJSONArray("roles");
            Workgroup commonGroup = getCommonGroup();
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

    @Size(min=3,max=128)
    private String cuid;
    @ApiModelProperty(value="User's workstation id", required=true)
    public String getCuid() { return this.cuid; }
    public void setCuid(String cuid) { this.cuid = cuid; }

    private String endDate;
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    private Workgroup[] workgroups;
    @ApiModelProperty(value="User's workgroups")
    public Workgroup[] getWorkgroups() { return workgroups; }
    public void setWorkgroups(Workgroup[] workgroups) { this.workgroups = workgroups; }

    public void setGroups(List<Workgroup> groups) {
        this.workgroups = groups.toArray(new Workgroup[groups.size()]);
    }

    @ApiModelProperty(hidden=true)
    public List<String> getRoles(String group) {
        if (workgroups==null) return null;
        for (Workgroup g : workgroups) {
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
        for (Workgroup group : workgroups) {
            if (!group.getName().equals(Workgroup.COMMON_GROUP))
              groupList.add(group.getName());
        }
        return groupList.toArray(new String[0]);
    }

    @ApiModelProperty(hidden=true)
    public Workgroup getCommonGroup() {
        if (workgroups == null)
            return null;
        for (Workgroup group : workgroups) {
            if (group.getName().equals(Workgroup.COMMON_GROUP))
              return group;
        }
        return null;
    }

    @ApiModelProperty(hidden=true)
    public String getGroupsAndRolesAsString() {
        StringBuffer buffer = new StringBuffer();
        if (workgroups != null) {
            for (Workgroup g : workgroups) {
                 if (buffer.length()>0) buffer.append(",");
                 buffer.append(g.getNameAndRolesAsString());
            }
        }
        return buffer.toString();
    }

    public Workgroup getUserGroup(String groupName) {
        if (workgroups == null)
            return null;
        for (Workgroup group : workgroups) {
            if (group.getName().equals(groupName))
                return group;
        }
        return null;
    }

    /**
     * Check whether user has the specified role in any of the groups
     */
    public boolean hasRole(String roleName) {
        return hasRole(Workgroup.COMMON_GROUP, roleName);
    }

    public boolean hasRole(String groupName, String roleName) {
        if (workgroups == null)
            return false;
        // Site Admin treated as role for some purposes
        if (Workgroup.SITE_ADMIN_GROUP.equals(roleName))
            return belongsToGroup(Workgroup.SITE_ADMIN_GROUP);
        for (Workgroup group : workgroups) {
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
        for (Workgroup g : workgroups) {
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

    public int compareTo(User other) {
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User))
          return false;
        return getId().equals(((User)o).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * This is only used when UserVO is a member of UserGroupVO.
     * Only that group is populated as a substructure to store roles.
     */
    public void addRoleForGroup(String groupName, String roleName) {
        if (workgroups==null) {
            workgroups = new Workgroup[1];
            workgroups[0] = new Workgroup(null, groupName, null);
        }
        List<String> roles = workgroups[0].getRoles();
        if (roles==null) {
            roles = new ArrayList<String>();
            workgroups[0].setRoles(roles);
        }
        roles.add(roleName);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        // json.put("id", getCuid());
        json.put("cuid", getCuid());
        if (name != null)
            json.put("name", getName());
        if (workgroups != null) {
            JSONArray grpsJson = new JSONArray();
            for (Workgroup group : workgroups) {
                grpsJson.put(group.getName());
            }
            json.put("workgroups", grpsJson);
        }
        if (attributes != null) {
            JSONObject attrsJson = create();
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
            for (Workgroup group : workgroups) {
                if (group.getRoles() != null) {
                    // show common roles at top level
                    if (Workgroup.COMMON_GROUP.equals(group.getName()) && !oldStyle) {
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
        String email = getAttribute(EMAIL);
        if (email == null)
            email = getAttribute(OLD_EMAIL);
        return email;
    }
    public void setEmail(String s) {
        setAttribute(EMAIL, s);
    }

    @ApiModelProperty(hidden=true)
    public String getPhone() {
        String phone = getAttribute(PHONE);
        if (phone == null)
            phone = getAttribute(OLD_PHONE);
        return phone;
    }
    public void setPhone(String s) {
        setAttribute(PHONE, s);
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

    /**
     * Set first and last name based on full name.
     */
    public void parseName() {
        if (getName() != null) {
            String name = getName().trim();
            int firstSp = name.indexOf(' ');
            if (firstSp > 0) {
                setFirst(name.substring(0, firstSp));
                int lastSp = name.lastIndexOf(' ');
                setLast(name.substring(lastSp + 1));
            }
            else {
                setFirst(name);
            }
        }
    }
}
