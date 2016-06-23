/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.bam;

public class Attribute {

	private String attributeName;
	private String attributeValue;
	private Long componentRowId;
	private Long eventRowId;

	/**
     * @return the eventRowId
     */
    public Long getEventRowId() {
        return eventRowId;
    }
    /**
     * @param eventRowId the eventRowId to set
     */
    public void setEventRowId(Long eventRowId) {
        this.eventRowId = eventRowId;
    }
    public String getAttributeName() {
		return attributeName;
	}
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	public String getAttributeValue() {
		return attributeValue;
	}
	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}
	public Long getComponentRowId() {
		return componentRowId;
	}
	public void setComponentRowId(Long componentRowId) {
		this.componentRowId = componentRowId;
	}

	@Override
	public boolean equals(Object another) {
		if (another instanceof Attribute) {
			return attributeName.equals(((Attribute)another).attributeName);
		} else return false;
	}

}
