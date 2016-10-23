/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.centurylink.mdw.model.attribute.Attribute;

public class Changes {
	
	public static final char NONE = 'U';
	public static final char NEW = 'N';
	public static final char CHANGE = 'C';
	public static final char DELETE = 'D';
	
	public static final String ATTRIBUTE_CHANGES = "__CHANGES__";
	public static final String UNKNOWN_VALUE = "unknown";
	
	private char changeType;
	private HashMap<String,String> changes;
	
	public Changes() {
		changeType = NONE;
		changes = null;
	}
	
	public Changes(List<Attribute> attrs) {
		changeType = NONE;
		changes = null;
		String cv = attrs==null?null:Attribute.findAttribute(attrs, ATTRIBUTE_CHANGES);
		fromString(cv);
	}
	
	public void fromString(String cv) {
		if (cv!=null && cv.length()>0) {
			changeType = cv.charAt(0);
			String[] changedAttrs = cv.substring(1).split(",");
			if (changedAttrs.length>0) changes = new HashMap<String,String>();
			for (String an : changedAttrs) {
				changes.put(an, UNKNOWN_VALUE);
			}
		}
	}
	
	@Override
	public String toString() {
		if (changeType==NONE) return null;
		StringBuffer sb = new StringBuffer();
		sb.append(changeType);
		if (changeType==CHANGE && changes!=null) {
			for (String an : changes.keySet()) {
				if (sb.length()>1) sb.append(',');
				sb.append(an);
			}
		}
		return sb.toString();
	}
	
	public void fromAttributes(List<Attribute> attrs) {
		changeType = NONE;
		changes = null;
		String cv = attrs==null?null:Attribute.findAttribute(attrs, ATTRIBUTE_CHANGES);
		fromString(cv);
	}
	
	public void toAttributes(List<Attribute> attrs) {
		String av = toString();
		Attribute.setAttribute(attrs, ATTRIBUTE_CHANGES, av);
	}

	public char getChangeType() {
		return changeType;
	}

	public void setChangeType(char changeType) {
		this.changeType = changeType;
		if (changeType!=CHANGE) changes = null;
	}

	public Set<String> getChangedAttributes() {
		return changes==null?null:changes.keySet();
	}
	
	public String getAttributeChange(String attrname) {
		return (changes==null)?null:changes.get(attrname);
	}
	
	public void setAttributeChange(String attrname, String oldvalue) {
		if (changeType==Changes.NONE) changeType = Changes.CHANGE;
		if (changeType==Changes.CHANGE) {
			if (changes==null) changes = new HashMap<String,String>();
			changes.put(attrname, oldvalue);
		}
	}
	
}
