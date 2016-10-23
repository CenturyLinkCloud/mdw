/**
* Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
*/

package com.centurylink.mdw.workflow.adapter.mqseries;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.PoolableAdapterBase;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;

/**
 * New implementation of JMS Adapter which can be
 * configured through Designer and does not implement
 * ControlledAdapterActivity interface.
 *
 * @author jxxu
 */
@Tracked(LogLevel.TRACE)
public class MqSeriesAdapter extends PoolableAdapterBase {

    public static final String PROP_QUEUE_MANAGER_NAME = "QueueManagerName";
    public static final String PROP_REQUEST_QUEUE_NAME = "QueueName";
    public static final String PROP_REPLY_QUEUE_NAME = "ReplyQueueName";
    public static final String PROP_HOST_NAME = "HostName";
    public static final String PROP_CHANNEL_NAME = "ChannelName";
    public static final String PROP_PORT = "Port";

    // per-call meta data
    public static final String PROP_CORRELATION_ID = "CorrelationId";

    private String qmgr_name;
	private String queue_name;
	private String replyQueueName;
	private boolean isSynchronous;
	private String hostName;
	private String channelName;
	private String port;
	private static final int defaultTimeout = 120;
	private MQQueueManager qmgr;

	@Override
	protected boolean canBeSynchronous() {
		return true;
	}

	@Override
	protected boolean canBeAsynchronous() {
		return true;
	}

	@Override
	public void init(Properties parameters) {
		qmgr_name = parameters.getProperty(PROP_QUEUE_MANAGER_NAME);
		queue_name = parameters.getProperty(PROP_REQUEST_QUEUE_NAME);
		replyQueueName = parameters.getProperty(PROP_REPLY_QUEUE_NAME);
		isSynchronous = "true".equalsIgnoreCase(parameters.getProperty(PROP_SYNCHRONOUS_RESPONSE));
		hostName = parameters.getProperty(PROP_HOST_NAME);
		channelName = parameters.getProperty(PROP_CHANNEL_NAME);
		port = (parameters.getProperty(PROP_PORT) != null) ? parameters.getProperty(PROP_PORT) : "1414" ;
	}

	@Override
	public void init() throws ConnectionException, AdapterException {
		qmgr_name = getAttribute(PROP_QUEUE_MANAGER_NAME, null, true);
		queue_name = getAttribute(PROP_REQUEST_QUEUE_NAME, null, true);
		replyQueueName = getAttribute(PROP_REPLY_QUEUE_NAME, null, true);
		isSynchronous = "true".equalsIgnoreCase(getAttributeValue(PROP_SYNCHRONOUS_RESPONSE));
		hostName = getAttribute(PROP_HOST_NAME,null, true);
		channelName = getAttribute(PROP_CHANNEL_NAME,null, true);
		port = getAttribute(PROP_PORT, "1414", true);
	}

	@Override
    protected Map<String,String> getRequestHeaders() {
		HashMap<String,String> meta_data;
    	try {
			String correlationId = this.getAttributeValueSmart(PROP_CORRELATION_ID);
			if (correlationId!=null) {
				meta_data = new HashMap<String,String>();
				meta_data.put(PROP_CORRELATION_ID, correlationId);
			} else meta_data = null;
		} catch (PropertyException e) {
			super.logexception("failed to evaluate correlation ID", e);
			meta_data = null;
		}
		return meta_data;
    }

	/**
     * The method overrides the one in the super class to perform
     * JMS specific functions.
     */
	public String invoke(Object connection, String requestData, int timeout, Map<String,String> metainfo)
	        throws AdapterException, ConnectionException
	{
	        try {
			    int msglen;
			    MQMessage msg = new MQMessage();
			    msg.format = MQConstants.MQFMT_STRING;
			    msg.writeString((String)requestData);
				int openQueueOptions = getOpenQueueOptions();
				MQPutMessageOptions putOptions = new MQPutMessageOptions();
				putOptions.options = putOptions.options | getPutQueueOptions();
				String correlationId = metainfo==null?null:metainfo.get(PROP_CORRELATION_ID);
				if (correlationId!=null) msg.correlationId = correlationId.getBytes();
	            if (replyQueueName!=null) msg.replyToQueueName = replyQueueName;

				MQQueue replyQueue;
				if (isSynchronous) {
					if (replyQueueName==null) replyQueueName = "REPLY_MODEL_QUEUE";
					String dynamicQueueName = replyQueueName + "*";
					replyQueue = qmgr.accessQueue(replyQueueName, openQueueOptions,
							 null,dynamicQueueName,null);
					msg.replyToQueueName = replyQueue.getName();
	            } else replyQueue = null;
				qmgr.put(queue_name, msg);
				String result;
				if (replyQueue!=null) {
					/* read reply */
				    MQGetMessageOptions getOptions = new MQGetMessageOptions();
					getOptions.options = getOptions.options|getGetQueueuOptions();
					getOptions.matchOptions = getOptions.matchOptions|getMatchQueueOptions();
					if (timeout<=0) timeout = defaultTimeout;
					getOptions.waitInterval = timeout*1000;
					MQMessage rmsg = new MQMessage();
					rmsg.messageId = msg.messageId;
					rmsg.correlationId = msg.correlationId;
					replyQueue.get(rmsg, getOptions, 1024*1024);
					msglen = rmsg.getTotalMessageLength();
					result = rmsg.readStringOfCharLength(msglen);
			    } else result = null;
	            return result;
	        } catch (Exception e) {
	            throw new AdapterException(-1, "Exception in invoking JmsAdapter" , e);
	        }
	}

    @Override
    public void closeConnection(Object connection) {
        try {
        	if (qmgr.isConnected()) {
        		qmgr.disconnect();
        	}
        } catch (Exception e) {
        }
    }

    @Override
    public Object openConnection() throws ConnectionException {
		try {
			Hashtable<String,Object> props = new Hashtable<String, Object>();

			if ( ! StringHelper.isEmpty(hostName)) {
	            props.put(MQConstants.HOST_NAME_PROPERTY, hostName);
	            props.put(MQConstants.CHANNEL_PROPERTY, channelName);
	            props.put(MQConstants.PORT_PROPERTY, Integer.parseInt(port));
			}
			qmgr = new MQQueueManager(qmgr_name,props);
			return this;
		} catch (Exception e) {
			throw new ConnectionException(ConnectionException.CONNECTION_DOWN,
					"Failed to open connection to MQ server", e);
		}
    }

	@Override
	public boolean ping(int timeout) {
		return false;		// need to implement this in application specific subclass
	}

	/**
	 * Default options used by MDW is MQC.MQOO_INPUT_AS_Q_DEF|MQC.MQOO_OUTPUT|MQC.MQOO_BROWSE
	 * Override this method if client application wants to set different open queue options.
	 * @return
	 */
	protected int getOpenQueueOptions() {
		return MQConstants.MQOO_INPUT_AS_Q_DEF|MQConstants.MQOO_OUTPUT|MQConstants.MQOO_BROWSE;
	}

	/**
	 * Default options used by MDW is MQC.MQPMO_SET_IDENTITY_CONTEXT
	 * Override this method if client application wants to set different put queue options.
	 * @return
	 */
	protected int getPutQueueOptions() {
		return MQConstants.MQPMO_SET_IDENTITY_CONTEXT;
	}

	/**
	 * Default options used by MDW is MQC.MQGMO_WAIT
	 * Override this method if client application wants to set different get queue options.
	 * @return
	 */
	protected int getGetQueueuOptions() {
		return MQConstants.MQGMO_WAIT;
	}

	/**
	 * Default options used by MDW is MQC.MQMO_MATCH_MSG_ID
	 * Override this method if client application wants to set different matched queue options.
	 * @return
	 */
	protected int getMatchQueueOptions() {
		return MQConstants.MQMO_MATCH_MSG_ID;
	}
}
