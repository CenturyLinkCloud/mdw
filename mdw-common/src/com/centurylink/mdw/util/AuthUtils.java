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
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.auth.LdapAuthenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.auth.OAuthAuthenticator;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.AuthConstants;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class AuthUtils {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String HTTP_BASIC_AUTHENTICATION = "Basic";
    public static final String GIT_HUB_SECRET_KEY = "GitHub";
    public static final String SLACK_TOKEN = "MDW_SLACK_TOKEN";
    public static final String OAUTH_AUTHENTICATION = "OAuth";
    public static final String MDW_APP_TOKEN = "MDW_APP_TOKEN";

    private static volatile Map<String,String> mdwAppTokenMap = null;
    private static final Object lock = new Object();

    public static boolean authenticate(String authMethod, Map<String,String> headers) {
        return authenticate(authMethod, headers, null);
    }

    public static boolean authenticate(String authMethod, Map<String,String> headers, String payload) {
        // avoid any fishiness -- only we should populate this header
        headers.remove(Listener.AUTHENTICATED_USER_HEADER);
        if (authMethod.equals(HTTP_BASIC_AUTHENTICATION)) {
            return authenticateHttpBasic(headers);
        }
        else if (authMethod.equals(GIT_HUB_SECRET_KEY)) {
            return authenticateGitHubSecretKey(headers, payload);
        }
        else if (authMethod.equals(SLACK_TOKEN)) {
            return authenticateSlackToken(headers, payload);
        }
        else if (authMethod.equals(MDW_APP_TOKEN)) {
            return authenticateMdwAppToken(headers, payload);
        }
        else {
            throw new IllegalArgumentException("Unsupported authentication method: " + authMethod);
        }
    }

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
    private static boolean authenticateHttpBasic(Map<String,String> headers) {
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

    private static boolean authenticateGitHubSecretKey(Map<String,String> headers, String payload) {
        String signature = headers.get(Listener.X_HUB_SIGNATURE);
        String key = System.getenv(PropertyNames.MDW_GITHUB_SECRET_TOKEN);
        try {
            String payloadSig = "sha1=" + HmacSha1Signature.getHMACHexdigestSignature(payload.trim().getBytes("UTF-8"), key);
            if (payloadSig.equals(signature)) {
                headers.put(Listener.AUTHENTICATED_USER_HEADER, "mdwapp"); // TODO: honor serviceUser in access.yaml
                return true;
            }
        }
        catch (Exception ex) {
            logger.severeException("Secret key authentication failure", ex);
            return false;
        }
        return false;
    }

    private static boolean authenticateSlackToken(Map<String,String> headers, String payload) {
        boolean okay = false;
        if (payload.startsWith("payload=")) {  // TODO: handle multiple params
            try {
                String decodedPayload = URLDecoder.decode(payload.substring(8), "utf-8");
                JSONObject json = new JSONObject(decodedPayload);
                okay = json.has("token") && json.getString("token").equals(System.getenv(SLACK_TOKEN));
                if (okay) {
                    json.remove("token");
                    headers.put(Listener.AUTHENTICATED_USER_HEADER, "mdwapp"); // TODO: honor serviceUser in access.yaml
                    headers.put(Listener.METAINFO_REQUEST_PAYLOAD, json.toString());
                }
            }
            catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("Apparently utf-8 is out of fashion", ex);
            }
        }
        else {
            // JSON request
            JSONObject json = new JSONObject(payload);
            okay = json.has("token") && json.getString("token").equals(System.getenv(SLACK_TOKEN));
            if (okay) {
                json.remove("token");
                headers.put(Listener.AUTHENTICATED_USER_HEADER, "mdwapp"); // TODO: honor serviceUser in access.yaml
                headers.put(Listener.METAINFO_REQUEST_PAYLOAD, json.toString());
            }
        }
        return okay;
    }

    private static boolean authenticateMdwAppToken(Map<String, String> headers, String payload) {
        boolean okay = false;
        if (mdwAppTokenMap == null) {
            synchronized(lock) {
                Map<String,String> tempMap = mdwAppTokenMap;
                if (tempMap == null) {
                    tempMap = new HashMap<String, String>();
                    DatabaseAccess db = new DatabaseAccess(null);
                    try (Connection conn = db.openConnection()) {
                        String select = "select name, value from values where owner_type='CLOUD' and owner_id='MDW_APP_TOKEN'";
                        ResultSet rs = db.runSelect(select, null);
                        while (rs.next()) {
                            tempMap.put(rs.getString("value"), rs.getString("name"));
                        }
                    }
                    catch (SQLException e) {
                        logger.severeException("Failed to retreive MDW Application Tokens", e);
                        return false;
                    }
                }
                mdwAppTokenMap = tempMap;
            }
        }
        else if (mdwAppTokenMap.isEmpty())  // No MDW Application Tokens stored in this instance's DB
            return false;

        try {
            // JSON request
            JSONObject json = new JSONObject(payload);
            okay = json.has("mdwAppToken") && mdwAppTokenMap.get(json.getString("mdwAppToken")) != null;
            if (okay) {
                logger.info("Request authenticated using MDW Application Token for " + mdwAppTokenMap.get(json.getString("mdwAppToken")));
                json.remove("mdwAppToken");
                headers.put(Listener.AUTHENTICATED_USER_HEADER, "mdwapp"); // TODO: honor serviceUser in access.yaml
                headers.put(Listener.METAINFO_REQUEST_PAYLOAD, json.toString());
                headers.put(Listener.METAINFO_CLOUD_ROUTING, URLDecoder.decode(headers.get("serviceDestination"), "utf-8"));
            }
        }
        catch (Exception e) {
            logger.severeException("Exception processing incoming Cloud Routing message", e);
        }
        return okay;
    }

    /**
     * @return true if no authentication at all or authentication is successful
     */
    private static boolean checkBasicAuthenticationHeader(Map<String,String> headers) {

        String authorizationHeader = headers.get(Listener.AUTHORIZATION_HEADER_NAME);
        if (authorizationHeader == null)
            authorizationHeader = headers.get(Listener.AUTHORIZATION_HEADER_NAME.toLowerCase());

        // Do NOT try to authenticate if it's not Basic auth
        if (authorizationHeader!=null && authorizationHeader.startsWith("Basic")) {
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

}
