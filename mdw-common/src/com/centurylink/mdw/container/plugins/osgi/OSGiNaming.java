/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugins.osgi;

import java.rmi.Remote;
import javax.naming.NamingException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.container.ContainerContextAware;
import com.centurylink.mdw.container.JmsProvider;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.container.plugins.MdwTransactionManager;
import com.centurylink.mdw.container.plugins.activemq.ActiveMqJms;

public class OSGiNaming implements NamingProvider, ContainerContextAware {

    public static final String MDW_COMMON_BUNDLE_NAME = "com.centurylink.mdw.common";

    private static volatile Object tranManager = null;
    private static volatile boolean transManagerInitialized = true;
    private boolean useMdwTransactionManager;

    public OSGiNaming() {
        String txMgr = System.getProperty(TRANSACTION_MANAGER_SYSTEM_PROPERTY);
        if (MdwTransactionManager.class.getName().equals(txMgr))
            useMdwTransactionManager = true;
        else
            transManagerInitialized = false;
    }

    private BundleContext bundleContext;
    public void setContainerContext(Object context) {
        this.bundleContext = (BundleContext) context;
    }

    public String qualifyServiceBeanName(String name) {
    	return name;  // no qualification necessary
    }

    public String qualifyJmsQueueName(String name) {
    	return name;
    }

    public String qualifyJmsTopicName(String name) {
    	return name;
    }

    public String getTransactionManagerName() {
    	return JAVA_TRANSACTION_MANAGER;
    }

    public String getUserTransactionName() {
    	return JAVA_USER_TRANSACTION;
    }

    private int serverPort;
    public int getServerPort() throws Exception {
        return serverPort;
    }
    public void setServerPort(int port) {
        this.serverPort = port;
    }

    // jndi not used in osgi container
	public Object lookup(String hostPort, String name, Class<?> cls) throws NamingException {
	    if (cls.getName().equals("javax.jms.Topic")) {
	        JmsProvider jmsProvider = ApplicationContext.getJmsProvider();
	        if (!(jmsProvider instanceof ActiveMqJms))
	            throw new NamingException("Unsupported JMS Provider: " + jmsProvider);
	        ActiveMqJms activeMqJms = (ActiveMqJms) jmsProvider;
	        return activeMqJms.getTopic(name);
	    } else if (cls.getName().equals(JAVA_TRANSACTION_MANAGER)) {
	        if (useMdwTransactionManager) {
	            return MdwTransactionManager.getInstance();
	        }
	        else {
	            if (!transManagerInitialized)
	                startup();

	            Object transManagerTemp = tranManager;
	            if (transManagerTemp == null) {
	                ServiceReference serviceRef = bundleContext.getServiceReference(JAVA_TRANSACTION_MANAGER);
	                if (serviceRef != null) {
	                    transManagerTemp = bundleContext.getService(serviceRef);
	                    if (transManagerTemp != null)
	                        tranManager = transManagerTemp;
	                }
	            }
                return transManagerTemp;
	        }
	    }
		return null;
	}

	public void bind(String name, Remote object) throws NamingException {
	}
	public void unbind(String name) throws NamingException {
	}

	public synchronized void startup() {
	    boolean transManagerInitializedtemp = transManagerInitialized;
	    if (!transManagerInitializedtemp) {
	        ServiceListener serviceListener = new ServiceListener() {
	            @SuppressWarnings("unchecked")
	            public void serviceChanged(ServiceEvent ev) {
	                ServiceReference serviceRef = ev.getServiceReference();
	                Object service = bundleContext.getService(serviceRef);
	                boolean registered = false;
	                if (tranManager != null)
	                    registered = true;


	                switch (ev.getType()) {
	                case ServiceEvent.REGISTERED: {
	                    if (!registered) {
	                        //        logger.info("Registering TransactionManager");
	                        tranManager = service;
	                    }
	                }
	                break;
	                case ServiceEvent.UNREGISTERING: {
	                    if (registered) {
	                        //         logger.info("Unregistering " + providerClass + " with unique alias '" + alias + "'");
	                        tranManager = null;
	                    }
	                }
	                break;
	                }
	            }
	        };

	        String filter = "(objectclass=" + JAVA_TRANSACTION_MANAGER + ")";
	        // notify previously started services
	        ServiceReference serviceRef = bundleContext.getServiceReference(JAVA_TRANSACTION_MANAGER);
	        if (serviceRef != null)
	            serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, serviceRef));

	        try {
	            bundleContext.addServiceListener(serviceListener, filter);
	            transManagerInitialized = true;
	        }
	        catch (InvalidSyntaxException e) {
	            LoggerUtil.getStandardLogger().severeException(e.getMessage(), e);
	        }
	    }
	}


}
