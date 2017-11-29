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
@Path("/Slack")
public class SlackService extends JsonRestService {

    @Override
    public JSONObject get(String path, Map<String,String> headers)
            throws ServiceException, JSONException {
        System.out.println("GET: " + path);
        System.out.println("HEADERS:\n" + String.valueOf(headers));
        String response = "{\n" +
                "  \"response_type\": \"ephemeral\",\n" +
                "  \"replace_original\": false,\n" +
                "  \"text\": \"Okay, sounds good.\"\n" +
                "}";
        return new JSONObject(response);
    }

    /**
     * Temporary impl for snapshot.
     */
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        SlackRequest request = new SlackRequest(content);
        String userId = getAuthUser(headers);
        // TODO: ability to map request.getUser() to corresponding MDW user
        String callbackId = request.getCallbackId();
        if (callbackId != null) {
            int hyphen = callbackId.indexOf('-');
            if (hyphen > 0) {
                String handlerType = callbackId.substring(0, hyphen);
                int underscore = callbackId.lastIndexOf('_');
                if (underscore > hyphen) {
                    String action = callbackId.substring(hyphen + 1, underscore);
                    String id = callbackId.substring(underscore + 1);
                    return runHandler(userId, handlerType, action, id, request);
                }
            }
        }
        throw new ServiceException(ServiceException.BAD_REQUEST, "Bad or missing callback_id");
    }


    JSONObject runHandler(String userId, String type, String action, String id, SlackRequest request) 
            throws ServiceException {
        Handler handler = null;
        if (type.equals("task")) {
            handler = new TaskHandler();
        }
        if (handler == null)
            throw new ServiceException("Unsupported action: " + action);
        
        return handler.handleAction(userId, action, id, request);
    }


    @Override
    public List<String> getRoles(String path) {
        return Arrays.asList(new String[]{Role.TASK_EXECUTION});
    }

    @Override
    protected Action getAction(String path, Object content, Map<String,String> headers) {
        return Action.Alert;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Document;
    }
}
