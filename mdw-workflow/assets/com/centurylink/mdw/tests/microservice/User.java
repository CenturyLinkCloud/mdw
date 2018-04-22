package com.centurylink.mdw.tests.microservice;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class User implements Jsonable {

    public User(JSONObject json) {
        bind(json);
    }

    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    private String firstName;
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    private String lastName;
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    private String emailAddress;
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

}
