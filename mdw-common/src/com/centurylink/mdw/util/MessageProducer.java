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
    void sendMessage(String requestMessage, String queueName, String correlationId,
            final Queue replyQueue, int delaySeconds, int deliveryMode) throws JMSException;


    void sendMessage(String requestMessage, String queueName, String correlationId,
            final Queue replyQueue) throws JMSException;

    void sendMessage(final String requestMessage, final String correlationId,
            final Queue replyQueue) throws JMSException;

    /**
     * Broadcast a message to a specific topic
     *
     * @param dest
     * @param requestMessage
     */
    void broadcastMessageToTopic(String dest, String requestMessage);

    // Overloaded methods follow

    /**
     * @param message
     * @param queueName
     * @param correlationId
     * @throws JMSException
     */
    void sendMessage(String message, String queueName, String correlationId)
            throws JMSException;

    /**
     * @param message
     * @param queueName
     * @param correlationId
     * @param delaySeconds
     * @param deliveryMode
     * @throws JMSException
     */
    void sendMessage(String message, String queueName, String correlationId,
            int delaySeconds, int deliveryMode) throws JMSException;

    /**
     * @param message
     * @param queueName
     * @param correlationId
     * @param delaySeconds
     * @throws JMSException
     */
    void sendMessage(String message, String queueName, String correlationId, int delaySeconds)
            throws JMSException;

    /**
     * @param message
     * @param correlationId
     * @throws JMSException
     */
    void sendMessage(String message, String correlationId) throws JMSException;

    /**
     * @param message
     * @param queue
     * @param correlationId
     * @throws JMSException
     */
    void sendMessage(String message, Queue queue, String correlationId)
            throws JMSException;

}