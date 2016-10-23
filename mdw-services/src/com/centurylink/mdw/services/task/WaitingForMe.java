/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.util.HashMap;
import java.util.Map;

public class WaitingForMe  {
    
    private static Map<String,WaitingForMe> waitingThreads 
        = new HashMap<String,WaitingForMe>();
    
    private long startTime;
    private boolean keepWaiting;
    private String eventContent;
    private String[] eventNames;
    
    public WaitingForMe(String eventName) {
        this.eventNames = new String[]{eventName};
    }
    
    public WaitingForMe(String[] eventNames) {
        this.eventNames = eventNames;
    }
    
    public static WaitingForMe getWaitOn(String eventName) {
        return waitingThreads.get(eventName);
    }
    
    public synchronized void notifyEvent(String eventContent) {
        keepWaiting = false;
        this.eventContent = eventContent;
        this.notify();
    }
    
    public synchronized String waitForEvent(int timeoutSeconds) {
        startTime = System.currentTimeMillis();
        keepWaiting = true;
        eventContent = null;
        for (String eventName : eventNames) {
            waitingThreads.put(eventName, this);
        }
        while (keepWaiting) {
            if (timeoutSeconds<=0) {
                try {
                    this.wait(0);
                } catch (InterruptedException e) {
                }
            } else {
                long curtime = System.currentTimeMillis();
                long towait = timeoutSeconds*1000+startTime - curtime;
                if (towait>0) {
                    try {
                        this.wait(towait);
                    } catch (InterruptedException e) {
                    }
                } else keepWaiting = false;
            }
        }
        for (String eventName : eventNames) {
            waitingThreads.remove(eventName);
        }
        return eventContent;
    }
    
}
