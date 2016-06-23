/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugins.tomcat;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.TopicConnectionFactory;
import javax.naming.NamingException;

import org.apache.activemq.ScheduledMessage;
import org.osgi.framework.BundleContext;

import com.centurylink.mdw.container.ContainerContextAware;
import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.container.NamingProvider;

public class RabbitMqJms implements JmsProvider, ContainerContextAware {

    /**
     *
     */
    public RabbitMqJms() {
        super();
        // TODO Auto-generated constructor stub
    }

    private BundleContext bundleContext;
    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.ContainerContextAware#setContainerContext(java.lang.Object)
     */
    @Override
    public void setContainerContext(Object context) {
        if (context instanceof BundleContext)
            this.bundleContext = (BundleContext) context;

    }

     /* (non-Javadoc)
     * @see com.centurylink.mdw.container.JmsProvider#getQueueConnectionFactory(com.centurylink.mdw.container.NamingProvider, java.lang.String)
     */
    @Override
    public QueueConnectionFactory getQueueConnectionFactory(NamingProvider namingProvider,
            String contextUrl) throws JMSException, NamingException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.JmsProvider#getTopicConnectionFactory(com.centurylink.mdw.container.NamingProvider, java.lang.String)
     */
    @Override
    public TopicConnectionFactory getTopicConnectionFactory(NamingProvider namingProvider,
            String contextUrl) throws JMSException, NamingException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.JmsProvider#getQueue(javax.jms.Session, com.centurylink.mdw.container.NamingProvider, java.lang.String)
     */
    @Override
    public Queue getQueue(Session session, NamingProvider namingProvider, String name)
            throws JMSException, NamingException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.container.JmsProvider#setMessageDelay(javax.jms.QueueSender, javax.jms.Message, long)
     */
    @Override
    public void setMessageDelay(QueueSender sender, Message message, long delaySeconds)
            throws JMSException {
        message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delaySeconds * 1000);

    }

}
