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
import javax.jms.Queue;

/**
 * Interface for JMS producers
 */
public interface MessageProducer {

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
            final Queue replyQueue, int delaySeconds, int deliveryMode) throws JMSException;


    public void sendMessage(String requestMessage, String queueName, String correlationId,
            final Queue replyQueue) throws JMSException;

    public void sendMessage(final String requestMessage, final String correlationId,
            final Queue replyQueue) throws JMSException;

    /**
     * Broadcast a message to a specific topic
     *
     * @param dest
     * @param requestMessage
     */
    public void broadcastMessageToTopic(String dest, String requestMessage);

    // Overloaded methods follow

    /**
     * @param message
     * @param queueName
     * @param correlationId
     * @throws JMSException
     */
    public void sendMessage(String message, String queueName, String correlationId)
            throws JMSException;

    /**
     * @param message
     * @param queueName
     * @param correlationId
     * @param delaySeconds
     * @param nonPersistent
     * @throws JMSException
     */
    public void sendMessage(String message, String queueName, String correlationId,
            int delaySeconds, int deliveryMode) throws JMSException;

    /**
     * @param xml
     * @param queueName
     * @param correlationId
     * @param delaySeconds
     * @throws JMSException
     */
    public void sendMessage(String message, String queueName, String correlationId, int delaySeconds)
            throws JMSException;

    /**
     * @param message
     * @param correlationId
     * @throws JMSException
     */
    public void sendMessage(String message, String correlationId) throws JMSException;

    /**
     * @param message
     * @param queue
     * @param correlationId
     * @throws JMSException
     * @throws AmqpException
     */
    public void sendMessage(String message, Queue queue, String correlationId)
            throws JMSException;


    /**
     * @param queueName
     * @return
     */
    public String getExchange(String queueName);

}