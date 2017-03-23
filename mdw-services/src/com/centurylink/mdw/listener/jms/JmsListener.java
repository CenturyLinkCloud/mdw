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
package com.centurylink.mdw.listener.jms;

import java.io.InterruptedIOException;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.util.JMSServices;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public abstract class JmsListener  {

    private Thread demon_thread;
    private int receive_timeout;
    private int poll_interval;
    private boolean _terminating;
    private String name;
    private String queue_name;
    protected String getQueueName() { return queue_name; }

    private ThreadPoolProvider thread_pool;        // when thread_pool is null, run in demon_thread itself
    private MessageConsumer consumer;


    /**
     * @param name
     * @param queue_name
     * @param thread_pool
     */
    public JmsListener(String name, String queue_name, ThreadPoolProvider thread_pool) {
        this.name = name;
        this.queue_name = queue_name;
        this.receive_timeout = PropertyManager.getIntegerProperty(PropertyNames.MDW_JMS_LISTENER_RECEIVE_TIMEOUT, 300);
        this.poll_interval = PropertyManager.getIntegerProperty(PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL, PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL_DEFAULT);
        this.thread_pool = thread_pool;
    }

    abstract protected Runnable getProcesser(TextMessage message) throws JMSException;

    protected TextMessage filterMessage(TextMessage message) throws Exception {
        return message;
    }

    protected void start_in_thread() {
        Connection connection = null;
        Session session = null;
        consumer = null;
        StandardLogger logger = LoggerUtil.getStandardLogger();
        try {
            logger.info("JMS listener " + name + " is listening");
            JMSServices jmsServices = JMSServices.getInstance();

            // ActiveMQ is not able to listen on a topic when connected as a queue
            if (queue_name.endsWith(".topic") &&
              ApplicationContext.getJmsProvider().getClass().getName().equals("com.centurylink.mdw.container.plugin.activemq.ActiveMqJms")) {
                connection = jmsServices.getTopicConnectionFactory(null).createConnection();
                connection.start();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination topic = jmsServices.getTopic(queue_name);
                consumer = session.createConsumer(topic);
            }
            else {
                QueueConnectionFactory connectionFactory = jmsServices.getQueueConnectionFactory(null);
                connection = connectionFactory.createConnection();
                connection.start();
                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                Destination queue = jmsServices.getQueue(session, queue_name);
                consumer = session.createConsumer(queue);
            }

            _terminating = false;
            if (thread_pool == null) {  // This is for ConfigurationEventListener
                while (!_terminating) {
                    try {
                        TextMessage message = (TextMessage)consumer.receive(receive_timeout*1000);
                        if (message!=null) {
                            message = filterMessage(message);
                            if (message!=null)
                                getProcesser(message).run();
                            message.acknowledge();        // commit later after persistence?
                        }
                    }
                    catch (InterruptedException e) {
                        logger.info(this.getClass().getName() + " interrupted.");
                    }
                    catch (JMSException e) {
                        if ("org.apache.activemq.transport.TransportDisposedIOException".equals(e.getCause().getClass().getName())) {
                            logger.severeException("Terminating JMS Listener due to Transport Disposed", e);
                            _terminating = true;
                        }
                        else {
                            logger.severeException(e.getMessage(), e);
                        }
                    }
                    catch (Throwable e) {
                        logger.severeException(e.getMessage(), e);
                    }
                }
            }
            else {  // This is for ExternalEventListener and InternalEventListener
                TextMessage message = null;
                while (!_terminating) {
                    try {
                        // If message == null, then we successfully processed previous message, so get next one from queue
                        // If NOT null, then we couldn't process previously received message (no available thread), so try again
                        if (message == null) {
                            message = (TextMessage)consumer.receive(receive_timeout*1000);

                            if (message!=null)
                                message = filterMessage(message);
                        }
                        if (message!=null) {
                            if (thread_pool.execute(name, "JMSListener " + name, getProcesser(message))) {
                                message.acknowledge();  // commit later after persistence?
                                message = null;  // Make null so we get next message from queue
                            }
                            else {
                                String msg = "JMS listener " + name + " has no thread available";
                                // make this stand out
                                logger.severeException(msg, new Exception(msg));
                                logger.info(thread_pool.currentStatus());
                                Thread.sleep(poll_interval*1000);  // Will try to process same message after waking up
                            }
                        }
                    }
                    catch (InterruptedException e) {
                        logger.info(this.getClass().getName() + " interrupted.");
                    }
                    catch (JMSException e) {
                        if ("org.apache.activemq.transport.TransportDisposedIOException".equals(e.getCause().getClass().getName())) {
                            logger.severeException("Terminating JMS Listener due to Transport Disposed", e);
                            _terminating = true;
                        }
                        else {
                            logger.severeException(e.getMessage(), e);
                        }
                    }
                    catch (Throwable e) {
                        logger.severeException(e.getMessage(), e);
                    }
                }
            }
            logger.info("JMS listener " + name + " is terminated");
        }
        catch (Exception e) {
            logger.severeException("JMS listener " + name + " terminated due to exception " + e.getMessage(), e);
        }
        finally {
            try {
                if (consumer != null) {
                    consumer.close();
                    consumer = null;
                }
                if (session != null)
                    session.close();
                if (connection != null)
                    connection.close();
            }
            catch (JMSException e) {
                if (_terminating && e.getCause() != null && (e.getCause() instanceof InterruptedIOException
                          || e.getCause().getClass().getName().equals("org.apache.activemq.transport.TransportDisposedIOException"))) {
                    // ignore errors during shutdown
                    if (logger.isTraceEnabled())
                        logger.infoException(e.getMessage(), e);
                }
                else {
                    logger.severeException("JMS listener " + name + " exception during closing resources " + e.getMessage(), e);
                }
            }
        }
    }

    public void start() {
        demon_thread = new Thread() {
            public void run() {
                this.setName(name);
                start_in_thread();
            }
        };
        demon_thread.start();
    }

    public void stop() {
        _terminating = true;

        try {
            if (consumer != null) {
                consumer.close();
                consumer = null;
            }
        }
        catch (Exception ex) {
            LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
        }

        demon_thread.interrupt();
    }

}