package com.centurylink.mdw.sendgrid;

import com.centurylink.mdw.model.Jsonable;

public class Address implements Jsonable {

    public Address(String email) {
        this.email = email;
    }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
