/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.messenger;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.model.listener.RMIListener;
import com.centurylink.mdw.services.ProcessException;

public class IntraMDWMessengerRmi extends IntraMDWMessenger {
		
	protected IntraMDWMessengerRmi(String destination) {
		super(destination);
	}

	/**
	 * jndi can be null, in which case send to the same site
	 */
	public void sendMessage(String message)
	throws ProcessException {
		invoke(message, 0);
	}
	
	/**
	 * jndi can be null, in which case send to the same site
	 */
	public String invoke(String message, int timeoutSeconds)
	throws ProcessException {
		// TODO - implement timeout? Weblogic does not seem to allow API to set timeout,
		// instead it can be set only globally, using JVM attributes
		//    -Dweblogic.iiop.connectTimeout=1000 -Dweblogic.iiop.requestTimeout=1000
		try {
	        RMIListener server = (RMIListener)ApplicationContext.getNamingProvider()
	        	.lookup(destination, RMIListener.JNDI_NAME, RMIListener.class);
	        return server.invoke(null, message);
		} catch (Exception e) {
			throw new ProcessException(-1, "Failed to send intra-MDW event", e);
		}
	}
	
	public void sendCertifiedMessage(String message, String msgid, int ackTimeoutSeconds)
		throws ProcessException,AdapterException {
		String acknowledgment;
		try {
			RMIListener server = (RMIListener)ApplicationContext.getNamingProvider()
        		.lookup(destination, RMIListener.JNDI_NAME, RMIListener.class);
			acknowledgment = server.invoke(msgid, message);
		} catch (Exception e) {
			throw new ProcessException(0, "Faile to send certified message", e);
		}
		if (!msgid.equals(acknowledgment)) throw new AdapterException("Incorrect acknowledgment for certified message");
	}

}
