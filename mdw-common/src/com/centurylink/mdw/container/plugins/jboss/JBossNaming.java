/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugins.jboss;

import java.rmi.Remote;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.centurylink.mdw.container.NamingProvider;

public class JBossNaming implements NamingProvider {


    private Context namingContext;
    private Context getNamingContext() throws NamingException {
        if (namingContext == null) {
            Hashtable<String,String> h = new Hashtable<String,String>();
            h.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
            namingContext = new InitialContext(h);
        }
        return namingContext;
    }

    private Context getNamingContext(String contextUrl) throws NamingException {
        Hashtable<String,String> h = new Hashtable<String,String>();
        h.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        h.put(Context.PROVIDER_URL, contextUrl);
        return new InitialContext(h);
    }

    private void closeNamingContext(Context context) {
    	if (context!=namingContext) {
    		try {
				context.close();
			} catch (NamingException e) {
			}
    	}
    }

    public String qualifyServiceBeanName(String name) {
    	return name;  // no qualification necessary
    }

    public String qualifyJmsQueueName(String name) {
    	return "/queue/" + name;
    }

    public String qualifyJmsTopicName(String name) {
    	return "/topic/" + name;  // TODO confirm
    }

    public String getTransactionManagerName() {
    	return "java:/TransactionManager";
    }

    public String getUserTransactionName() {
    	return "java:comp/UserTransaction";
    }

	public int getServerPort() {
		// TODO Implement this for JBoss
	    return 0;
	}

	/**
	 * Can be used to look up for Queue, QueueConnectionFactory, Topic, TopicConnectionFactory
	 */
	public Object lookup(String hostPort, String name, Class<?> cls)
			throws NamingException {
		Context context = null;
		try {
			if (hostPort==null) context = getNamingContext();
			else if (hostPort.contains("://")) context = getNamingContext(hostPort);
			else context = getNamingContext("t3://" + hostPort);	// TODO - what protocol???
			return context.lookup(name);
		} finally {
			closeNamingContext(context);
		}
	}

	public void bind(String name, Remote object) throws NamingException {
		Context context = null;
		try {
			context = getNamingContext();
			context.rebind(name, object);
		} finally {
			closeNamingContext(context);
		}
	}

	public void unbind(String name) throws NamingException {
		Context context = null;
		try {
			context = getNamingContext();
			context.unbind(name);
		} finally {
			closeNamingContext(context);
		}
	}
}
