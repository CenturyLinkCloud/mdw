/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.bam;

import java.util.Date;

public class Event {

	public static final String EVENT_NAME_SUBMIT = "Submit";
	public static final String EVENT_NAME_STATUS = "Status";	// for cancel/complete order
	public static final String EVENT_NAME_NEW_COMPONENT = "NewComponent";
	public static final String EVENT_NAME_CANCEL_COMPONENT = "CancelComponent";

	public static final String EVENT_CAT_STANDARD = "Standard";

	private String eventName;
	private String eventData;
	private Date eventTime;
	private String eventCategory;
	private String subCategory;
	private String eventId;			// source event ID
	private String sourceSystem;
	private Long rowId;		// unintelligent key
	private Long componentRowId;	// TODO - add a database column

	public String getSourceSystem() {
		return sourceSystem;
	}
	public void setSourceSystem(String sourceSystem) {
		this.sourceSystem = sourceSystem;
	}
	public String getEventName() {
		return eventName;
	}
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	public String getEventData() {
		return eventData;
	}
	public void setEventData(String eventData) {
		this.eventData = eventData;
	}
	public Date getEventTime() {
		return eventTime;
	}
	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
	}
	public String getEventCategory() {
		return eventCategory;
	}
	public void setEventCategory(String eventCategory) {
		this.eventCategory = eventCategory;
	}
	public String getSubCategory() {
		return subCategory;
	}
	public void setSubCategory(String subCategory) {
		this.subCategory = subCategory;
	}
	public String getEventId() {
		return eventId;
	}
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}
	public Long getRowId() {
		return rowId;
	}
	public void setRowId(Long rowId) {
		this.rowId = rowId;
	}
	public Long getComponentRowId() {
		return componentRowId;
	}
	public void setComponentRowId(Long componentRowId) {
		this.componentRowId = componentRowId;
	}

}
