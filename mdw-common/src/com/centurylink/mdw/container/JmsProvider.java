/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
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

	String JBOSS = "JBoss";
	String ACTIVEMQ = "ActiveMQ";
	String OSGI = "OSGi";
	String NONE = "none";

    public QueueConnectionFactory getQueueConnectionFactory(NamingProvider namingProvider, String contextUrl)
    throws JMSException, NamingException;

    public TopicConnectionFactory getTopicConnectionFactory(NamingProvider namingProvider, String contextUrl)
    throws JMSException, NamingException;

    public Queue getQueue(Session session, NamingProvider namingProvider, String name)
    throws JMSException,NamingException;

    public void setMessageDelay(QueueSender sender, Message message, long delaySeconds)
    throws JMSException;

}
