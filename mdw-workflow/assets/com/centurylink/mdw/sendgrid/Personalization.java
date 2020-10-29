package com.centurylink.mdw.sendgrid;

import java.util.List;

import com.centurylink.mdw.model.Jsonable;

public class Personalization implements Jsonable {

    private String subject;
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    private List<Address> to;
    public List<Address> getTo() { return to; }
    public void setTo(List<Address> to) { this.to = to; }

    private List<Address> cc;
    public List<Address> getCc() { return cc; }
    public void setCc(List<Address> cc) { this.cc = cc; }

}
