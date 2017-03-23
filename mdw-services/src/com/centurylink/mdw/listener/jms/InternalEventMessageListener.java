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

import javax.jms.Message;
import javax.jms.TextMessage;

import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Spring Replacement for InternalEventListener
 */
public class InternalEventMessageListener implements javax.jms.MessageListener{

    public InternalEventMessageListener() {
        super();
    }


    /* (non-Javadoc)
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    @Override
    public void onMessage(Message message) {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        String logtag = "EngineDriver.T" + Thread.currentThread().getId() + " - ";

        try {
            String txt = ((TextMessage) message).getText();
            if (logger.isDebugEnabled()) {
                logger.debug("JMS Spring InternalEvent Listener receives request: " + txt);
            }
            logger.info(logtag + "starts processing");
            String messageId = message.getJMSCorrelationID();
            ProcessEngineDriver driver = new ProcessEngineDriver();
            driver.processEvents(messageId, txt);
        } catch (Throwable e) { // only possible when failed to get ProcessManager ejb
            logger.severeException(logtag + "process exception " + e.getMessage(), e);
        }

    }

}