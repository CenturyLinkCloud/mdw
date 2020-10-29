package com.centurylink.mdw.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.model.Jsonable;

public class Mdw implements Jsonable {

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("version", ApplicationContext.getMdwVersion());
        json.put("build", ApplicationContext.getMdwBuildTimestamp());
        return json;
    }

    public String getJsonName() {
        return "mdw";
    }
}
