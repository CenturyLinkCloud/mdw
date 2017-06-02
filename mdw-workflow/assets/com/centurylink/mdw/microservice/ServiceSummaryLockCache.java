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
package com.centurylink.mdw.microservice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.provider.CacheService;

/**
 * TODO: remove old locks to prevent unlimited object accumulation
 */
@RegisteredService(CacheService.class)
public class ServiceSummaryLockCache implements CacheService {

    private static volatile Map<String,ReentrantLock> masterRequestLocks = Collections.synchronizedMap(new HashMap<String,ReentrantLock>());

    public void refreshCache() throws Exception {
        clearCache();
    }

    public void clearCache() {
        synchronized (masterRequestLocks) {
            masterRequestLocks.clear();
        }
    }

    public static void lock(String masterRequestId) {
        getLock(masterRequestId).lock();
    }

    public static ReentrantLock getLock(String masterRequestId) {
        ReentrantLock lock = masterRequestLocks.get(masterRequestId);
        if (lock == null) {
            synchronized (masterRequestLocks) {
                lock = masterRequestLocks.get(masterRequestId);
                if (lock == null) {
                    lock = new ReentrantLock();
                    masterRequestLocks.put(masterRequestId, lock);
                }
            }
        }
        return lock;
    }

    public static void unlock(String masterRequestId) {
        ReentrantLock lock = masterRequestLocks.get(masterRequestId);
        if (lock == null) {
            synchronized (masterRequestLocks) {
                lock = masterRequestLocks.get(masterRequestId);
            }
        }

        if (lock != null)
            lock.unlock();
    }

    public static ReentrantLock removeLock(String masterRequestId) {
        synchronized (masterRequestLocks) {
            return masterRequestLocks.remove(masterRequestId);
        }
    }

}
