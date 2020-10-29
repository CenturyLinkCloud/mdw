package com.centurylink.mdw.slack;

import org.json.JSONArray;
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
            if (event.has("attachments")) {
                JSONArray attachments = event.getJSONArray("attachments");
                for (int i = 0; i < attachments.length(); i++) {
                    JSONObject attachment = attachments.getJSONObject(i);
                    if (attachment.has("callback_id"))
                        this.callbackId = attachment.getString("callback_id");
                }
            }
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
     * The ts of the reply thread.  Or in the case of an event
     * with callback_id, the originating message ts.
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

    /**
     * When callbackId is present, this means that the event
     * relates to a message-initiated user action.
     */
    private String callbackId;
    public String getCallbackId() {
        return callbackId;
    }


}
