/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

import com.centurylink.mdw.model.value.user.AuthenticatedUser;

public interface TestUserService {

    public String ping(AuthenticatedUser user);

    public AuthenticatedUser createUser(Long id, String cuid, String name);

}
