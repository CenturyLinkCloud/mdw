/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Queue;

import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;

public class MDWMessageProducer implements MessageProducer{
    public MDWMessageProducer() {
        super();
    }

    @Autowired(required = false)
    @Qualifier("jmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired(required = false)
    @Qualifier("rabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    @Qualifier("bamTopicTemplate")
    private JmsTemplate jmsTopicTemplate;

    @Autowired(required = false)
    @Qualifier("bamTopicRabbitTemplate")
    private RabbitTemplate jmsTopicRabbitTemplate;

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
        else if (rabbitTemplate != null) {
            MessageProperties messageProperties = new MessageProperties();
            String targetQueueName = queueName;
            if (delaySeconds > 0) {
                /**
                 * For RabbitMQ, you can set a TTL (TimeToLive)
                 * on the message properties
                 */
                targetQueueName="com.centurylink.mdw.process.delay.queue";
                messageProperties.setExpiration(String.valueOf(delaySeconds * 1000));
            }
            messageProperties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
            if (replyQueue != null) {
                rabbitTemplate.setReplyQueue(new org.springframework.amqp.core.Queue(replyQueue
                        .getQueueName()));
            }
            rabbitTemplate.send(getExchange(targetQueueName), targetQueueName,
                    new Message(requestMessage.getBytes(), messageProperties), new CorrelationData(
                            correlationId));

            /**
             * new MDWMessageCreator(requestMessage, correlationId, replyQueue,
             * delaySeconds). if (!StringUtils.isEmpty(queueName)) {
             * rabbitTemplate.send(queueName, routingKey, new
             * Message(requestMessage.getBytes(), messageProperties),
             * correlationData);
             *
             * } else { rabbitTemplate.send(new
             * MDWMessageCreator(requestMessage, correlationId, replyQueue)); }
             */
        }
    }

    /**
     * For RabbitMQ, syou can set a TTL (TimeToLive)
     * on the message
     * @param requestMessage
     * @param correlationId
     * @param replyQueue
     * @param delaySeconds
     */
    public void sendDelayedMessageToRabbit(String queueName, String requestMessage,
            String correlationId, Queue replyQueue, int delaySeconds) {

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
     * Send a message to the BAM topic
     *
     * @param requestMessage
     * @param deliveryMode
     */
    public void sendBamMessageToTopic(String requestMessage, int deliveryMode) {
        jmsTopicTemplate.setDeliveryMode(deliveryMode);
        jmsTopicTemplate.send(new MDWMessageCreator(requestMessage));
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
     * @throws AmqpException
     */
    public void sendMessage(String message, Queue queue, String correlationId)
            throws AmqpException, JMSException {
        if (jmsTemplate != null) {
            jmsTemplate.send(queue, new MDWMessageCreator(message, correlationId, null, 0));
        }
        else if (rabbitTemplate != null) {
            rabbitTemplate.send(getExchange(queue.getQueueName()), null,
                    new Message(message.getBytes(), null), new CorrelationData(correlationId));
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