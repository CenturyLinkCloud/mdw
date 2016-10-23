/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.event;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class EventInstance implements Serializable {

    // external events
    public static final Integer STATUS_WAITING = 0;
    public static final Integer STATUS_WAITING_MULTIPLE = 1;
    public static final Integer STATUS_ARRIVED = 2;
    public static final Integer STATUS_CONSUMED = 3;
    // flag
    public static final Integer STATUS_FLAG = 4;
    // scheduled jobs (a.k.a timer tasks)
    public static final Integer STATUS_SCHEDULED_JOB = 5;
    // internal event - including following 3 types:
    //	  a) delayed message:
    //			consumeDate is not null, reference can be null or others,
    //			event name is often InternalEvent.<actInstId> when the event is for notifying an activity instance
    //			or InternalEvent.<procInstId>start<activityId> when the event is to start an activity instance
    //	  b) resume message due to connection pool down:
    //			consumerDate is null, reference is "pool:poolname", event name is InternalEvent.<actInstId>
    //	  c) active message: consumeDate is null, reference is "active"
    public static final Integer STATUS_INTERNAL_EVENT = 7;
    // certified messages
    public static final Integer STATUS_CERTIFIED_MESSAGE = 6;
    public static final Integer STATUS_CERTIFIED_MESSAGE_HOLD = 8;
    public static final Integer STATUS_CERTIFIED_MESSAGE_CANCEL = 9;
    public static final Integer STATUS_CERTIFIED_MESSAGE_DELIVERED = 10;
    public static final Integer STATUS_CERTIFIED_MESSAGE_RECEIVED = 11;

    public static final Integer RESUME_STATUS_SUCCESS  = new Integer(1);
    public static final Integer RESUME_STATUS_PARTIAL_SUCCESS  = new Integer(2);
    public static final Integer RESUME_STATUS_FAILURE  = new Integer(3);
    public static final Integer RESUME_STATUS_NO_WAITERS = new Integer(4);

    public static final String EVENT_OCCURANCE_RECURRING = "RECURRING";
    public static final String EVENT_OCCURANCE_NON_RECURRING = "NON_RECURRING";

    public static final String ACTIVE_INTERNAL_EVENT = "active";

    private String eventName;
    private Long documentId;
    private Date createDate;
    private Date consumeDate;
    private String data;
    private Integer status;
    private int preserveSeconds;
    private boolean existing;
    private String comments;
    private String auxdata;
    private String reference;

	private List<EventWaitInstance> waiters;	// for CACHE_ONLY engine mode only

    public EventInstance(){
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getConsumeDate() {
        return consumeDate;
    }

    public void setConsumeDate(Date consumeDate) {
        this.consumeDate = consumeDate;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

    public int getPreserveSeconds() {
        return preserveSeconds;
    }

    public void setPreserveSeconds(int preserveSeconds) {
        this.preserveSeconds = preserveSeconds;
    }

	public String getAuxdata() {
		return auxdata;
	}

	public void setAuxdata(String auxdata) {
		this.auxdata = auxdata;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

    public boolean isExisting() {
        return existing;
    }

    public void setExisting(boolean existing) {
        this.existing = existing;
    }

    public List<EventWaitInstance> getWaiters() {
    	return this.waiters;
    }

    public void setWaiters(List<EventWaitInstance> waiters) {
    	this.waiters = waiters;
    }

    public static String getStatusName(Integer status) {
    	if (status.equals(STATUS_WAITING)) return "Waiting";
    	else if (status.equals(STATUS_ARRIVED)) return "Arrived";
    	else if (status.equals(STATUS_CONSUMED)) return "Consumed-multiple";
    	else if (status.equals(STATUS_WAITING_MULTIPLE)) return "Waiting-multiple";
    	else if (status.equals(STATUS_FLAG)) return "Flag";
    	else if (status.equals(STATUS_CERTIFIED_MESSAGE)) return "Certified Message";
    	else if (status.equals(STATUS_INTERNAL_EVENT)) return "Internal Event";
    	else if (status.equals(STATUS_SCHEDULED_JOB)) return "Scheduled Job";
    	else if (status.equals(STATUS_CERTIFIED_MESSAGE_CANCEL)) return "CM - Cancelled";
    	else if (status.equals(STATUS_CERTIFIED_MESSAGE_DELIVERED)) return "CM - Delivered";
    	else if (status.equals(STATUS_CERTIFIED_MESSAGE_RECEIVED)) return "CM - Received";
    	else if (status.equals(STATUS_CERTIFIED_MESSAGE_HOLD)) return "CM - on hold";
    	else return "Unknown";
    }

    public static Integer getStatusCodeFromName(String v) {
    	if (v.equalsIgnoreCase("Waiting")) return STATUS_WAITING;
    	else if (v.equalsIgnoreCase("Arrived")) return STATUS_ARRIVED;
    	else if (v.equalsIgnoreCase("Consumed-multiple")) return STATUS_CONSUMED;
    	else if (v.equalsIgnoreCase("Waiting-multiple")) return STATUS_WAITING_MULTIPLE;
    	else if (v.equalsIgnoreCase("Flag")) return STATUS_FLAG;
    	else if (v.equalsIgnoreCase("Certified Message")) return STATUS_CERTIFIED_MESSAGE;
    	else if (v.equalsIgnoreCase("Internal Event")) return STATUS_INTERNAL_EVENT;
    	else if (v.equalsIgnoreCase("Scheduled Job")) return STATUS_SCHEDULED_JOB;
    	else if (v.equalsIgnoreCase("CM - Cancelled")) return STATUS_CERTIFIED_MESSAGE_CANCEL;
    	else if (v.equalsIgnoreCase("CM - Delivered")) return STATUS_CERTIFIED_MESSAGE_DELIVERED;
    	else if (v.equalsIgnoreCase("CM - Received")) return STATUS_CERTIFIED_MESSAGE_RECEIVED;
    	else if (v.equalsIgnoreCase("CM - on hold")) return STATUS_CERTIFIED_MESSAGE_HOLD;
    	else return null;
    }

    public static String getStatusName(String statusValueString) {
    	int status = Integer.parseInt(statusValueString);
    	return getStatusName(status);
    }

}
