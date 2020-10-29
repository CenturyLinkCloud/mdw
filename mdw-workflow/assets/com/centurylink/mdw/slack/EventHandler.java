package com.centurylink.mdw.slack;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;

/**
 * Interface for handling incoming slack events api messages:
 * https://api.slack.com/events-api
 */
@FunctionalInterface
public interface EventHandler {
    JSONObject handleEvent(SlackEvent event)
            throws ServiceException;
}
