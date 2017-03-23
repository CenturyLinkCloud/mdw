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

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.AdapterActivityBase;
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
public class SynchronousBusAdapter extends AdapterActivityBase implements IBusWorker {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static final String BUS_URI = "Bus URI";
    private static final String TOPIC = "Bus Topic";
    private static final String PROPERTY_OVERRIDE_INBOX = "Override Inbox";
    private static final String RESPONSE_TIMEOUT = "Response Timeout (sec)";
    protected static final String DATA_FIELD_NAME_DATA = "DATA";
    protected static final String DATA_FIELD_NAME_ATTRIBUTE = "DATA_FIELD_NAME_ATTRIBUTE";

    /**
     * The method overrides the one in the super class and always returns true
     */
    @Override
    public final boolean isSynchronous() {
        return true;
    }

    /**
     * This method returns the value of the attribute "Bus Topic" using getAttributeValueSmart,
     * so you can specify a variable or a property specification, in addition to
     * fixed Bus topic name. You can also override the method to obtain the topic in other ways.
     * @return the bus topic to which the request message will be published.
     * @throws PropertyException when there are any translation error.
     */
    protected String getTopicName() throws PropertyException {
        return this.getAttributeValueSmart(TOPIC);
    }

    /**
     * Return timeout when waiting for responses.
     * This default implementation gets the value from the attribute "Response Timeout (sec)".
     *
     * @return timeout in seconds
     */
    protected long getResponseTimeOut() {
        String v = this.getAttributeValue(RESPONSE_TIMEOUT);
        return (v==null)?1:new Long(v).longValue();
    }

    /**
     * Get the data field name of the bus message.
     * This default implementation returns the string "DATA".
     * @return the string "DATA".
     */
    protected String getDataFieldName() {
        String v = this.getAttributeValue(DATA_FIELD_NAME_ATTRIBUTE);
        return (v==null) ? DATA_FIELD_NAME_DATA : v;
    }

    /**
     * This method currently returns true all the time. It is intended for future
     * extension.
     * @return true.
     */
    protected boolean isRequestReply() {
        return true;
    }

    /**
     * The method overrides the one in the super class to perform
     * BUS specific functions.
     */
    @Override
    public Object invoke(Object conn, Object requestData)
        throws AdapterException, ConnectionException
    {
        BusConnector pConnection = (BusConnector)conn;
        String requestXML = (String)requestData;

        BusMessage reqMsg = null;
        BusMessage respMsg = null;
        String responseXML = null;
        try{
            String topicName = this.getTopicName();
            long timeOut = this.getResponseTimeOut();
            String dataFieldName = this.getDataFieldName();
             reqMsg = this.createBusMessage(requestXML, topicName, timeOut, dataFieldName);
             if(!this.isRequestReply()){
                 pConnection.publish(reqMsg);
//                 Bus.publish(reqMsg);
                 logger.info("Message has been published for topicName="+topicName);
                 return null;
             } else {
                 long time_a = System.currentTimeMillis();
                 respMsg = pConnection.request(reqMsg);
//             respMsg = Bus.request(reqMsg);
                 if (respMsg != null) {
                     responseXML = respMsg.getString(dataFieldName);
                 }else{
                     long time_b = System.currentTimeMillis();
                     if (time_b-time_a<(timeOut-2)*1000)
                         throw new AdapterException("Bus error, please investigate argos log for reasosn");
                     else throw new ConnectionException("Bus response timeout for request on topic "
                             + topicName);
                 }
             }
        } catch (BusMessageException ex){
            throw new AdapterException(-1, ex.getMessage(), ex);
        } catch (BusException ex){
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        } catch (PropertyException ex){
            throw new AdapterException(-1, ex.getMessage(), ex);
        }
        return responseXML;
    }

    @Override
    protected void closeConnection(Object connection) {
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

    @Override
    protected Object openConnection() throws ConnectionException,AdapterException {
        try {
            String topic = this.getTopicName();
            String bus_uri = this.getAttributeValueSmart(BUS_URI);
            if (bus_uri!=null && bus_uri.length()>0) {
                // assuming initialize from activity attributes
                setBusTopic(bus_uri, topic);
            } // else initialize from busconnector.xml for backward compatibility
            return Bus.getBusConnector();
        } catch (BusException e) {
            e.printStackTrace();
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, e.getMessage(), e);
        } catch (URISyntaxException e) {
            throw new AdapterException(AdapterException.CONFIGURATION_WRONG, e.getMessage(), e);
        } catch (PropertyException e) {
            throw new AdapterException(AdapterException.CONFIGURATION_WRONG, "Topic name property wrong", e);
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
            long pTimeOut, String  pDataFieldName)
            throws BusMessageException, BusException {
        BusMessage busMessage = new BusMessage();
        busMessage.setTopic(pTopicName);
        BusPolicy policy = getBusPolicy(pTopicName, (int)(pTimeOut * 1000), this.getOverrideInbox());
        // setting the bus worker for the message
        busMessage.setWorker(this);
        busMessage.setString(pDataFieldName, pRequestXML);
        busMessage.setPolicy(policy);
        return busMessage;
    }

    /**
     * Method that allows custom adapters to override the inbox property.
     * This default implementation obtains the value from the attribute
     * "Override Inbox".
     *
     * @return boolean true or false as to whether to override inbox.
     */
    protected boolean getOverrideInbox(){
        String propVal = this.getAttributeValue(PROPERTY_OVERRIDE_INBOX);
        return "true".equalsIgnoreCase(propVal);
    }

    /**
     * Method of IBusWorker.
     * Method that gets called when the registering the worker
     * IBusWorker behavior
     * Custom application adapters will override this one.
     * Please set your application's TOM ID
     * @return
     */
    public String getAccount() {
        // TODO Auto-generated method stub
        return "TODO";
    }

    /**
     * Method of IBusWorker
     * Subclass can override the method if they have credential
     */
    public String getCredential() {
        // TODO Auto-generated method stub
        return "";
    }

    /**
     * IBusWorker method.
     * This should never be invoked because this is an adapter,
     * not a listener
     */
    public void receive(BusMessage pMessage) {
        logger.warn("Adapter received a message."+
                " \nMessage="+pMessage.getString());
    }

    /**
     * IBusWorker method.
     * This will be called by the bus connected when the message is rejected
     * @param pMessage
     * @param pReason
     */
    public void reject(BusMessage pMessage, int pReason) {
        logger.warn("Message has been rejected. Reason:="+pReason +
                " \nMessage="+pMessage.getString());
    }

    /**
     * The method overrides the one from the super class and perform the followings:
     * <ul>
     *   <li>It gets the value of the variable with the name specified in the attribute
     *      REQUEST_VARIABLE. The value is typically an XML document or a string</li>
     *   <li>It invokes the variable translator to convert the value into a string
     *      and then return the string value.</li>
     * </ul>
     * The method will through exception if the variable is not bound,
     * or the value is not bound to a DocumentReference or String.
     */
    @Override
    protected Object getRequestData() throws ActivityException {
        Object request = super.getRequestData();
        if (request==null) throw new ActivityException("Request data is null");
        if (request instanceof DocumentReference) request = getDocumentContent((DocumentReference)request);
        if (request instanceof String) return request;
        else if (request instanceof Document) return
            VariableTranslator.realToString(Document.class.getName(), request);
        else if (request instanceof XmlObject) return ((XmlObject)request).xmlText();
        else throw new ActivityException(
                "Cannot handle request of type " + request.getClass().getName());
    }

}
