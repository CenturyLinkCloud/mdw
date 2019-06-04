package com.centurylink.mdw.tests.workflow;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

public class TestCase implements Jsonable {

    public TestCase(JSONObject json) {
        bind(json);
    }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
