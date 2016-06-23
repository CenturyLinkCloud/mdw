/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.pooling;

import java.util.Date;

/**
 * An instance of this class represents a connection in a connection pool.
 * 
 * 
 *
 */
public abstract class PooledConnection {
	
	private int id;
	private String assignee;
	private Date assignTime;
	
	/**
	 * @throws Exception
	 */
	public PooledConnection() {
		this.assignee = null;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public Date getAssignTime() {
		return assignTime;
	}
	
	public void setAssignTime(Date v) {
		assignTime = v;
	}
	
	public String getAssignee() {
		return assignee;
	}
	
	public void setAssignee(String v) {
		assignee = v;
	}
	
	abstract public void destroy();

}
