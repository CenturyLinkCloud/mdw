/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.service.Jsonable;

public class Mdw implements Jsonable {

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("version", ApplicationContext.getMdwVersion());
        json.put("build", ApplicationContext.getMdwBuildTimestamp());
        return json;
    }

    public String getJsonName() {
        return "mdw";
    }
}
