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
package com.centurylink.mdw.services.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.auth.Authenticator;
import com.centurylink.mdw.auth.LdapAuthenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.HmacSha1Signature;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class AuthUtils {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String AUTHORIZATION_HEADER_AUTHENTICATION = "Authorization";
    public static final String GIT_HUB_SECRET_KEY = "GitHub";
    public static final String SLACK_TOKEN = "MDW_SLACK_TOKEN";
    public static final String MDW_APP_TOKEN = "MDW_APP_TOKEN";
    public static final String MDW_AUTH = "mdwAuth";
    public static final String MDW_JWT_CUSTOM_KEY = "MDW_JWT_CUSTOM_KEY";

    private static final String APPTOKENCACHE = "com.centurylink.mdw.central.AppCache";

    private static final String JWTTOKENCACHE = "com.centurylink.mdw.authCTL.JwtTokenCache";
    private static final String CTLJWTPKG = "com.centurylink.mdw.authCTL";
    private static final String CTLJWTAUTH = "com.centurylink.mdw.authCTL.MdwAuthenticatorCTL";

    private static JWTVerifier verifier = null;
    private static JWTVerifier verifierCustom = null;
    private static long maxAge = 0;

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
        else if (hdr != null && hdr.startsWith("Bearer"))
            return checkBearerAuthenticationHeader(hdr, headers);

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
        String realToken = "";
        CacheService appTokenCacheInstance = CacheRegistration.getInstance().getCache(APPTOKENCACHE);
        try {
            Method compiledAssetGetter = appTokenCacheInstance.getClass().getMethod("getAppToken", String.class);
            realToken = (String)compiledAssetGetter.invoke(appTokenCacheInstance, appId);
        }
        catch (Exception ex) {
            logger.severeException("Exception trying to retreieve App token from cache", ex);
        }

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

    private static boolean checkBearerAuthenticationHeader(String authHeader, Map<String,String> headers) {
        try {
            // Do NOT try to authenticate if it's not Bearer
            if (authHeader == null || !authHeader.startsWith("Bearer"))
                throw new Exception("Invalid MDW Auth Header");  // This should never happen

            authHeader = authHeader.replaceFirst("Bearer ", "");
            DecodedJWT jwt = JWT.decode(authHeader);  // Validate it is a JWT and see which kind of JWT it is

            if (MDW_AUTH.equals(jwt.getIssuer()))  // JWT was issued by MDW Central
                verifyMdwJWT(authHeader, headers);
            else if (!StringHelper.isEmpty(PropertyManager.getProperty(PropertyNames.MDW_JWT_CUSTOM_ISSUER)) &&
                     !StringHelper.isEmpty(PropertyManager.getProperty(PropertyNames.MDW_JWT_CUSTOM_USER_CLAIM)) &&
                     PropertyManager.getProperty(PropertyNames.MDW_JWT_CUSTOM_ISSUER).equals(jwt.getIssuer()))  // Support for other issuers of JWTs
                verifyCustomJWT(authHeader, jwt.getAlgorithm(), headers);
            else
                throw new Exception("Invalid JWT Issuer");
        }
        catch (Throwable ex) {
            if (!ApplicationContext.isDevelopment()) {
                headers.put(Listener.AUTHENTICATION_FAILED, "Authentication failed for JWT '" + authHeader + "' " + ex.getMessage());
                logger.severeException("Authentication failed for JWT '"+authHeader+"'" + ex.getMessage(), ex);
            }
            return false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("authentication successful for user '"+headers.get(Listener.AUTHENTICATED_USER_HEADER)+"'");
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

            if (ApplicationContext.isMdwAuth()) {
                if (PackageCache.getPackage(CTLJWTPKG) == null)
                    throw new Exception("Basic Auth is not allowed when authMethod is mdw");

                String token = null;
                CacheService jwtTokenCacheInstance = CacheRegistration.getInstance().getCache(JWTTOKENCACHE);
                try {
                    Method compiledAssetGetter = jwtTokenCacheInstance.getClass().getMethod("getToken", String.class, String.class);
                    token = (String)compiledAssetGetter.invoke(jwtTokenCacheInstance, user, pass);
                }
                catch (Exception ex) {
                    logger.severeException("Exception trying to retreieve App token from cache", ex);
                }
                boolean validated = false;
                if (!StringHelper.isEmpty(token)) { // Use token if this user was already validated
                    try {
                        // Use cached token
                        verifyMdwJWT(token, headers);
                        validated = true;
                    } catch (Exception e) {}  // Token might be expired or some other issue with it - re-authenticate
                }
                if (!validated) {
                    // Authenticate using com/centurylink/mdw/central/auth service hosted in MDW Central
                    com.centurylink.mdw.model.workflow.Package pkg = PackageCache.getPackage(CTLJWTPKG);
                    Authenticator jwtAuth = (Authenticator) CompiledJavaCache.getInstance(CTLJWTAUTH, pkg.getCloudClassLoader(), pkg);
                    jwtAuth.authenticate(user, pass);  // This will populate JwtTokenCache with token for next time
                }
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
            if (!ApplicationContext.isDevelopment()) {
                headers.put(Listener.AUTHENTICATION_FAILED, "Authentication failed for '"+user+"'. "+ex.getMessage());
                logger.severeException("Authentication failed for user '"+user+"'. " + ex.getMessage(), ex);
            }
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

    private static void verifyMdwJWT(String token, Map<String,String> headers) throws Exception {
        // If first call, generate verifier
        JWTVerifier tempVerifier = verifier;
        if (tempVerifier == null)
            tempVerifier = createMdwTokenVerifier();

        if (tempVerifier == null)
            throw new Exception("Cannot generate MDW JWT verifier");

        DecodedJWT jwt = tempVerifier.verify(token);  // Verifies JWT is valid

        // Verify token is not too old, if application specifies property for max token age - in seconds
        if (maxAge > 0 && jwt.getIssuedAt() != null) {
            if ((new Date().getTime() - jwt.getIssuedAt().getTime()) > maxAge)
                throw new Exception("JWT token has expired");
        }

        // Get the user JWT was created for
        if (!StringHelper.isEmpty(jwt.getSubject()))
            headers.put(Listener.AUTHENTICATED_USER_HEADER, jwt.getSubject());
        else
            throw new Exception("Received valid JWT token, but cannot identify the user");
    }

    private static synchronized JWTVerifier createMdwTokenVerifier() {
        JWTVerifier tempVerifier = verifier;
        if (tempVerifier == null) {
            String appToken = System.getenv(MDW_APP_TOKEN);
            if (StringHelper.isEmpty(appToken))
                logger.severe("Exception processing incoming message using MDW Auth token - Missing System environment variable " + MDW_APP_TOKEN);
            else {
                try {
                    maxAge = PropertyManager.getIntegerProperty(PropertyNames.MDW_AUTH_TOKEN_MAX_AGE, 0) * 1000L;  // MDW default is token never expires
                    Algorithm algorithm = Algorithm.HMAC256(appToken);
                    verifier = tempVerifier = JWT.require(algorithm)
                            .withIssuer(MDW_AUTH)
                            .withAudience(ApplicationContext.getAppId())
                            .build(); //Reusable verifier instance
                }
                catch (IllegalArgumentException | UnsupportedEncodingException e) {
                    logger.severeException("Exception processing incoming message using MDW Auth token", e);
                }
            }
        }
        return tempVerifier;
    }

    private static void verifyCustomJWT(String token, String algorithm, Map<String,String> headers) throws Exception {
        // If first call, generate verifier
        JWTVerifier tempVerifier = verifierCustom;
        if (tempVerifier == null)
            tempVerifier = createCustomTokenVerifier(algorithm);

        if (tempVerifier == null)
            throw new Exception("Cannot generate Custom JWT verifier");

        DecodedJWT jwt = tempVerifier.verify(token);  // Verifies JWT is valid

        // Verify token is not too old, if application specifies property for max token age - in seconds
        if (maxAge > 0 && jwt.getIssuedAt() != null) {
            if ((new Date().getTime() - jwt.getIssuedAt().getTime()) > maxAge)
                throw new Exception("Custom JWT token has expired");
        }

        // Get the user JWT was created for (Claim specified in Property) - Check payload and header for the claim
        String user = jwt.getClaim(PropertyManager.getProperty(PropertyNames.MDW_JWT_CUSTOM_USER_CLAIM)).asString();
        if (StringHelper.isEmpty(user))
            user = jwt.getHeaderClaim(PropertyManager.getProperty(PropertyNames.MDW_JWT_CUSTOM_USER_CLAIM)).asString();

        if (!StringHelper.isEmpty(user))
            headers.put(Listener.AUTHENTICATED_USER_HEADER, user);
        else
            throw new Exception("Received valid Custom JWT token, but cannot identify the user");
    }

    private static synchronized JWTVerifier createCustomTokenVerifier(String algorithmName) {
        JWTVerifier tempVerifier = verifierCustom;
        if (tempVerifier == null) {
            String propAlg = PropertyManager.getProperty(PropertyNames.MDW_JWT_CUSTOM_ALGORITHM);
            if (StringHelper.isEmpty(algorithmName) || (!StringHelper.isEmpty(propAlg) && !algorithmName.equals(propAlg))) {
                String message = "Exception creating Custom JWT Verifier - ";
                message = StringHelper.isEmpty(algorithmName) ? "Missing 'alg' claim in JWT" : ("Mismatch algorithm with specified Property " + PropertyNames.MDW_JWT_CUSTOM_ALGORITHM);
                logger.severe(message);
                return null;
            }
            String key = System.getenv(MDW_JWT_CUSTOM_KEY);
            if (StringHelper.isEmpty(key)) {
                if (!algorithmName.startsWith("HS")) {  // Only allow use of Key in MDW properties for asymmetric algorithms
                    key = PropertyManager.getProperty(PropertyNames.MDW_JWT_CUSTOM_KEY);
                    if (StringHelper.isEmpty(key)) {
                        logger.severe("Exception creating Custom JWT Verifier - Missing Property " + PropertyNames.MDW_JWT_CUSTOM_KEY);
                        return null;
                    }
                }
                else {
                    logger.severe("Exception creating Custom JWT Verifier - Missing System environment variable " + MDW_JWT_CUSTOM_KEY);
                    return null;
                }
            }

            try {
                maxAge = PropertyManager.getIntegerProperty(PropertyNames.MDW_AUTH_TOKEN_MAX_AGE, 0) * 1000L;

                Algorithm algorithm = null;
                Method algMethod = null;
                if (algorithmName.startsWith("HS")) {  // HMAC
                    String methodName = "HMAC" + algorithmName.substring(2);
                    algMethod = Algorithm.none().getClass().getMethod(methodName, String.class);
                    algorithm = (Algorithm)algMethod.invoke(Algorithm.none(), key);
                }
                else if (algorithmName.startsWith("RS")) {   // RSA
                    String methodName = "RSA" + algorithmName.substring(2);
                    byte[] publicBytes = Base64.decodeBase64(key.getBytes());
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    PublicKey pubKey = keyFactory.generatePublic(keySpec);
                    algMethod = Algorithm.none().getClass().getMethod(methodName, RSAPublicKey.class, RSAPrivateKey.class);
                    algorithm = (Algorithm)algMethod.invoke(Algorithm.none(), pubKey, null);
                }
                else {
                    logger.severe("Exception creating Custom JWT Verifier - Unsupported Algorithm: " + algorithmName);
                    return null;
                }

                String issuer = PropertyManager.getProperty(PropertyNames.MDW_JWT_CUSTOM_ISSUER);
                String subject = PropertyManager.getProperty(PropertyNames.MDW_JWT_CUSTOM_SUBJECT);

                Verification tmp = JWT.require(algorithm);
                tmp = StringHelper.isEmpty(issuer) ? tmp : tmp.withIssuer(issuer);
                tmp = StringHelper.isEmpty(subject) ? tmp : tmp.withSubject(subject);
                verifierCustom = tempVerifier = tmp.build();
            }
            catch (IllegalArgumentException | NoSuchAlgorithmException | NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException | InvalidKeySpecException e) {
                logger.severeException("Exception creating Custom JWT Verifier", e);
            }

        }
        return tempVerifier;
    }
}
