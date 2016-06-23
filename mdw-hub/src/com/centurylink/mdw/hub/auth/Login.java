/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.auth;

import com.centurylink.mdw.common.constant.AuthConstants;

public class Login {
    private String user;
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isOAuth() {
        return AuthConstants.getOAuthTokenLocation() != null;
    }
}
