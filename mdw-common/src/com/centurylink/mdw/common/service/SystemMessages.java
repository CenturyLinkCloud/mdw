/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.common.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.model.system.Bulletin;
import com.centurylink.mdw.model.system.SystemMessage.Level;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * TODO: regular SystemMessages as well as Bulletins.
 */
public class SystemMessages {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Map<String,Bulletin> bulletins = new HashMap<>();

    public static Map<String,Bulletin> getBulletins() {
        return bulletins;
    }

    public static Bulletin bulletinOn(String message) {
        return bulletinOn(Level.Info, message);
    }

    public static Bulletin bulletinOn(Level level, String message) {
        return bulletinOn(new Bulletin(level, message));
    }

    public static Bulletin bulletinOn(Bulletin bulletin) {
        synchronized (bulletins) {
            bulletins.put(bulletin.getId(), bulletin);
        }
        try {
            WebSocketMessenger.getInstance().send("SystemMessage", bulletin.getJson().toString());
            return bulletin;
        }
        catch (IOException ex) {
            logger.warnException("Unable to publish to websocket", ex);
            return null;
        }
    }

    /**
     * Should be the same instance or have the same id as the bulletinOn message.
     */
    public static void bulletinOff(Bulletin bulletin) {
        bulletinOff(bulletin, Level.Info, null);
    }

    public static void bulletinOff(Bulletin bulletin, String message) {
        bulletinOff(bulletin, Level.Info, message);
    }

    /**
     * Should be the same instance or have the same id as the bulletinOn message.
     */
    public static void bulletinOff(Bulletin bulletin, Level level, String message) {
        if (bulletin == null)
            return;
        synchronized (bulletins) {
            bulletins.remove(bulletin.getId(), bulletin);
        }
        try {
            WebSocketMessenger.getInstance().send("SystemMessage", bulletin.off(message).getJson().toString());
        }
        catch (IOException ex) {
            logger.warnException("Unable to publish to websocket", ex);
        }
    }

}
