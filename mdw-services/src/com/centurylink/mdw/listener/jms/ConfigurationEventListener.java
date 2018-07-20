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
import javax.jms.TextMessage;

import com.centurylink.mdw.bpm.ConfigurationChangeRequestDocument;
import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.services.ConfigurationHelper;
import com.centurylink.mdw.services.event.BroadcastHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ConfigurationEventListener extends JmsListener {

    public ConfigurationEventListener() {
        super("ConfigurationEventListener",
                JMSDestinationNames.CONFIG_HANDLER_TOPIC, null);
    }

    protected Runnable getProcesser(TextMessage message) throws JMSException {
        return new ErrorQueueDriver(message.getText());
    }

    private class ErrorQueueDriver implements Runnable {

        private String eventMessage;

        public ErrorQueueDriver(String eventMessage) {
            this.eventMessage = eventMessage;
        }

        public void run() {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            try {
                if (eventMessage.startsWith("{")) {
                    BroadcastHelper helper = new BroadcastHelper();
                    helper.processBroadcastMessage(eventMessage);
                    logger.info("Received and processed broadcast: " + eventMessage);
                } else {
                    boolean status = false;
                    ConfigurationChangeRequestDocument doc
                        = ConfigurationChangeRequestDocument.Factory.parse(eventMessage);
                    String fileName = doc.getConfigurationChangeRequest().getFileName();

                    status = ConfigurationHelper.applyConfigChange(fileName, doc
                            .getConfigurationChangeRequest().getFileContents(), doc
                            .getConfigurationChangeRequest().getReactToChange());
                    if (status) {
                        logger.info(fileName + " has been successfully modified.");
                    }
                    else {
                        logger.info(fileName + " update FAILED.");
                    }
                }
            }
            catch (JMSException ex) {
                logger.severeException(ex.getMessage(), ex);
            } catch (Exception e) {
                logger.severeException("Failed to process broadcast message", e);
            }
        }

    }

}