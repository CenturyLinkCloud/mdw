/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.runtime;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public abstract class LogSubscriber implements MessageListener {
	
	private Context JNDIcontext=null;
	private Connection connection=null;
	private Session session=null;
	private boolean done = false;

	public LogSubscriber(String server_url, String topic_name) 
		throws NamingException, JMSException, InterruptedException {
		if (server_url.startsWith("http:")) {
			int k = server_url.indexOf('/', 10);
			// TODO how to handle this for JBoss???
			if (k>0) server_url = "t3:" + server_url.substring(5,k);
			else server_url = "t3:" + server_url.substring(5);
		}
		TopicSession session = (TopicSession)connect(server_url);
		Topic topic = (Topic)JNDIcontext.lookup(topic_name);
		TopicSubscriber subscriber = session.createSubscriber(topic);
		subscriber.setMessageListener(this);
		((TopicConnection)connection).start();
	}
	
	abstract protected boolean onMessage(String message);

	public void onMessage(Message message) {
		try {
			if (message instanceof TextMessage && !done) {
				done = onMessage(((TextMessage)message).getText());
			}
		} catch (JMSException e) {
			System.err.println(e.toString());
			e.printStackTrace(System.err);
		}
	}

	private Session connect(String url) throws NamingException,JMSException {
		Hashtable<String,String> h = new Hashtable<String,String>();
		h.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
		h.put(Context.PROVIDER_URL, url);
		JNDIcontext = new InitialContext(h);
		try {
			TopicConnectionFactory cf
				= (TopicConnectionFactory)JNDIcontext.lookup("weblogic.jms.XAConnectionFactory");
			connection = cf.createTopicConnection();
			connection.start();
			session = ((TopicConnection)connection).createTopicSession(
					false,   // non transacted
					Session.AUTO_ACKNOWLEDGE);
			return session;
		} catch (NamingException e) {
			e.printStackTrace();
			throw e;
		} catch (JMSException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public void close() {
		done = true;
		try {
			if (connection!=null) ((TopicConnection)connection).stop();
			if (session!=null) session.close();
			if (connection!=null) connection.close();
			if (JNDIcontext!=null) JNDIcontext.close();
		} catch (Exception e) {
		}
		session = null;
		connection = null;
		JNDIcontext = null;
	}
	
	public boolean isDone() {
		return done;
	}

}

