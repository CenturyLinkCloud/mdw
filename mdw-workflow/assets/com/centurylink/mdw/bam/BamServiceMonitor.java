/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.bam;

import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.SpringConstants;
import com.centurylink.mdw.common.spring.SpringAppContext;
import com.centurylink.mdw.common.utilities.MessageProducer;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.container.plugins.activemq.ActiveMqJms;
import com.centurylink.mdw.monitor.ServiceMonitor;
import com.centurylink.mdw.monitor.impl.Monitor;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.MbengException;

@RegisteredService(com.centurylink.mdw.monitor.ServiceMonitor.class)
public class BamServiceMonitor extends Monitor implements ServiceMonitor {
    private static String BlvTopic = null;
    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public Object onRequest(Object request, Map<String,String> headers) {
        try {
            if (!((String)request).startsWith("<"))
                return null;
            DomDocument doc = new DomDocument((String)request);
            if (!doc.getRootNode().getName().equalsIgnoreCase("CustomEvent"))
                return null;
            else {
                if (logger.isMdwDebugEnabled()) {
                    logger.mdwDebug("BAM Service Monitor Invoke  ---------------" + request);
                }
                if (ApplicationContext.isCloud() && SpringAppContext.getInstance().isBeanDefined(SpringConstants.MDW_SPRING_MESSAGE_PRODUCER)) {
                    MessageProducer mdwMessageProducer = (MessageProducer) SpringAppContext
                            .getInstance().getBean(SpringConstants.MDW_SPRING_MESSAGE_PRODUCER);
                    if (mdwMessageProducer != null) {
                        // Use injected Spring
                        mdwMessageProducer.sendBamMessageToTopic((String) request, DeliveryMode.NON_PERSISTENT);
                        if (logger.isMdwDebugEnabled()) {
                            logger.mdwDebug("Sent BAM message via Spring JMS to BAM topic ");
                        }
                    }
                }
                else {
                    // create the connection factory
                    ConnectionFactory connectionFactory = ((ActiveMqJms) ApplicationContext
                            .getJmsProvider()).getConnectionFactory();
                    if (connectionFactory == null)
                        throw new JMSException("JMS Connection Factory not Available");
                    Connection connection = connectionFactory.createConnection("guest", "password"); // TODO// can we use createConnection()
                    connection.start();
                    // Create the session and topic
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    if (BlvTopic == null) {
                        String blvTopicName = PropertyManager
                                .getProperty(PropertyNames.MDW_BLV_TOPIC);
                        if (blvTopicName == null || blvTopicName.length() <= 0)
                            blvTopicName = "blvTopic";
                        BlvTopic = blvTopicName;
                    }
                    Topic bamTopic = session.createTopic(BlvTopic);
                    javax.jms.MessageProducer bamProducer = session.createProducer(bamTopic);
                    bamProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                    bamProducer.send(session.createTextMessage((String) request));
                    if (logger.isMdwDebugEnabled()) {
                        logger.mdwDebug("Sent message to bamTopic" + bamTopic.toString());
                    }
                }
            }
        }
        catch (MbengException ex){
            //do nothing
        }
        catch (Exception ex) {
            logger.severe("Problem sending BAM Message..." + request);
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public Object onHandle(Object request, Map<String,String> headers) {
        return null;
    }

    @Override
    public Object onResponse(Object response, Map<String,String> headers) {
        return null;
    }

    @Override
    public Object onError(Throwable t, Map<String,String> headers) {
        return null;
    }
}