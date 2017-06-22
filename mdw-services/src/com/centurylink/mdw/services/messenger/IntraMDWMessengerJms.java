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


import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.util.JMSServices;

public class IntraMDWMessengerJms extends IntraMDWMessenger {

    protected IntraMDWMessengerJms(String destination) {
        super(destination);
    }

    /**
     * jndi can be null, in which case send to the same site
     */
    public void sendMessage(String message)
    throws ProcessException {
        try {
            JMSServices.getInstance().sendTextMessage(destination,
                    JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE, message, 0, null);
        } catch (Exception e) {
            throw new ProcessException(-1, e.getMessage(), e);
        }

    }

    /**
     * jndi can be null, in which case send to the same site
     */
    public String invoke(String message, int timeoutSeconds)
    throws ProcessException {
        try {
            return JMSServices.getInstance().invoke(destination,
                    JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE, message, timeoutSeconds);
        } catch (Exception e) {
            throw new ProcessException(-1, e.getMessage(), e);
        }
    }

    public void sendCertifiedMessage(String message, String msgid, int ackTimeoutSeconds)
        throws ProcessException,AdapterException {
        String acknowledgment;
        try {
            acknowledgment = JMSServices.getInstance().invoke(destination,
                    JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE,
                    message, ackTimeoutSeconds, msgid);
        } catch (Exception e) {
            throw new ProcessException(0, "Failed to send certified message", e);
        }
        if (!msgid.equals(acknowledgment)) throw new AdapterException("Incorrect acknowledgment for certified message");
    }

}
