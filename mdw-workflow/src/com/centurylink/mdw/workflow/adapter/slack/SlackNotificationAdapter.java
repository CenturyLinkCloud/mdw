/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.workflow.adapter.slack;


import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.workflow.adapter.rest.RestServiceAdapter;


/**
 * Slack Notification Adapter Activity
 * <p>
 * Posts an exception message onto a Slack channel using a webhook
 * </p>
 */
public class SlackNotificationAdapter extends RestServiceAdapter
{
    // User name to post with (this just appears in the message, so it's not a real user)
    public static String SLACK_USERNAME_ATTRIBUTE = "SLACK_USERNAME";
    // Prefix that identifies where the Slack message came from
    public static String SLACK_PREFIX = "sd-workflow : ";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /*
     * Make sure it's a JSON request
     */
    @Override
    public Map<String, String> getRequestHeaders() {
        Map<String,String> requestHeaders = super.getRequestHeaders();
        if (requestHeaders == null)
            requestHeaders = new HashMap<String,String>();
        requestHeaders.put("Content-Type", "application/json");
        return requestHeaders;
    }

    /*
     * <p>
     * Overriden to take the exception and build the Slack Request
     * Basic for starters, just takes the toString from the Activity exception
     * Can be overriden to provide a more sophisticated Slack message
     * </p>
     */
    @Override
    protected String getRequestData() throws ActivityException {
        String varname = getAttributeValue(REQUEST_VARIABLE);
        if (StringHelper.isEmpty(varname)) {
            throw new ActivityException("Variable not found for 'exception' for SlackNotificationAdapter");
        }
        Exception exception = (Exception) getVariableValue(varname);
        if (exception == null) {
            throw new ActivityException("Exception variable not defined for SlackNotificationAdapter");

        }
        String message = null;
        if (!(exception instanceof ActivityException)) {
            throw new ActivityException("Exception variable not an instance of ActivityException for SlackNotificationAdapter");
        } else {
            ActivityException actException = (ActivityException)exception;
            message = actException.toString();
        }

        try {
            return buildSlackRequest(message, null);
        }
        catch (PropertyException e) {
            throw new ActivityException("Unable to build message for SlackNotificationAdapter",e);
        }
    }
    /**
     *
     * @param message message for JSON String
     * @param link
     * @return JSON string to send to Slack
     * @throws PropertyException
     */
    public String buildSlackRequest(String message, String link) throws PropertyException {
        JSONObject slackRequest = new JSONObject();
        String env = ApplicationContext.getRuntimeEnvironment().toUpperCase();
        String slackUser = getAttributeValueSmart(SLACK_USERNAME_ATTRIBUTE);
        try {
            slackRequest.put("username", slackUser );
            slackRequest.put("icon_emoji", ":loudspeaker:");
            slackRequest.put("text", env+ " - "+SLACK_PREFIX+message);
        }
        catch (JSONException e) {
            logger.severe("Unable to build Slack Channel Request : message "+message+" link "+link+ " error: "+e.getMessage() );
        }
        return slackRequest.toString();

    }

    /*
     * Only do the call if the webhookurl and user are defined
     */
    @Override
    public String invoke(Object conn, String request, int timeout, Map<String, String> headers)
            throws ConnectionException, AdapterException {
        try {
            if (!StringHelper.isEmpty(getEndpointUri()) && !StringHelper.isEmpty(getAttributeValueSmart(SLACK_USERNAME_ATTRIBUTE))) {
                return super.invoke(conn, request, timeout, headers);
            }
            else {
                logger.debug("Slack Notification is disabled (endpoint uri and user are not defined, continuing with flow");
            }
        }
        catch (PropertyException e) {
            // Catch this quietly, log it and just return
            // If the property doesn't exist then either it's supposed
            // to quietly continue or it shouldn't matter
            logger.severe("Slack Notification unable to get properties : "+e.getMessage());
        }
        return "Ok";

    }


}
