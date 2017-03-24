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
package com.centurylink.mdw.container.plugin.tomcat;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.TopicConnectionFactory;
import javax.naming.NamingException;

import org.apache.activemq.ScheduledMessage;

import com.centurylink.mdw.container.ContainerContextAware;
import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.container.NamingProvider;

public class RabbitMqJms implements JmsProvider, ContainerContextAware {

    @Override
    public void setContainerContext(Object context) {
    }

    @Override
    public QueueConnectionFactory getQueueConnectionFactory(NamingProvider namingProvider,
            String contextUrl) throws JMSException, NamingException {
        return null;
    }

    @Override
    public TopicConnectionFactory getTopicConnectionFactory(NamingProvider namingProvider,
            String contextUrl) throws JMSException, NamingException {
        return null;
    }

    @Override
    public Queue getQueue(Session session, NamingProvider namingProvider, String name)
            throws JMSException, NamingException {
        return null;
    }

    @Override
    public void setMessageDelay(QueueSender sender, Message message, long delaySeconds)
            throws JMSException {
        message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delaySeconds * 1000);

    }

}
