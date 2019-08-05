package com.centurylink.mdw.service.data.task;

import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.Workgroup;

import java.util.List;

/**
 * @deprecated use {@link com.centurylink.mdw.service.data.user.UserGroupCache}
 */
@Deprecated
public class UserGroupCache {

    @Deprecated
    public static List<User> getUsers() {
        return com.centurylink.mdw.service.data.user.UserGroupCache.getUsers();
    }

    @Deprecated
    public static User getUser(Long id) {
        return com.centurylink.mdw.service.data.user.UserGroupCache.getUser(id);
    }

    @Deprecated
    public static User getUser(String cuid) {
        return com.centurylink.mdw.service.data.user.UserGroupCache.getUser(cuid);
    }

    @Deprecated
    public static List<Workgroup> getWorkgroups() {
        return com.centurylink.mdw.service.data.user.UserGroupCache.getWorkgroups();
    }

    @Deprecated
    public static Workgroup getWorkgroup(String name) {
        return com.centurylink.mdw.service.data.user.UserGroupCache.getWorkgroup(name);
    }

    @Deprecated
    public static List<Role> getRoles() {
        return com.centurylink.mdw.service.data.user.UserGroupCache.getRoles();
    }

    @Deprecated
    public static Role getRole(String name) {
        return com.centurylink.mdw.service.data.user.UserGroupCache.getRole(name);
    }

}
