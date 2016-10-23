/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.auth;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.util.HttpHelper;

public class OAuthRestAuthenticator implements Authenticator {

    private String endpoint;

    @Override
    public void authenticate(String user, String password) throws MdwSecurityException {

        try {
            endpoint = PropertyManager.getProperty(PropertyNames.MDW_OAUTH_REST_ENDPOINT);
            HttpHelper helper = new HttpHelper(new URL(endpoint));
            Map<String,String> headers = new HashMap<String,String>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Accept", "application/json");
            String headersProp = PropertyManager.getProperty(PropertyNames.MDW_OAUTH_REST_HEADERS);
            if (headersProp != null) {
                for (String header : headersProp.split(",")) {
                    int colon = headersProp.indexOf(':');
                    if (colon <= 0)
                        throw new IllegalArgumentException("Invalid " + PropertyNames.MDW_OAUTH_REST_HEADERS + " (missing ':')");
                    headers.put(header.substring(0, colon), header.substring(colon + 1));
                }
            }
            helper.setHeaders(headers);
            String userDomain = PropertyManager.getProperty(PropertyNames.MDW_OAUTH_REST_USER_DOMAIN);
            if (userDomain != null)
                user = user + "@" + userDomain;
            String body = "username=" + URLEncoder.encode(user, "UTF-8")
                + "&password=" + URLEncoder.encode(password, "UTF-8") + "&grant_type=password";

            String response = helper.post(body);
            JSONObject json = new JSONObject(response);
            if (json.has("status")) {
                String msg = json.getString("status");
                if (json.has("response_message"))
                    msg += ": " + json.getString("response_message");
                throw new MdwSecurityException(msg);
            }
            else {
                accessToken = new OAuthAccessToken(json, true);
            }
        }
        catch (MdwSecurityException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new MdwSecurityException(ex.getMessage(), ex);
        }
    }

    public String getKey() {
        return endpoint;
    }

    private OAuthAccessToken accessToken;
    public OAuthAccessToken getAccessToken() { return accessToken; }

}
