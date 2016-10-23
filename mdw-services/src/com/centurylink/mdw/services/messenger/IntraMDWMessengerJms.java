/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.messenger;


import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.util.JMSServices;

public class IntraMDWMessengerJms extends IntraMDWMessenger {

	protected IntraMDWMessengerJms(String destination) {
		super(destination);
	}

	/**
	 * jndi can be null, in which case send to the same site
	 */
	public void sendMessage(String message)
	throws ProcessException {
		try {
			JMSServices.getInstance().sendTextMessage(destination,
					JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE, message, 0, null);
		} catch (Exception e) {
			throw new ProcessException(-1, e.getMessage(), e);
		}

	}

	/**
	 * jndi can be null, in which case send to the same site
	 */
	public String invoke(String message, int timeoutSeconds)
	throws ProcessException {
		try {
			return JMSServices.getInstance().invoke(destination,
					JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE, message, timeoutSeconds);
		} catch (Exception e) {
			throw new ProcessException(-1, e.getMessage(), e);
		}
	}

	public void sendCertifiedMessage(String message, String msgid, int ackTimeoutSeconds)
		throws ProcessException,AdapterException {
		String acknowledgment;
		try {
			acknowledgment = JMSServices.getInstance().invoke(destination,
					JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE,
					message, ackTimeoutSeconds, msgid);
		} catch (Exception e) {
			throw new ProcessException(0, "Faile to send certified message", e);
		}
		if (!msgid.equals(acknowledgment)) throw new AdapterException("Incorrect acknowledgment for certified message");
	}

}
