/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.version4.UserDataAccessV4;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;

public class UserDAO extends UserDataAccessV4 {

    public UserDAO(DatabaseAccess db) {
        super(db, DataAccess.currentSchemaVersion, DataAccess.supportedSchemaVersion);
    }

    public int countUsers(String whereCondition)
    throws DataAccessException {
    	 try {
             db.openConnection();
             return super.countRows("USER_INFO", "USER_INFO_ID", whereCondition);
         } catch (Exception e) {
             throw new DataAccessException(0, "failed to count users", e);
         } finally {
             db.closeConnection();
         }
    }

	public List<UserVO> queryUsers(String whereCondition, boolean withGroups, int startIndex, int endIndex, String sortOn)
			throws DataAccessException {
		 try{
			 db.openConnection();
			 List<UserVO> users = new ArrayList<UserVO>();
			 if (startIndex>=0) {
				 if (sortOn==null) sortOn = "CUID";
				 String[] fields = {"USER_INFO_ID","CUID","NAME","END_DATE","COMMENTS"};
				 List<String[]> result = super.queryRows("USER_INFO", fields, whereCondition, sortOn, startIndex, endIndex);
				 for (String[] one: result) {
					 String name = one[2]!=null?one[2]:one[4];
					 UserVO user = new UserVO();
					 user.setId(new Long(one[0]));
					 user.setCuid(one[1]);
					 user.setName(name);
					 user.setEndDate(one[3]);
			         users.add(user);
				 }
			 } else {
				 String sql = "select " + USER_SELECT_FIELDS + " from USER_INFO u";
				 if (whereCondition!=null) sql = sql + " where " + whereCondition;
				 sql += sortOn == null ? " order by CUID" : (" order by " + sortOn);
				 ResultSet rs = db.runSelect(sql, null);
				 while (rs.next()) {
					 users.add(createUserInfoFromResultSet(rs));
				 }
			 }
			 if (withGroups) {
				 for (UserVO user : users) {
					 loadGroupsRolesForUser(user);
				 }
			 }
			 return users;
		 }catch(SQLException ex){
			 throw new DataAccessException(-1, "Failed to load users", ex);
		 } catch (CachingException e) {
			 throw new DataAccessException(-1, "Failed to load site admin group", e);
		} finally {
			 db.closeConnection();
		 }
	}

    protected Long getNextId(String sequenceName) throws SQLException {
        String query = "select " + sequenceName + ".NEXTVAL from dual";
        ResultSet rs = db.runSelect(query, null);
        rs.next();
        return new Long(rs.getString(1));
    }

	public Long saveUser(UserVO user)
		throws DataAccessException {
		try {
			db.openConnection();
			Long id = user.getId();
			if (id==null || id.longValue()<=0L) {
				id = db.isMySQL()?null:getNextId("MDW_COMMON_ID_SEQ");
				String query = "insert into USER_INFO" +
							" (USER_INFO_ID, CUID, CREATE_DT, CREATE_USR, NAME)" +
							" values (?, ?, " + now() + ", ?, ?)";
				Object[] args = new Object[4];
				args[0] = id;
				args[1] = user.getCuid();
				args[2] = "MDW Engine";
				args[3] = user.getName();
				if (db.isMySQL()) id = db.runInsertReturnId(query, args);
				else db.runUpdate(query, args);
			} else {
				String query = "update USER_INFO set CUID=?, NAME=? where USER_INFO_ID=?";
				Object[] args = new Object[3];
				args[0] = user.getCuid();
				args[1] = user.getName();
				args[2] = id;
				db.runUpdate(query, args);
			}
			db.commit();
			return id;
		} catch(Exception ex){
			db.rollback();
			throw new DataAccessException(-1, "Failed to save user", ex);
		} finally {
			db.closeConnection();
		}
	}

	public UserVO getUser(Long userId) throws DataAccessException {
		try {
			db.openConnection();
			String sql = "select " + USER_SELECT_FIELDS + " from USER_INFO u where u.USER_INFO_ID=?";
			ResultSet rs = db.runSelect(sql, userId);
			if (rs.next()) {
				UserVO user = createUserInfoFromResultSet(rs);
				loadGroupsRolesForUser(user);
				return user;
			} else return null;
		} catch(Exception ex){
			throw new DataAccessException(-1, "Failed to get user", ex);
		} finally {
			db.closeConnection();
		}
	}

	private void loadUsersRolesForGroup(String groupName, List<UserVO> users) throws SQLException {
		if (groupName.equals(UserGroupVO.COMMON_GROUP)) {
			// load global roles for the common group
			// we translate the old names to new ones
			String sql = "select u.CUID, r.USER_ROLE_NAME " +
				"from USER_INFO u, USER_ROLE r, USER_GROUP_MAPPING ugm, USER_GROUP g, USER_ROLE_MAPPING urm " +
				"where g.GROUP_NAME=? " +
				"and ugm.USER_GROUP_ID=g.USER_GROUP_ID " +
				"and ugm.USER_INFO_ID=u.USER_INFO_ID " +
				"and ugm.COMMENTS is null " +
				"and urm.USER_ROLE_MAPPING_OWNER='" + OwnerType.USER + "' " +
				"and urm.USER_ROLE_MAPPING_OWNER_ID=u.USER_INFO_ID " +
				"and urm.USER_ROLE_ID=r.USER_ROLE_ID";
			ResultSet rs = db.runSelect(sql, groupName);
			while (rs.next()) {
				String cuid = rs.getString(1);
				String role = rs.getString(2);
				for (UserVO user : users) {
					if (cuid.equals(user.getCuid())) {
						user.addRoleForGroup(groupName, UserRoleVO.toNewName(role));
						break;
					}
				}
			}
		} else {
			// load roles for the users in the group
			String sql = "select u.CUID, r.USER_ROLE_NAME " +
				"from USER_INFO u, USER_GROUP g, USER_GROUP_MAPPING ugm, USER_ROLE r, USER_ROLE_MAPPING ugrm " +
				"where g.GROUP_NAME = ?" +
				"    and ugm.USER_GROUP_ID = g.USER_GROUP_ID" +
				"    and ugm.USER_INFO_ID = u.USER_INFO_ID" +
				"    and ugrm.USER_ROLE_MAPPING_OWNER='" + OwnerType.USER_GROUP_MAP + "'" +
				"    and ugrm.USER_ROLE_MAPPING_OWNER_ID = ugm.USER_GROUP_MAPPING_ID" +
				"    and ugrm.USER_ROLE_ID = r.USER_ROLE_ID";
			ResultSet rs = db.runSelect(sql, groupName);
			while (rs.next()) {
				String cuid = rs.getString(1);
				String role = rs.getString(2);
				for (UserVO user : users) {
					if (cuid.equals(user.getCuid())) {
						user.addRoleForGroup(groupName, role);
						break;
					}
				}
			}
		}
	}

	public List<UserVO> getUsersForGroup(String groupName, boolean loadRoles) throws DataAccessException {
		 try{
			 db.openConnection();

			 List<UserVO> users = new ArrayList<UserVO>();
			 String sql = "select " + USER_SELECT_FIELDS +
	            	" from USER_INFO u, USER_GROUP_MAPPING ugm, USER_GROUP ug " +
	            	"where u.END_DATE is null " +
	            	"   and u.USER_INFO_ID = ugm.USER_INFO_ID" +
	            	"   and ugm.USER_GROUP_ID = ug.USER_GROUP_ID" +
	            	"   and ug.GROUP_NAME = ? " +
	            	"order by u.CUID";
			 ResultSet rs = db.runSelect(sql, groupName);
			 while (rs.next()) {
	             users.add(createUserInfoFromResultSet(rs));
			 }
			 if (loadRoles) this.loadUsersRolesForGroup(groupName, users);
			 return users;
		 }catch(SQLException ex){
			 throw new DataAccessException(-1, "Failed to load users", ex);
		 } finally {
			 db.closeConnection();
		 }
	}

	public UserGroupVO getGroup(String groupName) throws DataAccessException {
		try {
			db.openConnection();
			String sql = "select USER_GROUP_ID, COMMENTS, PARENT_GROUP_ID, END_DATE " +
	            	" from USER_GROUP where GROUP_NAME=?";
			 ResultSet rs = db.runSelect(sql, groupName);
			 if (rs.next()) {
				 Long id = rs.getLong(1);
				 String comments = rs.getString(2);
				 UserGroupVO group = new UserGroupVO(id, groupName, comments);
				 long pid = rs.getLong(3);
                 group.setEndDate(rs.getString(4));
				 if (pid>0L) {
					 rs = db.runSelect("select GROUP_NAME from USER_GROUP where USER_GROUP_ID=?", pid);
					 if (rs.next()) group.setParentGroup(rs.getString(1));
				 }
				 return group;
			 } else return null;
		} catch(Exception ex){
			throw new DataAccessException(-1, "Failed to get user group", ex);
		} finally {
			db.closeConnection();
		}
	}

	public UserGroupVO getGroup(Long groupId) throws DataAccessException {
		try {
			db.openConnection();
			String sql = "select GROUP_NAME, COMMENTS, PARENT_GROUP_ID, END_DATE " +
	            	" from USER_GROUP where USER_GROUP_ID=?";
			 ResultSet rs = db.runSelect(sql, groupId);
			 if (rs.next()) {
				 String groupName = rs.getString(1);
				 String comments = rs.getString(2);
				 UserGroupVO group = new UserGroupVO(groupId, groupName, comments);
				 long pid = rs.getLong(3);
				 if (pid>0L) {
					 rs = db.runSelect(sql, pid);
					 if (rs.next()) group.setParentGroup(rs.getString(1));
				 }
				 group.setEndDate(rs.getString(4));
				 return group;
			 } else return null;
		} catch(Exception ex){
			throw new DataAccessException(-1, "Failed to get user group", ex);
		} finally {
			db.closeConnection();
		}
	}

	public List<UserRoleVO> getAllRoles() throws DataAccessException {
		try {
			db.openConnection();
			List<UserRoleVO> roles = new ArrayList<UserRoleVO>();
			String sql = "select USER_ROLE_ID, USER_ROLE_NAME, COMMENTS from USER_ROLE order by USER_ROLE_NAME";
			 ResultSet rs = db.runSelect(sql, null);
			 while (rs.next()) {
				 UserRoleVO role = new UserRoleVO();
				 role.setId(rs.getLong(1));
				 role.setName(rs.getString(2));
				 role.setDescription(rs.getString(3));
				 roles.add(role);
			 }
			 return roles;
		} catch(Exception ex){
			throw new DataAccessException(-1, "Failed to get all user roles", ex);
		} finally {
			db.closeConnection();
		}
	}

	public UserRoleVO getRole(String roleName) throws DataAccessException {
		try {
			db.openConnection();
			String sql = "select USER_ROLE_ID, COMMENTS " +
	            	" from USER_ROLE where USER_ROLE_NAME=?";
			 ResultSet rs = db.runSelect(sql, roleName);
			 if (rs.next()) {
				 UserRoleVO role = new UserRoleVO();
				 role.setId(rs.getLong(1));
				 role.setName(roleName);
				 role.setDescription(rs.getString(2));
				 return role;
			 } else return null;
		} catch(Exception ex){
			throw new DataAccessException(-1, "Failed to get user role: " + roleName, ex);
		} finally {
			db.closeConnection();
		}
	}

	public UserRoleVO getRole(Long roleId) throws DataAccessException {
		try {
			db.openConnection();
			String sql = "select USER_ROLE_NAME, COMMENTS " +
	            	" from USER_ROLE where USER_ROLE_ID=?";
			 ResultSet rs = db.runSelect(sql, roleId);
			 if (rs.next()) {
				 UserRoleVO role = new UserRoleVO();
				 role.setId(roleId);
				 role.setName(rs.getString(1));
				 role.setDescription(rs.getString(2));
				 return role;
			 } else return null;
		} catch(Exception ex){
			throw new DataAccessException(-1, "Failed to get user role", ex);
		} finally {
			db.closeConnection();
		}
	}

	public List<String> getRolesForGroup(Long groupId) throws DataAccessException {
		try {
			List<String> roles = new ArrayList<String>();
			db.openConnection();
			String sql = "select ur.USER_ROLE_ID, ur.USER_ROLE_NAME, ur.COMMENTS " +
				"from USER_GROUP ug, USER_ROLE ur, USER_ROLE_MAPPING urm " +
				"where ug.USER_GROUP_ID = ? " +
				"   and urm.USER_ROLE_MAPPING_OWNER = 'USER_GROUP'" +
				"   and urm.USER_ROLE_MAPPING_OWNER_ID = ug.USER_GROUP_ID" +
				"   and urm.USER_ROLE_ID = ur.USER_ROLE_ID ";
			ResultSet rs = db.runSelect(sql, groupId);
			while (rs.next()) {
				roles.add(rs.getString(2));
			}
			return roles;
		} catch(Exception ex){
			throw new DataAccessException(-1, "Failed to get user role", ex);
		} finally {
			db.closeConnection();
		}
	}

	public List<UserRoleVO> getRolesForAction(Long taskActionId) throws DataAccessException {
		try {
			List<UserRoleVO> roles = new ArrayList<UserRoleVO>();
			db.openConnection();
			String sql = "select ur.USER_ROLE_ID, ur.USER_ROLE_NAME, ur.COMMENTS " +
				"from USER_ROLE ur, TASK_ACTN_USR_ROLE_MAPP taurm " +
				"where taurm.TASK_ACTION_ID = ?" +
				"   and ur.USER_ROLE_ID = taurm.USER_ROLE_ID " +
				"order by ur.USER_ROLE_NAME";
			ResultSet rs = db.runSelect(sql, taskActionId);
			while (rs.next()) {
				UserRoleVO role = new UserRoleVO();
				role.setId(rs.getLong(1));
				role.setName(rs.getString(2));
				role.setDescription(rs.getString(3));
				roles.add(role);
			}
			return roles;
		} catch(Exception ex){
			throw new DataAccessException(-1, "Failed to get roles for task action", ex);
		} finally {
			db.closeConnection();
		}
	}

	public List<UserVO> getUsersForRole(String roleName) throws DataAccessException {
		 try{
			 db.openConnection();

			 List<UserVO> users = new ArrayList<UserVO>();
			 String sql = "select " + USER_SELECT_FIELDS +
	            	" from USER_INFO u, USER_ROLE_MAPPING urm, USER_ROLE ur " +
	            	" where u.END_DATE is null and "
	            	+ " u.USER_INFO_ID = urm.USER_ROLE_MAPPING_OWNER_ID" +
	            	"   and urm.USER_ROLE_MAPPING_OWNER='USER'" +
	            	"   and urm.USER_ROLE_ID = ur.USER_ROLE_ID" +
	            	"   and ur.USER_ROLE_NAME = ? " +
	            	"order by u.CUID";
			 ResultSet rs = db.runSelect(sql, roleName);
			 while (rs.next()) {
	             users.add(createUserInfoFromResultSet(rs));
			 }
			 return users;
		 }catch(SQLException ex){
			 throw new DataAccessException(-1, "Failed to load users for role", ex);
		 } finally {
			 db.closeConnection();
		 }
	}

	public Long saveGroup(UserGroupVO group)
		throws DataAccessException {
		try {
			db.openConnection();
			Long id = group.getId();
			Long parentId;
			if (group.getParentGroup()!=null) {
				ResultSet rs = db.runSelect("select USER_GROUP_ID from USER_GROUP where GROUP_NAME=?",
						group.getParentGroup());
				if (rs.next()) parentId = rs.getLong(1);
				else parentId = null;
			} else parentId = null;
			if (id==null || id.longValue()<=0L) {
				id = db.isMySQL()?null:getNextId("MDW_COMMON_ID_SEQ");
				String query = "insert into USER_GROUP" +
							" (USER_GROUP_ID, GROUP_NAME, CREATE_DT, CREATE_USR, COMMENTS, PARENT_GROUP_ID)" +
							" values (?, ?, "+now()+", ?, ?, ?)";
				Object[] args = new Object[5];
				args[0] = id;
				args[1] = group.getName();
				args[2] = "MDW Engine";
				args[3] = group.getDescription();
				args[4] = parentId;
				if (db.isMySQL()) id = db.runInsertReturnId(query, args);
				else db.runUpdate(query, args);
			} else {
				String query = "update USER_GROUP set GROUP_NAME=?, COMMENTS=?, PARENT_GROUP_ID=? where USER_GROUP_ID=?";
				Object[] args = new Object[4];
				args[0] = group.getName();
				args[1] = group.getDescription();
				args[2] = parentId;
				args[3] = id;
				db.runUpdate(query, args);
			}
			db.commit();
			return id;
		} catch(Exception ex){
			db.rollback();
			throw new DataAccessException(-1, "Failed to save group", ex);
		} finally {
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
			// delete user preferences
			query = "delete from ATTRIBUTE where ATTRIBUTE_OWNER='USER' and ATTRIBUTE_OWNER_ID=?";
			db.runUpdate(query, userId);
			// end-date user itself
			query = "update USER_INFO set END_DATE="+now()+" where USER_INFO_ID=?";
			db.runUpdate(query, userId);
			db.commit();
		} catch(Exception ex){
			db.rollback();
			throw new DataAccessException(-1, "Failed to delete user", ex);
		} finally {
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
			// end-date the group itself
			query = "update USER_GROUP set END_DATE="+now()+" where USER_GROUP_ID=?";
			db.runUpdate(query, groupId);
			db.commit();
		} catch(Exception ex){
			db.rollback();
			throw new DataAccessException(-1, "Failed to delete group", ex);
		} finally {
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
		} catch(Exception ex){
			db.rollback();
			throw new DataAccessException(-1, "Failed to delete role", ex);
		} finally {
			db.closeConnection();
		}
	}

	public Long saveRole(UserRoleVO role)
		throws DataAccessException {
		try {
			db.openConnection();
			Long id = role.getId();
			if (id==null || id.longValue()<=0L) {
				id = db.isMySQL()?null:getNextId("MDW_COMMON_ID_SEQ");
				String query = "insert into USER_ROLE" +
							" (USER_ROLE_ID, USER_ROLE_NAME, CREATE_DT, CREATE_USR, COMMENTS)" +
							" values (?, ?, "+now()+", ?, ?)";
				Object[] args = new Object[4];
				args[0] = id;
				args[1] = role.getName();
				args[2] = "MDW Engine";
				args[3] = role.getDescription();
				if (db.isMySQL()) id = db.runInsertReturnId(query, args);
				else db.runUpdate(query, args);
			} else {
				String query = "update USER_ROLE set USER_ROLE_NAME=?, COMMENTS=? where USER_ROLE_ID=?";
				Object[] args = new Object[3];
				args[0] = role.getName();
				args[1] = role.getDescription();
				args[2] = id;
				db.runUpdate(query, args);
			}
			db.commit();
			return id;
		} catch(Exception ex){
			db.rollback();
			throw new DataAccessException(-1, "Failed to save role", ex);
		} finally {
			db.closeConnection();
		}
	}

	private void updateMembersByName(Long id, String[] members,
			String selectQuery, String deleteQuery,
			String findQuery, String insertQuery, String errmsg)
		throws DataAccessException {
		try {
			db.openConnection();
			ResultSet rs = db.runSelect(selectQuery, id);
			List<String> existing = new ArrayList<String>();
			HashMap<String,Long> existingIds = new HashMap<String,Long>();
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
					} else {
						throw new Exception("Cannot find " + m);
					}
				}
			}
			db.commit();
		} catch(Exception ex){
			db.rollback();
			throw new DataAccessException(-1, errmsg, ex);
		} finally {
			db.closeConnection();
		}
	}

	public void updateRolesForUser(Long userId, Long groupId, String[] roles)
		throws DataAccessException {
		if (groupId.equals(UserGroupVO.COMMON_GROUP_ID)) {
			String selectQuery = "select ur.USER_ROLE_ID, ur.USER_ROLE_NAME " +
				"from USER_INFO u, USER_ROLE ur, USER_ROLE_MAPPING urm " +
				"where u.USER_INFO_ID = ? " +
				"   and urm.USER_ROLE_MAPPING_OWNER = 'USER'" +
				"   and urm.USER_ROLE_MAPPING_OWNER_ID = u.USER_INFO_ID" +
				"   and urm.USER_ROLE_ID = ur.USER_ROLE_ID";
			String deleteQuery = "delete from USER_ROLE_MAPPING where USER_ROLE_MAPPING_OWNER='USER'" +
				" and USER_ROLE_MAPPING_OWNER_ID=? and USER_ROLE_ID=?";
			String findQuery = "select USER_ROLE_ID from USER_ROLE where USER_ROLE_NAME=?";
			String insertQuery = "insert into USER_ROLE_MAPPING" +
				" (USER_ROLE_MAPPING_ID, USER_ROLE_MAPPING_OWNER, USER_ROLE_MAPPING_OWNER_ID," +
				"  CREATE_DT,CREATE_USR,USER_ROLE_ID) values (" +
				(db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL") +
				",'USER',?,"+now()+",'MDW',?)";
			String errmsg = "Failed to update roles for user";
			updateMembersByName(userId, roles, selectQuery, deleteQuery,
					findQuery, insertQuery, errmsg);
		} else {
			Long ugmId;
			try {
				db.openConnection();
				String sql = "select USER_GROUP_MAPPING_ID " +
					"from USER_GROUP_MAPPING where USER_INFO_ID = ? and USER_GROUP_ID=?";
				Object[] args = new Object[2];
				args[0] = userId;
				args[1] = groupId;
				ResultSet rs = db.runSelect(sql, args);
				if (rs.next()) {
					ugmId = rs.getLong(1);
					sql = "update USER_GROUP_MAPPING set COMMENTS='Converted' where USER_GROUP_MAPPING_ID=?";
					db.runUpdate(sql, ugmId);
				} else throw new Exception("User-group mapping does not exist");
			} catch(Exception ex){
				throw new DataAccessException(-1, "Failed to find user-group mapping", ex);
			} finally {
				db.closeConnection();
			}
			String selectQuery = "select r.USER_ROLE_ID, r.USER_ROLE_NAME " +
				"from USER_ROLE r, USER_GROUP_MAPPING ugm, USER_ROLE_MAPPING urm " +
				"where ugm.USER_GROUP_MAPPING_ID = ? " +
				"   and urm.USER_ROLE_MAPPING_OWNER = '" + OwnerType.USER_GROUP_MAP + "'" +
				"   and urm.USER_ROLE_MAPPING_OWNER_ID = ugm.USER_GROUP_MAPPING_ID" +
				"   and urm.USER_ROLE_ID = r.USER_ROLE_ID";
			String deleteQuery = "delete from USER_ROLE_MAPPING where" +
				" USER_ROLE_MAPPING_OWNER='" + OwnerType.USER_GROUP_MAP + "'" +
				" and USER_ROLE_MAPPING_OWNER_ID=? and USER_ROLE_ID=?";
			String findQuery = "select USER_ROLE_ID from USER_ROLE where USER_ROLE_NAME=?";
			String insertQuery = "insert into USER_ROLE_MAPPING" +
				" (USER_ROLE_MAPPING_ID, USER_ROLE_MAPPING_OWNER, USER_ROLE_MAPPING_OWNER_ID," +
				"  CREATE_DT,CREATE_USR,USER_ROLE_ID) values (" +
				(db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL") +
				",'"+OwnerType.USER_GROUP_MAP + "',?,"+now()+",'MDW',?)";
			String errmsg = "Failed to update roles for user";
			updateMembersByName(ugmId, roles, selectQuery, deleteQuery,
					findQuery, insertQuery, errmsg);
		}
	}

	@Deprecated
	public void updateRolesForGroup(Long groupId, String[] roles)
		throws DataAccessException {
		String selectQuery = "select ur.USER_ROLE_ID, ur.USER_ROLE_NAME " +
			"from USER_GROUP ug, USER_ROLE ur, USER_ROLE_MAPPING urm " +
			"where ug.USER_GROUP_ID = ? " +
			"   and urm.USER_ROLE_MAPPING_OWNER = 'USER_GROUP'" +
			"   and urm.USER_ROLE_MAPPING_OWNER_ID = ug.USER_GROUP_ID" +
			"   and urm.USER_ROLE_ID = ur.USER_ROLE_ID";
		String deleteQuery = "delete from USER_ROLE_MAPPING where USER_ROLE_MAPPING_OWNER='USER_GROUP'" +
			" and USER_ROLE_MAPPING_OWNER_ID=? and USER_ROLE_ID=?";
		String findQuery = "select USER_ROLE_ID from USER_ROLE where USER_ROLE_NAME=?";
		String insertQuery = "insert into USER_ROLE_MAPPING" +
			" (USER_ROLE_MAPPING_ID, USER_ROLE_MAPPING_OWNER, USER_ROLE_MAPPING_OWNER_ID," +
			"  CREATE_DT,CREATE_USR,USER_ROLE_ID) values (" +
			(db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL") +
			",'USER_GROUP',?,"+now()+",'MDW',?)";
		String errmsg = "Failed to update roles for group";
		updateMembersByName(groupId, roles, selectQuery, deleteQuery,
			findQuery, insertQuery, errmsg);
	}

	public void updateGroupsForUser(Long userId, String[] groups)
		throws DataAccessException {
		String selectQuery = "select ug.USER_GROUP_ID, ug.GROUP_NAME " +
			"from USER_INFO u, USER_GROUP ug, USER_GROUP_MAPPING ugm " +
			"where u.USER_INFO_ID = ? " +
			"   and ugm.USER_INFO_ID = u.USER_INFO_ID" +
			"   and ugm.USER_GROUP_ID = ug.USER_GROUP_ID";
		String deleteQuery = "delete from USER_GROUP_MAPPING where USER_INFO_ID=? and USER_GROUP_ID=?";
		String findQuery = "select USER_GROUP_ID from USER_GROUP where GROUP_NAME=?";
		String insertQuery = "insert into USER_GROUP_MAPPING" +
			" (USER_GROUP_MAPPING_ID, USER_INFO_ID," +
			"  CREATE_DT,CREATE_USR,USER_GROUP_ID,COMMENTS) values (" +
			(db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL") +
			",?," + now() + ",'MDW',?,'Converted')";
		String errmsg = "Failed to update groups for user";
		updateMembersByName(userId, groups, selectQuery, deleteQuery,
			findQuery, insertQuery, errmsg);
	}

    public void addUserToGroup(String cuid, String group) throws DataAccessException {
        String query = "insert into USER_GROUP_MAPPING" +
            " (USER_GROUP_MAPPING_ID, USER_INFO_ID," +
            "  CREATE_USR, CREATE_DT, USER_GROUP_ID) values (" +
            (db.isMySQL() ? "null" : "MDW_COMMON_ID_SEQ.NEXTVAL") + ", " +
            "(select user_info_id from user_info where cuid = ?), 'MDW', " + now() + ", " +
            "(select user_group_id from user_group where group_name = ?))";
        try {
            db.openConnection();
            String[] params = new String[] { cuid, group };
            db.runUpdate(query, params);
            db.commit();
        } catch (Exception ex){
            db.rollback();
            throw new DataAccessException(-1, "Failed to add user " + cuid + " to group " + group, ex);
        } finally {
            db.closeConnection();
        }
    }

    public void removeUserFromGroup(String cuid, String group) throws DataAccessException {
        String query = "delete from USER_GROUP_MAPPING " +
            " where user_info_id = (select user_info_id from user_info where cuid = '" + cuid + "') " +
            " and user_group_id = (select user_group_id from user_group where group_name = '" + group + "')";
        try {
            db.openConnection();
            db.runUpdate(query, null);
            db.commit();
        } catch (Exception ex){
            db.rollback();
            throw new DataAccessException(-1, "Failed to remove user " + cuid + " from group " + group, ex);
        } finally {
            db.closeConnection();
        }
    }


    public void addUserToRole(String cuid, String role) throws DataAccessException {
        String query = "insert into USER_ROLE_MAPPING " +
                " (USER_ROLE_MAPPING_ID, USER_ROLE_MAPPING_OWNER, USER_ROLE_MAPPING_OWNER_ID," +
                "  CREATE_DT,CREATE_USR,USER_ROLE_ID) values (" +
                (db.isMySQL() ? "null" : "MDW_COMMON_ID_SEQ.NEXTVAL") + ",'USER', "
                + "(select user_info_id from user_info where cuid = ?),"+ now() + ",'MDW',"
                + "(select user_role_id from user_role where user_role_name = ?))";
        try {
            db.openConnection();
            String[] params = new String[] { cuid, role };
            db.runUpdate(query, params);
            db.commit();
        } catch (Exception ex){
            db.rollback();
            throw new DataAccessException(-1, "Failed to add user " + cuid + " to role " + role, ex);
        } finally {
            db.closeConnection();
        }
    }

    public void removeUserFromRole(String cuid, String role) throws DataAccessException {
        // delete user-role mapping
        String query = "delete from USER_ROLE_MAPPING "
                + " where USER_ROLE_MAPPING_OWNER_ID= (select user_info_id from user_info where cuid = '" + cuid + "') " +
                " and user_role_id = (select user_role_id from user_role where user_role_name = '" + role + "')";
        try {
            db.openConnection();
            db.runUpdate(query, null);
            db.commit();
        } catch (Exception ex){
            db.rollback();
            throw new DataAccessException(-1, "Failed to remove user " + cuid + " from role " + role, ex);
        } finally {
            db.closeConnection();
        }
    }


	public void updateUsersForGroup(Long groupId, Long[] users)
    throws DataAccessException {
		String selectQuery = "select u.USER_INFO_ID " +
			"from USER_INFO u, USER_GROUP ug, USER_GROUP_MAPPING ugm " +
			"where ug.USER_GROUP_ID = ? " +
			"   and ugm.USER_INFO_ID = u.USER_INFO_ID" +
			"   and ugm.USER_GROUP_ID = ug.USER_GROUP_ID";
		String deleteQuery = "delete from USER_GROUP_MAPPING where USER_GROUP_ID=? " +
			" and USER_INFO_ID=?";
		String insertQuery = "insert into USER_GROUP_MAPPING" +
			" (USER_GROUP_MAPPING_ID, USER_GROUP_ID, USER_INFO_ID," +
			"  CREATE_DT,CREATE_USR,COMMENTS) values (" +
			(db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL") +
			",?,?,"+now()+",'MDW','Converted')";
		String errmsg = "Failed to update users for group";
		this.updateMembersById(groupId, users, selectQuery, deleteQuery, insertQuery, errmsg);
	}

    public void updateUsersForGroup(Long groupId, String[] users)
    throws DataAccessException {
        String selectQuery = "select u.USER_INFO_ID, u.CUID " +
            "from USER_INFO u, USER_GROUP ug, USER_GROUP_MAPPING ugm " +
            "where ug.USER_GROUP_ID = ? " +
            "   and ugm.USER_INFO_ID = u.USER_INFO_ID" +
            "   and ugm.USER_GROUP_ID = ug.USER_GROUP_ID";
        String deleteQuery = "delete from USER_GROUP_MAPPING where USER_GROUP_ID=? and USER_INFO_ID=?";
        String findQuery = "select USER_INFO_ID from USER_INFO where CUID=?";
        String insertQuery = "insert into USER_GROUP_MAPPING" +
            " (USER_GROUP_MAPPING_ID, USER_GROUP_ID," +
            "  CREATE_DT,CREATE_USR,USER_INFO_ID,COMMENTS) values (" +
            (db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL") +
            ",?," + now() + ",'MDW',?,'Converted')";
        String errmsg = "Failed to update groups for user";
        updateMembersByName(groupId, users, selectQuery, deleteQuery,
            findQuery, insertQuery, errmsg);
    }

    public Map<String,String> getUserPreferences(Long userId) throws DataAccessException {
		try {
			db.openConnection();
			String sql = "select ATTRIBUTE_NAME, ATTRIBUTE_VALUE from ATTRIBUTE " +
				"where ATTRIBUTE_OWNER='" + OwnerType.USER + "' and ATTRIBUTE_OWNER_ID=?";
			 ResultSet rs = db.runSelect(sql, userId);
			 Map<String,String> map = new HashMap<String,String>();
			 while (rs.next()) {
				 map.put(rs.getString(1), rs.getString(2));
			 }
			 return map;
		} catch(Exception ex){
			throw new DataAccessException(-1, "Failed to get user preferences", ex);
		} finally {
			db.closeConnection();
		}
	}

	public void updateUserPreferences(Long userId, Map<String,String> preferences)
		throws DataAccessException {
		try {
			db.openConnection();
			String sql = "select ATTRIBUTE_NAME, ATTRIBUTE_VALUE from ATTRIBUTE " +
				"where ATTRIBUTE_OWNER='" + OwnerType.USER + "' and ATTRIBUTE_OWNER_ID=?";
			ResultSet rs = db.runSelect(sql, userId);
			Map<String,String> existing = new HashMap<String,String>();
			while (rs.next()) {
				existing.put(rs.getString(1), rs.getString(2));
			}

			String deleteQuery = "delete from ATTRIBUTE where " +
				" ATTRIBUTE_OWNER=? and ATTRIBUTE_OWNER_ID=? and ATTRIBUTE_NAME=?";
			Object[] args = new Object[3];
			args[0] = OwnerType.USER;
			args[1] = userId;
			for (String key : existing.keySet()) {
				if (preferences == null || !preferences.containsKey(key)) {
					args[2] = key;
					db.runUpdate(deleteQuery, args);
				}
			}
			if (preferences != null && !preferences.isEmpty()) {
    			String insertQuery = "insert into ATTRIBUTE" +
    				" (ATTRIBUTE_ID,ATTRIBUTE_OWNER,ATTRIBUTE_VALUE,ATTRIBUTE_OWNER_ID,ATTRIBUTE_NAME,CREATE_DT,CREATE_USR)" +
    				" values (" +
    				(db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL") +
    	            ",'"+OwnerType.USER+"',?,?,?,"+now()+",'MDW')";
    			String updateQuery = "update ATTRIBUTE set ATTRIBUTE_VALUE=? where " +
    				" ATTRIBUTE_OWNER='"+OwnerType.USER+"' and ATTRIBUTE_OWNER_ID=? and ATTRIBUTE_NAME=?";
    			args[1] = userId;
    			for (String key : preferences.keySet()) {
    				args[0] = preferences.get(key);
    				args[2] = key;
    				if (existing.containsKey(key)) {
    					db.runUpdate(updateQuery, args);
    				} else {
    					db.runUpdate(insertQuery, args);
    				}
    			}
			}
			db.commit();
		} catch(Exception ex){
			db.rollback();
			throw new DataAccessException(-1, "Failed to update user preferences", ex);
		} finally {
			db.closeConnection();
		}
	}

    /**
     * Ignores non-public attributes (those that contain a ':').
     */
    public void updateUserAttributes(Long userId, Map<String,String> attributes)
            throws DataAccessException {
            try {
                db.openConnection();

                String deleteQuery = "delete from ATTRIBUTE where " +
                    " ATTRIBUTE_OWNER='" + OwnerType.USER + "' and ATTRIBUTE_OWNER_ID=? and attribute_name not like '%:%'";
                db.runUpdate(deleteQuery, userId);


                if (attributes != null && !attributes.isEmpty()) {
                    List<AttributeVO> attrs = new ArrayList<AttributeVO>();
                    for (String name : attributes.keySet()) {
                        String value = attributes.get(name);
                        if (value != null && !value.isEmpty())
                            attrs.add(new AttributeVO(name, value));
                    }
                    addAttributes0(OwnerType.USER, userId, attrs);
                }
                db.commit();
            } catch(Exception ex){
                db.rollback();
                throw new DataAccessException(-1, "Failed to update user attributes for userId: " + userId, ex);
            } finally {
                db.closeConnection();
            }
        }

    public List<String> getPublicUserAttributeNames()
    throws DataAccessException {
        try {
            db.openConnection();
            List<String> attrs = new ArrayList<String>();
            String query = "select distinct attribute_name from attribute "
                + "where attribute_owner = 'USER' and attribute_name not like '%:%' "
                + "order by lower(attribute_name)";
            ResultSet rs = db.runSelect(query, null);
            while (rs.next())
                attrs.add(rs.getString("attribute_name"));
            return attrs;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to get task actions", e);
        } finally {
            db.closeConnection();
        }
    }

}
