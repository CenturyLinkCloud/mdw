/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.mqseries;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.provider.StartupException;
import com.centurylink.mdw.common.provider.StartupService;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.common.utilities.startup.StartupClass;

/**
 * You need to make sure the following libs are in class path");
 * 		$MQM/java/lib/com.ibm.mq.jar
 *		$MQM/java/lib/connector.jar
 * And you need the option -Djava.library.path=$MQM/java/lib
 *		(need lib mqjbnd05)
 *
 *
 */

public class MDWMQListenerRegistration implements StartupClass, StartupService {

	private static Map<String,MDWMQListener> listeners;

	public void onStartup() throws StartupException {
		StandardLogger logger = LoggerUtil.getStandardLogger();
		logger.info("Starts MQ Listeners");
		HashMap<String,Properties> listenerSpecs = new HashMap<String,Properties>();
    	Properties listenerProperties;
		listeners = new HashMap<String,MDWMQListener>();
		try {
			listenerProperties = PropertyUtil.getInstance().getPropertyManager().getProperties(PropertyNames.MDW_LISTENER_MQ);
			for (Object pn : listenerProperties.stringPropertyNames()) {
	    		String[] pnParsed = ((String)pn).split("\\.");
	    		if (pnParsed.length==5) {
	    			String listener_name = pnParsed[3];
	    			String attrname = pnParsed[4];
	    			Properties listenerSpec = listenerSpecs.get(listener_name);
	    			if (listenerSpec==null) {
	    				listenerSpec = new Properties();
	    				listenerSpecs.put(listener_name, listenerSpec);
	    			}
	    			String v = listenerProperties.getProperty((String)pn);
	    			listenerSpec.put(attrname, v);
	    		}
	    	}
			for (String listenerName : listenerSpecs.keySet()) {
				Properties props = listenerSpecs.get(listenerName);
				String clsname = props.getProperty(MDWMQListener.MQPOOL_CLASS_NAME);
				final MDWMQListener listener;
				if (clsname!=null) listener = (MDWMQListener)ApplicationContext.getClassInstance(clsname);
				else listener = new MDWMQListener();
				if (listener!=null) {
					listeners.put(listenerName, listener);
					listener.init(listenerName, props);
					Thread thread = new Thread() {
						@Override
						public void run() {
							this.setName("MQPOOL-thread");
	    					listener.start();
	    				}
					};
					thread.start();
				}
			}
		} catch (PropertyException e) {
			logger.severeException("Failed to load MQ properties", e);
		}
	}

	public void onShutdown() {
	    if (listeners != null) {
		    for (MDWMQListener listener : listeners.values()) {
			    listener.shutdown();
		    }
	    }
	}

    @Override
    public boolean isEnabled() {
        try {
            Properties listenerProps = PropertyUtil.getInstance().getPropertyManager().getProperties(PropertyNames.MDW_LISTENER_MQ);
            return !listenerProps.isEmpty();
        }
        catch (PropertyException ex){
            LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
            return false;
        }
    }
}
