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
package com.centurylink.mdw.services.event;

/*
 * Copyright (c) 2011 CenturyLink, Inc. All Rights Reserved.
 */

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.pooling.AdapterConnectionPool;
import com.centurylink.mdw.services.pooling.ConnectionPoolRegistration;
import com.centurylink.mdw.services.task.WaitingForMe;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;

public class BroadcastHelper  {

    public void processBroadcastMessage(String message)
        throws JSONException, MdwException
    {
        JSONObject json = new JsonObject(message);
        String action = json.getString("ACTION");
        if (action.equals("NOTIFY")) {
            String correlationId = json.getString("CORRELATION_ID");
            WaitingForMe waiter = WaitingForMe.getWaitOn(correlationId);
            if (waiter!=null) waiter.notifyEvent(json.getString("MESSAGE"));
        } else if (action.equals("REFRESH_PROPERTY")) {
            String name = json.getString("NAME");
            String value = json.getString("VALUE");
            PropertyManager propMgr = PropertyManager.getInstance();
            propMgr.setStringProperty(name, StringHelper.isEmpty(value)?null:value);
            LoggerUtil.getStandardLogger().refreshCache();
        } else if (action.equals("REFRESH_CACHES")) {
            CacheRegistration cacheRegister = new CacheRegistration();
            if (json.has("CACHE_NAMES")) {
                String cacheNames = json.getString("CACHE_NAMES");
                String[] caches = cacheNames.split(",");
                for (String c : caches) {
                    cacheRegister.refreshCache(c);
                }
            } else {
                new CacheRegistration().refreshCaches();
            }
        } else if (action.equals("INVALIDATE_EVENT")) {
            String eventName = json.getString("EVENT_NAME");
            String thisServer = ApplicationContext.getServer().toString();
            String fromServer = json.getString("FROM");
            if (!thisServer.equals(fromServer)) {
                ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
                queue.invalidate(eventName);
            }
        } else if (action.equals("ADAPTER_POOL_STATUS")) {
            String poolName = json.getString("POOL_NAME");
            String status = json.getString("STATUS");
            AdapterConnectionPool pool = ConnectionPoolRegistration.getPool(poolName);
            pool.processPoolStatusBroadcast(status);
        } else throw new MdwException("Unknown ACTION");

    }

}