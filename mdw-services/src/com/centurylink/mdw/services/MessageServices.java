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
package com.centurylink.mdw.services;

import javax.jms.JMSException;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.services.process.InternalEventDriver;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.util.MessageProducer;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * This class is used in the services project to send messages
 */
public class MessageServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static MessageServices instance;

    private MessageProducer mdwMessageProducer;

    private MessageServices() {
        try {
            mdwMessageProducer = (MessageProducer) SpringAppContext.getInstance().getBean(
                    SpringAppContext.MDW_SPRING_MESSAGE_PRODUCER);
        }
        catch (Exception e) {
            if (logger.isMdwDebugEnabled())
              logger.debug("Unable to get Spring bean 'messageProducer'", e.getMessage());
        }
    }

    public static MessageServices getInstance() {
        if (instance == null)
            instance = new MessageServices();
        return instance;
    }

    public boolean sendInternalMessageCheck(String worker, String messageId, String eventName,
            String eventMessage) throws JMSException {
        return sendInternalMessage(worker, messageId, eventName, eventMessage, true);
    }

    public void sendInternalMessage(String worker, String messageId, String eventName,
            String eventMessage) throws JMSException {
        sendInternalMessage(worker, messageId, eventName, eventMessage, false);
    }

    /**
     * <p>
     * Uses the injected mdwMessageProducer to send the message or defaults
     * back to InternalEventDriver
     * </p>
     * @param worker
     * @param messageId
     * @param eventName
     * @param eventMessage
     * @param checkAvailableThreads
     * @throws JMSException
     */
    public boolean sendInternalMessage(String worker, final String messageId, String eventName,
            final String eventMessage, boolean checkAvailableThreads) throws JMSException {
        // Spring Injection
        if (mdwMessageProducer != null) {
            mdwMessageProducer.sendMessage(eventMessage, messageId);

        }
        else {
            InternalEventDriver command = new InternalEventDriver(messageId, eventMessage);
            ThreadPoolProvider thread_pool = ApplicationContext.getThreadPoolProvider();
            if (checkAvailableThreads && !thread_pool.execute(worker, eventName, command)) {
                String msg = worker + " has no thread available for event: " + eventName
                        + " message:\n" + eventMessage;
                // make this stand out
                logger.warnException(msg, new Exception(msg));
                logger.info(thread_pool.currentStatus());
                return false;
            }
            else if (!checkAvailableThreads)
                thread_pool.execute(worker, eventName, command);

        }
        return true;
    }

}
