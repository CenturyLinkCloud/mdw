package com.centurylink.mdw.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class Download implements Jsonable {

    /**
     * Empty URL means no download available.
     */
    private String url;
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Download(String url) {
        this.url = url;
    }

    public Download(JSONObject json) throws JSONException {
        if (json.has("url"))
            url = json.getString("url");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("url", url);
        return json;
    }

    public String getJsonName() {
        return "Download";
    }

}
