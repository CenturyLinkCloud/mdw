/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;

import javax.jms.JMSException;
import javax.jms.Queue;

/**
 * Interface for RabbitMQ and ActiveMQ producers
 * @author aa70413
 *
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