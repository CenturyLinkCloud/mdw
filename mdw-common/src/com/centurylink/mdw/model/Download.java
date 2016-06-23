/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class Download implements Jsonable {

    private String url;
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    private String file;
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public Download(String url) {
        this.url = url;
    }

    public Download(JSONObject json) throws JSONException {
        url = json.getString("url");
        if (json.has("file"))
            file = json.getString("file");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("url", url);
        json.put("file", file);
        return json;
    }

    public String getJsonName() {
        return "Download";
    }

}
