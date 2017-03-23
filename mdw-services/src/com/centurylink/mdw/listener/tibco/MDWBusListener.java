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
package com.centurylink.mdw.listener.tibco;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.qwest.bus.BusMessage;
import com.qwest.bus.responder.BusProcessor;

public class MDWBusListener extends BusProcessor {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    public MDWBusListener(){
        super();
    }

    @Override
    public BusMessage process(BusMessage busMsg) {
        ListenerHelper helper = new ListenerHelper();
        Map<String,String> metaInfo = new HashMap<String,String>();
        metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_BUS);
        metaInfo.put(Listener.METAINFO_SERVICE_CLASS, this.getClass().getName());
        metaInfo.put(Listener.METAINFO_REQUEST_ID, busMsg.getIdentifier());
        metaInfo.put("Topic", busMsg.getTopic());
        metaInfo.put("ReplyToTopic", busMsg.getReplyToTopic());
        String req = busMsg.getString();
        if (logger.isDebugEnabled()) logger.debug("BUS receives message: " + req);
        String resp = helper.processEvent(req, metaInfo);
        BusMessage respMsg = prepareResponse(busMsg, resp);
        if (logger.isDebugEnabled()) {
            if (this.getStatusCapsule() != null) {
                logger.debug("BUS queue Length: " + this.queue.getQueueSize());
            }
            if (respMsg!=null) logger.debug("BUS sends response: " + resp);
        }
        return respMsg;
    }
    
    protected BusMessage prepareResponse(BusMessage pRequest, String pData) {
        BusMessage responseMsg = null;
        try {
            if (pRequest.getReplyToTopic() == null || pRequest.getReplyToTopic().length() == 0) {
                logger.warn("No ReplyTo topic in the Bus message. Unable to send response");
            } else {
                responseMsg = pRequest.createResponse();
                responseMsg.setReplyToTopic(responseMsg.getReplyToTopic());
                responseMsg.set(pData);
            }
        } catch (Exception e) {
            logger.severeException(e.getMessage(), e);
        }
        return responseMsg;
    }

}
