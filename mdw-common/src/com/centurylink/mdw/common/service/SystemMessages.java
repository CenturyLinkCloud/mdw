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
        if (level == Level.Info)
            logger.info(message);
        else if (level == Level.Error)
            logger.error(message);

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
            logger.warn("Unable to publish to websocket", ex);
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
            logger.warn("Unable to publish to websocket", ex);
        }
    }

}
