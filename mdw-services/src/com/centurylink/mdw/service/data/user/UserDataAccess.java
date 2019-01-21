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
package com.centurylink.mdw.service.data.user;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.util.StringHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserDataAccess extends CommonDataAccess {

    protected String USER_SELECT_FIELDS = "u.USER_INFO_ID, u.CUID, u.NAME, u.END_DATE, u.COMMENTS";

    public List<User> queryUsers(String whereCondition, boolean withGroups, int startIndex,
            int endIndex, String sortOn) throws DataAccessException {
        try {
            db.openConnection();
            List<User> users = new ArrayList<User>();
            if (startIndex >= 0) {
                if (sortOn == null)
                    sortOn = "CUID";
                String[] fields = { "USER_INFO_ID", "CUID", "NAME", "END_DATE", "COMMENTS" };
                List<String[]> result = super.queryRows("USER_INFO", fields, whereCondition, sortOn,
                        startIndex, endIndex);
                for (String[] one : result) {
                    String name = one[2] != null ? one[2] : one[4];
                    User user = new User();
                    user.setId(new Long(one[0]));
                    user.setCuid(one[1]);
                    user.setName(name);
                    user.setEndDate(one[3]);
                    users.add(user);
                }
            }
            else {
                String sql = "select " + USER_SELECT_FIELDS + " from USER_INFO u";
                if (whereCondition != null)
                    sql = sql + " where " + whereCondition;
                sql += sortOn == null ? " order by CUID" : (" order by " + sortOn);
                ResultSet rs = db.runSelect(sql);
                while (rs.next()) {
                    users.add(createUserInfoFromResultSet(rs));
                }
            }
            if (withGroups) {
                for (User user : users) {
                    loadGroupsRolesForUser(user);
                }
            }
            return users;
        }
        catch (SQLException ex) {
            throw new DataAccessException(-1, "Failed to load users", ex);
        }
        catch (CachingException e) {
            throw new DataAccessException(-1, "Failed to load site admin group", e);
        }
        finally {
            db.closeConnection();
        }
    }

    protected Long getNextId(String sequenceName) throws SQLException {
        String query = "select " + sequenceName + ".NEXTVAL from DUAL";
        ResultSet rs = db.runSelect(query);
        rs.next();
        return new Long(rs.getString(1));
    }

    public Long saveUser(User user) throws DataAccessException {
        try {
            db.openConnection();
            Long id = user.getId();

            // check if the user is in  deleted user-info list
            String sql = "select USER_INFO_ID  from USER_INFO u where u.CUID=? AND END_DATE is not NULL";
            ResultSet rs = db.runSelect(sql, user.getCuid());
            if (rs.next()) {
                id = rs.getLong(1);
            }
            if (id == null || id.longValue() <= 0L) {
                id = db.isMySQL() ? null : getNextId("MDW_COMMON_ID_SEQ");
                String query = "insert into USER_INFO"
                        + " (USER_INFO_ID, CUID, CREATE_DT, CREATE_USR, NAME)" + " values (?, ?, "
                        + now() + ", ?, ?)";
                Object[] args = new Object[4];
                args[0] = id;
                args[1] = user.getCuid();
                args[2] = "MDW Engine";
                args[3] = user.getName();
                if (db.isMySQL())
                    id = db.runInsertReturnId(query, args);
                else
                    db.runUpdate(query, args);
            }
            else {
                String query = "update USER_INFO set CUID=?, NAME=?,END_DATE=? where USER_INFO_ID=?";
                Object[] args = new Object[4];
                args[0] = user.getCuid();
                args[1] = user.getName();
                args[2] = null;
                args[3] = id;
                db.runUpdate(query, args);
            }
            db.commit();
            return id;
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1, "Failed to save user", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public User getUser(Long userId) throws DataAccessException {
        try {
            db.openConnection();
            String sql = "select " + USER_SELECT_FIELDS
                    + " from USER_INFO u where u.USER_INFO_ID=?";
            ResultSet rs = db.runSelect(sql, userId);
            if (rs.next()) {
                User user = createUserInfoFromResultSet(rs);
                loadGroupsRolesForUser(user);
                return user;
            }
            else
                return null;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, "Failed to get user", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public User getUser(String userName) throws DataAccessException {
        try {
            User user = null;
            db.openConnection();
            String sql = "select " + USER_SELECT_FIELDS + " from USER_INFO u where lower(u.CUID)=?";
               sql += " and END_DATE is null";
             ResultSet rs = db.runSelect(sql, userName.toLowerCase());
             if (rs.next()) {
                 user = createUserInfoFromResultSet(rs);
             }
             if (user != null) {
                 loadGroupsRolesForUser(user);
                 loadAttributesForUser(user);
             }
             return user;
        } catch(Exception ex){
            throw new DataAccessException(-1, "Failed to get user: " + userName, ex);
        } finally {
            db.closeConnection();
        }
    }

    private void loadUsersRolesForGroup(String groupName, List<User> users) throws SQLException {
        if (groupName.equals(Workgroup.COMMON_GROUP)) {
            // load global roles for the common group
            // we translate the old names to new ones
            String sql = "select u.CUID, r.USER_ROLE_NAME "
                    + "from USER_INFO u, USER_ROLE r, USER_GROUP_MAPPING ugm, USER_GROUP g, USER_ROLE_MAPPING urm "
                    + "where g.GROUP_NAME=? " + "and ugm.USER_GROUP_ID=g.USER_GROUP_ID "
                    + "and ugm.USER_INFO_ID=u.USER_INFO_ID " + "and ugm.COMMENTS is null "
                    + "and urm.USER_ROLE_MAPPING_OWNER='" + OwnerType.USER + "' "
                    + "and urm.USER_ROLE_MAPPING_OWNER_ID=u.USER_INFO_ID "
                    + "and urm.USER_ROLE_ID=r.USER_ROLE_ID";
            ResultSet rs = db.runSelect(sql, groupName);
            while (rs.next()) {
                String cuid = rs.getString(1);
                String role = rs.getString(2);
                for (User user : users) {
                    if (cuid.equals(user.getCuid())) {
                        user.addRoleForGroup(groupName, role);
                        break;
                    }
                }
            }
        }
        else {
            // load roles for the users in the group
            String sql = "select u.CUID, r.USER_ROLE_NAME "
                    + "from USER_INFO u, USER_GROUP g, USER_GROUP_MAPPING ugm, USER_ROLE r, USER_ROLE_MAPPING ugrm "
                    + "where g.GROUP_NAME = ?" + "    and ugm.USER_GROUP_ID = g.USER_GROUP_ID"
                    + "    and ugm.USER_INFO_ID = u.USER_INFO_ID"
                    + "    and ugrm.USER_ROLE_MAPPING_OWNER='" + OwnerType.USER_GROUP_MAP + "'"
                    + "    and ugrm.USER_ROLE_MAPPING_OWNER_ID = ugm.USER_GROUP_MAPPING_ID"
                    + "    and ugrm.USER_ROLE_ID = r.USER_ROLE_ID";
            ResultSet rs = db.runSelect(sql, groupName);
            while (rs.next()) {
                String cuid = rs.getString(1);
                String role = rs.getString(2);
                for (User user : users) {
                    if (cuid.equals(user.getCuid())) {
                        user.addRoleForGroup(groupName, role);
                        break;
                    }
                }
            }
        }
    }

    public List<User> getUsersForGroup(String groupName, boolean loadRoles)
            throws DataAccessException {
        try {
            db.openConnection();

            List<User> users = new ArrayList<User>();
            String sql = "select " + USER_SELECT_FIELDS
                    + " from USER_INFO u, USER_GROUP_MAPPING ugm, USER_GROUP ug "
                    + "where u.END_DATE is null " + "   and u.USER_INFO_ID = ugm.USER_INFO_ID"
                    + "   and ugm.USER_GROUP_ID = ug.USER_GROUP_ID" + "   and ug.GROUP_NAME = ? "
                    + "order by u.CUID";
            ResultSet rs = db.runSelect(sql, groupName);
            while (rs.next()) {
                users.add(createUserInfoFromResultSet(rs));
            }
            if (loadRoles)
                this.loadUsersRolesForGroup(groupName, users);
            return users;
        }
        catch (SQLException ex) {
            throw new DataAccessException(-1, "Failed to load users", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Workgroup getGroup(String groupName) throws DataAccessException {
        try {
            Workgroup group = null;
            db.openConnection();
            String sql = "select USER_GROUP_ID, COMMENTS, PARENT_GROUP_ID, END_DATE "
                    + " from USER_GROUP where GROUP_NAME=? and END_DATE is null";
            ResultSet rs = db.runSelect(sql, groupName);
            if (rs.next()) {
                Long id = rs.getLong(1);
                String comments = rs.getString(2);
                group = new Workgroup(id, groupName, comments);
                long pid = rs.getLong(3);
                group.setEndDate(rs.getString(4));
                if (pid > 0L) {
                    rs = db.runSelect("select GROUP_NAME from USER_GROUP where USER_GROUP_ID=?",
                            pid);
                    if (rs.next())
                        group.setParentGroup(rs.getString(1));
                }
            }
            if (group != null)
                loadAttributesForGroup(group);
            return group;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, "Failed to get user group", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Workgroup getGroup(Long groupId) throws DataAccessException {
        try {
            Workgroup group = null;
            db.openConnection();
            String sql = "select GROUP_NAME, COMMENTS, PARENT_GROUP_ID, END_DATE "
                    + " from USER_GROUP where USER_GROUP_ID=?";
            ResultSet rs = db.runSelect(sql, groupId);
            if (rs.next()) {
                String groupName = rs.getString(1);
                String comments = rs.getString(2);
                group = new Workgroup(groupId, groupName, comments);
                long pid = rs.getLong(3);
                if (pid > 0L) {
                    rs = db.runSelect(sql, pid);
                    if (rs.next())
                        group.setParentGroup(rs.getString(1));
                }
                group.setEndDate(rs.getString(4));
            }
            if (group != null)
                loadAttributesForGroup(group);
            return group;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, "Failed to get user group", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<Role> getAllRoles() throws DataAccessException {
        try {
            db.openConnection();
            List<Role> roles = new ArrayList<Role>();
            String sql = "select USER_ROLE_ID, USER_ROLE_NAME, COMMENTS from USER_ROLE order by USER_ROLE_NAME";
            ResultSet rs = db.runSelect(sql);
            while (rs.next()) {
                Role role = new Role();
                role.setId(rs.getLong(1));
                role.setName(rs.getString(2));
                role.setDescription(rs.getString(3));
                roles.add(role);
            }
            return roles;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, "Failed to get all user roles", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Role getRole(String roleName) throws DataAccessException {
        try {
            db.openConnection();
            String sql = "select USER_ROLE_ID, COMMENTS "
                    + " from USER_ROLE where USER_ROLE_NAME=?";
            ResultSet rs = db.runSelect(sql, roleName);
            if (rs.next()) {
                Role role = new Role();
                role.setId(rs.getLong(1));
                role.setName(roleName);
                role.setDescription(rs.getString(2));
                return role;
            }
            else
                return null;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, "Failed to get user role: " + roleName, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Role getRole(Long roleId) throws DataAccessException {
        try {
            db.openConnection();
            String sql = "select USER_ROLE_NAME, COMMENTS "
                    + " from USER_ROLE where USER_ROLE_ID=?";
            ResultSet rs = db.runSelect(sql, roleId);
            if (rs.next()) {
                Role role = new Role();
                role.setId(roleId);
                role.setName(rs.getString(1));
                role.setDescription(rs.getString(2));
                return role;
            }
            else
                return null;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, "Failed to get user role", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<String> getRolesForGroup(Long groupId) throws DataAccessException {
        try {
            List<String> roles = new ArrayList<String>();
            db.openConnection();
            String sql = "select ur.USER_ROLE_ID, ur.USER_ROLE_NAME, ur.COMMENTS "
                    + "from USER_GROUP ug, USER_ROLE ur, USER_ROLE_MAPPING urm "
                    + "where ug.USER_GROUP_ID = ? "
                    + "   and urm.USER_ROLE_MAPPING_OWNER = 'USER_GROUP'"
                    + "   and urm.USER_ROLE_MAPPING_OWNER_ID = ug.USER_GROUP_ID"
                    + "   and urm.USER_ROLE_ID = ur.USER_ROLE_ID ";
            ResultSet rs = db.runSelect(sql, groupId);
            while (rs.next()) {
                roles.add(rs.getString(2));
            }
            return roles;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, "Failed to get user role", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<Role> getRolesForAction(Long taskActionId) throws DataAccessException {
        try {
            List<Role> roles = new ArrayList<Role>();
            db.openConnection();
            String sql = "select ur.USER_ROLE_ID, ur.USER_ROLE_NAME, ur.COMMENTS "
                    + "from USER_ROLE ur, TASK_ACTN_USR_ROLE_MAPP taurm "
                    + "where taurm.TASK_ACTION_ID = ?"
                    + "   and ur.USER_ROLE_ID = taurm.USER_ROLE_ID " + "order by ur.USER_ROLE_NAME";
            ResultSet rs = db.runSelect(sql, taskActionId);
            while (rs.next()) {
                Role role = new Role();
                role.setId(rs.getLong(1));
                role.setName(rs.getString(2));
                role.setDescription(rs.getString(3));
                roles.add(role);
            }
            return roles;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, "Failed to get roles for task action", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<User> getUsersForRole(String roleName) throws DataAccessException {
        try {
            db.openConnection();

            List<User> users = new ArrayList<User>();
            String sql = "select " + USER_SELECT_FIELDS
                    + " from USER_INFO u, USER_ROLE_MAPPING urm, USER_ROLE ur "
                    + " where u.END_DATE is null and "
                    + " u.USER_INFO_ID = urm.USER_ROLE_MAPPING_OWNER_ID"
                    + "   and urm.USER_ROLE_MAPPING_OWNER='USER'"
                    + "   and urm.USER_ROLE_ID = ur.USER_ROLE_ID" + "   and ur.USER_ROLE_NAME = ? "
                    + "order by u.CUID";
            ResultSet rs = db.runSelect(sql, roleName);
            while (rs.next()) {
                users.add(createUserInfoFromResultSet(rs));
            }
            return users;
        }
        catch (SQLException ex) {
            throw new DataAccessException(-1, "Failed to load users for role", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Long saveGroup(Workgroup group) throws DataAccessException {
        try {
            db.openConnection();
            Long id = group.getId();
            Long parentId;
            if (group.getParentGroup() != null) {
                ResultSet rs = db.runSelect(
                        "select USER_GROUP_ID from USER_GROUP where GROUP_NAME=?",
                        group.getParentGroup());
                if (rs.next())
                    parentId = rs.getLong(1);
                else
                    parentId = null;
            }
            else
                parentId = null;

            // check if the group was in the deleted group list
            ResultSet rs = db.runSelect(
                    "select USER_GROUP_ID,GROUP_NAME from USER_GROUP where END_DATE IS NOT NULL AND GROUP_NAME=?",
                    group.getName());
            if (rs.next()) {
                id = rs.getLong(1);
            }
            if (id == null || id.longValue() <= 0L) {
                id = db.isMySQL() ? null : getNextId("MDW_COMMON_ID_SEQ");
                String query = "insert into USER_GROUP"
                        + " (USER_GROUP_ID, GROUP_NAME, CREATE_DT, CREATE_USR, COMMENTS, PARENT_GROUP_ID)"
                        + " values (?, ?, " + now() + ", ?, ?, ?)";
                Object[] args = new Object[5];
                args[0] = id;
                args[1] = group.getName();
                args[2] = "MDW Engine";
                args[3] = group.getDescription();
                args[4] = parentId;
                if (db.isMySQL())
                    id = db.runInsertReturnId(query, args);
                else
                    db.runUpdate(query, args);

            }
            else {
                String query = "update USER_GROUP set GROUP_NAME=?, COMMENTS=?, PARENT_GROUP_ID=?,END_DATE=? where USER_GROUP_ID=?";
                Object[] args = new Object[5];
                args[0] = group.getName();
                args[1] = group.getDescription();
                args[2] = parentId;
                args[3] = null;
                args[4] = id;
                db.runUpdate(query, args);
            }

            db.commit();
            return id;
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1, "Failed to save group", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void deleteUser(Long userId) throws DataAccessException {
        try {
            db.openConnection();
            // delete user-group mapping
            String query = "delete from USER_GROUP_MAPPING where USER_INFO_ID=?";
            db.runUpdate(query, userId);
            // delete user-role mapping
            query = "delete from USER_ROLE_MAPPING where USER_ROLE_MAPPING_OWNER='USER'"
                    + " and USER_ROLE_MAPPING_OWNER_ID=?";
            db.runUpdate(query, userId);
            // delete user attributes
            query = "delete from ATTRIBUTE where ATTRIBUTE_OWNER='USER' and ATTRIBUTE_OWNER_ID=?";
            db.runUpdate(query, userId);
            // end-date user itself
            query = "update USER_INFO set END_DATE=" + now() + " where USER_INFO_ID=?";
            db.runUpdate(query, userId);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1, "Failed to delete user", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void deleteGroup(Long groupId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "";
            // delete user-group to role mapping
            query = "delete from USER_ROLE_MAPPING where USER_ROLE_MAPPING_OWNER='"
                    + OwnerType.USER_GROUP_MAP + "'"
                    + " and USER_ROLE_MAPPING_OWNER_ID in (select USER_GROUP_MAPPING_ID "
                    + "      from USER_GROUP_MAPPING where USER_GROUP_ID=?)";
            db.runUpdate(query, groupId);
            // delete user-group mapping
            query = "delete from USER_GROUP_MAPPING where USER_GROUP_ID=?";
            db.runUpdate(query, groupId);
            // delete group-role mapping (backward compatibility code)
            query = "delete from USER_ROLE_MAPPING where USER_ROLE_MAPPING_OWNER='USER_GROUP'"
                    + " and USER_ROLE_MAPPING_OWNER_ID=?";
            db.runUpdate(query, groupId);
            // delete group attributes
            query = "delete from ATTRIBUTE where ATTRIBUTE_OWNER='" + OwnerType.USER_GROUP + "' and ATTRIBUTE_OWNER_ID=?";
            db.runUpdate(query, groupId);
            // end-date the group itself
            query = "update USER_GROUP set END_DATE=" + now() + " where USER_GROUP_ID=?";
            db.runUpdate(query, groupId);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1, "Failed to delete group", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void deleteRole(Long roleId) throws DataAccessException {
        try {
            db.openConnection();
            // delete user-role and group-role mapping
            String query = "delete from USER_ROLE_MAPPING where USER_ROLE_ID=?";
            db.runUpdate(query, roleId);
            // delete the role itself
            query = "delete from USER_ROLE where USER_ROLE_ID=?";
            db.runUpdate(query, roleId);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1, "Failed to delete role", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Long saveRole(Role role) throws DataAccessException {
        try {
            db.openConnection();
            Long id = role.getId();
            if (id == null || id.longValue() <= 0L) {
                id = db.isMySQL() ? null : getNextId("MDW_COMMON_ID_SEQ");
                String query = "insert into USER_ROLE"
                        + " (USER_ROLE_ID, USER_ROLE_NAME, CREATE_DT, CREATE_USR, COMMENTS)"
                        + " values (?, ?, " + now() + ", ?, ?)";
                Object[] args = new Object[4];
                args[0] = id;
                args[1] = role.getName();
                args[2] = "MDW Engine";
                args[3] = role.getDescription();
                if (db.isMySQL())
                    id = db.runInsertReturnId(query, args);
                else
                    db.runUpdate(query, args);
            }
            else {
                String query = "update USER_ROLE set USER_ROLE_NAME=?, COMMENTS=? where USER_ROLE_ID=?";
                Object[] args = new Object[3];
                args[0] = role.getName();
                args[1] = role.getDescription();
                args[2] = id;
                db.runUpdate(query, args);
            }
            db.commit();
            return id;
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1, "Failed to save role", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private void updateMembersByName(Long id, String[] members, String selectQuery,
            String deleteQuery, String findQuery, String insertQuery, String errmsg)
            throws DataAccessException {
        try {
            db.openConnection();
            ResultSet rs = db.runSelect(selectQuery, id);
            List<String> existing = new ArrayList<String>();
            HashMap<String, Long> existingIds = new HashMap<String, Long>();
            while (rs.next()) {
                Long mid = rs.getLong(1);
                String mname = rs.getString(2);
                existing.add(mname);
                existingIds.put(mname, mid);
            }
            Object[] args = new Object[2];
            args[0] = id;
            for (String e : existing) {
                boolean found = false;
                for (String m : members) {
                    if (m.equals(e)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    args[1] = existingIds.get(e);
                    db.runUpdate(deleteQuery, args);
                }
            }
            for (String m : members) {
                boolean found = false;
                for (String e : existing) {
                    if (m.equals(e)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    rs = db.runSelect(findQuery, m);
                    if (rs.next()) {
                        args[1] = rs.getLong(1);
                        db.runUpdate(insertQuery, args);
                    }
                    else {
                        throw new Exception("Cannot find " + m);
                    }
                }
            }
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1, errmsg, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void updateRolesForUser(Long userId, Long groupId, String[] roles)
            throws DataAccessException {
        if (groupId.equals(Workgroup.COMMON_GROUP_ID)) {
            String selectQuery = "select ur.USER_ROLE_ID, ur.USER_ROLE_NAME "
                    + "from USER_INFO u, USER_ROLE ur, USER_ROLE_MAPPING urm "
                    + "where u.USER_INFO_ID = ? " + "   and urm.USER_ROLE_MAPPING_OWNER = 'USER'"
                    + "   and urm.USER_ROLE_MAPPING_OWNER_ID = u.USER_INFO_ID"
                    + "   and urm.USER_ROLE_ID = ur.USER_ROLE_ID";
            String deleteQuery = "delete from USER_ROLE_MAPPING where USER_ROLE_MAPPING_OWNER='USER'"
                    + " and USER_ROLE_MAPPING_OWNER_ID=? and USER_ROLE_ID=?";
            String findQuery = "select USER_ROLE_ID from USER_ROLE where USER_ROLE_NAME=?";
            String insertQuery = "insert into USER_ROLE_MAPPING"
                    + " (USER_ROLE_MAPPING_ID, USER_ROLE_MAPPING_OWNER, USER_ROLE_MAPPING_OWNER_ID,"
                    + "  CREATE_DT,CREATE_USR,USER_ROLE_ID) values ("
                    + (db.isMySQL() ? "null" : "MDW_COMMON_ID_SEQ.NEXTVAL") + ",'USER',?," + now()
                    + ",'MDW',?)";
            String errmsg = "Failed to update roles for user";
            updateMembersByName(userId, roles, selectQuery, deleteQuery, findQuery, insertQuery,
                    errmsg);
        }
        else {
            Long ugmId;
            try {
                db.openConnection();
                String sql = "select USER_GROUP_MAPPING_ID "
                        + "from USER_GROUP_MAPPING where USER_INFO_ID = ? and USER_GROUP_ID=?";
                Object[] args = new Object[2];
                args[0] = userId;
                args[1] = groupId;
                ResultSet rs = db.runSelect(sql, args);
                if (rs.next()) {
                    ugmId = rs.getLong(1);
                    sql = "update USER_GROUP_MAPPING set COMMENTS='Converted' where USER_GROUP_MAPPING_ID=?";
                    db.runUpdate(sql, ugmId);
                }
                else
                    throw new Exception("User-group mapping does not exist");
            }
            catch (Exception ex) {
                throw new DataAccessException(-1, "Failed to find user-group mapping", ex);
            }
            finally {
                db.closeConnection();
            }
            String selectQuery = "select r.USER_ROLE_ID, r.USER_ROLE_NAME "
                    + "from USER_ROLE r, USER_GROUP_MAPPING ugm, USER_ROLE_MAPPING urm "
                    + "where ugm.USER_GROUP_MAPPING_ID = ? "
                    + "   and urm.USER_ROLE_MAPPING_OWNER = '" + OwnerType.USER_GROUP_MAP + "'"
                    + "   and urm.USER_ROLE_MAPPING_OWNER_ID = ugm.USER_GROUP_MAPPING_ID"
                    + "   and urm.USER_ROLE_ID = r.USER_ROLE_ID";
            String deleteQuery = "delete from USER_ROLE_MAPPING where"
                    + " USER_ROLE_MAPPING_OWNER='" + OwnerType.USER_GROUP_MAP + "'"
                    + " and USER_ROLE_MAPPING_OWNER_ID=? and USER_ROLE_ID=?";
            String findQuery = "select USER_ROLE_ID from USER_ROLE where USER_ROLE_NAME=?";
            String insertQuery = "insert into USER_ROLE_MAPPING"
                    + " (USER_ROLE_MAPPING_ID, USER_ROLE_MAPPING_OWNER, USER_ROLE_MAPPING_OWNER_ID,"
                    + "  CREATE_DT,CREATE_USR,USER_ROLE_ID) values ("
                    + (db.isMySQL() ? "null" : "MDW_COMMON_ID_SEQ.NEXTVAL") + ",'"
                    + OwnerType.USER_GROUP_MAP + "',?," + now() + ",'MDW',?)";
            String errmsg = "Failed to update roles for user";
            updateMembersByName(ugmId, roles, selectQuery, deleteQuery, findQuery, insertQuery,
                    errmsg);
        }
    }

    public void updateGroupsForUser(Long userId, String[] groups) throws DataAccessException {
        String selectQuery = "select ug.USER_GROUP_ID, ug.GROUP_NAME "
                + "from USER_INFO u, USER_GROUP ug, USER_GROUP_MAPPING ugm "
                + "where u.USER_INFO_ID = ? " + "   and ugm.USER_INFO_ID = u.USER_INFO_ID"
                + "   and ugm.USER_GROUP_ID = ug.USER_GROUP_ID";
        String deleteQuery = "delete from USER_GROUP_MAPPING where USER_INFO_ID=? and USER_GROUP_ID=?";
        String findQuery = "select USER_GROUP_ID from USER_GROUP where GROUP_NAME=?";
        String insertQuery = "insert into USER_GROUP_MAPPING"
                + " (USER_GROUP_MAPPING_ID, USER_INFO_ID,"
                + "  CREATE_DT,CREATE_USR,USER_GROUP_ID,COMMENTS) values ("
                + (db.isMySQL() ? "null" : "MDW_COMMON_ID_SEQ.NEXTVAL") + ",?," + now()
                + ",'MDW',?,'Converted')";
        String errmsg = "Failed to update groups for user";
        updateMembersByName(userId, groups, selectQuery, deleteQuery, findQuery, insertQuery,
                errmsg);
    }

    public void addUserToGroup(String cuid, String group) throws DataAccessException {
        String query = "insert into USER_GROUP_MAPPING" + " (USER_GROUP_MAPPING_ID, USER_INFO_ID,"
                + "  CREATE_USR, CREATE_DT, USER_GROUP_ID) values ("
                + (db.isMySQL() ? "null" : "MDW_COMMON_ID_SEQ.NEXTVAL") + ", "
                + "(select distinct user_info_id from USER_INFO where cuid = ? and END_DATE is NULL), 'MDW', "
                + now() + ", " + "(select user_group_id from USER_GROUP where group_name = ?))";
        try {
            db.openConnection();
            db.runUpdate(query, new Object[]{cuid, group});
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1, "Failed to add user " + cuid + " to group " + group,
                    ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void removeUserFromGroup(String cuid, String group) throws DataAccessException {
        String query = "delete from USER_GROUP_MAPPING "
                + " where user_info_id = (select distinct user_info_id from USER_INFO where cuid = '"
                + cuid + "' and END_DATE is NULL)"
                + " and user_group_id = (select user_group_id from USER_GROUP where group_name = '"
                + group + "')";
        try {
            db.openConnection();
            db.runUpdate(query);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1,
                    "Failed to remove user " + cuid + " from group " + group, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void addUserToRole(String cuid, String role) throws DataAccessException {
        String query = "insert into USER_ROLE_MAPPING "
                + " (USER_ROLE_MAPPING_ID, USER_ROLE_MAPPING_OWNER, USER_ROLE_MAPPING_OWNER_ID,"
                + "  CREATE_DT,CREATE_USR,USER_ROLE_ID) values ("
                + (db.isMySQL() ? "null" : "MDW_COMMON_ID_SEQ.NEXTVAL") + ",'USER', "
                + "(select distinct user_info_id from USER_INFO where cuid = ? and END_DATE is NULL),"
                + now() + ",'MDW',"
                + "(select user_role_id from USER_ROLE where user_role_name = ?))";
        try {
            db.openConnection();
            db.runUpdate(query, new Object[]{cuid, role});
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1, "Failed to add user " + cuid + " to role " + role,
                    ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void removeUserFromRole(String cuid, String role) throws DataAccessException {
        // delete user-role mapping
        String query = "delete from USER_ROLE_MAPPING "
                + " where USER_ROLE_MAPPING_OWNER_ID= (select distinct user_info_id from USER_INFO where cuid = '"
                + cuid + "' and END_DATE is NULL ) "
                + " and user_role_id = (select user_role_id from USER_ROLE where user_role_name = '"
                + role + "')";
        try {
            db.openConnection();
            db.runUpdate(query);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1,
                    "Failed to remove user " + cuid + " from role " + role, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void updateUsersForGroup(Long groupId, Long[] users) throws DataAccessException {
        String selectQuery = "select u.USER_INFO_ID "
                + "from USER_INFO u, USER_GROUP ug, USER_GROUP_MAPPING ugm "
                + "where ug.USER_GROUP_ID = ? " + "   and ugm.USER_INFO_ID = u.USER_INFO_ID"
                + "   and ugm.USER_GROUP_ID = ug.USER_GROUP_ID";
        String deleteQuery = "delete from USER_GROUP_MAPPING where USER_GROUP_ID=? "
                + " and USER_INFO_ID=?";
        String insertQuery = "insert into USER_GROUP_MAPPING"
                + " (USER_GROUP_MAPPING_ID, USER_GROUP_ID, USER_INFO_ID,"
                + "  CREATE_DT,CREATE_USR,COMMENTS) values ("
                + (db.isMySQL() ? "null" : "MDW_COMMON_ID_SEQ.NEXTVAL") + ",?,?," + now()
                + ",'MDW','Converted')";
        String errmsg = "Failed to update users for group";
        this.updateMembersById(groupId, users, selectQuery, deleteQuery, insertQuery, errmsg);
    }

    public void updateUsersForGroup(Long groupId, String[] users) throws DataAccessException {
        String selectQuery = "select u.USER_INFO_ID, u.CUID "
                + "from USER_INFO u, USER_GROUP ug, USER_GROUP_MAPPING ugm "
                + "where ug.USER_GROUP_ID = ? " + "   and ugm.USER_INFO_ID = u.USER_INFO_ID"
                + "   and ugm.USER_GROUP_ID = ug.USER_GROUP_ID";
        String deleteQuery = "delete from USER_GROUP_MAPPING where USER_GROUP_ID=? and USER_INFO_ID=?";
        String findQuery = "select USER_INFO_ID from USER_INFO where CUID=?";
        String insertQuery = "insert into USER_GROUP_MAPPING"
                + " (USER_GROUP_MAPPING_ID, USER_GROUP_ID,"
                + "  CREATE_DT,CREATE_USR,USER_INFO_ID,COMMENTS) values ("
                + (db.isMySQL() ? "null" : "MDW_COMMON_ID_SEQ.NEXTVAL") + ",?," + now()
                + ",'MDW',?,'Converted')";
        String errmsg = "Failed to update groups for user";
        updateMembersByName(groupId, users, selectQuery, deleteQuery, findQuery, insertQuery,
                errmsg);
    }

    public void updateUserAttributes(Long userId, Map<String,String> attributes)
            throws DataAccessException {
        try {
            db.openConnection();

            String deleteQuery = "delete from ATTRIBUTE where " + " ATTRIBUTE_OWNER='"
                    + OwnerType.USER + "' and ATTRIBUTE_OWNER_ID=?";
            db.runUpdate(deleteQuery, userId);

            if (attributes != null && !attributes.isEmpty()) {
                List<Attribute> attrs = new ArrayList<Attribute>();
                for (String name : attributes.keySet()) {
                    String value = attributes.get(name);
                    if (value != null && !value.isEmpty())
                        attrs.add(new Attribute(name, value));
                }
                addAttributes0(OwnerType.USER, userId, attrs);
            }
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1,
                    "Failed to update user attributes for userId: " + userId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void updateGroupAttributes(Long groupId, Map<String, String> attributes)
            throws DataAccessException {
        try {
            db.openConnection();

            String deleteQuery = "delete from ATTRIBUTE where " + " ATTRIBUTE_OWNER='"
                    + OwnerType.USER_GROUP
                    + "' and ATTRIBUTE_OWNER_ID=? ";
            db.runUpdate(deleteQuery, groupId);

            if (attributes != null && !attributes.isEmpty()) {
                List<Attribute> attrs = new ArrayList<Attribute>();
                for (String name : attributes.keySet()) {
                    String value = attributes.get(name);
                    if (value != null && !value.isEmpty())
                        attrs.add(new Attribute(name, value));
                }
                addAttributes0(OwnerType.USER_GROUP, groupId, attrs);
            }
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1,
                    "Failed to update user attributes for userId: " + groupId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<String> getUserAttributeNames() throws DataAccessException {
        try {
            db.openConnection();
            List<String> attrs = new ArrayList<String>();
            String query = "select distinct attribute_name from ATTRIBUTE "
                    + "where attribute_owner = 'USER' "
                    + "order by lower(attribute_name)";
            ResultSet rs = db.runSelect(query);
            while (rs.next())
                attrs.add(rs.getString("attribute_name"));
            return attrs;
        }
        catch (Exception e) {
            throw new DataAccessException(0, "failed to get user attribute names", e);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<String> getGroupAttributeNames() throws DataAccessException {
        try {
            db.openConnection();
            List<String> attrs = new ArrayList<String>();
            String query = "select distinct attribute_name from ATTRIBUTE "
                    + "where attribute_owner = '" + OwnerType.USER_GROUP + "' "
                    + "order by lower(attribute_name)";
            ResultSet rs = db.runSelect(query);
            while (rs.next())
                attrs.add(rs.getString("attribute_name"));
            return attrs;
        }
        catch (Exception e) {
            throw new DataAccessException(0, "failed to get group attribute names", e);
        }
        finally {
            db.closeConnection();
        }
    }

    protected User createUserInfoFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong(1));
        user.setCuid(rs.getString(2));
        String name = rs.getString(3);
        if (name==null) name = rs.getString(5);
        // Set Cuid as name to handle migrated users from MDW4 to 5
        // and comment is missing in user_info table
        if (StringHelper.isEmpty(name)) name = rs.getString(2);
        user.setEndDate(rs.getString(4));
        user.setName(name);
        user.parseName();
        return user;
    }

    protected void loadGroupsRolesForUser(User user) throws SQLException, CachingException {
        // load groups
        String sql = "select g.USER_GROUP_ID, g.GROUP_NAME, g.COMMENTS, ug.COMMENTS " +
            "from USER_GROUP_MAPPING ug, USER_GROUP g " +
            "where ug.USER_GROUP_ID = g.USER_GROUP_ID and ug.USER_INFO_ID = ? ";
        sql += "order by lower(g.GROUP_NAME)";

        ResultSet rs = db.runSelect(sql, user.getId());
        ArrayList<Workgroup> groups = new ArrayList<Workgroup>();
        Map<String,Boolean> rolesConverted = new HashMap<String,Boolean>();
        while (rs.next()) {
            Long groupId = rs.getLong(1);
            String groupName = rs.getString(2);
            String comment = rs.getString(3);
            String converted = rs.getString(4);
            Workgroup group = new Workgroup(groupId, groupName, comment);
            rolesConverted.put(groupName, "Converted".equalsIgnoreCase(converted));
            groups.add(group);
        }
        // load roles for the groups other than the shared
        sql = "select r.USER_ROLE_NAME, ug.USER_GROUP_ID " +
            "from USER_GROUP_MAPPING ug, USER_ROLE r, USER_ROLE_MAPPING ugr " +
            "where ug.USER_INFO_ID = ? " +
            "    and ugr.USER_ROLE_MAPPING_OWNER='" + OwnerType.USER_GROUP_MAP + "'" +
            "    and ugr.USER_ROLE_MAPPING_OWNER_ID = ug.USER_GROUP_MAPPING_ID" +
            "    and ugr.USER_ROLE_ID = r.USER_ROLE_ID";
        rs = db.runSelect(sql, user.getId());
        while (rs.next()) {
            Long groupId = rs.getLong(2);
            for (Workgroup group : groups) {
                if (group.getId().equals(groupId)) {
                    List<String> roles = group.getRoles();
                    if (roles==null) {
                        roles = new ArrayList<String>();
                        group.setRoles(roles);
                    }
                    roles.add(rs.getString(1));
                    break;
                }
            }
        }
        // load roles for the shared group
        sql = "select r.USER_ROLE_NAME " +
            "from USER_INFO u, USER_ROLE r, USER_ROLE_MAPPING ur " +
            "where u.CUID = ?" +
            "   and ((u.USER_INFO_ID = ur.USER_ROLE_MAPPING_OWNER_ID" +
            "         and ur.USER_ROLE_MAPPING_OWNER = '" + OwnerType.USER + "'" +
            "          and r.USER_ROLE_ID = ur.USER_ROLE_ID)" +
            "       or (ur.USER_ROLE_MAPPING_OWNER = '" + OwnerType.USER_GROUP + "'" +
            "         and ur.USER_ROLE_MAPPING_OWNER_ID in " +
            "           (select ug.USER_GROUP_ID from USER_GROUP_MAPPING ug" +
            "            where ug.USER_INFO_ID = u.USER_INFO_ID" +
            "              and r.USER_ROLE_ID = ur.USER_ROLE_ID))) " +
            "order by r.USER_ROLE_NAME";
        rs = db.runSelect(sql, user.getCuid());
        List<String> sharedRoles = new ArrayList<String>();
        while (rs.next()) {
            String roleName = rs.getString(1);
            if (!sharedRoles.contains(roleName))
                sharedRoles.add(roleName);
        }
        Workgroup sharedGroup = new Workgroup(Workgroup.COMMON_GROUP_ID, Workgroup.COMMON_GROUP, null);
        sharedGroup.setRoles(sharedRoles);
        groups.add(sharedGroup);
        // set groups to user
        Collections.sort(groups);
        user.setGroups(groups);
    }

    protected void loadAttributesForUser(User user) throws SQLException, CachingException {
        // load attributes for user
        String sql = "select DISTINCT att1.attribute_name, att1.attribute_value from ATTRIBUTE att1  " +
                " where att1.attribute_owner = '" + OwnerType.USER + "' and att1.attribute_owner_id  = ?" +
                " UNION  " +
                " select DISTINCT att2.attribute_name, '' from ATTRIBUTE att2 " +
                " where att2.attribute_owner = '" + OwnerType.USER + "' and att2.attribute_owner_id  != ? " +
                " and att2.attribute_name not in (select att3.attribute_name from ATTRIBUTE att3" +
                " where att3.attribute_owner = '" + OwnerType.USER + "' and att3.attribute_Owner_id  = ? )";

        ResultSet rs = db.runSelect(sql, new Object[]{user.getId(), user.getId(), user.getId()});
        while (rs.next()) {
            user.setAttribute(rs.getString("attribute_name"), rs.getString("attribute_value"));
        }
    }

    protected void loadAttributesForGroup(Workgroup group) throws SQLException, CachingException {
        // load attributes for workgroup
        String sql = "select DISTINCT att1.attribute_name, att1.attribute_value from ATTRIBUTE  att1  " +
            " where att1.attribute_owner = '" + OwnerType.USER_GROUP + "' and att1.attribute_owner_id  = ?" +
            " UNION  " +
            " select DISTINCT att2.attribute_name, '' from ATTRIBUTE  att2 " +
            " where att2.attribute_owner = '" + OwnerType.USER_GROUP + "' and att2.attribute_owner_id  != ?" +
            " and att2.attribute_name not in (select att3.attribute_name from ATTRIBUTE att3" +
            " where att3.attribute_owner = '" + OwnerType.USER_GROUP + "' and att3.attribute_Owner_id  = ? )";

        ResultSet rs = db.runSelect(sql, new Object[]{group.getId(), group.getId(), group.getId()});
        while (rs.next())
            group.setAttribute(rs.getString("attribute_name"), rs.getString("attribute_value"));
    }

    public List<Workgroup> getAllGroups(boolean includeDeleted) throws DataAccessException {
        try {
            List<Workgroup> groups = new ArrayList<Workgroup>();
            db.openConnection();
            String sql = "select USER_GROUP_ID, GROUP_NAME, COMMENTS, PARENT_GROUP_ID, END_DATE from USER_GROUP";
            if (!includeDeleted) sql = sql + " where END_DATE is null";
            sql += " order by GROUP_NAME";
            ResultSet rs = db.runSelect(sql);
            Map<Long,String> nameMap = new HashMap<Long,String>();
            while (rs.next()) {
                Long groupId = rs.getLong(1);
                String groupName = rs.getString(2);
                String comments = rs.getString(3);
                Workgroup group = new Workgroup(groupId, groupName, comments);
                long pid = rs.getLong(4);
                if (pid>0L) group.setParentGroup(Long.toString(pid));
                group.setEndDate(rs.getString(5));
                nameMap.put(groupId, groupName);
                groups.add(group);
            }
            for (Workgroup group : groups) {
                loadAttributesForGroup(group);
                if (group.getParentGroup()!=null) {
                    Long pid = new Long(group.getParentGroup());
                    group.setParentGroup(nameMap.get(pid));
                }
            }
            return groups;
        } catch(Exception ex){
            throw new DataAccessException(-1, "Failed to get user group", ex);
        } finally {
            db.closeConnection();
        }
    }

    public void auditLogUserAction(UserAction userAction)
    throws DataAccessException {
        try {
            db.openConnection();
            Long id = db.isMySQL()?null:this.getNextId("EVENT_LOG_ID_SEQ");
            String query = "insert into EVENT_LOG " +
                "(EVENT_LOG_ID, EVENT_NAME, EVENT_CATEGORY, EVENT_SUB_CATEGORY, " +
                "EVENT_SOURCE, EVENT_LOG_OWNER, EVENT_LOG_OWNER_ID, CREATE_USR, CREATE_DT, COMMENTS, STATUS_CD) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, " + nowPrecision() + ", ?, '1')";
            Object[] args = new Object[9];
            args[0] = id;
            args[1] = userAction.getAction().toString();
            args[2] = EventLog.CATEGORY_AUDIT;
            args[3] = "User Action";
            args[4] = userAction.getSource();
            args[5] = userAction.getEntity().toString();
            args[6] = userAction.getEntityId();
            args[7] = userAction.getUser();
            args[8] = userAction.getDescription();
            db.runUpdate(query, args);
            db.commit();
        }
        catch (SQLException ex) {
            throw new DataAccessException(-1, "failed to insert audit log", ex);
        }
        finally {
            db.closeConnection();
        }
    }

}
