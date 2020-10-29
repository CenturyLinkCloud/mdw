package com.centurylink.mdw.model.event;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

/**
 * Represents a workflow event notification.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "id", "message", "completionCode", "delay" })
@XmlRootElement(name = "Event")
public class Event implements Jsonable {

    @XmlElement(name = "Id", required = true)
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @XmlElement(name = "Message")
    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @XmlElement(name = "Delay")
    private int delay;
    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }

    @XmlElement(name = "CompletionCode")
    private String completionCode;
    public String getCompletionCode() { return completionCode; }
    public void setCompletionCode(String completionCode) { this.completionCode = completionCode; }

    public Event() {
    }

    public Event(JSONObject json) throws JSONException {
        bind(json);
    }

    @Override
    public String getJsonName() {
        return "event";
    }

}
