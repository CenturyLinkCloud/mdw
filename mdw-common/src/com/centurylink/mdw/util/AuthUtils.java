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
package com.centurylink.mdw.util;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.auth.LdapAuthenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.auth.OAuthAuthenticator;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.AuthConstants;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class AuthUtils {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    //TODO Add more as required
    public static final String HTTP_BASIC_AUTHENTICATION = "Basic";
    public static final String GIT_HUB_SECRETE_KEY = "GitHub";
   /**
     * <p>
     * Currently uses the metainfo property "Authorization" and checks
     * specifically for Basic Authentication in a property.
     * In the future, probably change this.
     * </p>
     * <p>
     * TODO: call this from every protocol channel
     * If nothing else we should to this to avoid spoofing the AUTHENTICATED_USER_HEADER.
     * </p>
     * @param headers
     * @return
     */
    public static boolean authenticate(Map<String,String> headers, String authMethod) {
        headers.remove(Listener.AUTHENTICATED_USER_HEADER); // avoid any fishiness -- only we should populate this header
        String hdr = headers.get(Listener.AUTHORIZATION_HEADER_NAME);
        if (hdr == null)
            hdr = headers.get(Listener.AUTHORIZATION_HEADER_NAME.toLowerCase());
        if (PropertyManager.getBooleanProperty(PropertyNames.HTTP_BASIC_AUTH_MODE, false)) {
            // auth required
            if (hdr == null) {
                if (ApplicationContext.isDevelopment() && ApplicationContext.getDevUser() != null) {
                    headers.put(Listener.AUTHENTICATED_USER_HEADER, ApplicationContext.getDevUser());
                    return true;
                }
                return false;
            }
            else
                return checkBasicAuthenticationHeader(headers);
        }
        else {
            // auth not required but accepted
            if (hdr == null) {
                if (ApplicationContext.isDevelopment() && ApplicationContext.getDevUser() != null)
                    headers.put(Listener.AUTHENTICATED_USER_HEADER, ApplicationContext.getDevUser());
                return true;
            }
            else
                return checkBasicAuthenticationHeader(headers);
        }
    }

    /**
     * @return true if no authentication at all or authentication is successful
     */
    public static boolean checkBasicAuthenticationHeader(Map<String,String> headers) {

        String authorizationHeader = headers.get(Listener.AUTHORIZATION_HEADER_NAME);
        if (authorizationHeader == null)
            authorizationHeader = headers.get(Listener.AUTHORIZATION_HEADER_NAME.toLowerCase());

        if (authorizationHeader!=null) {
            authorizationHeader = authorizationHeader.replaceFirst("Basic ", "");

            byte[] valueDecoded= Base64.decodeBase64(authorizationHeader.getBytes());
            authorizationHeader = new String(valueDecoded);

            String[] creds = authorizationHeader.split(":");
            String user = creds[0];
            String pass = creds[1];

            try {
                if (AuthConstants.getOAuthTokenLocation() != null) {
                    oauthAuthenticate(user, pass);
                }
                else {
                    ldapAuthenticate(user, pass);
                }

                /**
                 * Authentication passed so take credentials out
                 * of the metadata and just put user name in.
                 */
                headers.remove(Listener.AUTHORIZATION_HEADER_NAME);
                headers.remove(Listener.AUTHORIZATION_HEADER_NAME.toLowerCase());
                headers.put(Listener.AUTHENTICATED_USER_HEADER, user);
                if (logger.isDebugEnabled()) {
                    logger.debug("authentication successful for user '"+user+"'");
                }
            }
            catch (Exception ex) {
                headers.remove(Listener.AUTHORIZATION_HEADER_NAME);
                headers.remove(Listener.AUTHORIZATION_HEADER_NAME.toLowerCase());
                headers.put(Listener.AUTHENTICATION_FAILED, "Authentication failed for '"+user+"' "+ex.getMessage());
                logger.severeException("Authentication failed for user '"+user+"'" + ex.getMessage(), ex);
                return false;
            }
        }
        return true;
    }

    public static void ldapAuthenticate(String user, String password) throws MdwSecurityException {
        String ldapProtocol = PropertyManager.getProperty(PropertyNames.LDAP_PROTOCOL);
        if (ldapProtocol == null)
            ldapProtocol = "ldap";
        String ldapHost = PropertyManager.getProperty(PropertyNames.LDAP_HOST);
        String ldapPort = PropertyManager.getProperty(PropertyNames.LDAP_PORT);
        String ldapUrl = ldapProtocol + "://" + ldapHost + ":" + ldapPort;
        String baseDn = PropertyManager.getProperty(PropertyNames.BASE_DN);
        LdapAuthenticator auth = new LdapAuthenticator(ldapUrl, baseDn);
        auth.authenticate(user, password);
    }

    public static void oauthAuthenticate(String user, String password) throws MdwSecurityException {
        new OAuthAuthenticator().authenticate(user, password);
    }

    public static boolean authenticate(Map<String,String> headers, String authMethod, String payload) {
        headers.remove(Listener.AUTHENTICATED_USER_HEADER); // avoid any fishiness -- only we should populate this header
        String signature = headers.get("x-hub-signature");
        logger.debug("signature " + signature);
        String key = PropertyManager.getProperty(PropertyNames.MDW_GITHUB_SECRET_TOKEN);
        String payloadSig;
        try {
            payloadSig = "sha1=" + HmacSha1Signature.getHMACHexdigestSignature(payload.trim().getBytes("UTF-8"), key);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
        logger.debug("payloadSignature " + payloadSig);
        if (payloadSig.equals(signature)) {
            headers.put(Listener.AUTHENTICATED_USER_HEADER, "mdwapp");
            if (logger.isDebugEnabled()) {
                logger.debug("authentication successful for user mdwapp");
            }
            return true;
        }
        return false;
    }
}
