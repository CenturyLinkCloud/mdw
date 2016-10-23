/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.jms;

/*
 * Copyright (c) 2011 CenturyLink, Inc. All Rights Reserved.
 */


import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.monitor.CertifiedMessage;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.JMSServices;
import com.centurylink.mdw.util.ServiceLocatorException;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ExternalEventListener extends JmsListener {

    public ExternalEventListener(String queueName, ThreadPoolProvider thread_pool) {
        super(ThreadPoolProvider.WORKER_LISTENER, queueName, thread_pool);
    }

    @Override
    protected TextMessage filterMessage(TextMessage message) throws DataAccessException, ServiceLocatorException, JMSException {
        String msgid = message.getJMSCorrelationID();
        if (isCertifiedMessage(msgid)) {
            EventManager eventManager = ServiceLocator.getEventManager();
            boolean consumed = eventManager.consumeCertifiedMessage(msgid);
            acknowledge(message, msgid);
            return consumed?message:null;
        }
        return message;
    }

    private void acknowledge(Message jmsMessage, String msgid) throws JMSException, ServiceLocatorException {
        QueueConnection connection = null;
        QueueSession session = null;
        QueueSender sender = null;
        try {
            Queue respQueue = (Queue) jmsMessage.getJMSReplyTo();
            QueueConnectionFactory qcf = JMSServices.getInstance().getQueueConnectionFactory(null);
            connection = qcf.createQueueConnection();
            session = connection.createQueueSession(false, QueueSession.DUPS_OK_ACKNOWLEDGE);
            sender = session.createSender(respQueue);
            Message respMsg = session.createTextMessage(msgid);
//        respMsg.setJMSCorrelationID(correlationId); not used
            sender.send(respMsg);
        } finally {
            if (sender != null) sender.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
        }
    }

    protected Runnable getProcesser(TextMessage message) throws JMSException {
        return new ExternalEventDriver(message);
    }

    private boolean isCertifiedMessage(String correlationId) {
        return (correlationId!=null &&
                correlationId.startsWith(CertifiedMessage.CERTIFIED_MESSAGE_PREFIX));
    }

    class ExternalEventDriver implements Runnable {

        TextMessage message;
        ExternalEventDriver(TextMessage message) { this.message=message; }
        public void run() {
            QueueConnection connection = null;
            StandardLogger logger = LoggerUtil.getStandardLogger();
            try {
                String txt = message.getText();
                if (logger.isDebugEnabled()) {
                    logger.debug("JMS Listener receives request: " + txt);
                }
                String resp;
                ListenerHelper helper = new ListenerHelper();
                Map<String, String> metaInfo = new HashMap<String, String>();
                metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_JMS);
                metaInfo.put(Listener.METAINFO_REQUEST_PATH, getQueueName());
                metaInfo.put(Listener.METAINFO_SERVICE_CLASS, this.getClass().getName());
                metaInfo.put(Listener.METAINFO_REQUEST_ID, message.getJMSMessageID());
                metaInfo.put(Listener.METAINFO_CORRELATION_ID, message.getJMSCorrelationID());
                if (message.getJMSReplyTo() != null)
                    metaInfo.put("ReplyTo", message.getJMSReplyTo().toString());

                    resp = helper.processEvent(txt, metaInfo);
                    Queue respQueue = (Queue) message.getJMSReplyTo();
                    String correlId = message.getJMSCorrelationID();
                    if (resp != null && respQueue != null && !isCertifiedMessage(correlId)) {
                        // String msgId = jmsMessage.getJMSMessageID();
                        QueueConnectionFactory qcf
                            = JMSServices.getInstance().getQueueConnectionFactory(null);
                        connection = qcf.createQueueConnection();
                        QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                        QueueSender sender = session.createSender(respQueue);
                        Message respMsg = session.createTextMessage(resp);
                        respMsg.setJMSCorrelationID(correlId);
                        sender.send(respMsg);
                        if (logger.isDebugEnabled()) {
                            logger.debug("JMS Listener sends response (corr id='" +
                                    correlId + "'): " + resp);
                        }
                    }

            }
            catch (Throwable ex) {
                logger.severeException(ex.getMessage(), ex);
            }
            finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (JMSException e) {
                        logger.severeException(e.getMessage(), e);
                    }
                }
            }
        }
    }

}