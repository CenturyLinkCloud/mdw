/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter.oauth;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.auth.OAuthAuthenticator;
import com.centurylink.mdw.common.MDWException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.util.ExpressionUtil;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.AdapterActivityBase;

@Tracked(LogLevel.TRACE)
public class OAuthServiceAdapter extends AdapterActivityBase {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    // OAuth specific
    public static final String OAUTH_TOKEN_LOCATION = "OAuthTokenLocation";
    public static final String OAUTH_CLIENT_ID = "OAuthClientId";
    public static final String OAUTH_CLIENT_SECRET = "OAuthClientSecret";
    public static final String OAUTH_USER = "OAuthUser";
    public static final String USER = "USER";
    public static final String PASSWORD = "PASS";
    public static final String BINDINGS = "Bindings";
    public static final String HTTP_METHOD = "HttpMethod";
    public static final String ENDPOINT_URI = "EndpointUri";  // includes the resource path

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
            String user = getAttributeValueSmart(USER);
            String pass = getAttributeValueSmart(PASSWORD);
            String clientId = getAttributeValueSmart(OAUTH_CLIENT_ID);
            String clientSecret = getAttributeValueSmart(OAUTH_CLIENT_SECRET);

            // Authenticate
            OAuthAuthenticator auth = new OAuthAuthenticator(tokenHost, clientId, clientSecret);
            String accessToken = auth.doAuthentication(user, pass);
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
        OAuthResourceResponse resourceResponse = null;
        try {
            String httpMethod = getHttpMethod();
            if ("GET".equals(httpMethod)) {
                OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(
                        getAttributeValueSmart(ENDPOINT_URI)).setAccessToken((String) accessToken)
                        .buildHeaderMessage();
                OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

                resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET,
                        OAuthResourceResponse.class);
                if (logger.isDebugEnabled()) {
                    logger.debug("OAuthAdapter...got resourceResponse =" + resourceResponse);
                }
            } else if ("POST".equals(httpMethod)) {
                OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(
                        getAttributeValueSmart(ENDPOINT_URI)).setAccessToken((String) accessToken)
                        .buildQueryMessage();
                bearerClientRequest.setBody(getRequestData().toString());
                OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

                resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.POST,
                        OAuthResourceResponse.class);
                if (logger.isDebugEnabled()) {
                    logger.debug("OAuthAdapter...got resourceResponse =" + resourceResponse);
                }
            }
            else
                throw new AdapterException("Unsupported HTTP Method: " + httpMethod);

            return resourceResponse.getBody();

        }
        catch (Exception ex) {
            int responseCode = -1;
            if (resourceResponse != null)
                responseCode = resourceResponse.getResponseCode();
            throw new AdapterException(responseCode, ex.getMessage() , ex);
        }


            /**
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
             *
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
            */
    }
    /**
     * The method overrides the one from the super class and does the following:
     * <ul>
     *   <li>For HTTP GET and DELETE requests, it returns an empty string</li>
     *   <li>Otherwise it gets the value of the variable with the name specified in the
     *      attribute REQUEST_VARIABLE. The value is typically an XML document or a string</li>
     *   <li>It invokes the variable translator to convert the value into a string
     *      and then returns the string value.</li>
     * </ul>
     * For HTTP methods other than GET and DELETE this will throw an exception if the
     * request data variable is not bound, or the value is not a DocumentReference or String.
     */
    @Override
    protected Object getRequestData() throws ActivityException {
        String httpMethod = getHttpMethod();
        if (!"GET".equals(httpMethod)) {

            Object request = super.getRequestData();
            if (request == null)
                throw new ActivityException("Request data attribute is missing for HTTP method: "
                        + httpMethod);

            if (request instanceof String)
                return request;
            else if (request instanceof DocumentReference)
                return getDocumentContent((DocumentReference) request);
            else
                throw new ActivityException("Cannot handle request of type "
                        + request.getClass().getName());
        }
        else {
            return null;
        }
    }

    protected String getHttpMethod() throws ActivityException {
        try {
            String httpMethod = getAttributeValueSmart(HTTP_METHOD);
            if (httpMethod == null)
                throw new ActivityException("OAuth adapter required attribute missing: " + HTTP_METHOD);
            return httpMethod;
        }
        catch (PropertyException ex) {
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }
    /**
     * Replaces expressions in an attribute value. This is used instead of the
     * logic in getAttributeValueSmart() since the Mbeng parser does not like
     * symbols like '=' inside the attribute.  // TODO: still needed
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


}
