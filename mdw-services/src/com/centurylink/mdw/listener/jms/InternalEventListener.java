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

/*
 * Copyright (c) 2011 CenturyLink, Inc. All Rights Reserved.
 */


import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.monitor.CertifiedMessage;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.process.InternalEventDriver;
import com.centurylink.mdw.util.JMSServices;
import com.centurylink.mdw.util.ServiceLocatorException;
import com.centurylink.mdw.util.log.LoggerUtil;

public class InternalEventListener extends JmsListener {

    public InternalEventListener(ThreadPoolProvider thread_pool) {
        super(ThreadPoolProvider.WORKER_ENGINE,
                JMSDestinationNames.PROCESS_HANDLER_QUEUE, thread_pool);
    }

    protected Runnable getProcesser(TextMessage message) throws JMSException {
        String msgid = message.getJMSCorrelationID();
        return new InternalEventDriver(msgid, message.getText());
    }

    /**
     * For backward compatibility with MDW 5.1 and older certified messages
     * to internal queue for remote process call
     */
    @Override
    protected TextMessage filterMessage(TextMessage message) throws DataAccessException, ServiceLocatorException, JMSException {
        String msgid = message.getJMSCorrelationID();
        if (isCertifiedMessage(msgid)) {
            LoggerUtil.getStandardLogger().debug("Received certified internal message " + message.getText());
            EventManager eventManager = ServiceLocator.getEventManager();
            boolean consumed = eventManager.consumeCertifiedMessage(msgid);
            acknowledge(message, msgid);
            message.setJMSCorrelationID(null);
            return consumed?message:null;
        }
        return message;
    }

    private boolean isCertifiedMessage(String correlationId) {
        return (correlationId!=null &&
                correlationId.startsWith(CertifiedMessage.CERTIFIED_MESSAGE_PREFIX));
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

}