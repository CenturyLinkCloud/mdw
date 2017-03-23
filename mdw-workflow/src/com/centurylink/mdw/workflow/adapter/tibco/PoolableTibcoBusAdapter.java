/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.workflow.adapter.tibco;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.PoolableAdapterBase;
import com.qwest.bus.Bus;
import com.qwest.bus.BusConnector;
import com.qwest.bus.BusException;
import com.qwest.bus.BusMessage;
import com.qwest.bus.BusMessageException;
import com.qwest.bus.BusPolicy;
import com.qwest.bus.IBusWorker;


/**
 * New implementation of Web Service Adapter which can be
 * configured through Designer and does not implement
 * ControlledAdapterActivity interface.
 *
 */
@Tracked(LogLevel.TRACE)
public class PoolableTibcoBusAdapter extends PoolableAdapterBase implements IBusWorker {

    public static final String PROP_URI = "uri";
    public static final String PROP_TOPIC = "topic";
    public static final String PROP_DATA_FIELD_NAME = "data_field";
    public static final String PROP_OVERRIDE_INBOX = "override_inbox";
    public static final String PROP_PUBLISH = "publish";
    public static final String PROP_ACCOUNT = "account";
    public static final String PROP_CREDENTIAL = "credential";

    public static final String DEFAULT_DATA_FIELD_NAME = "DATA";
    public static final int DEFAULT_TIMEOUT = 120;

    private String bus_uri;
    private String topicName;
    private String dataFieldName;
    private boolean isPublish;
    private boolean overrideInbox;
    private String account, credential;
    protected BusConnector qBusConnector;

    @Override
    protected boolean canBeSynchronous() {
        return true;
    }

    @Override
    protected boolean canBeAsynchronous() {
        return false;
    }

    /**
     * This method must be implemented for PoolableAdapter
     */
    @Override
    public void init() throws ConnectionException, AdapterException {
        bus_uri = getAttribute(PROP_URI, null, true);
        topicName = getAttribute(PROP_TOPIC, null, true);
        dataFieldName = getAttribute(PROP_DATA_FIELD_NAME, DEFAULT_DATA_FIELD_NAME, false);
        isPublish = "true".equalsIgnoreCase(getAttributeValue(PROP_PUBLISH));
        overrideInbox = "true".equalsIgnoreCase(getAttributeValue(PROP_OVERRIDE_INBOX));
        account = getAttribute(PROP_ACCOUNT, "MDW", false);
        credential = getAttribute(PROP_CREDENTIAL, "MDW", false);
    }

    /**
     * This method must be implemented for PoolableAdapter
     */
    @Override
    public void init(Properties parameters) {
        bus_uri = parameters.getProperty(PROP_URI);
        topicName = parameters.getProperty(PROP_TOPIC);
        dataFieldName = parameters.getProperty(PROP_DATA_FIELD_NAME, DEFAULT_DATA_FIELD_NAME);
        isPublish = "true".equalsIgnoreCase(parameters.getProperty(PROP_PUBLISH));
        String propVal = parameters.getProperty(PROP_OVERRIDE_INBOX);
        overrideInbox = "true".equalsIgnoreCase(propVal);
        account = parameters.getProperty(PROP_ACCOUNT, "MDW");
        credential = parameters.getProperty(PROP_CREDENTIAL, "MDW");
    }

    /**
     * Bus.getTopic() is not thread safe, so prevent duplicate Bus.createBusTopic() calls.
     */
    protected static synchronized void setBusTopic(String busUri, String topicName) throws BusException, URISyntaxException {
        if (Bus.getTopic(topicName) == null) {
            Bus.createBusTopic(topicName, new URI(busUri), "com.qwest.bus.TibrvBusHandler");
        }
    }

    /**
     * Bus.getPolicy() is not thread safe, so prevent simultaneous calls from separate threads.
     */
    protected static synchronized BusPolicy getBusPolicy(String policyName, int timeout, boolean overrideInbox) throws BusException {
        BusPolicy policy = Bus.getPolicy(policyName);
        if (policy == null) {
            policy = new BusPolicy(policyName);
            policy.set("OverrideInbox", new Boolean(overrideInbox));
            policy.setTimeout(timeout);
        }
        return policy;
    }

    /**
     * This method must be implemented for PoolableAdapter
     */
    @Override
    public Object openConnection()
    throws ConnectionException, AdapterException {
        try {
            if (bus_uri!=null && bus_uri.length()>0) {
                // assuming initialize from activity attributes
                setBusTopic(bus_uri, topicName);
            } // else assuming initialize from busConnector file
            qBusConnector = Bus.getBusConnector();
            return this;
        } catch (BusException e) {
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, e.getMessage(), e);
        } catch (URISyntaxException e) {
            throw new AdapterException(-1, e.getMessage(), e);
        }
    }

    /**
     * creates the bus message based on the passed in parameters.
     * The method allows further customization when creating a bus message
     * such as creating additional BUS data field.
     * It is not recommended to override this method.
     *
     * @param pRequestXML the request XML string
     * @param pTopicName the topic name
     * @param pTimeOut in seconds
     * @param pDataFieldName data field name
     * @return BusMessage the message to be published on BUS
     * @throws BusMessageException
     * @throws BusException
     */
    protected BusMessage createBusMessage(String pRequestXML, String pTopicName,
            long pTimeOut, String  pDataFieldName, boolean overrideInbox)
            throws BusMessageException, BusException {
        BusMessage busMessage = new BusMessage();
        busMessage.setTopic(pTopicName);
        BusPolicy policy = getBusPolicy(pTopicName, (int)(pTimeOut * 1000), overrideInbox);
        // setting the bus worker for the message
        busMessage.setWorker(this);
        busMessage.setString(pDataFieldName, pRequestXML);
        busMessage.setPolicy(policy);
        return busMessage;
    }

    /**
     * This method must be implemented for PoolableAdapter
     */
    @Override
    public String invoke(Object connection, String request, int timeout, Map<String,String> metainfo)
    throws ConnectionException, AdapterException {
        String response;
        try{
            if (timeout<=0) timeout = DEFAULT_TIMEOUT;
            BusMessage reqMsg = this.createBusMessage(request,
                    topicName, timeout, dataFieldName, overrideInbox);
            if (metainfo!=null) {
                for (String name : metainfo.keySet()) {
                    reqMsg.setString(name, metainfo.get(name));
                }
            }
            if(isPublish) {
                qBusConnector.publish(reqMsg);
                return null;
            } else {
                long time_a = System.currentTimeMillis();
                BusMessage respMsg = qBusConnector.request(reqMsg);
                if (respMsg != null) {
                    response = respMsg.getString(dataFieldName);
                } else {
                     long time_b = System.currentTimeMillis();
                     if (time_b-time_a<(timeout-2)*1000)
                         throw new AdapterException("Bus error, please investigate argos log for reasosn");
                     else throw new ConnectionException("Bus response timeout for request on topic " + topicName);
                 }
             }
        } catch (BusMessageException ex){
            throw new AdapterException(-1, ex.getMessage(), ex);
        } catch (BusException ex){
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        }
        return response;
    }

    /**
     * This method must be implemented for PoolableAdapter
     */
    @Override
    public void closeConnection(Object connection) {
        // nothing needs to be done
    }

    /**
     * This method must be implemented for PoolableAdapter
     */
    @Override
    public boolean ping(int timeout) {
        return false;    // this needs to be overridden for specific application connector
    }

    /**
     * This method must be implemented for IBusWorker
     */
    @Override
    public String getAccount() {
        return account;
    }

    /**
     * This method must be implemented for IBusWorker
     */
    @Override
    public String getCredential() {
        return credential;
    }

    /**
     * This method must be implemented for IBusWorker
     */
    @Override
    public void receive(BusMessage arg0) {
        // this should never be invoked because this is not a listener
    }

    /**
     * This method must be implemented for IBusWorker
     */
    @Override
    public void reject(BusMessage message, int reason) {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        logger.warn("Message has been rejected. Reason:="+reason +
                " \nMessage="+message.getString());
    }

}
