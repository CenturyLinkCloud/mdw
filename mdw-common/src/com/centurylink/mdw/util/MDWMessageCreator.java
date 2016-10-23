/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;

import org.springframework.jms.core.MessageCreator;

import com.centurylink.mdw.app.ApplicationContext;

/**
 * <p>
 * Provides a easier method to create a MessageCreator object
 * for use with a JmsTemplate
 * </p>
 * TODO - Perhaps make this a factory
 * @author aa70413
 *
 */
public class MDWMessageCreator implements MessageCreator {

    // This requestMessage is mandatory
    private String requestMessage;
    // Correlation Id may be needed
    private String correlationId;
    // ReplyQueue may be needed
    private Queue replyQueue;
    private int delaySeconds;


    public MDWMessageCreator(String requestMessage, String correlationId, Queue replyQueue) {
        this(requestMessage,correlationId,replyQueue,0);
    }
    public MDWMessageCreator(String requestMessage, String correlationId, Queue replyQueue, int delaySeconds) {
        super();
        this.requestMessage = requestMessage;
        this.correlationId = correlationId;
        this.replyQueue = replyQueue;
        this.delaySeconds = delaySeconds;
    }

    public MDWMessageCreator() {
        super();
    }

    /**
     * @param requestMessage
     */
    public MDWMessageCreator(String requestMessage) {
        this.requestMessage = requestMessage;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.jms.core.MessageCreator#createMessage(javax.jms
     * .Session)
     */
    @Override
    public Message createMessage(Session session) throws JMSException {
        Message message = session.createTextMessage(requestMessage);
        if (correlationId != null)
            message.setJMSCorrelationID(correlationId);
        if (replyQueue != null) {
            message.setJMSReplyTo(replyQueue);
        }
        if (delaySeconds >0) {
            ApplicationContext.getJmsProvider().setMessageDelay(null, message, delaySeconds);
        }
        return message;
    }

}
