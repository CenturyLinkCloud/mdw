/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test.impl;

import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.test.TestUserService;

public class TestUserServiceBean implements TestUserService {

    public String ping(AuthenticatedUser user) {
        return "pong: " + user.getName();
    }

    public AuthenticatedUser createUser(Long id, String cuid, String name) {
        AuthenticatedUser authUser = new AuthenticatedUser(cuid);
        authUser.setId(id);
        authUser.setName(name);
        return authUser;
    }
}
