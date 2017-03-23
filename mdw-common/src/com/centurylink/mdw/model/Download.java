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
package com.centurylink.mdw.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class Download implements Jsonable {

    /**
     * Empty URL means no download available.
     */
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
        if (json.has("url"))
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
