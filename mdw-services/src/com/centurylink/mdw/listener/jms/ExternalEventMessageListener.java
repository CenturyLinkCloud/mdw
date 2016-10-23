/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.jms;

/*
 * Copyright (c) 2011 CenturyLink, Inc. All Rights Reserved.
 */

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.listener.SessionAwareMessageListener;

import com.centurylink.mdw.constant.SpringConstants;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.util.MessageProducer;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Replaces ExternalEventListener for Spring
 */
public class ExternalEventMessageListener implements SessionAwareMessageListener<Message> {

    @Autowired
    @Qualifier(SpringConstants.MDW_SPRING_MESSAGE_PRODUCER)
    private MessageProducer mdwMessageProducer;

    public ExternalEventMessageListener() {
        super();
    }

    /*
     * <p>
     * Calls the ListenerHelper to process the event
     * and sends back a message to the reply queue
     * </p>
     *
     * @see
     * org.springframework.jms.listener.SessionAwareMessageListener#onMessage
     * (javax.jms.Message, javax.jms.Session)
     */
    @Override
    public void onMessage(Message message, Session session) throws JMSException {

        StandardLogger logger = LoggerUtil.getStandardLogger();
        try {
            String txt = ((TextMessage) message).getText();
            if (logger.isDebugEnabled()) {
                logger.debug("JMS Spring ExternalEvent Listener receives request: " + txt);
            }
            String resp;
            ListenerHelper helper = new ListenerHelper();
            Map<String, String> metaInfo = new HashMap<String, String>();
            metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_JMS);
            metaInfo.put(Listener.METAINFO_REQUEST_PATH, ((Queue)message.getJMSDestination()).getQueueName());
            metaInfo.put(Listener.METAINFO_SERVICE_CLASS, this.getClass().getName());
            metaInfo.put(Listener.METAINFO_REQUEST_ID, message.getJMSMessageID());
            metaInfo.put(Listener.METAINFO_CORRELATION_ID, message.getJMSCorrelationID());
            if (message.getJMSReplyTo() != null)
                metaInfo.put("ReplyTo", message.getJMSReplyTo().toString());

            resp = helper.processEvent(txt, metaInfo);
            String correlId = message.getJMSCorrelationID();

            Queue respQueue = (Queue) message.getJMSReplyTo();
            if (resp != null && respQueue != null) {
                mdwMessageProducer.sendMessage(resp, respQueue, correlId);
                if (logger.isDebugEnabled()) {
                    logger.debug("JMS Listener sends response (corr id='" + correlId + "'): "
                            + resp);
                }

            }

        }
        catch (Throwable ex) {
            logger.severeException(ex.getMessage(), ex);
        }

    }

}