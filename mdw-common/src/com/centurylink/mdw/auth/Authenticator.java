/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.auth;

/**
 * Authentication API for Designer and MDWHub.
 */
public interface Authenticator {

    /**
     * AuthenticationException indicates bad credentials, whereas general MdwSecurityException
     * indicates some other type of error.
     */
    public void authenticate(String user, String password) throws MdwSecurityException;

    /**
     * @return Identifies this authenticator versus others of the same type to allow clients
     * to avoid re-prompting for credentials when the user has already logged in to the same location.
     */
    public String getKey();
}
