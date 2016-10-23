/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.messenger;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.process.ProcessExecutor;

public abstract class InternalMessenger {
    
    public static final int CACHE_OFF = 0;
    public static final int CACHE_ON = 1;
    public static final int CACHE_ONLY = 2;
    
    private Queue<InternalEvent> messageQueue;
    private int cacheOption;
    
    public InternalMessenger() {
        this.cacheOption = CACHE_OFF;
        messageQueue = null;
    }
    
    /**
     * 
     * @param msg
     * @return null if the message is cached; message ID if not cached
     * @throws SQLException 
     */
    protected String addMessage(InternalEvent msg, EngineDataAccess edao) throws SQLException {
        if (cacheOption==CACHE_ONLY) {
            synchronized (messageQueue) {
                messageQueue.add(msg);
            }
            return null;
        } else if (cacheOption==CACHE_ON) {
            String msgid = generateMessageId(msg);
            edao.persistInternalEvent(msgid, msg.toXml());
            msg.setMessageId(msgid);
            synchronized (messageQueue) {
                messageQueue.add(msg);
            }
            return null;
        } else {    // CACHE_OFF
            String msgid = generateMessageId(msg);
            edao.persistInternalEvent(msgid, msg.toXml());
            return msgid;
        }
    }
    
    /*
     * for InternalMessengerJms short delay only
     */
    protected String addMessageNoCaching(InternalEvent msg, EngineDataAccess edao, String msgid) throws SQLException {
        if (cacheOption==CACHE_ONLY) {
            return null;
        } else {    // CACHE_ON/CACHE_OFF
            edao.persistInternalEvent(msgid, msg.toXml());
            return msgid;
        }
    }

    public void setCacheOption(int v) {
        this.cacheOption = v;
        if (messageQueue==null) messageQueue = new LinkedList<InternalEvent>();
    }
    
    abstract public void sendMessage(InternalEvent msg, EngineDataAccess edao)
    throws ProcessException;

    abstract public void sendDelayedMessage(InternalEvent msg, int delaySeconds, 
            String msgid, boolean isUpdate, EngineDataAccess edao)
    throws ProcessException;
    
    abstract public void broadcastMessage(String msg)
    throws ProcessException;
    
    public InternalEvent getNextMessageFromQueue(ProcessExecutor engine)
    throws DataAccessException {
        if (messageQueue==null) return null;
        synchronized (messageQueue) {
            if (messageQueue.isEmpty()) return null;
            InternalEvent nextMessage = messageQueue.remove();
            while (nextMessage!=null) {
                String msgid = nextMessage.getMessageId();
                if (msgid==null || engine.deleteInternalEvent(msgid)) break;
                nextMessage = messageQueue.remove();
            }
            return nextMessage;
        }
    }
    
    private String generateMessageId(InternalEvent msg) throws SQLException {
        // ServerName:serverStartTime:memory_sequence_number
        String msgid;
        if (msg.isProcess()) {
            if (msg.getEventType().equals(EventType.START)) {
                msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + "process." + msg.getOwnerId() + "start" + msg.getWorkId();
                // only the following 2 places really send process start message, so the id should be unique
                // 1. EventManagerBean:launchProcess
                // 2. InvokeSubProcess.execute (only when calling asynchronously)
            } else {
                msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + "process." + msg.getWorkInstanceId() + "." + msg.getEventType();                
            }
        } else {
            if (msg.getEventType().equals(EventType.START)) {
                msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + msg.getOwnerId() + "start" + msg.getWorkId();
                if (msg.getTransitionInstanceId()!=null) msgid += "by" + msg.getTransitionInstanceId();
            } else {
                msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + msg.getWorkInstanceId() + "." + msg.getEventType();
            }
        }
        return msgid;
    }
    
}
