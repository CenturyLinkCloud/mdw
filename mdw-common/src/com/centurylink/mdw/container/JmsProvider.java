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
