/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.config.PropertyUtil;

/**
 * Dynamic Java workflow asset.
 */
@RegisteredService(StartupService.class)
public class MDWKafkaListenerRegistration implements StartupService {

    private static Map<String,MDWKafkaListener> listeners;

    public void onStartup() throws StartupException {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        logger.info("Starts Kafka Listeners");
        HashMap<String,Properties> listenerSpecs = new HashMap<String,Properties>();
        Properties listenerProperties;
        listeners = new HashMap<String,MDWKafkaListener>();
        try {
            listenerProperties = PropertyUtil.getInstance().getPropertyManager().getProperties(PropertyNames.MDW_LISTENER_KAFKA);
            for (Object pn : listenerProperties.stringPropertyNames()) {
                String[] pnParsed = ((String)pn).split("\\.", 5);
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
                String clsname = props.getProperty(MDWKafkaListener.KAFKAPOOL_CLASS_NAME);
                final MDWKafkaListener listener;
                if (clsname!=null) listener = (MDWKafkaListener)ApplicationContext.getClassInstance(clsname);
                else listener = new MDWKafkaListener();
                if (listener!=null) {
                    listeners.put(listenerName, listener);
                    listener.init(listenerName, props);
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            this.setName("KAFKAPOOL-thread");
                            listener.start();
                        }
                    };
                    thread.start();
                }
            }
        } catch (PropertyException e) {
            logger.severeException("Failed to load Kafka properties", e);
        }
    }

    public void onShutdown() {
        if (listeners != null) {
            for (MDWKafkaListener listener : listeners.values()) {
                listener.shutdown();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        try {
            Properties listenerProps = PropertyUtil.getInstance().getPropertyManager().getProperties(PropertyNames.MDW_LISTENER_KAFKA);
            return !listenerProps.isEmpty();
        }
        catch (PropertyException ex){
            LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
            return false;
        }
    }
}