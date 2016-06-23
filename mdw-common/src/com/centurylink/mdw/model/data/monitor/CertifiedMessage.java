/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.data.monitor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.StringHelper;


public class CertifiedMessage implements Comparable<CertifiedMessage> {
	
	public static final String CERTIFIED_MESSAGE_PREFIX = "MDWCM-";

	public static final String PROP_PROTOCOL = "protocol";
	public static final String PROP_JNDI_URL = "jndi_url";		// MDW 4 must be t3://h:p
																// MDW 5.2 allows any server spec accepted by IntraMDWMessenger
	public static final String PROP_POOL_NAME = "pool_name";
	public static final String PROP_NO_INITIAL_SEND = "no_initial_send";
	public static final String PROP_RETRY_INTERVAL = "retry_interval";		// in seconds
	public static final String PROP_TIMEOUT = "timeout";		// in seconds
	public static final String PROP_MAX_TRIES = "max_tries";	// max (automated) tries
	
	public static final String PROTOCOL_MDW2MDW = "MDW2MDW";		// MDW->MDW, support exactly-once semantics
	public static final String PROTOCOL_POOL = "ConnectionPool";	// MDW->any, support at-least-once semantics

	private Date initiateTime;
	private Date nextTryTime;
	private String propertyString;
	private Map<String,String> properties;
	private Long documentId;
	private String content;
	private Integer status;
	private int tryCount;
	private String reference;
	
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public Date getInitiateTime() {
		return initiateTime;
	}
	public void setInitiateTime(Date initiateTime) {
		this.initiateTime = initiateTime;
	}
	public Date getNextTryTime() {
		return nextTryTime;
	}
	public void setNextTryTime(Date nextTryTime) {
		this.nextTryTime = nextTryTime;
	}
//	public void setNextTryTime(int defaultIntervalInSeconds) {
//		int seconds = StringHelper.getInteger(getProperty(PROP_RETRY_INTERVAL),defaultIntervalInSeconds);
//		this.nextTryTime = new Date(System.currentTimeMillis()+seconds*1000);
//	}
	public String getPropertyString() {
		if (propertyString==null && properties!=null)
			propertyString = StringHelper.formatMap(properties);
		return propertyString;
	}
	public void setPropertyString(String v) {
		this.propertyString = v;
		properties = null;
	}
	public Map<String,String> getProperties() {
		if (properties==null && propertyString!=null)
			properties = StringHelper.parseMap(propertyString);
		return properties;
	}
	public void setProperties(Map<String,String> v) {
		this.properties = v;
		propertyString = null;
	}
	public String getProperty(String name) {
		if (properties==null && propertyString!=null)
			properties = StringHelper.parseMap(propertyString);
		if (properties==null) return null;
		return properties.get(name);
	}
	public void setProperty(String name, String value) {
		if (properties==null) {
			if (propertyString!=null) properties = StringHelper.parseMap(propertyString);
			else properties = new HashMap<String,String>();
		}
		properties.put(name, value);
	}
	public Long getDocumentId() {
		return documentId;
	}
	public void setDocumentId(Long documentId) {
		this.documentId = documentId;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	
	@Override
	public int compareTo(CertifiedMessage o) {
		return nextTryTime.compareTo(o.nextTryTime);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CertifiedMessage)) return false;
		return documentId == ((CertifiedMessage)obj).documentId;
	}
	
	public String getId() {
		return CERTIFIED_MESSAGE_PREFIX + ApplicationContext.getApplicationName() 
			+ "-" + documentId;
	}
	public int getTryCount() {
		return tryCount;
	}
	public void setTryCount(int tryCount) {
		this.tryCount = tryCount;
	}
	
}
