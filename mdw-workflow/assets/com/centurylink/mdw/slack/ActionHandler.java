package com.centurylink.mdw.slack;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;

/**
 * Interface for handling incoming slack action/options requests:
 * https://api.slack.com/interactive-messages
 */
@FunctionalInterface
public interface ActionHandler {
    JSONObject handleRequest(String userId, String id, SlackRequest request)
            throws ServiceException;
}
