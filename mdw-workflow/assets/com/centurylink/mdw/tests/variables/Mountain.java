package com.centurylink.mdw.tests.variables;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

import java.io.Serializable;

public class Mountain implements Jsonable, Serializable {

    public static final long serialVersionUID = 3;  // match asset version

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

    public Mountain(String name, int elevation, String country) {
        this.name = name;
        this.elevation = elevation;
        this.country = country;
    }

    @Override
    public String toString() {
        return name + ", " + elevation + " ft, " + country;
    }
}
