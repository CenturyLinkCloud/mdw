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
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.rest.JsonRestService;

import java.lang.System;

@Path("/Slack")
public class Slack extends JsonRestService {

    /**
     * Temporary impl for snapshot.
     */
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
        System.out.println("POST: " + path + ":\n");
        System.out.println(content.toString(2));
        System.out.println("HEADERS:\n" + String.valueOf(headers));
        String response = "{\n" +
                "  \"response_type\": \"ephemeral\",\n" +
                "  \"replace_original\": false,\n" +
                "  \"text\": \"Okay, sounds good.\"\n" +
                "}";
        return new JSONObject(response);
    }




    @Override
    public List<String> getRoles(String path) {
        return null; // TODO: temp
    }

    @Override
    protected Action getAction(String path, Object content, Map<String,String> headers) {
        return Action.Collaborate;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Document;
    }
}
