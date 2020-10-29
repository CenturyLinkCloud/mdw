package com.centurylink.mdw.services.event;

/*
 * Copyright (c) 2011 CenturyLink, Inc. All Rights Reserved.
 */

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.task.WaitingForMe;
import com.centurylink.mdw.util.log.LoggerUtil;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class BroadcastHelper  {

    public void processBroadcastMessage(String message)
        throws JSONException, MdwException
    {
        JSONObject json = new JsonObject(message);
        String action = json.getString("ACTION");
        if (action.equals("NOTIFY")) {
            String correlationId = json.getString("CORRELATION_ID");
            WaitingForMe waiter = WaitingForMe.getWaitOn(correlationId);
            if (waiter != null)
                waiter.notifyEvent(json.getString("MESSAGE"));
        } else if (action.equals("REFRESH_PROPERTY")) {
            String name = json.getString("NAME");
            String value = json.getString("VALUE");
            PropertyManager propMgr = PropertyManager.getInstance();
            propMgr.setStringProperty(name, StringUtils.isBlank(value)?null:value);
            LoggerUtil.getStandardLogger().refreshWatcher();
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
            try {
                String eventName = json.getString("EVENT_NAME");
                String thisServer = ApplicationContext.getServer().toString();
                String fromServer = json.getString("FROM");
                if (!thisServer.equals(fromServer)) {
                    ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
                    queue.invalidate(eventName);
                }
            } catch (Exception ex) {
                throw new MdwException(ex.getMessage(), ex);
            }
        } else throw new MdwException("Unknown ACTION");

    }

}