/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.messenger;

import java.util.Date;

import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.event.BroadcastHelper;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.services.process.EventServices;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class InternalMessengerSameServer extends InternalMessenger {


    public InternalMessengerSameServer() {
    }

	public void sendMessage(InternalEvent msg, EngineDataAccess edao)
		throws ProcessException
	{
		try {
			String msgid = addMessage(msg, edao);
			if (msgid==null) return;	// cached
            if (!EventServices.getInstance().sendInternalMessageCheck(ThreadPoolProvider.WORKER_LISTENER, msgid, "InternalMessengerSingleServer", msg.toXml()))
                throw new Exception("No thread available to send internal message");
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
                if (!EventServices.getInstance().sendInternalMessageCheck(ThreadPoolProvider.WORKER_LISTENER, msgid, "InternalMessengerSingleServer", msg.toXml()))
                    throw new Exception("No thread available to send internal message");
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
		try {
			BroadcastHelper broadcastHelper = new BroadcastHelper();
			broadcastHelper.processBroadcastMessage(msg);
		} catch (Exception e) {
			StandardLogger logger = LoggerUtil.getStandardLogger();
			logger.severeException("Failed to process broadcast", e);
		}
	}
}
