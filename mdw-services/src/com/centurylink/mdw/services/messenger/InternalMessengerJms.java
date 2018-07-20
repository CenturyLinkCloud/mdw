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

import java.util.Date;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.util.JMSServices;

public class InternalMessengerJms extends InternalMessenger {

    private static int threshold = -1;

    public void sendMessage(InternalEvent msg, EngineDataAccess edao)
        throws ProcessException
    {
        try {
            String msgid = addMessage(msg, edao);
            if (msgid==null) return;    // cached
            JMSServices.getInstance().sendTextMessage(null,
                    JMSDestinationNames.PROCESS_HANDLER_QUEUE, msg.toXml(), 0, msgid);
        } catch (Exception e) {
            throw new ProcessException(-1, "Failed to send internal event", e);
        }
    }

    @Override
    public void sendDelayedMessage(InternalEvent msg, int delaySeconds, String msgid, boolean isUpdate,
            EngineDataAccess edao) throws ProcessException {
        try {
            if (threshold<0) {
                threshold = 60*PropertyManager.getIntegerProperty(PropertyNames.MDW_TIMER_THRESHOLD_FOR_DELAY, 60);
            }
            if (delaySeconds>=threshold) {
                ScheduledEventQueue eventQueue = ScheduledEventQueue.getSingleton();
                Date time = new Date(DatabaseAccess.getCurrentTime()+delaySeconds*1000L);
                if (isUpdate) eventQueue.rescheduleInternalEvent(msgid, time, msg.toXml());
                else eventQueue.scheduleInternalEvent(msgid, time, msg.toXml(), null);
            } else {
                msgid = addMessageNoCaching(msg, edao, msgid);
                JMSServices.getInstance().sendTextMessage(null, JMSDestinationNames.PROCESS_HANDLER_QUEUE,
                        msg.toXml(), delaySeconds, msgid);
            }
        } catch (Exception e) {
            throw new ProcessException(-1, "Failed to send internal event", e);
        }
    }

    public void broadcastMessage(String msg) throws ProcessException {
        try {
            JMSServices.getInstance().broadcastTextMessage(JMSDestinationNames.CONFIG_HANDLER_TOPIC, msg, 0);
        } catch (Exception e) {
            throw new ProcessException(-1, "Failed to broadcast message", e);
        }
    }
}
