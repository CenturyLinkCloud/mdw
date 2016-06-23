/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.bam;

import java.util.List;


public class Component {

	private Long componentRowid;
	private String componentId;
	private String componentType;
	private List<Attribute> attributes;
	private List<Event> events;
	private String currentStatus;

	public Long getRowId() {
		return componentRowid;
	}
	public void setRowId(Long componentRowid) {
		this.componentRowid = componentRowid;
	}
	public String getComponentId() {
		return componentId;
	}
	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}
	public List<Attribute> getAttributes() {
		return attributes;
	}
	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}
	public List<Event> getEvents() {
		return events;
	}
	public void setEvents(List<Event> events) {
		this.events = events;
	}
	public String getComponentType() {
		return componentType;
	}
	public void setComponentType(String componentType) {
		this.componentType = componentType;
	}
	public String getCurrentStatus() {
		return currentStatus;
	}
	public void setCurrentStatus(String currentStatus) {
		this.currentStatus = currentStatus;
	}

	@Override
	public boolean equals(Object another) {
		if (another instanceof Component) {
			return componentId.equals(((Component)another).componentId);
		} else return false;
	}
}
