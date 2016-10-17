/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.tibco;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.common.constant.PropertyGroups;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.provider.StartupException;
import com.centurylink.mdw.common.provider.StartupService;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.common.utilities.startup.StartupClass;
import com.qwest.bus.Bus;
import com.qwest.bus.responder.BusResponder;
import com.qwest.capsule.DataCapsule;
import com.qwest.util.log.ConsoleAppender;
import com.qwest.util.log.Log;

/**
 * The start up class registers all the services that listen on the bus. The services gets
 * registered when the server starts up and the services can de registered when the server shuts
 * down
 */

public class BusListenerRegistration implements StartupClass, StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static BusResponder responder = null;

    /**
         * Method that gets invoked when the server comes up The impl class will have logic to that
         * gets executed when the server starts up
         * @throws StartupException
         */
    public void onStartup() throws StartupException {
    	if (responder != null) {
    		responder.shutdown();
    	}
    	String cfgfilename = this.getResponderConfigFile();
    	if (cfgfilename!=null && (new File(cfgfilename)).exists()) {
    		// for backward compatibility
    		String[] args = new String[2];
    		args[0] = "-b";
    		args[1] = cfgfilename;
    		logger.info("Configuring bus responder from file: " + cfgfilename);
        	this.setUpLogger();
        	responder = new BusResponder(args);
    	} else {
    		try {
        		String cfgxml = constructConfigXml();
        		if (cfgxml!=null) {
            		logger.info("Configuring bus responder from properties");
	    	    	this.setUpLogger();
	        		logger.info(cfgxml);
	        		DataCapsule dc = new DataCapsule(cfgxml);
	    			Bus.configure(dc);
	        		String[] args = { "-b" };
	    	    	responder = new BusResponder(args);
        		} else {
        			logger.info("No bus processor (listener) is specified");
        		}
    		} catch (Exception e) {
    			logger.severeException("Failed to configure bus responder", e);
			}
    	}
	}

    /**
     * Method that returns the responder config file path.
     * This is for backward compatibility.
     *
     * @return Config file location
     */
    private String getResponderConfigFile() {
		String fileName = PropertyManager.getProperty(PropertyGroups.APPLICATION_DETAILS
		    	+ "/BusResponderFile");
	    if (fileName == null) {
	    	fileName = System.getProperty("cfg.uri");
	    	if (fileName != null) {
	    	    String filePrefix = "file://";
	    	    if (fileName.startsWith(filePrefix))
	    	    	fileName = fileName.substring(filePrefix.length());
	    	}
	    }
		return fileName;
    }

    private class BusProcessorSpec {
    	String busProcessor;
    	String uri;
    	String topic;
    	String minWorker, maxWorker;
    	String queueSize;
    	String dqName;
    }

    private String constructConfigXml() throws PropertyException {
    	String account = PropertyManager.getProperty(PropertyNames.MDW_BUS_ACCOUNT);
    	if (account==null) account = "MDW";
    	boolean hasListeners = false;
    	StringBuffer sb = new StringBuffer();
    	sb.append("<BusConnector>\n");
    	Map<String,BusProcessorSpec> processors = new HashMap<String,BusProcessorSpec>();
    	Properties topicProperties = PropertyManager.getInstance().getProperties(PropertyNames.MDW_LISTENER_BUS);
    	for (String pn : topicProperties.stringPropertyNames()) {
    		String[] pnParsed = pn.split("\\.");
    		if (pnParsed.length==5) {
    			String name = pnParsed[3];
    			String attrname = pnParsed[4];
    			BusProcessorSpec procspec = processors.get(name);
    			if (procspec==null) {
    				procspec = new BusProcessorSpec();
    				procspec.busProcessor = "com.centurylink.mdw.listener.tibco.MDWBusListener";
    				processors.put(name, procspec);
    			}
    			String v = topicProperties.getProperty(pn);
    			if (attrname.equals("topic")) procspec.topic = v;
       			else if (attrname.equals("uri")) procspec.uri = v;
       			else if (attrname.equals("busProcessor")) procspec.busProcessor = v;
       			else if (attrname.equals("queueSize")) procspec.queueSize = v;
       			else if (attrname.equals("minWorker")) procspec.minWorker = v;
       			else if (attrname.equals("maxWorker")) procspec.maxWorker = v;
       			else if (attrname.equals("dqName")) procspec.dqName = v;
    		}
    	}
    	Map<String,String> topics = new HashMap<String,String>();
    	for (String name : processors.keySet()) {
    		BusProcessorSpec procspec = processors.get(name);
    		if (procspec.topic==null || procspec.uri==null)
    			throw new PropertyException("Bus processor spec needs topic and uri: " + name);
    		String uri = topics.get(procspec.topic);
    		if (uri==null) {
    			topics.put(procspec.topic, procspec.uri);
    			sb.append("  <BusTopic name='").append(procspec.topic)
    				.append("' class='com.qwest.bus.TibrvBusHandler'>\n");
    			sb.append("    <Attribute name='service' type='Array[1]'>\n");
    			sb.append("      <entry type='URI'>").append(procspec.uri).append("</entry>\n");
    			sb.append("    </Attribute>\n");
    			sb.append("  </BusTopic>\n");
    		} else if (!uri.equals(procspec.uri)) {
    			throw new PropertyException("Bus uri specifications different for topic "
    					+ procspec.topic);
    		}
    	}
    	for (String name : processors.keySet()) {
    		BusProcessorSpec procspec = processors.get(name);
    		sb.append("  <BusProcessor name='").append(name)
    			.append("' class='").append(procspec.busProcessor).append("'>\n");
    		sb.append("    <Attribute name='topic' type='String'>")
    			.append(procspec.topic).append("</Attribute>\n");
    		if (procspec.queueSize!=null)
    			sb.append("    <Attribute name='queue.size' type='Integer'>")
					.append(procspec.queueSize).append("</Attribute>\n");
    		if (procspec.minWorker!=null)
    			sb.append("    <Attribute name='worker.min' type='Integer'>")
					.append(procspec.minWorker).append("</Attribute>\n");
    		if (procspec.maxWorker!=null)
    			sb.append("    <Attribute name='worker.max' type='Integer'>")
					.append(procspec.maxWorker).append("</Attribute>\n");
    		if (procspec.dqName!=null) {
    			sb.append("    <BusPolicy name='MDWBusListenerPolicy").append(name).append("'>\n");
    			sb.append("      <Attribute name='guaranteed' type='Boolean'>false</Attribute>\n");
    			sb.append("      <Attribute name='queue' type='String'>")
    				.append(procspec.dqName).append("</Attribute>\n");
    			sb.append("    </BusPolicy>\n");
    		}
    		sb.append("  </BusProcessor>\n");
    		hasListeners = true;
    	}
    	sb.append("  <Attribute name='Account' type='String'>").append(account).append("</Attribute>\n");
////	sb.append("  <Attribute name='log.categories' type='String'>TIBCO,DEBUG,INFO,WARN,ERROR,FATAL</Attribute>\n");
//    	if (logger.isDebugEnabled())
//    		sb.append("  <Attribute name='log.categories' type='String'>DEBUG,INFO,WARN,ERROR,FATAL</Attribute>\n");
//    	else sb.append("  <Attribute name='log.categories' type='String'>INFO,WARN,ERROR,FATAL</Attribute>\n");
//    	String logfile = PropertyManager.getProperty(PropertyNames.MDW_LISTENER_BUS_LOGFILE);
//    	if (logfile==null) logfile = PropertyManager.getProperty(
//    			PropertyGroups.APPLICATION_DETAILS + "/BusLogFile");
//    	if (logfile!=null) {
//    		if (!logfile.startsWith("file:")) {
//    			if (logfile.startsWith("/")) logfile = "file://" + logfile;
//    			else logfile = "file:///" + logfile;
//    		}
//    		sb.append("  <Attribute name='log.class' type='String'>com.qwest.util.log.FileAppender</Attribute>\n");
//    		sb.append("  <Attribute name='log.target' type='String'>").append(logfile).append("</Attribute>\n");
//    	}
    	sb.append("</BusConnector>\n");
    	return hasListeners?sb.toString():null;
    }

    /**
         * Initialize this processor.
         *
         * <p>
         * This optional method allows the developer to perform initialization operations before
         * processing any messages. It is called once just prior to receiving any messages.
         * </p> //
         */
    private void setUpLogger() {
    	try {
//    		String logFileName = PropertyManager.getProperty(PropertyNames.MDW_LISTENER_BUS_LOGFILE);
//    		if (logFileName==null) logFileName = PropertyManager.getProperty(
//    				PropertyGroups.APPLICATION_DETAILS + "/BusLogFile");
//    		if (logFileName==null) return;	// use default logger
//    		FileAppender appender = new FileAppender(new File(logFileName));
    		Log.removeDefaultLogger();
//    		Log.addLogger(Log.DEFAULT_LOGGER_NAME, appender);
    		Log.addLogger(Log.DEFAULT_LOGGER_NAME, new ConsoleAppender());
    		Log.startLogging(Log.ERROR);
    		Log.startLogging(Log.FATAL);
    		Log.startLogging(Log.WARN);
    		if (logger.isDebugEnabled()) Log.startLogging(Log.DEBUG);
//    		logger.info("Bus responder uses log file " + logFileName);
    	} catch (Exception ex) {
    		logger.severeException(ex.getMessage(), ex);
    	}
    }

    /**
     * Method that gets invoked when the server shuts down
     */
    public void onShutdown() {
        if (responder!=null) {
            responder.shutdown();
            responder = null;
        }
    }

    @Override
    public boolean isEnabled() {
        try {
            Properties listenerProps = PropertyUtil.getInstance().getPropertyManager().getProperties(PropertyNames.MDW_LISTENER_BUS);
            return !listenerProps.isEmpty();
        }
        catch (PropertyException ex){
            LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
            return false;
        }
    }
}