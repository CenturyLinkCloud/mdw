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

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Queue;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;

public class MDWMessageProducer implements MessageProducer {
    public MDWMessageProducer() {
        super();
    }

    @Autowired(required = false)
    @Qualifier("jmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired(required = false)
    private JmsTemplate jmsTopicTemplate;

    /**
     * Send a message to a queue with corrId, delay and potential replyQueue
     *
     * @param requestMessage
     * @param queueName
     * @param correlationId
     * @param replyQueue
     * @param delaySeconds
     * @throws JMSException
     */
    public void sendMessage(String requestMessage, String queueName, String correlationId,
            final Queue replyQueue, int delaySeconds, int deliveryMode) throws JMSException {
        /**
         * If it's an internal message then always use jmsTemplate for ActiveMQ
         */

        if (jmsTemplate != null) {
            jmsTemplate.setDeliveryMode(deliveryMode);
            if (!StringUtils.isEmpty(queueName)) {
                jmsTemplate.send(queueName, new MDWMessageCreator(requestMessage, correlationId,
                        replyQueue, delaySeconds));

            }
            else {
                jmsTemplate.send(new MDWMessageCreator(requestMessage, correlationId, replyQueue));
            }
        }
    }

    public void sendMessage(String requestMessage, String queueName, String correlationId,
            final Queue replyQueue) throws JMSException {
        sendMessage(requestMessage, queueName, correlationId, replyQueue, 0,
                DeliveryMode.PERSISTENT);
    }

    public void sendMessage(final String requestMessage, final String correlationId,
            final Queue replyQueue) throws JMSException {
        sendMessage(requestMessage, null, correlationId, replyQueue);
    }

    /**
     * Broadcast a message to a specific topic
     *
     * @param dest
     * @param requestMessage
     */
    public void broadcastMessageToTopic(String dest, String requestMessage) {
        jmsTopicTemplate.setDeliveryPersistent(true);

        jmsTopicTemplate.send(dest, new MDWMessageCreator(requestMessage));
    }

    // Overloaded methods follow

    /**
     * @param message
     * @param queueName
     * @param correlationId
     * @throws JMSException
     */
    public void sendMessage(String message, String queueName, String correlationId)
            throws JMSException {
        sendMessage(message, queueName, correlationId, null);

    }

    /**
     * @param message
     * @param queueName
     * @param correlationId
     * @param delaySeconds
     * @param nonPersistent
     * @throws JMSException
     */
    public void sendMessage(String message, String queueName, String correlationId,
            int delaySeconds, int deliveryMode) throws JMSException {
        sendMessage(message, queueName, correlationId, null, delaySeconds, deliveryMode);
    }

    /**
     * @param xml
     * @param queueName
     * @param correlationId
     * @param delaySeconds
     * @throws JMSException
     */
    public void sendMessage(String message, String queueName, String correlationId, int delaySeconds)
            throws JMSException {
        sendMessage(message, queueName, correlationId, null, delaySeconds, DeliveryMode.PERSISTENT);
    }

    /**
     * @param message
     * @param correlationId
     * @throws JMSException
     */
    public void sendMessage(String message, String correlationId) throws JMSException {
        sendMessage(message, null, correlationId, null, 0, DeliveryMode.PERSISTENT);

    }

    /**
     * @param message
     * @param queue
     * @param correlationId
     * @throws JMSException
     */
    public void sendMessage(String message, Queue queue, String correlationId)
            throws JMSException {
        if (jmsTemplate != null) {
            jmsTemplate.send(queue, new MDWMessageCreator(message, correlationId, null, 0));
        }
    }

    /**
     * @param queueName
     * @return
     */
    public String getExchange(String queueName) {
        // Choose the exchange
        String exchange = "mdw.direct.exchange";
        return exchange;
    }

}