/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.version4;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.UserDataAccess;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;

public class UserDataAccessV4 extends CommonDataAccess implements UserDataAccess {

	protected String USER_SELECT_FIELDS;

    public UserDataAccessV4(DatabaseAccess db, int databaseVersion, int supportedVersion) {
        super(db, databaseVersion, supportedVersion);
        if (databaseVersion>=DataAccess.schemaVersion52)
        	USER_SELECT_FIELDS = "u.USER_INFO_ID, u.CUID, u.NAME, u.END_DATE, u.COMMENTS";
        else USER_SELECT_FIELDS = "u.USER_INFO_ID, u.CUID, u.COMMENTS";
    }

    /**
     * This code is cloned from getUser(String) in UserDAO
     */
    public UserVO getUser(String userName) throws DataAccessException {
        try {
            db.openConnection();
            String sql = "select " + USER_SELECT_FIELDS + " from USER_INFO u where lower(u.CUID)=?";
            if (this.getDatabaseVersion()>=DataAccess.schemaVersion52)
            	sql += " and END_DATE is null";
             ResultSet rs = db.runSelect(sql, userName.toLowerCase());
             if (rs.next()) {
                 UserVO user = createUserInfoFromResultSet(rs);
                 loadGroupsRolesForUser(user);
                 loadAttributesForUser(user);
                 return user;
             } else return null;
        } catch(Exception ex){
            throw new DataAccessException(-1, "Failed to get user: " + userName, ex);
        } finally {
            db.closeConnection();
        }
    }

    protected UserVO createUserInfoFromResultSet(ResultSet rs) throws SQLException {
		UserVO user = new UserVO();
		user.setId(rs.getLong(1));
		user.setCuid(rs.getString(2));
		String name = rs.getString(3);
    	if (getDatabaseVersion()>=DataAccess.schemaVersion52) {
    		if (name==null) name = rs.getString(5);
    		// Set Cuid as name to handle migrated users from MDW4 to 5
    		// and comment is missing in user_info table
    		if (StringHelper.isEmpty(name)) name = rs.getString(2);
    		user.setEndDate(rs.getString(4));
    	}
		user.setName(name);
		user.parseName();
		return user;
    }

    protected void loadGroupsRolesForUser(UserVO user) throws SQLException, CachingException {
		// load groups
		String sql = "select g.USER_GROUP_ID, g.GROUP_NAME, g.COMMENTS, ug.COMMENTS " +
			"from USER_GROUP_MAPPING ug, USER_GROUP g " +
			"where ug.USER_GROUP_ID = g.USER_GROUP_ID and ug.USER_INFO_ID = ? ";
        if (getSupportedVersion() < DataAccess.schemaVersion51)
          sql += " and ug.MAPP_END_DATE is null ";
        sql += "order by lower(g.GROUP_NAME)";

		ResultSet rs = db.runSelect(sql, user.getId());
		ArrayList<UserGroupVO> groups = new ArrayList<UserGroupVO>();
		Map<String,Boolean> rolesConverted = new HashMap<String,Boolean>();
		while (rs.next()) {
			Long groupId = rs.getLong(1);
			String groupName = rs.getString(2);
			String comment = rs.getString(3);
			String converted = rs.getString(4);
			UserGroupVO group = new UserGroupVO(groupId, groupName, comment);
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
			for (UserGroupVO group : groups) {
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
			"		  and r.USER_ROLE_ID = ur.USER_ROLE_ID)" +
	        "       or (ur.USER_ROLE_MAPPING_OWNER = '" + OwnerType.USER_GROUP + "'" +
	        "         and ur.USER_ROLE_MAPPING_OWNER_ID in " +
	        "           (select ug.USER_GROUP_ID from USER_GROUP_MAPPING ug" +
	        "            where ug.USER_INFO_ID = u.USER_INFO_ID" +
	        "              and r.USER_ROLE_ID = ur.USER_ROLE_ID))) " +
	        "order by r.USER_ROLE_NAME";
		rs = db.runSelect(sql, user.getCuid());
		List<String> sharedRoles = new ArrayList<String>();
		while (rs.next()) {
			String role = rs.getString(1);
			sharedRoles.add(UserRoleVO.toNewName(role));
		}
		UserGroupVO sharedGroup = new UserGroupVO(UserGroupVO.COMMON_GROUP_ID, UserGroupVO.COMMON_GROUP, null);
		sharedGroup.setRoles(sharedRoles);
		groups.add(sharedGroup);
		// set groups to user
		Collections.sort(groups);
		user.setGroups(groups);
	}

    protected void loadAttributesForUser(UserVO user) throws SQLException, CachingException {
        // load groups
        String sql = "select attribute_name, attribute_value from attribute " +
            "where attribute_owner = 'USER' " +
            "and attribute_owner_id = ? ";
        ResultSet rs = db.runSelect(sql, user.getId());
        while (rs.next())
            user.setAttribute(rs.getString("attribute_name"), rs.getString("attribute_value"));
    }

    public List<UserGroupVO> getAllGroups(boolean includeDeleted) throws DataAccessException {
		try {
			List<UserGroupVO> groups = new ArrayList<UserGroupVO>();
			db.openConnection();
	    	if (getDatabaseVersion()>=DataAccess.schemaVersion52) {
				String sql = "select USER_GROUP_ID, GROUP_NAME, COMMENTS, PARENT_GROUP_ID, END_DATE from USER_GROUP";
				if (!includeDeleted) sql = sql + " where END_DATE is null";
				sql += " order by GROUP_NAME";
				ResultSet rs = db.runSelect(sql, null);
				Map<Long,String> nameMap = new HashMap<Long,String>();
				while (rs.next()) {
					Long groupId = rs.getLong(1);
					String groupName = rs.getString(2);
					String comments = rs.getString(3);
					UserGroupVO group = new UserGroupVO(groupId, groupName, comments);
					long pid = rs.getLong(4);
					if (pid>0L) group.setParentGroup(Long.toString(pid));
					group.setEndDate(rs.getString(5));
					nameMap.put(groupId, groupName);
					groups.add(group);
				}
				for (UserGroupVO group : groups) {
					if (group.getParentGroup()!=null) {
						Long pid = new Long(group.getParentGroup());
						group.setParentGroup(nameMap.get(pid));
					}
				}
	    	} else {
	    		String sql = "select USER_GROUP_ID, GROUP_NAME, COMMENTS " +
            		" from USER_GROUP order by GROUP_NAME";
	    		ResultSet rs = db.runSelect(sql, null);
	    		while (rs.next()) {
	    			Long groupId = rs.getLong(1);
	    			String groupName = rs.getString(2);
	    			String comments = rs.getString(3);
	    			UserGroupVO group = new UserGroupVO(groupId, groupName, comments);
	    			groups.add(group);
	    		}
	    	}
	    	return groups;
		} catch(Exception ex){
			throw new DataAccessException(-1, "Failed to get user group", ex);
		} finally {
			db.closeConnection();
		}
	}

    public List<String> getRoleNames() throws DataAccessException {
        try {
            db.openConnection();
            List<String> names = new ArrayList<String>();
            String query = "select USER_ROLE_NAME from USER_ROLE";
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
                names.add(rs.getString(1));
            }
            return names;
        } catch (SQLException e) {
            throw new DataAccessException(0,"failed to load role names", e);
        } finally {
            db.closeConnection();
        }
    }

    public void auditLogUserAction(UserActionVO userAction)
    throws DataAccessException {
        try {
            db.openConnection();
            Long id = db.isMySQL()?null:this.getNextId("EVENT_LOG_ID_SEQ");
            String query = "insert into EVENT_LOG " +
                "(EVENT_LOG_ID, EVENT_NAME, EVENT_CATEGORY, EVENT_SUB_CATEGORY, " +
                "EVENT_SOURCE, EVENT_LOG_OWNER, EVENT_LOG_OWNER_ID, CREATE_USR, CREATE_DT, COMMENTS, STATUS_CD) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, " + now() + ", ?, '1')";
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

    // no longer used since MDW 5.2
    public Set<String> getPrivilegesForUser(String userName)
        throws DataAccessException {
        try{
            db.openConnection();
            Set<String> roles = new HashSet<String>();
            String sql = "select ur.USER_ROLE_NAME from USER_INFO ui, USER_ROLE ur, USER_ROLE_MAPPING urm " +
                " where lower(ui.CUID) = ? and ui.USER_INFO_ID = urm.USER_ROLE_MAPPING_OWNER_ID " +
                " and urm.USER_ROLE_MAPPING_OWNER = 'USER' " +
                " and urm.USER_ROLE_ID = ur.USER_ROLE_ID";
            ResultSet rs = db.runSelect(sql, userName.toLowerCase());
            while (rs.next()) {
                roles.add(rs.getString(1));
            }
            sql = "select ug.GROUP_NAME from USER_INFO ui, USER_GROUP ug, USER_GROUP_MAPPING ugm " +
                " where lower(ui.CUID) = ? and ui.USER_INFO_ID = ugm.USER_INFO_ID and " +
                " ugm.USER_GROUP_ID = ug.USER_GROUP_ID";
            rs = db.runSelect(sql, userName.toLowerCase());
            while (rs.next()) {
                roles.add(rs.getString(1));
            }
            return roles;
        }catch(SQLException ex){
            throw new DataAccessException(-1, ex.getMessage(), ex);
        } finally {
            db.closeConnection();
        }
    }


}
