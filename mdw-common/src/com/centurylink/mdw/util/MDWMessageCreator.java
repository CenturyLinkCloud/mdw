/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
