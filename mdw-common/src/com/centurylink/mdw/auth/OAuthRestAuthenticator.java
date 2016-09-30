/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.auth;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.property.PropertyManager;

public class OAuthRestAuthenticator implements Authenticator {

    @Override
    public void authenticate(String user, String password) throws MdwSecurityException {

        try {
            String endpoint = PropertyManager.getProperty(PropertyNames.MDW_OAUTH_REST_ENDPOINT);
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

//            {
//                "access_token": "c6bbe1b08be99759ee0e6477fe15a9ae977bb01850892da64b8fec33083e315e",
//                "token_type": "bearer",
//                "expires_in": 7200,
//                "refresh_token": "5e24ec73e02b77cc5ccd33a78e45563557c909ddf3de59570292b7e9fefc5517",
//                "account_name": "servicedelivery"
//            }

        }
        catch (Exception ex) {

        }

        // TODO Auto-generated method stub

    }

    @Override
    public String getKey() {
        // TODO Auto-generated method stub
        return null;
    }

}
