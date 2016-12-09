/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.auth;

/**
 * Performs no authentication whatsoever.
 */
public class NoOpAuthenticator implements Authenticator {

    @Override
    public void authenticate(String user, String password) throws MdwSecurityException {
    }

    @Override
    public String getKey() {
        return null;
    }
}
