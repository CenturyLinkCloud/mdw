/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.constant;

/**
 * System properties trump environment variables.
 */
public class AuthConstants {

    // For OAuth authentication
    public static final String OAUTH_TOKEN_LOCATION_SYS_PROP = "mdw.oauth.token.location";
    public static final String OAUTH_TOKEN_LOCATION_ENV_VAR = "MDW_OAUTH_TOKEN_LOCATION";
    public static String getOAuthTokenLocation() {
        String tokenLoc = System.getProperty(OAUTH_TOKEN_LOCATION_SYS_PROP);
        if (tokenLoc == null)
            tokenLoc = System.getenv(OAUTH_TOKEN_LOCATION_ENV_VAR);
        return tokenLoc;
    }
    // For OAuth session token
    public static final String OAUTH_TOKEN_SESSION = "mdw_oauth_token";

    public static final String OAUTH_CLIENT_ID_SYS_PROP = "mdw.oauth.client.id";
    public static final String OAUTH_CLIENT_ID_ENV_VAR = "MDW_OAUTH_CLIENT_ID";
    public static final String OAUTH_DEFAULT_CLIENT_ID = "mdw";
    public static String getOAuthClientId() {
        String clientId = System.getProperty(OAUTH_CLIENT_ID_SYS_PROP);
        if (clientId == null)
            clientId = System.getenv(OAUTH_CLIENT_ID_ENV_VAR);
        return clientId == null ? OAUTH_DEFAULT_CLIENT_ID : clientId;
    }

    public static final String OAUTH_CLIENT_SECRET_SYS_PROP = "mdw.oauth.client.secret";
    public static final String OAUTH_CLIENT_SECRET_ENV_VAR = "MDW_OAUTH_CLIENT_SECRET";
    public static final String OAUTH_DEFAULT_CLIENT_SECRET = "mdwsecret";
    public static String getOAuthClientSecret() {
        String clientSecret = System.getProperty(OAUTH_CLIENT_SECRET_SYS_PROP);
        if (clientSecret == null)
            clientSecret = System.getenv(OAUTH_CLIENT_SECRET_ENV_VAR);
        return clientSecret == null ? OAUTH_DEFAULT_CLIENT_SECRET : clientSecret;
    }

    public static final String ALLOW_ANY_AUTHENTICATED_USER_SYS_PROP = "mdw.allow.any.authenticated.user";
    public static final String ALLOW_ANY_AUTHENTICATED_USER_ENV_VAR = "MDW_ALLOW_ANY_AUTHENTICATED_USER";
    public static boolean isAllowAnyAuthenticatedUser() {
        String allowAny = System.getProperty(ALLOW_ANY_AUTHENTICATED_USER_SYS_PROP);
        if (allowAny == null)
            allowAny = System.getenv(ALLOW_ANY_AUTHENTICATED_USER_ENV_VAR);
        return "true".equalsIgnoreCase(allowAny);
    }

    public static final String AUTH_EXCLUSION_PATTERNS_SYS_PROP = "mdw.auth.exclusion.patterns";
    public static final String AUTH_EXCLUSION_PATTERNS_ENV_VAR = "MDW_AUTH_EXCLUSION_PATTERNS";
    public static final String DEFAULT_AUTH_EXCLUSION_PATTERNS = "*.js,*.css,*.jpg,*.gif,*.png,*.ico,/index.html,/index.xhtml,/error.jsf," +
            "/confirm.jsf,/offline.jsf,/sysInfo.jsf,/authentication/login.jsf,/authentication/accessDenied.jsf,/authentication/loginError.jsf," +
            "/authentication/logout.jsf,/login,/loginError,/tasks/taskAction.jsf,/AttachmentDownload,/doc/*,/javadoc/*,/download," +
            "/imageServlet,/imageServlet/*,/resources,/resources/*,/configManager,/configManager/*,/system/sysInfoPlain.jsf,/index.jsf," +
            "/system/summary.jsf,/system/filepanel,/system/filepanel/*,/system/property/*,/system/faces/*,/Services,/Services/*,/SOAP,/SOAP/*," +
            "/RCP,/RCP/*,/MDWWebService";
    public static String getAuthExclusionPatterns() {
        String authExclusionPatterns = System.getProperty(AUTH_EXCLUSION_PATTERNS_SYS_PROP);
        if (authExclusionPatterns == null)
            authExclusionPatterns = System.getenv(AUTH_EXCLUSION_PATTERNS_ENV_VAR);
        return authExclusionPatterns == null ? DEFAULT_AUTH_EXCLUSION_PATTERNS : authExclusionPatterns;
    }

    public static final String MDW_LDAP_AUTH_SYS_PROP = "mdw.ldap.auth";
    public static boolean isMdwLdapAuth() {
        String mdwLdapAuth = System.getProperty(MDW_LDAP_AUTH_SYS_PROP);
        return "true".equalsIgnoreCase(mdwLdapAuth);
    }

}
