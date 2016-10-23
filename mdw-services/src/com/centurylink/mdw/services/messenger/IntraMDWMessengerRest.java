/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.messenger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.util.HttpHelper;

public class IntraMDWMessengerRest extends IntraMDWMessenger {

	protected IntraMDWMessengerRest(String destination, String serviceContext) {
		super(destination);
	}

	/**
	 *
	 * @param jndi
	 * @return
	 * @throws MalformedURLException
	 */
	private URL getURL(String engineUrl) throws MalformedURLException {
		if (engineUrl == null)
			return new URL(ApplicationContext.getServicesUrl() + "/Services/REST");
		else
		    return new URL(engineUrl + "/Services/REST");
	}

	/**
	 * jndi can be null, in which case send to the same site
	 */
	public void sendMessage(String message)
	throws ProcessException {
		try {
			HttpHelper httpHelper = new HttpHelper(getURL(destination));
			httpHelper.post(message);
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
			HttpHelper httpHelper = new HttpHelper(getURL(destination));
			String res = httpHelper.post(message, timeoutSeconds, timeoutSeconds);
            return res==null?null:res.trim();
		} catch (Exception e) {
			throw new ProcessException(-1, e.getMessage(), e);
		}
	}

	public void sendCertifiedMessage(String message, String msgid, int ackTimeoutSeconds)
		throws ProcessException,AdapterException {
		String acknowledgment;
		try {
			HttpHelper httpHelper = new HttpHelper(getURL(destination));
			HashMap<String,String> headers = new HashMap<String,String>();
			headers.put("MDWCertifiedMessageId", msgid);
			httpHelper.setHeaders(headers);
			acknowledgment = httpHelper.post(message, ackTimeoutSeconds, ackTimeoutSeconds);
		} catch (Exception e) {
			throw new ProcessException(0, "Faile to send certified message", e);
		}
		if (!msgid.equals(acknowledgment.trim()))
			throw new AdapterException("Incorrect acknowledgment for certified message");
	}

}
