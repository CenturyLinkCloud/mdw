/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.process;

import javax.jms.JMSException;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.constant.SpringConstants;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.util.MessageProducer;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * This class is used in the services project to send messages
 *
 * @author aa70413
 *
 */
public class EventServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static EventServices instance;

    private MessageProducer mdwMessageProducer;

    private EventServices() {
        try {
            // Grab this from the context...should probably inject
            mdwMessageProducer = (MessageProducer) SpringAppContext.getInstance().getBean(
                    SpringConstants.MDW_SPRING_MESSAGE_PRODUCER);
        }
        catch (Exception e) {
            logger.debug("Unable to get Spring bean 'messageProducer'", e.getMessage());
        }
    }

    public static EventServices getInstance() {
        if (instance == null)
            instance = new EventServices();
        return instance;
    }

    public boolean sendInternalMessageCheck(String worker, String messageId, String eventName,
            String eventMessage) throws JMSException {
        return sendInternalMessage(worker, messageId, eventName, eventMessage, true);
    }

    public void sendInternalMessage(String worker, String messageId, String eventName,
            String eventMessage) throws JMSException {
        sendInternalMessage(worker, messageId, eventName, eventMessage, false);
    }

    /**
     * <p>
     * Uses the injected mdwMessageProducer to send the message or defaults
     * back to InternalEventDriver
     * </p>
     * @param worker
     * @param messageId
     * @param eventName
     * @param eventMessage
     * @param checkAvailableThreads
     * @throws JMSException
     */
    public boolean sendInternalMessage(String worker, final String messageId, String eventName,
            final String eventMessage, boolean checkAvailableThreads) throws JMSException {
        // Spring Injection
        if (mdwMessageProducer != null) {
            mdwMessageProducer.sendMessage(eventMessage, messageId);

        }
        else {
            InternalEventDriver command = new InternalEventDriver(messageId, eventMessage);
            ThreadPoolProvider thread_pool = ApplicationContext.getThreadPoolProvider();
            if (checkAvailableThreads && !thread_pool.execute(worker, eventName, command)) {
                String msg = worker + " has no thread available for event: " + eventName
                        + " message:\n" + eventMessage;
                // make this stand out
                logger.warnException(msg, new Exception(msg));
                logger.info(thread_pool.currentStatus());
                return false;
            }
            else if (!checkAvailableThreads)
                thread_pool.execute(worker, eventName, command);

        }
        return true;
    }

}
