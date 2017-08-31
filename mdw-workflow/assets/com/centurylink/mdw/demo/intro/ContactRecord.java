package com.centurylink.mdw.demo.intro;

import java.util.Date;

public class ContactRecord implements java.io.Serializable {
    
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    private String phoneNumber;
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phone) {
        this.phoneNumber = phone;
    }
    
    private Date date;
    public Date getDate() { 
        return date;
    }
    public void setDate(Date d) {
        this.date = d;
    }
    
    private String notes;
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String toString() {
        return name + " (" + phoneNumber + "):\n" + notes;
    }
    
    public boolean equals(Object o) {
        return o instanceof ContactRecord && o.toString().equals(toString());
    }
}