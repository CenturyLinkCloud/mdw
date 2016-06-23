/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.messenger;

import java.rmi.RemoteException;
import java.util.Date;

import javax.naming.NamingException;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.listener.RMIListener;
import com.centurylink.mdw.model.value.event.InternalEventVO;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.dao.process.EngineDataAccess;
import com.centurylink.mdw.services.event.ScheduledEventQueue;

public class InternalMessengerRmi extends InternalMessenger {

    private void sendMessageSub(InternalEventVO msg, String msgid) throws NamingException, RemoteException {
        RMIListener server = (RMIListener)ApplicationContext.getNamingProvider()
            .lookup(null, RMIListener.JNDI_NAME, RMIListener.class);
        server.invoke(msgid, msg.toXml());
    }


    public void sendMessage(InternalEventVO msg, EngineDataAccess edao) throws ProcessException {
        try {
            String msgid = addMessage(msg, edao);
            if (msgid==null) return;    // cached
            sendMessageSub(msg, msgid);
        } catch (Exception e) {
            throw new ProcessException(-1, "Failed to send internal event", e);
        }
    }

    public void sendDelayedMessage(InternalEventVO msg, int delaySeconds, String msgid, boolean isUpdate, EngineDataAccess edao)
    throws ProcessException    {
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
        for (String hostport : ApplicationContext.getManagedServerList()) {
            try {
                RMIListener server = (RMIListener)ApplicationContext.getNamingProvider().
                    lookup(hostport, RMIListener.JNDI_NAME, RMIListener.class);
                server.invoke(null, RMIListener.BROADCAST_MARKER + msg);
            } catch (Exception e) {
                StandardLogger logger = LoggerUtil.getStandardLogger();
                logger.severeException("Failed to broadcast to server " + hostport, e);
            }
        }
    }
}
