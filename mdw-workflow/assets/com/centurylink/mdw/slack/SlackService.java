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
package com.centurylink.mdw.slack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.rest.JsonRestService;

/**
 * Slack integration API.
 */
@Path("/")
public class SlackService extends JsonRestService {

    /**
     * Responds to requests from Slack.
     */
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String sub = getSegment(path, 4);
        if ("event".equals(sub)) {
            SlackEvent event = new SlackEvent(content);
            EventHandler eventHandler = getEventHandler(event.getType(), event);
            if (eventHandler == null) {
                throw new ServiceException("Unsupported handler type: " + event.getType()
                        + (event.getChannel() == null ? "" : (":" + event.getChannel())));
            }
            return eventHandler.handleEvent(event);
        } else {
            // action/options request
            SlackRequest request = new SlackRequest(content);
            String userId = getAuthUser(headers);
            // TODO: ability to map request.getUser() to corresponding MDW user
            String callbackId = request.getCallbackId();
            if (callbackId != null) {
                int underscore = callbackId.lastIndexOf('_');
                if (underscore > 0) {
                    String handlerType = callbackId.substring(0, underscore);
                    String id = callbackId.substring(callbackId.lastIndexOf('/') + 1);
                    ActionHandler actionHandler = getActionHandler(handlerType, userId, id,
                            request);
                    if (actionHandler == null)
                        throw new ServiceException("Unsupported handler type: " + handlerType);

                    return actionHandler.handleRequest(userId, id, request);
                }
            }
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad or missing callback_id");
        }
    }

    EventHandler getEventHandler(String type, SlackEvent event) {
        if ("url_verification".equals(type)) {
            return evt -> {
                JSONObject obj = new JSONObject();
                obj.put("challenge", event.getChallenge());
                return obj;
            };
        } 
        else {
            String channel = event.getChannel();
            // TODO translate channel (requires auth): https://api.slack.com/methods/channels.list
            if ("C85DLE1U7".equals(channel)) {
                return new TaskHandler();
            } else if (channel != null) {
                // non-tasks channel
                return evt -> {
                    return null; // not interested
                };
            } else {
                return null; // unknown event type
            }
        }
    }

    ActionHandler getActionHandler(String type, String userId, String id, SlackRequest request)
            throws ServiceException {
        if ("task".equals(type)) {
            return new TaskHandler();
        }
        return null;
    }

    @Override
    public List<String> getRoles(String path) {
        return Arrays.asList(new String[] { Role.TASK_EXECUTION });
    }

    @Override
    protected Action getAction(String path, Object content, Map<String, String> headers) {
        return Action.Alert;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String, String> headers) {
        return Entity.Document;
    }
}
