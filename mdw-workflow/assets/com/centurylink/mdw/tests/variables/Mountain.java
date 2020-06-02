package com.centurylink.mdw.tests.variables;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

public class Mountain implements Jsonable {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private int elevation;
    public int getElevation() { return elevation; }
    public void setElevation(int elevation) { this.elevation = elevation; }

    private String country;
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Mountain(JSONObject json) {
        bind(json);
    }
}
