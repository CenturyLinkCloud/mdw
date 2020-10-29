package com.centurylink.mdw.sendgrid;

import java.util.List;

import com.centurylink.mdw.model.Jsonable;

public class Email implements Jsonable {

    private List<Personalization> personalizations;
    public List<Personalization> getPersonalizations() { return personalizations; }
    public void setPersonalizations(List<Personalization> personalizations) {
        this.personalizations = personalizations;
    }

    private Address from;
    public Address getFrom() { return from; }
    public void setFrom(Address from) { this.from = from; }

    private List<Message> content;
    public List<Message> getContent() { return content; }
    public void setContent(List<Message> content) { this.content = content; }

    private List<Attachment> attachments;
    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
}
