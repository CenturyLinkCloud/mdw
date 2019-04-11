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
package com.centurylink.mdw.auth;

import java.util.Hashtable;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.SSLHandshakeException;

public class LdapAuthenticator implements Authenticator {

    public static final String DEFAULT_PROTOCOL = "ldap";
    public static final String DEFAULT_HOST = "ldap.example.com";
    public static final String DEFAULT_TEST_HOST = "ldapt.example.com";
    public static final int DEFAULT_PORT = 1636;
    public static final String DEFAULT_BASE_DN = "dc=mynet,dc=example,dc=com";

    private String ldapUrl;
    private String baseDn;

    public LdapAuthenticator() {
        this(DEFAULT_PROTOCOL + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT, DEFAULT_BASE_DN);
    }

    public LdapAuthenticator(String ldapUrl) {
        this(ldapUrl, DEFAULT_BASE_DN);
    }

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
