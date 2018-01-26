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
package com.centurylink.mdw.service.data.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.util.timer.CodeTimer;

/**
 * Cache for MDW users/groups/roles (plus group relationships for tasks).
 *
 * TODO: Currently everything is loaded into memory. Beyond 10,000 users this
 * may become unwieldy.  For MDW 6.0 we'll investigate a NoSQL, BigData solution.
 *
 */
public class UserGroupCache implements PreloadableCache {

    private static UserGroupCache instance;

    // these are loaded initially (and users are shallow)
    private List<User> users;
    private List<Workgroup> workgroups;
    private List<Role> roles;
    private List<String> userAttributeNames;

    // these are loaded on demand
    private volatile Map<String,User> usersByCuid = new HashMap<String,User>(); // includes group-roles and attributes
    private volatile Map<Long,User> usersById = new HashMap<Long,User>(); // TODO: remove usages
    private volatile Map<String,Workgroup> groupsByName = new HashMap<String,Workgroup>(); // includes (shallow) users
    private volatile Map<String,Role> rolesByName = new HashMap<String,Role>();

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
    }

    public synchronized void refreshCache() throws CachingException {
        clearCache();
        load();
    }

    /**
     * Does not include deleted.
     */
    public static List<Workgroup> getWorkgroups() {
        return instance.getWorkgroups0();
    }

    public static Workgroup getWorkgroup(String groupName) throws CachingException {
        return instance.getWorkgroup0(groupName);
    }

    public static void set(Workgroup workgroup) {
        instance.workgroups.remove(workgroup);
        instance.workgroups.add(workgroup);
        Collections.sort(instance.workgroups); // in case new or name changed
        instance.clearCache();
    }

    public static void remove(Workgroup workgroup) {
        instance.workgroups.remove(workgroup);
        instance.clearCache();
    }

    /**
     * Clears only the relationships (not the base data).
     */
    public static void clear() {
        instance.clearCache();
    }

    public static void set(Role role) {
        instance.roles.remove(role);
        instance.roles.add(role);
        Collections.sort(instance.roles); // in case new or name changed
        instance.clearCache();
    }

    public static void remove(Role role) {
        instance.roles.remove(role);
        instance.clearCache();
    }


    public static List<User> getUsers() {
        return instance.users;
    }

    public static List<User> getUsers(int start, int pageSize) {
        int end = start + pageSize;
        int max = instance.users.size();
        return instance.users.subList(start, end > max ? max : end);
    }

    public static int getTotalUsers() {
        return instance.users.size();
    }

    public static User getUser(String cuid) throws CachingException {
        return instance.getUser0(cuid);
    }

    public static List<User> findUsers(String prefix) {
        int limit = 15;
        List<User> matches = new ArrayList<User>();
        prefix = prefix.toLowerCase();
        for (int i = 0; i < instance.users.size() && matches.size() <= limit; i++) {
            User user = instance.users.get(i);
            if ((user.getFirst() != null && user.getFirst().toLowerCase().startsWith(prefix))
                    || (user.getLast() != null && user.getLast().toLowerCase().startsWith(prefix))
                    || user.getName().toLowerCase().startsWith(prefix) || user.getCuid().toLowerCase().startsWith(prefix))
                matches.add(user);
        }
        return matches;
    }

    public static List<User> findUsers(String[] workgroups, String prefix) throws CachingException {
        int limit = 15;
        List<User> matches = new ArrayList<User>();
        prefix = prefix.toLowerCase();
        int count = 0;
        for (String groupName : workgroups) {
            Workgroup workgroup = getWorkgroup(groupName);
            for (User user : workgroup.getUsers()) {
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

    private static boolean isMatch(User user, String prefix) {
        return prefix == null
                || (user.getFirst() != null && user.getFirst().toLowerCase().startsWith(prefix))
                || (user.getLast() != null && user.getLast().toLowerCase().startsWith(prefix))
                || user.getName().toLowerCase().startsWith(prefix) || user.getCuid().toLowerCase().startsWith(prefix);
    }

    public static void set(User user) {
        instance.users.remove(user);
        instance.users.add(user);
        Collections.sort(instance.users); // in case new or name changed
        instance.clearCache();
    }

    public static void remove(User user) {
        instance.users.remove(user);
        instance.clearCache();
    }

    public static List<String> getUserAttributeNames() {
        return instance.userAttributeNames;
    }

    public static User getUser(Long id) throws CachingException {
        return instance.getUser0(id);
    }

    public static List<Role> getRoles() {
        return instance.roles;
    }

    public static Role getRole(String name) throws CachingException {
        return instance.getRole0(name);
    }

    private List<Workgroup> getWorkgroups0() {
        return workgroups;
    }

    private Workgroup getWorkgroup0(String name) throws CachingException {
        Workgroup group = groupsByName.get(name);
        if (group == null) {
            synchronized(groupsByName) {
                group = groupsByName.get(name);
                if (group == null) {
                    group = loadWorkgroup(name);
                    if (group != null)
                        groupsByName.put(name, group);
                }
            }
        }
        return group;
    }

    private User getUser0(String cuid) throws CachingException {
        User user = usersByCuid.get(cuid);
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

    private User getUser0(Long id) throws CachingException {
        User user = usersById.get(id);
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

    private Role getRole0(String name) throws CachingException {
        Role role = rolesByName.get(name);
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
            if (!userAttributeNames.contains(User.EMAIL_ADDRESS) && !getUserAttributeNames().contains(User.OLD_EMAIL_ADDRESS))
                userAttributeNames.add(User.EMAIL_ADDRESS);
            if (!userAttributeNames.contains(User.PHONE_NUMBER) && !getUserAttributeNames().contains(User.OLD_PHONE_NUMBER))
                userAttributeNames.add(User.PHONE_NUMBER);
        }
        catch (Exception ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
        finally {
            timer.stopAndLogTiming("UserGroupCache.load()");
        }
    }

    private Role loadRole(String name) throws CachingException {
        try {
            UserManager userManager = ServiceLocator.getUserManager();
            return userManager.getUserRole(name);
        }
        catch (Exception ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }

    private Workgroup loadWorkgroup(String name) throws CachingException {
        try {
            UserManager userManager = ServiceLocator.getUserManager();
            Workgroup group = userManager.getUserGroup(name, false);
            if (group != null && group.getAttribute(Workgroup.SLACK_CHANNEL) == null)
                group.setAttribute(Workgroup.SLACK_CHANNEL, "");
            return group;
        }
        catch (Exception ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }

    private User loadUser(String cuid) throws CachingException {
        try {
            UserManager userManager = ServiceLocator.getUserManager();
            return userManager.getUser(cuid);
        }
        catch (Exception ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }

    private User loadUser(Long id) throws CachingException {
        try {
            UserManager userManager = ServiceLocator.getUserManager();
            return userManager.getUser(id);
        }
        catch (Exception ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }
}
