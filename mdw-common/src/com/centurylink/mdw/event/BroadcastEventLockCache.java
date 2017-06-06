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
package com.centurylink.mdw.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.centurylink.mdw.provider.CacheService;

/**
 * TODO: remove old locks to prevent unlimited object accumulation
 */
public class BroadcastEventLockCache implements CacheService {

    private static volatile Map<String,ReentrantLock> eventLocks = Collections.synchronizedMap(new HashMap<String,ReentrantLock>());

    public void refreshCache() throws Exception {
        clearCache();
    }

    public void clearCache() {
        synchronized (eventLocks) {
            eventLocks.clear();
        }
    }

    public static void lock(String eventName) {
        getLock(eventName).lock();
    }

    public static ReentrantLock getLock(String eventName) {
        ReentrantLock lock = eventLocks.get(eventName);
        if (lock == null) {
            synchronized (eventLocks) {
                lock = eventLocks.get(eventName);
                if (lock == null) {
                    lock = new ReentrantLock();
                    eventLocks.put(eventName, lock);
                }
            }
        }
        return lock;
    }

    public static void unlock(String eventName) {
        ReentrantLock lock = eventLocks.get(eventName);
        if (lock == null) {
            synchronized (eventLocks) {
                lock = eventLocks.get(eventName);
            }
        }

        if (lock != null)
            lock.unlock();
    }

    public static ReentrantLock removeLock(String eventName) {
        synchronized (eventLocks) {
            return eventLocks.remove(eventName);
        }
    }

}
