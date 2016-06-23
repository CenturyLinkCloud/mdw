/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugins.jboss;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.TopicConnectionFactory;
import javax.naming.NamingException;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.container.NamingProvider;

public class JBossJms implements JmsProvider {

    private StandardLogger logger = LoggerUtil.getStandardLogger();

    public QueueConnectionFactory getQueueConnectionFactory(NamingProvider namingProvider, String contextUrl)
    throws NamingException {
    	return (QueueConnectionFactory) namingProvider.lookup(
    			contextUrl, getConnectionFactoryName(), QueueConnectionFactory.class);
    }

    public Queue getQueue(Session session, NamingProvider namingProvider, String name) throws NamingException {
    	return (Queue)namingProvider.lookup(null, name, Queue.class);
    }

    public TopicConnectionFactory getTopicConnectionFactory(NamingProvider namingProvider, String contextUrl)
    throws NamingException {
       	return (TopicConnectionFactory) namingProvider.lookup(
    			contextUrl, getConnectionFactoryName(), TopicConnectionFactory.class);
    }

    private String getConnectionFactoryName() {
        return "ConnectionFactory";
    }

    public void setMessageDelay(QueueSender sender, Message message, long delaySeconds) {
        long timeToDeliver = System.currentTimeMillis() + delaySeconds*1000;
        try {
          message.setLongProperty("JMS_JBOSS_SCHEDULED_DELIVERY", timeToDeliver);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }
}
