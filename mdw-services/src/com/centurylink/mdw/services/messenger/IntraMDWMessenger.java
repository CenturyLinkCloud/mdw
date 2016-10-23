/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.messenger;

import com.centurylink.mdw.common.MDWException;
import com.centurylink.mdw.connector.adapter.AdapterException;

public abstract class IntraMDWMessenger {
	
	protected String destination;
	
	/**
	 * 
	 * @param destination can be null, in which case send to the same site (domain)
	 */
	protected IntraMDWMessenger(String destination) {
		this.destination = destination;
	}
		
	/**
	 * 
	 * @param message
	 * @throws MDWException
	 */
	abstract public void sendMessage(String message)
	throws MDWException;

	/**
	 * 
	 * @param request request message
	 * @param timeoutSeconds
	 * @return response message
	 * @throws MDWException
	 */
	abstract public String invoke(String request, int timeoutSeconds)
	throws MDWException;
	
	/**
	 * This method is a low level method for use by CertifiedMessageManager only.
	 * Should not be used in other places.
	 * 
	 * @param message
	 * @param msgId
	 * @param askTimeoutSeconds
	 * @throws MDWException
	 */
	abstract public void sendCertifiedMessage(String message, String msgid, int ackTimeoutSeconds)
	throws MDWException,AdapterException;
	
	
}
