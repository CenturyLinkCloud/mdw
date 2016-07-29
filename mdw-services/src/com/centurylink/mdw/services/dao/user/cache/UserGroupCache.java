/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.user.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.cache.PreloadableCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.provider.CacheService;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;

/**
 * Cache for MDW users/groups/roles (plus group relationships for tasks).
 *
 * TODO: Currently everything is loaded into memory. Beyond 10,000 users this
 * may become unwieldy.  For MDW 6.0 we'll investigate a NoSQL, BigData solution.
 *
 */
public class UserGroupCache implements PreloadableCache, CacheService {

	private static UserGroupCache instance;

	// these are loaded initially (and users are shallow)
	private List<UserVO> users;
    private List<UserGroupVO> workgroups;
    private List<UserRoleVO> roles;
    private List<String> userAttributeNames;

    // these are loaded on demand
    private volatile Map<String,UserVO> usersByCuid = new HashMap<String,UserVO>(); // includes group-roles and attributes
    private volatile Map<Long,UserVO> usersById = new HashMap<Long,UserVO>(); // TODO: remove usages
    private volatile Map<String,UserGroupVO> groupsByName = new HashMap<String,UserGroupVO>(); // includes (shallow) users
    private volatile Map<String,UserRoleVO> rolesByName = new HashMap<String,UserRoleVO>();
    private volatile Map<String,List<TaskVO>> groupTasks = new HashMap<String,List<TaskVO>>();

    public void initialize(Map<String,String> params) {
    }

    public void loadCache() throws CachingException {
        instance = this;
        load();
    }

    public void clearCache() {
        synchronized(usersByCuid) {
        usersByCuid.clear();
        usersById.clear();
        }
        synchronized(groupsByName) {
        groupsByName.clear();
        }
        synchronized(rolesByName) {
        rolesByName.clear();
        }
        synchronized(groupTasks) {
        groupTasks.clear();
        }
    }

    public synchronized void refreshCache() throws CachingException {
        clearCache();
        load();
    }

    /**
     * Does not include deleted.
     */
    public static List<UserGroupVO> getWorkgroups() {
        return instance.getWorkgroups0();
    }

    public static UserGroupVO getWorkgroup(String groupName) throws CachingException {
    	return instance.getWorkgroup0(groupName);
    }

    public static void set(UserGroupVO workgroup) {
        instance.workgroups.remove(workgroup);
        instance.workgroups.add(workgroup);
        Collections.sort(instance.workgroups); // in case new or name changed
        instance.clearCache();
    }

    public static void remove(UserGroupVO workgroup) {
        instance.workgroups.remove(workgroup);
        instance.clearCache();
    }

    /**
     * Clears only the relationships (not the base data).
     */
    public static void clear() {
        instance.clearCache();
    }

    public static void set(UserRoleVO role) {
        instance.roles.remove(role);
        instance.roles.add(role);
        Collections.sort(instance.roles); // in case new or name changed
        instance.clearCache();
    }

    public static void remove(UserRoleVO role) {
        instance.roles.remove(role);
        instance.clearCache();
    }


    public static List<UserVO> getUsers() {
        return instance.users;
    }

    public static List<UserVO> getUsers(int start, int pageSize) {
        int end = start + pageSize;
        int max = instance.users.size();
        return instance.users.subList(start, end > max ? max : end);
    }

    public static int getTotalUsers() {
        return instance.users.size();
    }

    public static UserVO getUser(String cuid) throws CachingException {
        return instance.getUser0(cuid);
    }

    public static List<UserVO> findUsers(String prefix) {
        int limit = 15;
        List<UserVO> matches = new ArrayList<UserVO>();
        prefix = prefix.toLowerCase();
        for (int i = 0; i < instance.users.size() && matches.size() <= limit; i++) {
            UserVO user = instance.users.get(i);
            if ((user.getFirst() != null && user.getFirst().toLowerCase().startsWith(prefix))
                    || (user.getLast() != null && user.getLast().toLowerCase().startsWith(prefix))
                    || user.getName().toLowerCase().startsWith(prefix) || user.getCuid().toLowerCase().startsWith(prefix))
                matches.add(user);
        }
        return matches;
    }

    public static List<UserVO> findUsers(String[] workgroups, String prefix) throws CachingException {
        int limit = 15;
        List<UserVO> matches = new ArrayList<UserVO>();
        prefix = prefix.toLowerCase();
        int count = 0;
        for (String groupName : workgroups) {
            UserGroupVO workgroup = getWorkgroup(groupName);
            for (UserVO user : workgroup.getUsers()) {
                user = getUser(user.getId()); // load full user
                if (isMatch(user, prefix) && !matches.contains(user)) {
                    matches.add(user);
                    if (++count > limit)
                        return matches;
                }
            }
        }
        return matches;
    }

    private static boolean isMatch(UserVO user, String prefix) {
        return (user.getFirst() != null && user.getFirst().toLowerCase().startsWith(prefix))
                || (user.getLast() != null && user.getLast().toLowerCase().startsWith(prefix))
                || user.getName().toLowerCase().startsWith(prefix) || user.getCuid().toLowerCase().startsWith(prefix);
    }

    public static void set(UserVO user) {
        instance.users.remove(user);
        instance.users.add(user);
        Collections.sort(instance.users); // in case new or name changed
        instance.clearCache();
    }

    public static void remove(UserVO user) {
        instance.users.remove(user);
        instance.clearCache();
    }

    public static List<String> getUserAttributeNames() {
        return instance.userAttributeNames;
    }

    public static UserVO getUser(Long id) throws CachingException {
        return instance.getUser0(id);
    }

    public static List<UserRoleVO> getRoles() {
        return instance.roles;
    }

    public static UserRoleVO getRole(String name) throws CachingException {
        return instance.getRole0(name);
    }

    public static List<TaskVO> getTasksForGroup(String groupName) throws CachingException {
    	return instance.getTasksForGroup0(groupName);
    }


    private List<UserGroupVO> getWorkgroups0() {
        return workgroups;
    }

    private UserGroupVO getWorkgroup0(String name) throws CachingException {
        UserGroupVO group = groupsByName.get(name);
        if (group == null) {
            synchronized(groupsByName) {
                group = groupsByName.get(name);
                if (group == null) {
                    group = loadWorkgroup(name);
                    if (group == null)
                        throw new CachingException("Cannot find workgroup: " + name);
                    else
                        groupsByName.put(name, group);
                }
            }
        }
        return group;
    }

    private UserVO getUser0(String cuid) throws CachingException {
        UserVO user = usersByCuid.get(cuid);
        if (user == null) {
            synchronized(usersByCuid) {
                user = usersByCuid.get(cuid);
                if (user == null) {
                    user = loadUser(cuid);
                    if (user == null)
                        return null;
                    else {
                        usersByCuid.put(cuid, user);
                        usersById.put(user.getId(), user);
                    }
                }
            }
        }
        return user;
    }

    private UserVO getUser0(Long id) throws CachingException {
        UserVO user = usersById.get(id);
        if (user == null) {
            synchronized(usersByCuid) {
                user = usersById.get(id);
                if (user == null) {
                    user = loadUser(id);
                    if (user == null)
                        throw new CachingException("Cannot find user id: " + id);
                    else {
                        usersById.put(id, user);
                        usersByCuid.put(user.getCuid(), user);
                    }
                }
            }
        }
        return user;
    }

    private UserRoleVO getRole0(String name) throws CachingException {
        UserRoleVO role = rolesByName.get(name);
        if (role == null) {
            synchronized(rolesByName) {
                role = rolesByName.get(name);
                if (role == null) {
                    role = loadRole(name);
                    if (role == null)
                        throw new CachingException("Cannot find role: " + name);
                    else
                        rolesByName.put(name, role);
                }
            }
        }
        return role;
    }

    private synchronized void load() throws CachingException {
        CodeTimer timer = null;
        try {
            timer = new CodeTimer(true);
            UserManager userManager = ServiceLocator.getUserManager();

            // roles
            roles = userManager.getUserRoles();

            // groups
            workgroups = userManager.getUserGroups(false);

            // users TODO: one query per user is executed for loading groups/roles
            users = userManager.getUsers();

            userAttributeNames = userManager.getPublicUserAttributeNames();
            if (!userAttributeNames.contains(UserVO.EMAIL_ADDRESS) && !getUserAttributeNames().contains(UserVO.OLD_EMAIL_ADDRESS))
                userAttributeNames.add(UserVO.EMAIL_ADDRESS);
            if (!userAttributeNames.contains(UserVO.PHONE_NUMBER) && !getUserAttributeNames().contains(UserVO.OLD_PHONE_NUMBER))
                userAttributeNames.add(UserVO.PHONE_NUMBER);
        }
        catch (Exception ex) {
            throw new CachingException(-1, ex.getMessage(), ex);
        }
        finally {
            timer.stopAndLogTiming("UserGroupCache.load()");
        }
    }

    private UserRoleVO loadRole(String name) throws CachingException {
        try {
            UserManager userManager = ServiceLocator.getUserManager();
            return userManager.getUserRole(name);
        }
        catch (Exception ex) {
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }

    private UserGroupVO loadWorkgroup(String name) throws CachingException {
        try {
            UserManager userManager = ServiceLocator.getUserManager();
            return userManager.getUserGroup(name, false);
        }
        catch (Exception ex) {
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }

    private UserVO loadUser(String cuid) throws CachingException {
        try {
            UserManager userManager = ServiceLocator.getUserManager();
            return userManager.getUser(cuid);
        }
        catch (Exception ex) {
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }

    private UserVO loadUser(Long id) throws CachingException {
        try {
            UserManager userManager = ServiceLocator.getUserManager();
            return userManager.getUser(id);
        }
        catch (Exception ex) {
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }

    /**
     * Returns tasks mapped to a group at the task definition (or template) level.
     */
    private List<TaskVO> getTasksForGroup0(String groupName) throws CachingException {
        List<TaskVO> tasks = groupTasks.get(groupName);
        if (tasks == null) {
            synchronized(groupTasks) {
                tasks = groupTasks.get(groupName);
                if (tasks == null) {
                    tasks = loadTasksForGroup(groupName);
                    groupTasks.put(groupName, tasks);
                }
            }
        }
        return tasks;
    }

    private List<TaskVO> loadTasksForGroup(String groupName) throws CachingException {
        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();
            return taskManager.getTasksForWorkgroup(groupName);
        }
        catch (Exception ex) {
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }

}
