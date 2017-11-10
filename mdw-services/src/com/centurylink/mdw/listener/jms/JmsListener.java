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

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Thread daemonThread;
    private int receiveTimeout;
    private int pollInterval;
    private boolean terminating;
    private String name;
    private String queueName;
    protected String getQueueName() { return queueName; }

    private ThreadPoolProvider threadPool;  // when threadPool is null, run in daemonThread itself
    private MessageConsumer consumer;

    /**
     * @param name
     * @param queueName
     * @param threadPool
     */
    public JmsListener(String name, String queueName, ThreadPoolProvider threadPool) {
        this.name = name;
        this.queueName = queueName;
        this.receiveTimeout = PropertyManager.getIntegerProperty(PropertyNames.MDW_JMS_LISTENER_RECEIVE_TIMEOUT, 300);
        this.pollInterval = PropertyManager.getIntegerProperty(PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL, PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL_DEFAULT);
        this.threadPool = threadPool;
    }

    abstract protected Runnable getProcesser(TextMessage message) throws JMSException;

    protected TextMessage filterMessage(TextMessage message) throws Exception {
        return message;
    }

    protected void startInThread() {
        Connection connection = null;
        Session session = null;
        consumer = null;
        try {
            logger.info("JMS listener " + name + " (" + queueName + ") is listening");
            JMSServices jmsServices = JMSServices.getInstance();

            // ActiveMQ is not able to listen on a topic when connected as a queue
            if (queueName.endsWith(".topic") &&
              ApplicationContext.getJmsProvider().getClass().getName().equals("com.centurylink.mdw.container.plugin.activemq.ActiveMqJms")) {
                connection = jmsServices.getTopicConnectionFactory(null).createConnection();
                connection.start();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination topic = jmsServices.getTopic(queueName);
                consumer = session.createConsumer(topic);
            }
            else {
                QueueConnectionFactory connectionFactory = jmsServices.getQueueConnectionFactory(null);
                connection = connectionFactory.createConnection();
                connection.start();
                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                Destination queue = jmsServices.getQueue(session, queueName);
                consumer = session.createConsumer(queue);
            }

            terminating = false;
            if (threadPool == null) {  // This is for ConfigurationEventListener
                while (!terminating) {
                    try {
                        TextMessage message = (TextMessage)consumer.receive(receiveTimeout*1000);
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
                    catch (Throwable th) {
                        logger.severeException("JMS Listener Error: " + queueName + " (" + JmsListener.this.hashCode() + ")", th);
                    }
                }
            }
            else {  // this is for ExternalEventListener and InternalEventListener
                TextMessage message = null;
                while (!terminating) {
                    try {
                        // If message == null, then we successfully processed previous message, so get next one from queue
                        // If NOT null, then we couldn't process previously received message (no available thread), so try again
                        if (message == null) {
                            message = (TextMessage)consumer.receive(receiveTimeout*1000);

                            if (message!=null)
                                message = filterMessage(message);
                        }
                        if (message!=null) {
                            if (threadPool.execute(name, "JMSListener " + name, getProcesser(message))) {
                                message.acknowledge();  // commit later after persistence?
                                message = null;  // Make null so we get next message from queue
                            }
                            else {
                                String msg = "JMS listener " + name + " (" + queueName + ") has no thread available";
                                // make this stand out
                                logger.severeException(msg, new Exception(msg));
                                logger.info(threadPool.currentStatus());
                                Thread.sleep(pollInterval*1000);  // Will try to process same message after waking up
                            }
                        }
                    }
                    catch (InterruptedException e) {
                        logger.info(this.getClass().getName() + " interrupted.");
                    }
                    catch (Throwable th) {
                        logger.severeException("JMS Listener Error: " + queueName + " (" + JmsListener.this.hashCode() + ")", th);
                    }
                }
            }
            logger.info("JMS listener " + name + " (" + queueName + ") is terminated");
        }
        catch (Exception ex) {
            logger.severeException("JMS listener " + name + " (" + queueName + ") terminated due to exception " + ex.getMessage(), ex);
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
            catch (JMSException ex) {
                logger.severeException("JMS listener " + name + " error closing resources " + ex.getMessage(), ex);
            }
        }
    }

    public void start() {
        daemonThread = new Thread() {
            public void run() {
                this.setName(name);
                startInThread();
            }
        };
        daemonThread.start();
    }

    public void stop() {
        terminating = true;

        try {
            if (consumer != null) {
                consumer.close();
                logger.info("Closed JMS listener consumer: " + queueName + " (" + JmsListener.this.hashCode() + ")");
                consumer = null;
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }

        daemonThread.interrupt();
    }

}
