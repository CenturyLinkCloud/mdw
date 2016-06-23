/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.bam;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.transform.TransformerException;

import com.centurylink.bam.AttributeListT;
import com.centurylink.bam.AttributeT;
import com.centurylink.bam.EventListT;
import com.centurylink.bam.EventT;
import com.centurylink.bam.LiveEventDocument;
import com.centurylink.bam.LiveEventT;


public class MasterRequest {

	private Long rowId;
	private String masterRequestId;
	private Date createTime;
	private Date recordTime;
	private String realm;
	private List<Component> components;
	private List<Attribute> attributes;
	private List<ComponentRelation> componentRelations;
	private List<Event> events;		// loaded separately; contains component events as well
	private String currentStatus;

	public List<ComponentRelation> getComponentRelations() {
		return componentRelations;
	}
	public void setComponentRelations(List<ComponentRelation> componentRelations) {
		this.componentRelations = componentRelations;
	}
	public Long getRowId() {
		return rowId;
	}
	public void setRowId(Long rowId) {
		this.rowId = rowId;
	}
	public String getRealm() {
		return realm;
	}
	public void setRealm(String realm) {
		this.realm = realm;
	}
	public String getMasterRequestId() {
		return masterRequestId;
	}
	public void setMasterRequestId(String masterRequestId) {
		this.masterRequestId = masterRequestId;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	public Date getRecordTime() {
		return recordTime;
	}
	public void setRecordTime(Date recordTime) {
		this.recordTime = recordTime;
	}
	public List<Component> getComponents() {
		return components;
	}
	public void setComponents(List<Component> components) {
		this.components = components;
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
	public String getCurrentStatus() {
		return currentStatus;
	}
	public void setCurrentStatus(String currentStatus) {
		this.currentStatus = currentStatus;
	}

	public Component findComponent(String componentId) {
		if (components==null) return null;
		for (Component one : components) {
			if (one.getComponentId().equals(componentId)) return one;
		}
		return null;
	}

	public Component findComponent(Long componentRowId) {
		if (components==null) return null;
		for (Component one : components) {
			if (one.getRowId().equals(componentRowId)) return one;
		}
		return null;
	}
    public String toXml() throws TransformerException {
        LiveEventDocument  lFactory = LiveEventDocument.Factory.newInstance();
        LiveEventT liveEvent = lFactory.addNewLiveEvent();
        EventListT eventList = liveEvent.addNewEventList();
        AttributeListT attrList = liveEvent.addNewAttributeList();

        List<Event> mEventList = this.getEvents();
        for (Event mEvent : mEventList) {
            EventT event =  eventList.addNewEvent();
            event.setEventName(mEvent.getEventName());
            event.setEventData(mEvent.getEventData());
            event.setEventCategory(mEvent.getEventCategory());
            event.setEventId(mEvent.getEventId());
            event.setEventRowId("" + mEvent.getRowId());
            event.setSubCategory(mEvent.getSubCategory());
            Calendar eCal = Calendar.getInstance();
            eCal.setTime(mEvent.getEventTime());
            event.setEventTime(eCal);
        }

        List<Attribute> mAttrList = this.getAttributes();
        for (Attribute mAttr : mAttrList) {
            AttributeT attribute =  attrList.addNewAttribute();
            attribute.setName(mAttr.getAttributeName());
            attribute.setValue(mAttr.getAttributeValue());
            attribute.setEventRowId("" + mAttr.getEventRowId());
        }
        return lFactory.xmlText();
    }
}
