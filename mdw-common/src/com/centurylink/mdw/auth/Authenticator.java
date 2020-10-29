package com.centurylink.mdw.auth;

/**
 * Authentication API.
 */
public interface Authenticator {

    /**
     * AuthenticationException indicates bad credentials, whereas general MdwSecurityException
     * indicates some other type of error.
     */
    void authenticate(String user, String password) throws MdwSecurityException;

    /**
     * @return Identifies this authenticator versus others of the same type to allow clients
     * to avoid re-prompting for credentials when the user has already logged in to the same location.
     */
    String getKey();
}
