/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.jms;

/*
 * Copyright (c) 2011 CenturyLink, Inc. All Rights Reserved.
 */


import javax.jms.JMSException;
import javax.jms.TextMessage;

import com.centurylink.mdw.bpm.ConfigurationChangeRequestDocument;
import com.centurylink.mdw.common.constant.JMSDestinationNames;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.ConfigurationHelper;
import com.centurylink.mdw.services.event.BroadcastHelper;

public class ConfigurationEventListener extends JmsListener {

	public ConfigurationEventListener() {
		super("ConfigurationEventListener",
				JMSDestinationNames.CONFIG_HANDLER_TOPIC, null);
	}

	protected Runnable getProcesser(TextMessage message) throws JMSException {
		return new ErrorQueueDriver(message.getText());
	}

	private class ErrorQueueDriver implements Runnable {

	    private String eventMessage;

	    public ErrorQueueDriver(String eventMessage) {
	    	this.eventMessage = eventMessage;
	    }

		public void run() {
			StandardLogger logger = LoggerUtil.getStandardLogger();
	        try {
			    if (eventMessage.startsWith("{")) {
			    	BroadcastHelper helper = new BroadcastHelper();
			    	helper.processBroadcastMessage(eventMessage);
		    		logger.info("Received and processed broadcast: " + eventMessage);
			    } else {
			        boolean status = false;
			        ConfigurationChangeRequestDocument doc
			        	= ConfigurationChangeRequestDocument.Factory.parse(eventMessage);
		            String fileName = doc.getConfigurationChangeRequest().getFileName();

		            status = ConfigurationHelper.applyConfigChange(fileName, doc
		                    .getConfigurationChangeRequest().getFileContents(), doc
		                    .getConfigurationChangeRequest().getReactToChange());
		            if (status) {
		                logger.info(fileName + " has been successfully modified.");
		            }
		            else {
		                logger.info(fileName + " update FAILED.");
		            }
			    }
	        }
	        catch (JMSException ex) {
	            logger.severeException(ex.getMessage(), ex);
	        } catch (Exception e) {
				logger.severeException("Failed to process broadcast message", e);
			}
		}

	}

}