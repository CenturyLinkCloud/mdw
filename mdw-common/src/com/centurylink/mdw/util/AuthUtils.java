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
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.auth.LdapAuthenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.cache.impl.AppTokenCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class AuthUtils {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String AUTHORIZATION_HEADER_AUTHENTICATION = "Authorization";
    public static final String GIT_HUB_SECRET_KEY = "GitHub";
    public static final String SLACK_TOKEN = "MDW_SLACK_TOKEN";
    public static final String OAUTH_AUTHENTICATION = "OAuth";
    public static final String MDW_APP_TOKEN = "MDW_APP_TOKEN";
    public static final String MDW_AUTH_TOKEN = "MDW_Auth";

    private static JWTVerifier verifier = null;

    public static boolean authenticate(String authMethod, Map<String,String> headers) {
        return authenticate(authMethod, headers, null);
    }

    public static boolean authenticate(String authMethod, Map<String,String> headers, String payload) {
        // avoid any fishiness -- only we should populate this header
        headers.remove(Listener.AUTHENTICATED_USER_HEADER);
        if (authMethod.equals(AUTHORIZATION_HEADER_AUTHENTICATION)) {
            return authenticateAuthorizationHeader(headers);
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
     * specifically for Basic Authentication or MDW-JWT Authentication.
     * In the future, probably change this.
     * </p>
     * <p>
     * TODO: call this from every protocol channel
     * If nothing else we should to this to avoid spoofing the AUTHENTICATED_USER_HEADER.
     * </p>
     * @param headers
     * @return
     */
    private static boolean authenticateAuthorizationHeader(Map<String,String> headers) {
        String hdr = headers.get(Listener.AUTHORIZATION_HEADER_NAME);
        if (hdr == null)
            hdr = headers.get(Listener.AUTHORIZATION_HEADER_NAME.toLowerCase());

        headers.remove(Listener.AUTHORIZATION_HEADER_NAME);
        headers.remove(Listener.AUTHORIZATION_HEADER_NAME.toLowerCase());

        if (hdr != null && hdr.startsWith("Basic"))
            return checkBasicAuthenticationHeader(hdr, headers);
        else if (hdr != null && hdr.startsWith("MDW-JWT"))
            return checkMdwAuthenticationHeader(hdr, headers);

        return false;
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
        // If routing is not enabled, do not authenticate this way
        if (!PropertyManager.getBooleanProperty(PropertyNames.MDW_ROUTING_REQUESTS_ENABLED, false))
            return false;

        // If appId and Token were not provided in header, do not authenticate this way
        if (headers.get(Listener.METAINFO_MDW_APP_ID) == null || headers.get(Listener.METAINFO_MDW_APP_TOKEN) == null)
            return false;

        String appId = headers.get(Listener.METAINFO_MDW_APP_ID);
        String providedToken = headers.get(Listener.METAINFO_MDW_APP_TOKEN);
        String realToken = AppTokenCache.getAppToken(appId);

        // If the provided token doesn't match real token for specified appId, fail authentication
        if (providedToken == null || !providedToken.equals(realToken)) {
            logger.debug("Routing request failed authentication using MDW Application Token for " + appId);
            return false;
        }

        logger.debug("Routing request authenticated using MDW Application Token for " + appId);
        headers.put(Listener.AUTHENTICATED_USER_HEADER, "mdwapp"); // TODO: honor serviceUser in access.yaml
        headers.remove(Listener.METAINFO_MDW_APP_TOKEN);

        return true;
    }

    private static boolean checkMdwAuthenticationHeader(String authHeader, Map<String,String> headers) {
        String user = "Unknown";
        try {
        // Do NOT try to authenticate if it's not MDW auth
        if (authHeader == null || !authHeader.startsWith("MDW-JWT"))
            throw new Exception("Invalid MDW Auth Header");  // This should never happen

        authHeader = authHeader.replaceFirst("MDW-JWT ", "");

        String[] creds = authHeader.split("/");

        if (creds.length < 2)
            throw new Exception("Invalid MDW Auth Header");

        user = creds[0];
        String token = creds[1];

        // If first call, generate verifier
        JWTVerifier tempVerifier = verifier;
        if (tempVerifier == null)
            tempVerifier = createMdwTokenVerifier();

        if (tempVerifier == null)
            throw new Exception("Cannot generate JWT verifier");

            // TODO: Consider adding more verifications if we decide to sign token with additional claims/headers and not checked in createMdwTokenVerifier()
            DecodedJWT jwt = tempVerifier.verify(token);

            // Verify token is for same user as specified in Authorization header
            String tokenUser = jwt.getHeaderClaim("user").asString();
            if (tokenUser != null && tokenUser.equals(user))
                headers.put(Listener.AUTHENTICATED_USER_HEADER, user);
            else
                throw new Exception("Received valid JWT token, but cannot validate the user");
        }
        catch (Throwable ex) {
            headers.put(Listener.AUTHENTICATION_FAILED, "Authentication failed for '" + user + "' " + ex.getMessage());
            logger.severeException("Authentication failed for user '"+user+"'" + ex.getMessage(), ex);
            return false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("authentication successful for user '"+user+"'");
        }
        return true;
    }

    /**
     * @return true if no authentication at all or authentication is successful
     */
    private static boolean checkBasicAuthenticationHeader(String authorizationHeader, Map<String,String> headers) {
        String user = "Unknown";
        try {
            // Do NOT try to authenticate if it's not Basic auth
            if (authorizationHeader  == null || !authorizationHeader.startsWith("Basic"))
                throw new Exception("Invalid Basic Auth Header");  // This should never happen

            authorizationHeader = authorizationHeader.replaceFirst("Basic ", "");

            byte[] valueDecoded= Base64.decodeBase64(authorizationHeader.getBytes());
            authorizationHeader = new String(valueDecoded);

            String[] creds = authorizationHeader.split(":");

            if (creds.length < 2)
                throw new Exception("Invalid Basic Auth Header");

            user = creds[0];
            String pass = creds[1];

            if (ApplicationContext.getAuthMethod().equals("mdw")) {
                // TODO - Authenticate using com/centurylink/mdw/central/auth service hosted in MDW Central
            }
            else {
                ldapAuthenticate(user, pass);
            }

            headers.put(Listener.AUTHENTICATED_USER_HEADER, user);
            if (logger.isDebugEnabled()) {
                logger.debug("authentication successful for user '"+user+"'");
            }
        }
        catch (Exception ex) {
            headers.put(Listener.AUTHENTICATION_FAILED, "Authentication failed for '"+user+"' "+ex.getMessage());
            logger.severeException("Authentication failed for user '"+user+"'" + ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    public static void ldapAuthenticate(String user, String password) throws MdwSecurityException {
        String ldapProtocol = PropertyManager.getProperty(PropertyNames.MDW_LDAP_PROTOCOL);
        if (ldapProtocol == null)
            ldapProtocol = PropertyManager.getProperty("LDAP/Protocol"); // compatibility
        if (ldapProtocol == null)
            ldapProtocol = "ldap";
        String ldapHost = PropertyManager.getProperty(PropertyNames.MDW_LDAP_HOST);
        if (ldapHost == null)
            ldapHost = PropertyManager.getProperty("LDAP/Host");
        String ldapPort = PropertyManager.getProperty(PropertyNames.MDW_LDAP_PORT);
        if (ldapPort == null)
            ldapPort = PropertyManager.getProperty("LDAP/Port");
        String ldapUrl = ldapProtocol + "://" + ldapHost + ":" + ldapPort;
        String baseDn = PropertyManager.getProperty(PropertyNames.MDW_LDAP_BASE_DN);
        if (baseDn == null)
            baseDn = PropertyManager.getProperty("LDAP/BaseDN");
        LdapAuthenticator auth = new LdapAuthenticator(ldapUrl, baseDn);
        auth.authenticate(user, password);
    }

    private static synchronized JWTVerifier createMdwTokenVerifier() {
        JWTVerifier tempVerifier = verifier;
        if (tempVerifier == null) {
            String appToken = System.getenv("MDW_APP_TOKEN");
            if (StringHelper.isEmpty(appToken))
                logger.severe("Exception processing incoming message using MDW Auth token - Missing System variable MDW_APP_TOKEN");
            else {
                try {
                    Algorithm algorithm = Algorithm.HMAC256(appToken);
                    // TODO: Add additional ".with*" clauses if we decide to add additional claims/headers
                    verifier = tempVerifier = JWT.require(algorithm)
                            .withIssuer("mdwAuth")
                            .build(); //Reusable verifier instance
                }
                catch (IllegalArgumentException | UnsupportedEncodingException e) {
                    logger.severeException("Exception processing incoming message using MDW Auth token", e);
                }
            }
        }
        return tempVerifier;
    }
}
