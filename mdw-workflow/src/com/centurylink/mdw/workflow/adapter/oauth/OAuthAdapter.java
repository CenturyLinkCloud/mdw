/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter.oauth;

import java.util.HashMap;
import java.util.Map;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.auth.OAuthAuthenticator;
import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.utilities.ExpressionUtil;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.workflow.adapter.AdapterActivityBase;

@Tracked(LogLevel.TRACE)
public class OAuthAdapter extends AdapterActivityBase {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String OAUTH_TOKEN_LOCATION = "OAuthTokenLocation";
    public static final String OAUTH_USER = "OAuthUser";
    public static final String OAUTH_ALLUSERS_LOCATION = "OAuthAllUsersLocation";
    public static final String OAUTH_USERS_LOCATION = "OAuthUsersLocation";
    public static final String APP_CUID = "AppCuid";
    public static final String APP_PASSWORD = "AppPassword";
    public static final String BINDINGS = "Bindings";
    private String oauthResults;

    @Override
    public final boolean isSynchronous() {
        return true;
    }

    /**
     * Returns an OAuth accessToken based on the configured token host
     */
    @Override
    protected Object openConnection() throws ConnectionException {
        try {
            String tokenHost = getAttributeValueSmart(OAUTH_TOKEN_LOCATION);
            String appCuid = getAttributeValueSmart(APP_CUID);
            String appPassword = getAttributeValueSmart(APP_PASSWORD);

            // Authenticate
            OAuthAuthenticator auth = new OAuthAuthenticator(tokenHost);
            String accessToken = auth.doAuthentication(appCuid, appPassword);
            return accessToken;

        }
        catch (Exception ex) {
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        }
    }

    @Override
    protected void closeConnection(Object connection) {
        // TBD
    }

    /**
     * Sends the request to the OAuth endpoint.
     */
    @Override
    public Object invoke(Object accessToken, Object userData) throws AdapterException {
        try {
            // Version 2.42 of AppFog doesn't support full /user (need the guid)
            // so we first have to get all the users, to get the guid
            OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(
                    getAttributeValueSmart(OAUTH_ALLUSERS_LOCATION)).setAccessToken(
                    (String) accessToken).buildHeaderMessage();
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

            OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest,
                    OAuth.HttpMethod.GET, OAuthResourceResponse.class);
            if (logger.isDebugEnabled()) {
                logger.debug("OAuthAdapter...got resourceResponse for all users =" + resourceResponse);
            }
            Map<String, String> users = new HashMap<String, String>();
            if (resourceResponse != null && resourceResponse.getBody() != null) {
                JSONObject json = new JSONObject(resourceResponse.getBody());
                if (logger.isDebugEnabled()) {
                    logger.debug("OAuthAdapter - Number of users :" + json.getString("total_results"));
                }
                JSONArray resources = json.getJSONArray("resources");
                for (int i = 0; i < resources.length(); i++) {
                    JSONObject jsonObj = resources.getJSONObject(i);
                    JSONObject metadata = jsonObj.getJSONObject("metadata");
                    String guid = metadata.getString("guid");
                    JSONObject entity = jsonObj.getJSONObject("entity");
                    users.put(entity.getString("username"), guid);
                }
            }
            /**
             * Now access details about the particular user
             */
            String wantedUser = getAttributeValueSmart(OAUTH_USER);
            StringBuffer userUrl = new StringBuffer(getAttributeValueSmart(OAUTH_USERS_LOCATION));
            if (!userUrl.toString().endsWith("/")) {
                userUrl.append("/");
            }

            bearerClientRequest = new OAuthBearerClientRequest(
                    userUrl + users.get(wantedUser))
                    .setAccessToken((String) accessToken).buildHeaderMessage();
            oAuthClient = new OAuthClient(new URLConnectionClient());

            resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET,
                    OAuthResourceResponse.class);
            if (logger.isDebugEnabled()) {
                logger.debug("OAuthAdapter...user response user"+wantedUser+" = " + resourceResponse);
            }
            return resourceResponse.getBody();
        }
        catch (Exception ex) {
            throw new AdapterException(-1, ex.getMessage(), ex);
        }
    }

    /**
     * Replaces expressions in an attribute value. This is used instead of the
     * logic in getAttributeValueSmart() since the Mbeng parser does not like
     * symbols like '=' inside the attribute.
     *
     * @param input
     *            raw attribute value
     * @return value with expressions substituted
     */
    protected String substitute(String input) throws ActivityException {

        try {
            return ExpressionUtil.substitute(input, getParameters());
        }
        catch (MDWException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    @Override
    protected Map<String, Object> getPostScriptBindings(Object response) throws ActivityException {
        Map<String, Object> bindings = super.getPostScriptBindings(response);
        if (oauthResults != null)
            bindings.put("results", oauthResults);
        return bindings;
    }

    @Override
    protected void handleAdapterSuccess(Object response) throws ActivityException, AdapterException {

        String results = (String) response;
        Map<String, String> bindings = StringHelper.parseMap(getAttributeValue(BINDINGS));
        oauthResults = new String();

        try {
            for (String varName : bindings.keySet()) {

                setVariableValue(varName, results);

            }
        }
        catch (ActivityException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

}
