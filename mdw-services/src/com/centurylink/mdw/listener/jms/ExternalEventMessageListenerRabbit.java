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

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.util.MessageProducer;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/*
 * Copyright (c) 2011 CenturyLink, Inc. All Rights Reserved.
 */


/**
 * Replaces ExternalEventListener for Spring
 */
public class ExternalEventMessageListenerRabbit  implements MessageListener {

    @Autowired
    @Qualifier(SpringAppContext.MDW_SPRING_MESSAGE_PRODUCER)
    private MessageProducer mdwMessageProducer;

    public ExternalEventMessageListenerRabbit() {
        super();
    }

    /*
         * <p>
         * Calls the ListenerHelper to process the event
         * and sends back a message to the reply queue
         * </p>
         *

     * @see org.springframework.amqp.core.MessageListener#onMessage(org.springframework.amqp.core.Message)
     */
    @Override
        public void onMessage(org.springframework.amqp.core.Message message) {

            StandardLogger logger = LoggerUtil.getStandardLogger();
            try {
                String txt = new String(message.getBody());
               if (logger.isDebugEnabled()) {
                    logger.debug("JMS Rabbit ExternalEvent Listener receives request: " + txt);
                }
                String resp;
                ListenerHelper helper = new ListenerHelper();
                Map<String, String> metaInfo = new HashMap<String, String>();
                metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_JMS);
                metaInfo.put(Listener.METAINFO_REQUEST_PATH, message.getMessageProperties().getConsumerQueue());
                metaInfo.put(Listener.METAINFO_SERVICE_CLASS, this.getClass().getName());
                metaInfo.put(Listener.METAINFO_REQUEST_ID, message.getMessageProperties().getMessageId());
                MessageProperties props = message.getMessageProperties();
                if (props != null && props.getCorrelationId() !=null) {
                    metaInfo.put(Listener.METAINFO_CORRELATION_ID, new String(props.getCorrelationId()));
                }
                    if (props!=null && props.getReplyTo() != null)
                    metaInfo.put("ReplyTo", message.getMessageProperties().getReplyTo());

                resp = helper.processEvent(txt, metaInfo);
                String correlId = null;
                if  (props.getCorrelationId() !=null) correlId = new String(message.getMessageProperties().getCorrelationId());

                String respQueue =  props==null || props.getReplyTo()==null ? null:props.getReplyTo();
                if (resp != null && respQueue != null) {
                    mdwMessageProducer.sendMessage(resp, respQueue, correlId);
                    if (logger.isDebugEnabled()) {
                        logger.debug("JMS Rabbit ExternalEvent Listener sends response (corr id='" + correlId + "'): "
                                + resp);
                    }

                }

            }
            catch (Throwable ex) {
                logger.severeException(ex.getMessage(), ex);
            }

        }




}