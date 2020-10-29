package com.centurylink.mdw.container;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.TopicConnectionFactory;
import javax.naming.NamingException;

public interface JmsProvider {

    QueueConnectionFactory getQueueConnectionFactory(ContextProvider contextProvider, String contextUrl)
    throws JMSException, NamingException;

    TopicConnectionFactory getTopicConnectionFactory(ContextProvider contextProvider, String contextUrl)
    throws JMSException, NamingException;

    Queue getQueue(Session session, ContextProvider contextProvider, String name)
    throws JMSException,NamingException;

    void setMessageDelay(QueueSender sender, Message message, long delaySeconds)
    throws JMSException;

}
