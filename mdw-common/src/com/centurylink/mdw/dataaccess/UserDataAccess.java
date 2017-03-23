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
package com.centurylink.mdw.dataaccess;

import java.util.List;
import java.util.Set;

import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.user.User;

public interface UserDataAccess {

    /**
     * The get the user, whose significant contents are
     * group names and role names for each group name.
     * @param userName
     * @return UserVO object that contains group names and role names for each group.
     * @throws DataAccessException
     */
    User getUser(String userName)
    throws DataAccessException;

    /**
     * Get the list of all groups.
     * @return
     * @throws DataAccessException
     */
    List<Workgroup> getAllGroups(boolean includeDeleted)
    throws DataAccessException;

    List<String> getRoleNames()
    throws DataAccessException;

    void auditLogUserAction(UserAction userAction)
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
