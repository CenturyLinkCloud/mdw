/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.auth;

import java.util.Hashtable;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.SSLHandshakeException;

public class LdapAuthenticator implements Authenticator {

    public static final String DEFAULT_PROTOCOL = "ldap";
    public static final String DEFAULT_HOST = "ldap.qintra.com";
    public static final String DEFAULT_TEST_HOST = "ldapt.dev.qintra.com";
    public static final int DEFAULT_PORT = 1636;
    public static final String DEFAULT_BASE_DN = "dc=mnet,dc=qintra,dc=com";

    private String ldapUrl;
    private String baseDn;

    public LdapAuthenticator() {
        this(DEFAULT_PROTOCOL + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT, DEFAULT_BASE_DN);
    }

    public LdapAuthenticator(String ldapUrl) {
        this(ldapUrl, DEFAULT_BASE_DN);
    }

    /**
     * @param ldapUrl = ldap://ldapt.dev.qintra.com:1636
     * @param baseDn = dc=mnet,dc=qintra,dc=com
     */
    public LdapAuthenticator(String ldapUrl, String baseDn) {
        this.ldapUrl = ldapUrl;
        this.baseDn = baseDn;
    }

    public void authenticate(String user, String password) throws MdwSecurityException {
        String principal = "uid=" + user + ",ou=people," + baseDn;
        try {
            authenticate(ldapUrl + "/" + baseDn, principal, password);
        }
        catch (NamingException ex) {
            throw new MdwSecurityException(ex.getMessage(), ex);
        }
    }


    /**
     * @param ldapUrl = ldap://ldapt.dev.qintra.com:1636/dc=mnet,dc=qintra,dc=com
     * @param principal = uid=<USER>,ou=people,dc=mnet,dc=qintra,dc=com
     * @param password = <PASSWORD>
     * @throws MdwSecurityException
     */
    public void authenticate(String ldapUrl, String principal, String password) throws NamingException, MdwSecurityException {
        try {
            Hashtable<String,String> env = new Hashtable<String,String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, principal);
            env.put(Context.SECURITY_CREDENTIALS, password);
            new InitialDirContext(env);
        }
        catch (CommunicationException ex) {
            Throwable t = ex.getCause();
            if (t instanceof SSLHandshakeException)
                throw new MdwSecurityException(MdwSecurityException.UNTRUSTED_CERT, "Untrusted SSL Certificate Chain", ex);
            else
                throw new MdwSecurityException(ex.getMessage(), ex);
        }
        catch (javax.naming.AuthenticationException ex) {
            throw new AuthenticationException("LDAP authentication failure", ex);
        }
        catch (Exception ex) {
            throw new MdwSecurityException(ex.getMessage(), ex);
        }
    }

    public String getKey() {
        return ldapUrl + "_" + baseDn;
    }

    public static void main(String[] args) {
        if (args.length != 2)
            throw new RuntimeException("args: <user> <password>");
        String ldapUrl = DEFAULT_PROTOCOL + "://" + DEFAULT_TEST_HOST + ":" + DEFAULT_PORT;
        String baseDn = DEFAULT_BASE_DN;
        Authenticator auth = new LdapAuthenticator(ldapUrl, baseDn);
        try {
            auth.authenticate(args[0], args[1]);
            System.out.print("authenticated user " + args[0]);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
