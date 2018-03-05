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
package com.centurylink.mdw.listener.rmi;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.listener.RMIListener;
import com.centurylink.mdw.services.event.BroadcastHelper;
import com.centurylink.mdw.services.process.EventServices;
import com.centurylink.mdw.services.util.AuthUtils;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class RMIListenerImpl implements RMIListener {

    public RMIListenerImpl(ThreadPoolProvider threadPool) {
    }

    public void login(String cuid, String pass)
    throws RemoteException {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        if (cuid != null) {
            if (pass!=null&&pass.length()>0) {
                try {
                    AuthUtils.ldapAuthenticate(cuid, pass);
                } catch (Exception e) {
                    String msg = e.getMessage();
                    logger.info("user fails authentication: " + msg);
                    if (msg.indexOf("error code 32")>0)
                        throw new RemoteException("User does not exist in MNET");
                    else if (msg.indexOf("error code 49")>0)
                        throw new RemoteException("Invalid password");
                    else throw new RemoteException(msg);
                }
            } // else direct entry from Task Manager
            logger.debug("user logs in");
        } else {
            logger.info("User name is not given for logging in");
            throw new RemoteException("User name and/or password is not given for logging in");
        }
    }

    public String invoke(String meta, String message) throws RemoteException {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        if (message.startsWith("<EventMessage ")) {
            try {
                if(!EventServices.getInstance().sendInternalMessageCheck(ThreadPoolProvider.WORKER_ENGINE, meta, "RMIListener", message))
                    throw new JMSException("No thread available to send internal message");
            }
            catch (JMSException e) {
                logger.severeException("Failed to send Internal Message ", e);
            }
             return null;
        } else if (message.startsWith(BROADCAST_MARKER)) {
            try {
                BroadcastHelper helper = new BroadcastHelper();
                message = message.substring(BROADCAST_MARKER.length());
                helper.processBroadcastMessage(message);
                logger.info("Received and processed broadcast: " + message);
            } catch (Exception e) {
                logger.severeException("Failed to process broadcast", e);
            }
            return null;
        } else {
            if (logger.isDebugEnabled())
                logger.debug("RMIListener receives: " + message);
            ListenerHelper helper = new ListenerHelper();
            String response = helper.processEvent(message, buildMetaInfo(meta));
            if (response!=null && logger.isDebugEnabled())
                logger.debug("RMIListener replies: " + response);
            return response;
        }
    }

    private Map<String,String> buildMetaInfo(String meta) {
        Map<String,String> metaInfo = new HashMap<String,String>();
        metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_RMI);
        return metaInfo;
    }

}
