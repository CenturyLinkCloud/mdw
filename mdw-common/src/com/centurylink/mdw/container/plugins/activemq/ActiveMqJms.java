/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugins.activemq;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.naming.NamingException;

import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQTopic;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.connection.DelegatingConnectionFactory;

import com.centurylink.mdw.common.spring.SpringAppContext;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.container.ContainerContextAware;
import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.container.NamingProvider;

public class ActiveMqJms implements JmsProvider, ContainerContextAware {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Autowired
    @Qualifier("pooledConnectionFactory")
    private ConnectionFactory defaultConnectionFactory;

    public ConnectionFactory getConnectionFactory() throws JMSException, NamingException {
        return getQueueConnectionFactory(null, null);
    }

    public QueueConnectionFactory getQueueConnectionFactory(NamingProvider namingProvider, String name) throws JMSException, NamingException {
        ConnectionFactory connectionFactory = retrieveConnectionFactory(name);
        if (connectionFactory instanceof QueueConnectionFactory) {
            return (QueueConnectionFactory) connectionFactory;
        }
        else {
            DelegatingConnectionFactory delegatingFactory = new DelegatingConnectionFactory();
            delegatingFactory.setTargetConnectionFactory(connectionFactory);
            return delegatingFactory;
        }
    }

    public TopicConnectionFactory getTopicConnectionFactory(NamingProvider namingProvider, String name) throws JMSException, NamingException {
        ConnectionFactory connectionFactory = retrieveConnectionFactory(name);
        if (connectionFactory instanceof TopicConnectionFactory) {
            return (TopicConnectionFactory) connectionFactory;
        }
        else {
            DelegatingConnectionFactory delegatingFactory = new DelegatingConnectionFactory();
            delegatingFactory.setTargetConnectionFactory(connectionFactory);
            return delegatingFactory;
        }
    }

    public Topic getTopic(String name) {
        return new ActiveMQTopic(name);
    }

    public Queue getQueue(Session session, NamingProvider namingProvider, String name) throws JMSException {
        return session.createQueue(name);
    }

    public void setMessageDelay(QueueSender sender, Message message, long delaySeconds) throws JMSException {
        message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delaySeconds * 1000);
    }

    /**
     * Pooling and remote queues are configured via Spring in broker config XML.
     */
    protected ConnectionFactory retrieveConnectionFactory(String name) throws JMSException {
        if (name == null && defaultConnectionFactory != null)
            return defaultConnectionFactory; // injected

        if (bundleContext != null) {
            ConnectionFactory factory = null;
            try {
                if (name != null) {
                  String filter = "(name=" + name + ")";
                  ServiceReference[] srs = bundleContext.getServiceReferences(ConnectionFactory.class.getName(), filter);
                  if (srs != null && srs.length > 0) {
                      factory = (ConnectionFactory)bundleContext.getService(srs[0]);
                  }
                }
                else {
                    String connectionFactoryName = PropertyManager.getProperty("mdw.activemq.connection.factory");
                    if (connectionFactoryName != null) {
                        String filter = "(name=" + connectionFactoryName + ")";
                        ServiceReference[] srs = bundleContext.getServiceReferences(ConnectionFactory.class.getName(), filter);
                        if (srs != null && srs.length > 0)
                            factory = (ConnectionFactory)bundleContext.getService(srs[0]);
                        else
                            throw new JMSException("No service reference found: " + connectionFactoryName);
                    }
                    else {
                        ServiceReference sr = bundleContext.getServiceReference(ConnectionFactory.class.getName());
                        if (sr != null)
                            factory = (ConnectionFactory)bundleContext.getService(sr);
                    }
                }
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new JMSException("Error injecting JMS Connection Factory");
            }
            if (factory == null)
                throw new JMSException("Unable to obtain ConnectionFactory service reference");

            return factory;
        }
        else {
            try {
                // not OSGi and autowiring did not occur
                return (ConnectionFactory)SpringAppContext.getInstance().getBean(name);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new JMSException("JMS ConnectionFactory not found: " + name);
            }
        }
    }

    private BundleContext bundleContext;
    public void setContainerContext(Object context) {
        if (context instanceof BundleContext)
            this.bundleContext = (BundleContext) context;
    }
}
