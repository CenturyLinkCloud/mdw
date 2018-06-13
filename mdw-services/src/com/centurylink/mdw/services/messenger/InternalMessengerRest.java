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
package com.centurylink.mdw.services.messenger;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.system.Server;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class InternalMessengerRest extends InternalMessenger {

    private String serviceContext;

    public InternalMessengerRest(String serviceContext) {
        this.serviceContext = serviceContext;
    }

    private void sendMessageSub(InternalEvent msg, String msgid) throws IOException {
        HttpHelper httpHelper = new HttpHelper(new URL(ApplicationContext.getLocalServiceAccessUrl()));
        HashMap<String,String> headers = new HashMap<String,String>();
        headers.put("MDWInternalMessageId", msgid);
        httpHelper.setHeaders(headers);
        httpHelper.post(msg.toXml());
    }

    public void sendMessage(InternalEvent msg, EngineDataAccess edao)
        throws ProcessException
    {
        try {
            String msgid = addMessage(msg, edao);
            if (msgid==null) return;    // cached
            sendMessageSub(msg, msgid);
        } catch (Exception e) {
            throw new ProcessException(-1, "Failed to send internal event", e);
        }
    }

    public void sendDelayedMessage(InternalEvent msg, int delaySeconds, String msgid, boolean isUpdate,
            EngineDataAccess edao) throws ProcessException
    {
        if (delaySeconds<=0) {
            try {
                addMessageNoCaching(msg, edao, msgid);
                sendMessageSub(msg, msgid);
            } catch (Exception e) {
                throw new ProcessException(-1, "Failed to send internal event", e);
            }
        } else {
            try {
                ScheduledEventQueue eventQueue = ScheduledEventQueue.getSingleton();
                Date time = new Date(DatabaseAccess.getCurrentTime()+delaySeconds*1000L);
                if (isUpdate) eventQueue.rescheduleInternalEvent(msgid, time, msg.toXml());
                else eventQueue.scheduleInternalEvent(msgid, time, msg.toXml(), null);
            } catch (Exception e) {
                throw new ProcessException(-1, "Failed to send internal event", e);
            }
        }
    }

    public void broadcastMessage(String msg) throws ProcessException {
        for (Server server : ApplicationContext.getServerList().getServers()) {
            String serviceUrl = "http://" + server + serviceContext + "/Services/REST";
            try {
                HttpHelper httpHelper = new HttpHelper(new URL(serviceUrl));
                HashMap<String,String> headers = new HashMap<String,String>();
                headers.put("MDWBroadcast", "true");
                httpHelper.setHeaders(headers);
                httpHelper.post(msg);
            } catch (Exception e) {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.severeException("Failed to broadcast to server " + server, e);
            }
        }
    }
}
