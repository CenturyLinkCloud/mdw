/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.messenger;

import javax.naming.NamingException;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.ApplicationConstants;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class MessengerFactory {

    private static final String JMS = "jms";
    private static final String RMI = "rmi";
    private static final String HTTP = "http";
    private static final String SAME_SERVER = "same_server";

    private static String internalMessenger;
    private static String serviceContext;

    public static void init(String restServiceContext) {
        serviceContext = restServiceContext;
        StandardLogger logger = LoggerUtil.getStandardLogger();
        String v = PropertyManager.getProperty(PropertyNames.MDW_CONTAINER_MESSENGER);
        if (JMS.equalsIgnoreCase(v)) internalMessenger = JMS;
        else if (RMI.equalsIgnoreCase(v)) internalMessenger = RMI;
        else if (HTTP.equalsIgnoreCase(v)) internalMessenger = HTTP;
        else if (SAME_SERVER.equalsIgnoreCase(v)) internalMessenger = SAME_SERVER;
        else internalMessenger  = (ApplicationContext.getJmsProvider()==null)?HTTP:JMS;
        logger.info("Internal Messenger: " + internalMessenger);
    }

    public static InternalMessenger newInternalMessenger() {
        if (internalMessenger.equals(RMI)) return new InternalMessengerRmi();
        else if (internalMessenger.equals(HTTP)) return new InternalMessengerRest(serviceContext);
        else if (internalMessenger.equals(JMS)) return new InternalMessengerJms();
        else return new InternalMessengerSameServer();
    }

    /**
     * Server specification can be one of the following forms
     *   a) URL of the form t3://host:port, iiop://host:port, rmi://host:port, http://host:port/context_and_path
     *   b) a logical server name, in which case the URL is obtained from property mdw.remote.server.<server-name>
     *   c) server_name@URL
     *   d) null, in which case it indicates on the same site (same domain)
     * @param serverSpec see description above
     * @return the messenger for the corresponding server
     */
    public static IntraMDWMessenger newIntraMDWMessenger(String serverSpec)
        throws NamingException {
        if (serverSpec==null || serverSpec.equals(ApplicationConstants.THIS_APPLICATION)) {
            if (internalMessenger.equals(RMI))
                return new IntraMDWMessengerRmi(null);
            else if (internalMessenger.equals(JMS))
                return new IntraMDWMessengerJms(null);
            else return new IntraMDWMessengerRest(null, serviceContext);
        } else {
            String url = getEngineUrl(serverSpec);
            int k = url.indexOf("://");
            if (k<=0) throw new NamingException("Incorrect engine URL for " + serverSpec);
            String scheme = url.substring(0,k);
            if (scheme.equals("iiop") || scheme.equals("rmi"))
                return new IntraMDWMessengerRmi(url);
            if (scheme.equals("http") || scheme.equals("https"))
                return new IntraMDWMessengerRest(url, serviceContext);
            return new IntraMDWMessengerJms(url);
        }
    }

    public static boolean internalMessageUsingJms() {
        return internalMessenger.equals(JMS);
    }

    /**
     * Returns URL for this engine. Used to pass it to remove servers:
     *   a) when invoking a remote process
     *   b) when sending message to remote detail task manager
     *   c) for central task manager to generate a unique session ID when linking to detail page
     * For WebLogic: t3://host:port (works for both JMS for RMI. iiop may work as well for RMI)
     * For Tomcat: rmi://host:port
     */
    public static String getEngineUrl() throws NamingException {
        return ApplicationContext.getMdwHubUrl() + "/services";
    }

    /**
     * Returns engine URL for another server, using server specification as input.
     *
     * Server specification can be one of the following forms
     *   a) URL of the form t3://host:port, iiop://host:port, rmi://host:port, http://host:port/context_and_path
     *   b) a logical server name, in which case the URL is obtained from property mdw.remote.server.<server-name>
     *   c) server_name@URL
     *
     * @param serverSpec
     * @return URL for the engine
     * @throws NamingException
     */

    public static String getEngineUrl(String serverSpec) throws NamingException {
        String url;
        int at = serverSpec.indexOf('@');
        if (at>0) {
            url = serverSpec.substring(at+1);
        } else {
            int colonDashDash = serverSpec.indexOf("://");
            if (colonDashDash>0) {
                url = serverSpec;
            } else {
                url = PropertyManager.getProperty(PropertyNames.MDW_REMOTE_SERVER + "." + serverSpec);
                if (url==null) throw new NamingException("Cannot find engine URL for " + serverSpec);
            }
        }
        return url;
    }

}
