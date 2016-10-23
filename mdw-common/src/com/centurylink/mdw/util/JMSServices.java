/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import java.util.Hashtable;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.NamingException;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.constant.SpringConstants;
import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class JMSServices {

    public static final String ERROR_CODE_TIMEOUT = "jmsTimout";
    public static final String THIS_SERVER = "THIS_SERVER";

    private static JMSServices instance;

    private Map<String,Queue> queueCache;
    private Map<String,QueueConnectionFactory> queueConnFactoryCache;
    private Map<String,TopicConnectionFactory> topicConnFactoryCache;

    private StandardLogger logger;
    private NamingProvider namingProvider;
    private JmsProvider jmsProvider;

    private MessageProducer mdwMessageProducer;

    private JMSServices(NamingProvider namingProvider, JmsProvider jmsProvider) {
    	this.namingProvider = namingProvider;
    	this.jmsProvider = jmsProvider;
    	logger = LoggerUtil.getStandardLogger();
        queueCache = new Hashtable<String,Queue>();
        queueConnFactoryCache = new Hashtable<String,QueueConnectionFactory>();
        topicConnFactoryCache = new Hashtable<String,TopicConnectionFactory>();
        try {
            mdwMessageProducer = (MessageProducer) SpringAppContext.getInstance().getBean(
                    SpringConstants.MDW_SPRING_MESSAGE_PRODUCER);
        }
        catch (Exception e) {
            logger.info("Unable to get Spring bean 'messageProducer' " +e.getMessage());
        }
    }

    public void clearCached() {
        queueCache = new Hashtable<String,Queue>();
        queueConnFactoryCache = new Hashtable<String,QueueConnectionFactory>();
        topicConnFactoryCache = new Hashtable<String,TopicConnectionFactory>();
    }

    public static JMSServices getInstance() {
    	if (instance == null)
    	    instance = new JMSServices(ApplicationContext.getNamingProvider(), ApplicationContext.getJmsProvider());
    	return instance;
    }

    public boolean initialized() {
    	return namingProvider!=null;
    }

    public void initialize(NamingProvider namingProvider, JmsProvider jmsProvider) {
    	this.namingProvider = namingProvider;
    	this.jmsProvider = jmsProvider;
    }

    /**
     * Sends a JMS text message to a local queue.
     * @param queueName local queues are based on logical queue names
     * @param message the message string
     * @param delaySeconds 0 for immediate
     * @throws ServiceLocatorException
     */
    public void sendTextMessage(String queueName, String message, int delaySeconds)
    throws NamingException, JMSException, ServiceLocatorException {
        sendTextMessage(null, queueName, message, delaySeconds, null);
    }

    /**
     * Sends a JMS text message.
     * @param contextUrl null for local queues
     * @param queueName local queues are based on logical queue names
     * @param message the message string
     * @param delaySeconds 0 for immediate
     * @throws ServiceLocatorException
     */
    public void sendTextMessage(String contextUrl, String queueName, String message,
    		int delaySeconds, String correlationId)
    throws NamingException, JMSException, ServiceLocatorException {
        if (logger.isDebugEnabled())
            logger.debug("Send JMS message: " + message);
        if (mdwMessageProducer != null) {
            if (logger.isDebugEnabled())
                logger.debug("Send JMS message: queue " + queueName+" corrId "+correlationId+" delay "+delaySeconds);
            mdwMessageProducer.sendMessage(message, queueName, correlationId, delaySeconds, DeliveryMode.NON_PERSISTENT);
        }
        else {
            QueueConnection connection = null;
            QueueSession session = null;
            QueueSender sender = null;
            Queue queue = null;

            try {
                QueueConnectionFactory connectionFactory = getQueueConnectionFactory(contextUrl);
                connection = connectionFactory.createQueueConnection();
                session = connection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
                if (contextUrl == null)
                    queue = getQueue(session, queueName);
                else
                    queue = getQueue(contextUrl, queueName);

                if (queue == null)
                    queue = session.createQueue(queueName);

                sender = session.createSender(queue);
                TextMessage textMessage = session.createTextMessage(message);

                if (delaySeconds > 0)
                    jmsProvider.setMessageDelay(sender, textMessage, delaySeconds);
                if (correlationId != null)
                    textMessage.setJMSCorrelationID(correlationId);

                connection.start();
                if (contextUrl == null)
                    sender.send(textMessage, DeliveryMode.NON_PERSISTENT, sender.getPriority(),
                            sender.getTimeToLive());
                else
                    sender.send(textMessage);
// }
// catch(ServiceLocatorException ex) {
// logger.severeException(ex.getMessage(), ex);
// JMSException jmsEx = new JMSException(ex.getMessage());
// jmsEx.setLinkedException(ex);
// throw jmsEx;
            }
            finally {
                closeResources(connection, session, sender);
            }
        }
    }

    /**
     * @return the jms queue connection factory
     */
    public QueueConnectionFactory getQueueConnectionFactory(String contextUrl)
    throws ServiceLocatorException {
        QueueConnectionFactory factory = (QueueConnectionFactory)
        	queueConnFactoryCache.get(contextUrl==null?THIS_SERVER:contextUrl);
        if (factory == null) {
            try {
                factory = jmsProvider.getQueueConnectionFactory(namingProvider, contextUrl);
            	if (contextUrl==null)
            		queueConnFactoryCache.put(THIS_SERVER, factory);
            	else
            		queueConnFactoryCache.put(contextUrl, factory);
            }
            catch (Exception ex) {
                throw new ServiceLocatorException(-1, ex.getMessage(), ex);
            }
        }
        return factory;
    }


    /**
     * Uses the container-specific qualifier to look up a JMS queue.
     * @param commonName the vendor-neutral logical queue name
     * @return javax.jms.Queue
     */
    public Queue getQueue(Session session, String commonName) throws ServiceLocatorException {
        Queue queue = (Queue) queueCache.get(commonName);
        if (queue == null) {
            try {
                String name = namingProvider.qualifyJmsQueueName(commonName);
                queue = jmsProvider.getQueue(session, namingProvider, name);
                if (queue != null)
                  queueCache.put(commonName, queue);
            }
            catch (Exception ex) {
                throw new ServiceLocatorException(-1, ex.getMessage(), ex);
            }
        }
        return queue;
    }

    /**
     * Looks up and returns a JMS queue.
     * @param queueName remote queue name
     * @param contextUrl the context url (or null for local)
     * @return javax.jms.Queue
     */
    public Queue getQueue(String contextUrl, String queueName) throws ServiceLocatorException {
        try {
            String jndiName = null;
            if (contextUrl == null) {
                jndiName = namingProvider.qualifyJmsQueueName(queueName);
            }
            else {
                jndiName = queueName;  // don't qualify remote queue names
            }
            return (Queue) namingProvider.lookup(contextUrl, jndiName, Queue.class);
        }
        catch (Exception ex) {
            throw new ServiceLocatorException(-1, ex.getMessage(), ex);
        }
    }

    public TopicConnectionFactory getTopicConnectionFactory(String contextUrl)
    throws ServiceLocatorException {
        TopicConnectionFactory factory = (TopicConnectionFactory)
        	topicConnFactoryCache.get(contextUrl==null?THIS_SERVER:contextUrl);
        if (factory == null) {
            try {
                factory = jmsProvider.getTopicConnectionFactory(namingProvider, contextUrl);
            	if (contextUrl == null)
            		topicConnFactoryCache.put(THIS_SERVER, factory);
            	else
            		topicConnFactoryCache.put(contextUrl, factory);
            }
            catch (Exception ex) {
                throw new ServiceLocatorException(-1, ex.getMessage(), ex);
            }
        }
        return factory;
    }

    public Topic getTopic(String commonName) throws ServiceLocatorException {
        try {
            String jndiName = namingProvider.qualifyJmsTopicName(commonName);
            return (Topic) namingProvider.lookup(null, jndiName, Topic.class);
        }
        catch (Exception ex) {
            throw new ServiceLocatorException(-1, ex.getMessage(), ex);
		}
    }

    /**
     * Sends the passed in text message to a local topic
     * @param topicName
     * @param pMessage
     * @param delaySeconds
     * @throws ServiceLocatorException
     */
    public void broadcastTextMessage(String topicName, String textMessage, int delaySeconds)
    throws NamingException, JMSException, ServiceLocatorException {
        if (mdwMessageProducer != null) {
            mdwMessageProducer.broadcastMessageToTopic(topicName, textMessage);
        }
        else {
            TopicConnectionFactory tFactory = null;
            TopicConnection tConnection = null;
            TopicSession tSession = null;
            TopicPublisher tPublisher = null;
            try {
                // if (logger.isDebugEnabled()) logger.debug("broadcast JMS message: " +
                // textMessage);
                // cannot log above - causing recursive broadcasting
                tFactory = getTopicConnectionFactory(null);
                tConnection = tFactory.createTopicConnection();
                tSession = tConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                Topic topic = getTopic(topicName);
                tPublisher = tSession.createPublisher(topic);

                // TODO: platform-independent delay
                // WLMessageProducer wlMessageProducer = (WLMessageProducer)tPublisher;
                // long delayInMilliSec = 0;
                // if(pMinDelay > 0){
                // delayInMilliSec = delaySeconds*1000;
                // }
                // wlMessageProducer.setTimeToDeliver(delayInMilliSec);
                TextMessage message = tSession.createTextMessage();
                tConnection.start();

                message.setText(textMessage);
                tPublisher.publish(message, DeliveryMode.PERSISTENT,
                        TextMessage.DEFAULT_DELIVERY_MODE, TextMessage.DEFAULT_TIME_TO_LIVE);
                // }catch(ServiceLocatorException ex){
                // ex.printStackTrace();
                // never log exception here!!! infinite loop when publishing log messages
                // throw new JMSException(ex.getMessage());
            }
            finally {
                closeResources(tConnection, tSession, tPublisher);

            }
        }
    }

    private void closeResources(QueueConnection pConn, QueueSession pSession, QueueSender pSender)
    throws JMSException {
        if (pSender != null) {
            pSender.close();
        }
        if (pSession != null) {
            pSession.close();
        }
        if (pConn != null) {
            pConn.close();
        }
    }

    private static void closeResources(TopicConnection pConn, TopicSession pSession, TopicPublisher pPublisher)
    throws JMSException {
        if (pPublisher != null) {
            pPublisher.close();
        }
        if (pSession != null) {
            pSession.close();
        }
        if (pConn != null) {
            pConn.close();
        }
    }

    public String invoke(String contextUrl, String reqqueue_name, String request, int timeoutSeconds)
    throws NamingException, JMSException, ServiceLocatorException {
        return invoke(contextUrl, reqqueue_name, request, timeoutSeconds, null);
    }

    public String invoke(String contextUrl, String reqqueue_name, String request,
    		int timeoutSeconds, String correlationId)
    throws NamingException, JMSException, ServiceLocatorException {

        QueueConnection connection = null;
        QueueSession session = null;
        QueueSender sender = null;
        Queue reqqueue = null;
        Queue respqueue = null;
        try {
        	if (logger.isDebugEnabled()) logger.debug("Invoke jms request message: " + request);
            QueueConnectionFactory connectionFactory = getQueueConnectionFactory(contextUrl);
            connection = connectionFactory.createQueueConnection();
            session = connection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            if (contextUrl == null) {
                reqqueue = getQueue(session, reqqueue_name);
            }
            else {
                reqqueue = getQueue(contextUrl, reqqueue_name);
                if (reqqueue == null) {
                    // jndi lookup does not work for ActiveMQ
                    reqqueue = getQueue(session, reqqueue_name);
                }
            }
            respqueue = session.createTemporaryQueue();
            sender = session.createSender(reqqueue);
            TextMessage textMessage = session.createTextMessage();
            if (correlationId!=null) textMessage.setJMSCorrelationID(correlationId);
            textMessage.setJMSReplyTo(respqueue);
            connection.start();
            textMessage.setText(request);
            sender.send(textMessage);
            MessageConsumer consumer = session.createConsumer(respqueue);
            textMessage = (TextMessage)consumer.receive(timeoutSeconds*1000);
        	if (textMessage==null) {
        		throw new JMSException("Synchronous JMS call times out while waiting for response");
        	} else {
        		return textMessage.getText();
        	}
        }
//        catch(ServiceLocatorException ex) {
//        	ex.printStackTrace();	// cannot use logger for chicken-egg issue
//        	JMSException jmsEx = new JMSException(ex.getMessage());
//            jmsEx.setLinkedException(ex);
//            throw jmsEx;
//        }
        finally {
            closeResources(connection, session, sender);
        }
    }

}
