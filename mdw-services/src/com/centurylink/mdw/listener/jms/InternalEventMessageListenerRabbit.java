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