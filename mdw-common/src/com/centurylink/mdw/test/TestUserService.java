/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

import com.centurylink.mdw.model.user.AuthenticatedUser;

public interface TestUserService {

    public String ping(AuthenticatedUser user);

    public AuthenticatedUser createUser(Long id, String cuid, String name);

}
