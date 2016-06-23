/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugins.tomcat;

import java.lang.reflect.Method;
import java.rmi.Remote;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PaaSConstants;
import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.container.plugins.MdwTransactionManager;
import com.centurylink.mdw.container.plugins.activemq.ActiveMqJms;


public class TomcatNaming implements NamingProvider {

    private boolean useMdwTransactionManager;

    public TomcatNaming() {
        String txMgr = System.getProperty(TRANSACTION_MANAGER_SYSTEM_PROPERTY);
        if (txMgr == null || txMgr.equals(MdwTransactionManager.class.getName()))
            useMdwTransactionManager = true;
    }

    public String qualifyServiceBeanName(String name) {
        return name;
    }

    public String qualifyJmsQueueName(String name) {
        return name;  // no qualification necessary
    }

    public String qualifyJmsTopicName(String name) {
        return name; // no qualification necessary
    }

    public String getTransactionManagerName() {
        return JAVA_TRANSACTION_MANAGER;	// not really used
    }

    public String getUserTransactionName() {
        return JAVA_USER_TRANSACTION;    // not really used
    }

    public int getServerPort() throws Exception {
        // Check for Cloud info first
        if (PaaSConstants.PAAS_INSTANCE_PORT != null ) return Integer.parseInt(PaaSConstants.PAAS_INSTANCE_PORT);

        MBeanServer mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
        ObjectName name = new ObjectName("Catalina", "type", "Server");
        Object server = mBeanServer.getAttribute(name, "managedResource");

        Method findServices = server.getClass().getMethod("findServices", (Class<?>[])null);
        Object[] services = (Object[])findServices.invoke(server, (Object[])null);
        for (Object service : services) {
            Method findConnectors = service.getClass().getMethod("findConnectors", (Class<?>[])null);
            Object[] connectors = (Object[])findConnectors.invoke(service, (Object[])null);
            for (Object connector : connectors) {
                Method getProtocolHandler = connector.getClass().getMethod("getProtocolHandler", (Class<?>[])null);
                Object protocolHandler = getProtocolHandler.invoke(connector, (Object[])null);
                if (protocolHandler != null && protocolHandler.getClass().getSimpleName().startsWith("Http")) {
                    Method getPort = connector.getClass().getMethod("getPort", (Class<?>[])null);
                    Integer portObj = (Integer) getPort.invoke(connector, (Object[])null);
                    if (portObj.intValue() > 0)
                        return portObj.intValue(); // return first match
                }
            }
        }
        return 0;
    }

    public Object lookup(String hostPort, String name, Class<?> cls) throws NamingException {

    	if (cls.getName().equals("javax.transaction.TransactionManager") && useMdwTransactionManager) {
    		return MdwTransactionManager.getInstance();
    	}
    	else if (cls.getName().equals("javax.jms.Topic")) {
            JmsProvider jmsProvider = ApplicationContext.getJmsProvider();
            if (!(jmsProvider instanceof ActiveMqJms))
                throw new NamingException("Unsupported JMS Provider: " + jmsProvider);
            ActiveMqJms activeMqJms = (ActiveMqJms) jmsProvider;
            return activeMqJms.getTopic(name);
        }

        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            return envCtx.lookup(name);
        }
        catch (Exception e) {
            NamingException ne = new NamingException("Failed to look up " + name);
            ne.initCause(e);
            throw ne;
        }
    }

    public void bind(String name, Remote object) throws NamingException {
    }
    public void unbind(String name) throws NamingException {
    }
}
