/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.util.List;
import java.util.Set;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;

public interface UserDataAccess {

    /**
     * The get the user, whose significant contents are
     * group names and role names for each group name.
     * @param userName
     * @return UserVO object that contains group names and role names for each group.
     * @throws DataAccessException
     */
    UserVO getUser(String userName)
    throws DataAccessException;

    /**
     * Get the list of all groups.
     * @return
     * @throws DataAccessException
     */
    List<UserGroupVO> getAllGroups(boolean includeDeleted)
    throws DataAccessException;

    List<String> getRoleNames()
    throws DataAccessException;

    void auditLogUserAction(UserActionVO userAction)
    throws DataAccessException;

    public boolean isOnline() throws DataAccessException;

    public DataAccessOfflineException getDataAccessOfflineException();

    /**
     * No longer used since MDW 5.2
     * @param userName
     * @return
     * @throws DataAccessException
     */
    Set<String> getPrivilegesForUser(String userName)
    throws DataAccessException;
}
