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
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Authenticates using MDW auth via JSON Web Tokens.
 */
public class MdwAuthenticator implements Authenticator {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();
    private String appId;

    public MdwAuthenticator(String appId) {
        this.appId = appId;
    }

    public void authenticate(String user, String pass) throws MdwSecurityException {
        doAuthentication(user, pass);
    }

    public String doAuthentication(String user, String pass) throws MdwSecurityException {
        String accessToken = null;
        String authUrl = ApplicationContext.getMdwAuthUrl();

        try {
            String appToken = System.getenv("MDW_APP_TOKEN");
            if (appToken == null)
                throw new IOException("Missing environment variable: MDW_APP_TOKEN");

            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("Authenticating " + user + " for " + appId + " via " + authUrl);

            JSONObject json = new JSONObject();
            json.put("user", user);
            json.put("password", pass);
            json.put("appId", appId);

            HttpHelper helper = new HttpHelper(new URL(authUrl));
            Map<String,String> hdrs = new HashMap<>();
            hdrs.put("Content-Type", "application/json; charset=utf-8");
            hdrs.put("mdw-app-token", appToken);
            helper.setHeaders(hdrs);
            String response = helper.post(json.toString());
            if (helper.getResponseCode() != 200)
                throw new IOException("User authentication failed with response code:" + helper.getResponseCode());
            JSONObject responseJson = new JSONObject(response);
            accessToken = responseJson.getString("mdwauth");
            if (accessToken == null || accessToken.isEmpty())
                throw new IOException("User authentication failed with response:" + responseJson);
        }
        catch (IOException ex) {
            logger.severeException("Failed to authenticate " + user + " for " + appId + " via " + authUrl, ex);
            throw new AuthenticationException("Authentication failure");
        }
        return accessToken;
    }

    public String getKey() {
        return appId;
    }
}
