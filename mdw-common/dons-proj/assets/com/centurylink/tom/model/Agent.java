/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.tom.model;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class Agent implements Jsonable {

    public Agent(JSONObject json) {
        bind(json);    
    }

    private String type;
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    private Name name;
    public Name getName() { return name; }
    public void setName(Name name) { this.name = name; }

    private ContactInfo contactInfo;
    public ContactInfo getContactInfo() { return contactInfo; }
    public void setContactInfo(ContactInfo contactInfo) { this.contactInfo = contactInfo; }
    
    public class ContactInfo implements Jsonable {
        
        public ContactInfo(JSONObject json) {
            bind(json);    
        }
        
        private TelephoneNumber telephoneNumber;
        public TelephoneNumber getTelephoneNumber() { return telephoneNumber; }
        public void setTelephoneNumber(TelephoneNumber telephoneNumber) { this.telephoneNumber = telephoneNumber; }
        
        private EmailAddress emailAddress;
        public EmailAddress getEmailAddress() { return emailAddress; }
        public void setEmailAddress(EmailAddress emailAddress) { this.emailAddress = emailAddress; }
        
        public class TelephoneNumber implements Jsonable {
            
            public TelephoneNumber(JSONObject json) {
                bind(json);    
            }
            
            private String type;
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            
            private String tn;
            public String getTn() { return tn; }
            public void setTn(String tn) { this.tn = tn; } 
        }
        
        public class EmailAddress implements Jsonable {

            public EmailAddress(JSONObject json) {
                bind(json);    
            }
            
            private String type;
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            
            private String email;
            public String getEmail() { return email; }
            public void setEmail(String email) { this.email = email; } 
        }
    }
}
