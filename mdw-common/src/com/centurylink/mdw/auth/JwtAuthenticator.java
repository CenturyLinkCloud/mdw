/*
 * Copyright (C) 2018 CenturyLink, Inc.
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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * <p>
 * Authenticates using JWT - normally used in the PaaS space
 * <br/>
 * </p>
 *
 * @author aa56486
 *
 */
public class JwtAuthenticator implements Authenticator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static String tokenLocation;
    private static String appId;

    public static String getAppId() {
        if (appId == null)
            appId = ApplicationContext.getAppId();
        return appId;
    }

    public static void setAppId(String appId) {
        JwtAuthenticator.appId = appId;
    }

    public static String getJwtTokenLocation() {
        if (tokenLocation == null) {
            String tokenLoc = ApplicationContext.getMdwAuthUrl();
            if (tokenLoc == null)
                tokenLoc = System.getenv(PropertyNames.MDW_CENTRAL_AUTH_URL);
            tokenLocation = tokenLoc;
        }
        return tokenLocation;
    }

    public static final void setJwtTokenLoc(String tokenLoc) {
        tokenLocation = tokenLoc;
    }

    public JwtAuthenticator() {
        this(getJwtTokenLocation());
    }

    public JwtAuthenticator(String tokenLoc) {
        tokenLocation = tokenLoc;
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
     * Takes a cuid and pass combination and authenticates against JWT.
     * </p>
     * @param cuid
     * @param pass
     * @return the JWT access token
     */
    public String doAuthentication(String cuid, String pass) throws MdwSecurityException {
        String accessToken = null;
        try {
            getJwtTokenLocation();
            if (StringHelper.isEmpty(tokenLocation)) {
                throw new MdwSecurityException("Token location is empty, should point to an JWT token location endpoint."
                        + " Unable to authenticate user " + cuid + " with JWT");
            }

            JSONObject json = new JSONObject();
            json.put("user", cuid);
            json.put("password", pass);
            json.put("appId", appId);

            if (logger.isDebugEnabled())
                logger.debug("JwtAuthenticator...authenticating to " + tokenLocation +  " user " + cuid + " appid " +  ApplicationContext.getAppId());

            try {
                HttpHelper helper = new HttpHelper(new URL(tokenLocation));
                Map<String,String> hdrs = new HashMap<>();
                hdrs.put("Content-Type", "application/json; charset=utf-8");
                helper.setHeaders(hdrs);
                String response = helper.post(json.toString());
                JSONObject responseJson = new JSONObject(response);
                accessToken = responseJson.getString("mdwauth");
                if (accessToken == null || accessToken.isEmpty())
                    throw new IOException("User authentication failed with response:" + responseJson);
            }
            catch (IOException ex) {
                throw new ServiceException(ex.getMessage(), ex);
            }
        }
        catch (Exception ex) {
            String msg = "Unable to authenticate user " + cuid + " with JWT";
            throw new AuthenticationException(msg, ex);
        }
        return accessToken;
    }

    public String getKey() {
        return tokenLocation + "_" + ApplicationContext.getAppId();
    }

    public static void main(String[] args) {
        if (args.length != 2)
            throw new RuntimeException("args: <user> <password>");
        JwtAuthenticator.setAppId("mdw6");
        Authenticator auth = new JwtAuthenticator("https://mdw.useast.appfog.ctl.io/mdw/services/com/centurylink/mdw/central/auth");
        try {
            auth.authenticate(args[0], args[1]);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
