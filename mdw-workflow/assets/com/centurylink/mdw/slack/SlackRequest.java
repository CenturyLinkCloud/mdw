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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class SlackRequest implements Jsonable {
    
    public SlackRequest(JSONObject json) {
        if (json.has("name"))
            this.name = json.getString("name");
        if (json.has("value"))
            this.value = json.getString("value");
        if (json.has("callback_id"))
            this.callbackId = json.getString("callback_id");
        if (json.has("trigger_id"))
            this.triggerId = json.getString("trigger_id");
        if (json.has("channel")) {
            JSONObject channelObj = json.getJSONObject("channel");
            if (channelObj.has("name"))
                this.channel = channelObj.getString("name");
        }
        if (json.has("team")) {
            JSONObject teamObj = json.getJSONObject("team");
            if (teamObj.has("domain"))
                this.team = teamObj.getString("domain");
        }
        if (json.has("type"))
            this.type = json.getString("type");
        if (json.has("user")) {
            JSONObject userObj = json.getJSONObject("user");
            if (userObj.has("name"))
                this.user = userObj.getString("name");
        }
        if (json.has("actions")) {
            this.actions = new ArrayList<>();
            JSONArray actionsArr = json.getJSONArray("actions");
            for (int i = 0; i < actionsArr.length(); i++) {
                JSONObject actionObj = actionsArr.getJSONObject(i);
                if (actionObj.has("name"))
                    this.actions.add(actionObj.getString("name"));
                if (actionObj.has("value")) // value is used for user selection
                    this.value = actionObj.getString("value");
            }
        }
        if (json.has("response_url"))
            this.responseUrl = json.getString("response_url");
            
    }
    
    private String name;
    public String getName() {
        return name;
    }
    
    private String value;
    public String getValue() {
        return value;
    }
    
    private String callbackId;
    public String getCallbackId() {
        return callbackId;
    }
    
    private String triggerId;
    public String getTriggerId() {
        return triggerId;
    }
    
    private String channel;
    public String getChannel() {
        return channel;
    }
    
    private String team;
    public String getTeam() { 
        return team;
    }
    
    private String type;
    public String getType() {
        return type;
    }
    
    private String user;
    public String getUser() {
        return user;
    }
    
    private List<String> actions;
    public List<String> getActions() {
        return actions;
    }
    
    private String responseUrl;
    public String getResponseUrl() {
        return responseUrl;
    }
}
