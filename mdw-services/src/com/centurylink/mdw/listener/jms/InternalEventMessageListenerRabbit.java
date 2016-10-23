/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.jms;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;

import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Spring Replacement for InternalEventListener
 */
public class InternalEventMessageListenerRabbit implements MessageListener {

    public InternalEventMessageListenerRabbit()  {
        super();
    }



    @Override
    public void onMessage(org.springframework.amqp.core.Message message) {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        String logtag = "EngineDriver.T" + Thread.currentThread().getId() + " - ";

        try {
            String txt = new String(message.getBody());
            if (logger.isDebugEnabled()) {
                logger.debug("JMS Spring Rabbit InternalEvent Listener receives request: " + txt);
            }
            logger.info(logtag + "starts processing");

            String messageId = null;
            MessageProperties props = message.getMessageProperties();

            if (message.getMessageProperties() != null && props.getCorrelationId() != null)
                messageId = new String (props.getCorrelationId());
            ProcessEngineDriver driver = new ProcessEngineDriver();
            driver.processEvents(messageId, txt);
        } catch (Throwable e) { // only possible when failed to get ProcessManager ejb
            logger.severeException(logtag + "process exception " + e.getMessage(), e);
        }

    }

}