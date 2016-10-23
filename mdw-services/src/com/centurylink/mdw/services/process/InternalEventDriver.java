/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.process;

import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class InternalEventDriver implements Runnable {

    private String eventMessage;
    private String messageId;

    public InternalEventDriver(String messageId, String eventMessage) {
    	this.messageId = messageId;
    	this.eventMessage = eventMessage;
    }

	public void run() {
		StandardLogger logger = LoggerUtil.getStandardLogger();
		String logtag = "EngineDriver.T" + Thread.currentThread().getId() + " - ";
		try {
			logger.info(logtag + "starts processing");
			ProcessEngineDriver driver = new ProcessEngineDriver();
			driver.processEvents(messageId, eventMessage);
		} catch (Throwable e) {	// only possible when failed to get ProcessManager ejb
			logger.severeException(logtag + "process exception " + e.getMessage(), e);
		}
	}

}