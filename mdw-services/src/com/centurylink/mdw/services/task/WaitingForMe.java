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
