package com.centurylink.mdw.listener.jms;

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
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.util.JMSServices;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ExternalEventListener extends JmsListener {

    public ExternalEventListener(String queueName, ThreadPoolProvider thread_pool) {
        super(ThreadPoolProvider.WORKER_LISTENER, queueName, thread_pool);
    }

    protected Runnable getProcesser(TextMessage message) throws JMSException {
        return new ExternalEventDriver(message);
    }

    class ExternalEventDriver implements Runnable {

        TextMessage message;
        ExternalEventDriver(TextMessage message) { this.message = message; }
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
                Map<String, String> metaInfo = new HashMap<>();
                metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_JMS);
                metaInfo.put(Listener.METAINFO_REQUEST_PATH, getQueueName());
                metaInfo.put(Listener.METAINFO_SERVICE_CLASS, this.getClass().getName());
                metaInfo.put(Listener.METAINFO_REQUEST_ID, message.getJMSMessageID());
                metaInfo.put(Listener.METAINFO_CORRELATION_ID, message.getJMSCorrelationID());
                if (message.getJMSReplyTo() != null)
                    metaInfo.put("ReplyTo", message.getJMSReplyTo().toString());

                    resp = helper.processRequest(txt, metaInfo);
                    Queue respQueue = (Queue) message.getJMSReplyTo();
                    String correlId = message.getJMSCorrelationID();
                    if (resp != null && respQueue != null) {
                        // String msgId = jmsMessage.getJMSMessageID();
                        QueueConnectionFactory qcf
                            = JMSServices.getInstance().getQueueConnectionFactory(null);
                        connection = qcf.createQueueConnection();

                        Message respMsg;
                        try (QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)) {
                            try (QueueSender sender = session.createSender(respQueue)) {
                                respMsg = session.createTextMessage(resp);
                                respMsg.setJMSCorrelationID(correlId);
                                sender.send(respMsg);
                            }
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("JMS Listener sends response (corr id='" +
                                    correlId + "'): " + resp);
                        }
                    }

            }
            catch (Throwable ex) {
                logger.error(ex.getMessage(), ex);
            }
            finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (JMSException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

}