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

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class SlackEvent implements Jsonable {
    
    public SlackEvent(JSONObject json) {
        if (json.has("type"))
            this.type = json.getString("type");
        if (json.has("challenge"))
            this.challenge = json.getString("challenge");
        if (json.has("team_id"))
            this.team = json.getString("team_id");
        if (json.has("api_app_id"))
            this.app = json.getString("api_app_id");
        if (json.has("event")) {
            JSONObject event = json.getJSONObject("event");
            // overrides above
            if (event.has("type"))
                this.type = event.getString("type");
            if (event.has("channel"))
                this.channel = event.getString("channel");
            if (event.has("user"))
                this.user = event.getString("user");
            // overrides above
            if (event.has("source_team"))
                this.team = event.getString("source_team");
            if (event.has("ts"))
                this.ts = event.getString("ts");
            if (event.has("thread_ts"))
                this.threadTs = event.getString("thread_ts");
            if (event.has("text"))
                this.text = event.getString("text");
        }
    }
    
    private String type;
    public String getType() { return type; }

    private String challenge;
    public String getChallenge() { return challenge; }
    
    private String team;
    public String getTeam() { return team; }
    
    private String app;
    public String getApp() { return app; }
    
    private String channel;
    public String getChannel() { return channel; }
    
    private String user;
    public String getUser() { return user; }
    
    /**
     * The ts of the reply thread.
     */
    private String ts;
    public String getTs() { return ts; }
    
    /**
     * The ts of the original message.
     */
    private String threadTs;
    public String getThreadTs() { return threadTs; }
    
    private String text;
    public String getText() { return text; }
    
}
