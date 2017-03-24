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

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;

import com.centurylink.mdw.constant.AuthConstants;
import com.centurylink.mdw.constant.PaaSConstants;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * <p>
 * Authenticates using OAuth - normally used in the PaaS space
 * <br/>
 * For an example of how to use this
 * @see com.centurylink.mdw.workflow.adapter.oauth.OAuthAdapter
 *
 * <b>
 * Note that, despite the fact that this has similar functionality
 * to LdapAdapter, the info coming back is different in the PaaS space
 * </p>
 *
 * @author aa70413
 *
 */
public class OAuthAuthenticator implements Authenticator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String MDW_OAUTH_CLIENT_ID = "mdw.oauth.client.id";
    public static final String MDW_OAUTH_CLIENT_SECRET = "mdw.oauth.client.secret";

    private String tokenLocation;
    private String clientId;
    private String clientSecret;

    public OAuthAuthenticator() {
        this(AuthConstants.getOAuthTokenLocation());
    }

    public OAuthAuthenticator(String tokenLocation) {
        this(tokenLocation, AuthConstants.getOAuthClientId(), AuthConstants.getOAuthClientSecret());
    }

    public OAuthAuthenticator(String tokenLocation, String clientId, String clientSecret) {
        this.tokenLocation = tokenLocation;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Supports existing api with just cuid and pass
     * @param cuid
     * @param pass
     */
    public void authenticate(String cuid, String pass) throws MdwSecurityException {
        doAuthentication(cuid, pass);
    }

    /**
     * <p>
     * Takes a cuid and pass combination and authenticates against OAuth.
     *
     * TODO Read the token location from the env rather than pass it in
     * </p>
     *
     * @see PaaSConstants for client id and secret
     * @param cuid
     * @param pass
     * @return the OAuth access token
     */
    public String doAuthentication(String cuid, String pass) throws MdwSecurityException {
        String accessToken = null;
        try {
            if (StringHelper.isEmpty(tokenLocation)) {
                throw new OAuthSystemException("Token location is empty, should point to an OAuth token location endpoint");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("OAuthAuthenticator...authenticating to " + tokenLocation
                        + " clientID " + clientId + " clientsecret " + clientSecret + " user "
                        + cuid);
            }
            OAuthClientRequest request = OAuthClientRequest.tokenLocation(tokenLocation)
                    .setGrantType(GrantType.PASSWORD).setClientId(clientId)
                    .setClientSecret(clientSecret).setUsername(cuid).setPassword(pass)
                    .buildBodyMessage();
            if (logger.isDebugEnabled()) {
                logger.debug("OAuthAuthenticator...got request " + request);
            }
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

            OAuthClientResponse response = oAuthClient.accessToken(request);

            if (response != null) {
                accessToken = response.getParam("access_token");
                if (logger.isDebugEnabled()) {
                    logger.debug("OAuthAuthenticator...got response =" + response);
                }
                logger.debug("OAuthAuthenticator...response access token "
                        + response.getParam("access_token"));
            }
            else {
                logger.info("OAuthAuthenticator...response is null");
            }
        }
        catch (OAuthSystemException ex) {
            String msg = "Unable to authenticate user " + cuid + " with OAuth";
            throw new MdwSecurityException(msg, ex);
        }
        catch (OAuthProblemException ex) {
            String msg = "Unable to authenticate user " + cuid + " with OAuth";
            throw new AuthenticationException(msg, ex);
        }
        return accessToken;
    }

    public String getKey() {
        return tokenLocation + "_" + clientId;
    }

    public static void main(String[] args) {
        if (args.length != 2)
            throw new RuntimeException("args: <user> <password>");

        Authenticator auth = new OAuthAuthenticator("http://clc-login.useast.appfog.ctl.io/oauth/token");
        try {
            auth.authenticate(args[0], args[1]);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
