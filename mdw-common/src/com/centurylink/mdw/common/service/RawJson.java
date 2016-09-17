/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import org.json.JSONObject;

/**
 * Jsonable that just holds a raw JSONObject to avoid reserializing.
 *
 */
public class RawJson implements Jsonable {

    private JSONObject json;
    public JSONObject getJson() { return json; }

    public RawJson(JSONObject json) {
        this.json = json;
    }

    public String getJsonName() {
        return null;
    }
}
