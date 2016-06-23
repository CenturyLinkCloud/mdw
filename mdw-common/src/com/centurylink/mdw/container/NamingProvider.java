/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container;

import java.rmi.Remote;

import javax.naming.NamingException;


/**
 * JNDI-naming provider
 */
public interface NamingProvider {

    public static final String TRANSACTION_MANAGER_SYSTEM_PROPERTY = "com.centurylink.mdw.transaction.manager";

	// container names
	String JBOSS = "JBoss";
	String TOMCAT = "Tomcat";
	String OSGI = "OSGi";

	// standard J2EE resource names
	String JAVA_TRANSACTION_MANAGER = "javax.transaction.TransactionManager";
	String JAVA_USER_TRANSACTION = "javax.transaction.UserTransaction";

    public String qualifyServiceBeanName(String name);

    public String qualifyJmsQueueName(String name);
    public String qualifyJmsTopicName(String name);

    public String getTransactionManagerName();
    public String getUserTransactionName();
    public int getServerPort() throws Exception;

    public Object lookup(String hostPort, String name, Class<?> cls) throws NamingException;
    public void bind(String name, Remote object) throws NamingException;
    public void unbind(String name) throws NamingException;

}
