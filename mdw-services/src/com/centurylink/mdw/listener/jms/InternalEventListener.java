package com.centurylink.mdw.listener.jms;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.services.process.InternalEventDriver;

public class InternalEventListener extends JmsListener {

    public InternalEventListener(ThreadPoolProvider thread_pool) {
        super(ThreadPoolProvider.WORKER_ENGINE,
                JMSDestinationNames.PROCESS_HANDLER_QUEUE, thread_pool);
    }

    protected Runnable getProcesser(TextMessage message) throws JMSException {
        String msgid = message.getJMSCorrelationID();
        return new InternalEventDriver(msgid, message.getText());
    }
}