/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.tests.workflow;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

/**
 * JavaBean for testing Jsonable and Yaml variables.
 */
public class Person implements Jsonable {

    private String firstName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String first) {
        this.firstName = first;
    }

    private String lastName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String last) {
        this.lastName = last;
    }

    /**
     * no-arg constructor required for yaml
     */
    public Person() {
    }

    /**
     * Jsonable requires this constructor
     */
    public Person(JSONObject json) throws JSONException {
        firstName = json.getString("firstName");
        lastName = json.getString("lastName");
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("firstName", firstName);
        json.put("lastName", lastName);
        return json;
    }

    @Override
    public String getJsonName() {
        return "person";
    }
}
