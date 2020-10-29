package com.centurylink.mdw.services.messenger;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class MessengerFactory {

    private static final String JMS = "jms";
    private static final String HTTP = "http";
    private static final String SAME_SERVER = "same_server";

    private static String internalMessenger;

    public static void init() {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        String v = PropertyManager.getProperty(PropertyNames.MDW_CONTAINER_MESSENGER);
        if (JMS.equalsIgnoreCase(v))
            internalMessenger = JMS;
        else if (HTTP.equalsIgnoreCase(v))
            internalMessenger = HTTP;
        else if (SAME_SERVER.equalsIgnoreCase(v))
            internalMessenger = SAME_SERVER;
        else
            internalMessenger = (ApplicationContext.getJmsProvider() == null) ? HTTP : JMS;
        logger.info("Internal Messenger: " + internalMessenger);
    }

    public static InternalMessenger newInternalMessenger() {
        if (internalMessenger.equals(HTTP))
            return new InternalMessengerRest();
        else if (internalMessenger.equals(JMS))
            return new InternalMessengerJms();
        else
            return new InternalMessengerSameServer();
    }

    public static boolean internalMessageUsingJms() {
        return internalMessenger.equals(JMS);
    }
}
