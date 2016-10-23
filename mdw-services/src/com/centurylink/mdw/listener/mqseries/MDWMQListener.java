/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.mqseries;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;

/**
 * You need to make sure the following libs are in class path");
 * $MQM/java/lib/com.ibm.mq.jar $MQM/java/lib/connector.jar And you need the
 * option -Djava.library.path=$MQM/java/lib (need lib mqjbnd05)
 *
 * Files needed for stand-alone execution: StandaloneLogger a. change package to
 * "mqlistener" b. remove "implements StandardLogger" MDWMQListener a. change
 * package to "mqlistener" b. comment out all lines with "WITHENGINE" c.
 * uncomment all lines with "STANDALONE" The steps can be done via
 * "sed -f sed.script MDWMQListener.java > mqlistener/MDWMQListener.java"
 *
 * @author jxxu
 *
 */

public class MDWMQListener {

    public static final String MQPOOL_CLASS_NAME = "className";
    public static final String HOST_NAME = "host";
    public static final String PORT = "port";;
    public static final String CHANNEL = "channel";
    private static final String QUEUE_MGR_NAME = "queueManagerName";
    private static final String QUEUE_NAME = "queueName";
    private static final String NEED_RESPONSE = "needResponse"; // default false
    private static final String XML_WRAPPER = "xmlWrapper";
    // the followings are MQ connect options
    private static final String MQOO_OPTIONS = "mqOpenOptions"; // default
// MQC.MQOO_INPUT_AS_Q_DEF (0x1)
    // the followings are MQ get options
    private static final String POLL_TIMEOUT = "pollTimeout"; // in seconds,
// default 60
    private static final String MAX_MESSAGE_SIZE = "maxMessageSize";
    private static final String MQGMO_OPTIONS = "mqGetOptions"; // default
// MQC.MQGMO_WAIT (0x1)
    private static final String MQMO_OPTIONS = "mqMatchOptions"; // default
// MQC.MQMO_NONE (0)
    // the followings are MQ put options
    private static final String MQPMO_OPTIONS = "mqPutOptions"; // default
// MQC.MQPMO_SET_IDENTITY_CONTEXT

    // the followings are for thread pools
    private static final String USE_THREAD_POOL = "useThreadPool"; // default
// false
    private static final String HEX = "0123456789ABCDEF";
    private static final String REASONS_TO_SUPPRESS = "reasonsToSuppress";
    private static final String NUMBER_OF_SUPPRESSED_REASONS_TO_SHOW = "numberOfSuppressedReasonsToShow";

    private String qManagerName;
    private String receiveQName;
    private String hostName;
    private int port = 1414;
    private String channelName;
    private int poll_timeout;
    private boolean use_thread_pool;
    private boolean need_response;
    private String xmlWrapper;
    private int max_message_size;
// private int timeout_for_termination;
    private int mq_open_options;
    private int mq_get_options;
    private int mq_match_options;
    private int mq_put_options;

    private String mqListenerName;
    private int sameReason = 0;
    private int numSuppressedReasonsToShow = 0;
    private int lastReason = 0;
    private Vector<String> reasonsToSuppress = new Vector<String>();
    Properties initParameters = null;
    Hashtable<String, Object> props = new Hashtable<String, Object>();

    // STANDALONE private StandaloneLogger logger;
    /* WITHENGINE */private StandardLogger logger;

    protected MQQueueManager qMgr;
    private boolean _terminating;
    private ThreadPoolProvider thread_pool; // null if processing using the same
// thread

    public void init(String listenerName, Properties parameters) {
        // STANDALONE logger = StandaloneLogger.getSingleton();
        /* WITHENGINE */logger = LoggerUtil.getStandardLogger();

        logger.info("Starting MQ Listener name = " + listenerName);

        mqListenerName = listenerName;
        initParameters = parameters;

        qManagerName = parameters.getProperty(QUEUE_MGR_NAME);
        receiveQName = parameters.getProperty(QUEUE_NAME);
        hostName = parameters.getProperty(HOST_NAME);
        channelName = parameters.getProperty(CHANNEL);
        port = getIntegerProperty(parameters, PORT, port);
        poll_timeout = 1000 * getIntegerProperty(parameters, POLL_TIMEOUT, 60);
        use_thread_pool = getBooleanProperty(parameters, USE_THREAD_POOL, false);
        max_message_size = getIntegerProperty(parameters, MAX_MESSAGE_SIZE, 100 * 1024);
        need_response = getBooleanProperty(parameters, NEED_RESPONSE, false);
        xmlWrapper = parameters.getProperty(XML_WRAPPER);
// timeout_for_termination = getIntegerProperty(parameters, TERMINATION_TIMEOUT,
// 120);

        mq_open_options = getIntegerProperty(parameters, MQOO_OPTIONS, MQConstants.MQOO_INPUT_AS_Q_DEF);
        // useful options
// MQC.MQOO_INPUT_AS_Q_DEF|MQC.MQOO_OUTPUT|MQC.MQOO_BROWSE;
        mq_get_options = getIntegerProperty(parameters, MQGMO_OPTIONS, MQConstants.MQGMO_WAIT);
        // useful options MQC.MQGMO_WAIT, MQC.MQGMO_NO_WAIT
        mq_match_options = getIntegerProperty(parameters, MQMO_OPTIONS, MQConstants.MQMO_NONE);
        // useful options MQC.MQMO_MATCH_MSG_ID;
        mq_put_options = getIntegerProperty(parameters, MQPMO_OPTIONS, MQConstants.MQPMO_SET_IDENTITY_CONTEXT);
        // useful options MQC.MQPMO_SET_IDENTITY_CONTEXT;

        numSuppressedReasonsToShow = getIntegerProperty(parameters, NUMBER_OF_SUPPRESSED_REASONS_TO_SHOW, 0); // numberOfSuppressedReasonsToShow

        String reasonsToSuppressString = parameters.getProperty(REASONS_TO_SUPPRESS); // reasonsToSuppress
        String reasons[] = null;

        /**
         * Initialize reasons Vector
         */
        if (reasonsToSuppressString != null) {
            reasons = reasonsToSuppressString.split(",");
            reasonsToSuppress = new Vector<String>(Arrays.asList(reasons));
        }

        if (reasonsToSuppress != null) {
            for (String reason : reasonsToSuppress) {
                try {
                    if(logger.isMdwDebugEnabled())
                        logger.mdwDebug("Reason to suppress = "+reason);
                    MQException.logExclude(Integer.parseInt(reason));
                }
                catch (Exception e) {
                    logger.severeException("MQException.logExclude(" + reason + ")", e);
                }
            }
        }
    }

    private int getIntegerProperty(Properties parameters, String propname, int defval) {
        String v = parameters.getProperty(propname);
        if (v == null)
            return defval;
        try {
            return Integer.parseInt(v);
        }
        catch (Exception e) {
            return defval;
        }
    }

    private boolean getBooleanProperty(Properties parameters, String propname, boolean defval) {
        String v = parameters.getProperty(propname);
        if (defval)
            return !"false".equalsIgnoreCase(v);
        else
            return "true".equalsIgnoreCase(v);
    }

    public void shutdown() {
        _terminating = true;
    }

    public void start() {
        try {
            if (qManagerName == null)
                throw new Exception("MQ Queue Manager name is not specified");
            if (receiveQName == null)
                throw new Exception("MQ Queue name is not specified");

            if (logger.isMdwDebugEnabled())
            {
                logger.mdwDebug("hostName*=" + hostName);
                logger.mdwDebug("port*=" + port);
                logger.mdwDebug("channel*=" + channelName);
            }

            if (!StringHelper.isEmpty(hostName)) {
                props.put(MQConstants.HOST_NAME_PROPERTY, hostName);
                props.put(MQConstants.CHANNEL_PROPERTY, channelName);
                props.put(MQConstants.PORT_PROPERTY, port);
            }
            MQException.logExclude(MQException.MQRC_NO_MSG_AVAILABLE); // Exclude 2033 reason code.
            qMgr = new MQQueueManager(qManagerName, props);
            MQQueue receiveQ = qMgr.accessQueue(receiveQName, mq_open_options, null, null, null);
            _terminating = false;
            if (!use_thread_pool) {
                thread_pool = null;
                while (!_terminating) {
                    try {
                        MQMessage request = getMsg(poll_timeout, receiveQ);
                        if (request != null)
                            process_message(request);
                    }
                    catch (MQException ex) {
                        if (!isMQUp(ex)) {
                            receiveQ = handleMQReconnect(poll_timeout);
                        }
                    }
                }
            }
            else {
                thread_pool = ApplicationContext.getThreadPoolProvider();
                MQMessage request = null;
                MQRunnable runnable = null;
                while (!_terminating) {
                    // If request == null, then we successfully processed previous request, so get next one from queue
                    // If NOT null, then we couldn't process previously received request (no available thread), so try again
                    try {
                        if (request == null)
                            request = getMsg(poll_timeout, receiveQ);

                        if (request != null) {
                            if (runnable == null)
                                runnable = new MQRunnable(request);

                            if (thread_pool.execute(ThreadPoolProvider.WORKER_LISTENER, "MDWMQListener", runnable)) {
                                request = null;
                                runnable = null;
                            }
                            else {
                                String msg = "MDWMQ listener " + ThreadPoolProvider.WORKER_LISTENER + " has no thread available";
                                // make this stand out
                                logger.severeException(msg, new Exception(msg));
                                logger.info(thread_pool.currentStatus());
                                Thread.sleep(poll_timeout);  // Will try to process same request after waking up
                            }
                        }
                    }
                    catch (MQException ex) {
                        if (!isMQUp(ex)) {
                            receiveQ = handleMQReconnect(poll_timeout);
                        }
                    }
                    catch (InterruptedException e) {
                        logger.info(this.getClass().getName() + " interrupted.");
                    }
                }
            }
            receiveQ.close();
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
        }
    }

    private boolean isMQUp(MQException ex) {
        return !(ex.reasonCode == MQException.MQRC_CONNECTION_BROKEN
                || ex.reasonCode == MQException.MQRC_Q_MGR_NOT_AVAILABLE || ex.reasonCode == MQException.MQRC_HOBJ_ERROR);
    }

    private MQQueue handleMQReconnect(int timeout) {
        boolean isReconnected = false;// queue.close();
        MQQueue receiveQ = null;

        qMgr = null;

        // MQ Reconnection Logic when reason code 2009 | 2019 | 2059
        while (!isReconnected /* queue.isOpen() */) {
            try {
                // If MQ not up then it throws reason code 2059
                qMgr = new MQQueueManager(qManagerName, props);
                receiveQ = qMgr.accessQueue(receiveQName, mq_open_options, null, null, null);
                isReconnected = true;

                if(logger.isMdwDebugEnabled())
                    logger.mdwDebug("Reconnected to MQ");
            }
            catch (MQException e) {
                handleMQException(e);
                if(logger.isMdwDebugEnabled())
                    logger.mdwDebug("Thread.sleep for = " + timeout);
                try {
                    // MQ may be NOT up, so wait for specified time before next attempt
                    Thread.sleep(timeout);
                }
                catch (InterruptedException e1) {
                    logger.severeException("Exception", e1);
                }
            }
        }

        return receiveQ;

    }

    private void handleMQException(MQException ex) {
        if (ex.reasonCode == 2033) {
            if(logger.isMdwDebugEnabled())
            logger.mdwDebug("no more message for reason=" + ex.reasonCode);
            return;
        }

        if ((reasonsToSuppress != null) && reasonsToSuppress.contains(String.valueOf(ex.reasonCode))
                && (numSuppressedReasonsToShow > 0)) {
            if (lastReason == ex.reasonCode)
                sameReason++;

            if ((sameReason > (numSuppressedReasonsToShow - 1)) && (lastReason == ex.reasonCode)) {

                if(logger.isMdwDebugEnabled())
                    logger.mdwDebug("no more message for reason*=" + ex.reasonCode);

                MQException.logExclude(ex.reasonCode);
            }
            else {
                String msg = "MQ exception in getmsg: Completion code " + ex.completionCode + "Reason code "
                        + ex.reasonCode;
                logger.severeException(msg, ex);

                // if (lastReason != ex.reasonCode)
                // sameReason = 0; // Reset
            }

            lastReason = ex.reasonCode;

        }
        else {
            String msg = "MQ exception in getmsg: Completion code " + ex.completionCode + "Reason code "
                    + ex.reasonCode;
            logger.severeException(msg, ex);
        }

    }

    protected void process_message(MQMessage request) {
        try {
            int msgLen = request.getTotalMessageLength();
            String msgTxt = request.readStringOfCharLength(msgLen);
            String hexMsgId = byteArrayToHexString(request.messageId);
            if (logger.isMdwDebugEnabled())
            {
                logger.mdwDebug("MDWMQListener name = " + mqListenerName+ " MQRECV (" + hexMsgId + "): " + msgTxt);
            }

            if (xmlWrapper != null)
                msgTxt = wrapXml(msgTxt, xmlWrapper);
            Map<String, String> metaInfo = new HashMap<String, String>();
            /* WITHENGINE */metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_MQSERIES);
            /* WITHENGINE */metaInfo.put(Listener.METAINFO_SERVICE_CLASS, this.getClass().getName());
            /* WITHENGINE */metaInfo.put(Listener.METAINFO_HEX_REQUEST_ID, hexMsgId);
            /* WITHENGINE */metaInfo.put(Listener.METAINFO_HEX_CORRELATION_ID,
                    byteArrayToHexString(request.correlationId));
            /* WITHENGINE */metaInfo.put(Listener.METAINFO_REQUEST_ID, new String(request.messageId));
            /* WITHENGINE */metaInfo.put(Listener.METAINFO_CORRELATION_ID, new String(request.correlationId));
            /* WITHENGINE */metaInfo.put("ReplyToQueueName", request.replyToQueueName);
            /* WITHENGINE */ListenerHelper helper = new ListenerHelper();
            /* WITHENGINE */String response = helper.processEvent(msgTxt, metaInfo);
            // STANDALONE String response = null;

            if (need_response) {
                mqPut1(response, request);
                if (logger.isDebugEnabled())
                    logger.debug("MQRESP: " + (response == null ? "null" : response));
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

    protected String wrapXml(String data, String wrapper) {
        StringBuffer sb = new StringBuffer();
        sb.append("<").append(wrapper).append("><![CDATA[");
        sb.append(data);
        sb.append("]]></").append(wrapper).append(">");
        return sb.toString();
    }

    protected MQMessage getMsg(int timeout, MQQueue queue) throws MQException {
        MQMessage retrieveMessage = new MQMessage();
        try {
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.options = gmo.options | mq_get_options;
            gmo.matchOptions = gmo.matchOptions | mq_match_options;
            gmo.waitInterval = timeout;
            queue.get(retrieveMessage, gmo, max_message_size);
            sameReason = 0; // Reset
            return retrieveMessage;
        }
        catch (MQException e) {
            if (isMQUp(e))
                handleMQException(e);
            else
                throw e;
        }
        return null;
    }

    protected void mqPut1(String response, MQMessage requestMessage) throws IOException, MQException {
        MQPutMessageOptions pmo = new MQPutMessageOptions();
        pmo.options = pmo.options | mq_put_options;
        MQMessage putMsg = new MQMessage();
        putMsg.writeString(response);
        putMsg.messageId = requestMessage.messageId;
        putMsg.correlationId = requestMessage.correlationId;
        String replyQueue = requestMessage.replyToQueueName;
        String replyQueueMgr = requestMessage.replyToQueueManagerName;
        qMgr.put(replyQueue, replyQueueMgr, putMsg, pmo);
    }

    protected class MQRunnable implements Runnable {
        private MQMessage _request;

        MQRunnable(MQMessage request) {
            _request = request;
        }

        public void run() {
            process_message(_request);
        }
    }

    private static Properties loadConfig(String configFile) throws Exception {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream(configFile);
        props.load(in);
        in.close();
        return props;
    }

    public static void main(String args[]) throws Exception {
        MDWMQListener mqserver = new MDWMQListener();
        Properties config = loadConfig("mqlistener.config");
        mqserver.init("StandAlone", config);
        mqserver.start();
    }

    public String byteArrayToHexString(byte[] byteArray) {
        int len = byteArray.length;
        StringBuffer hexStr = new StringBuffer();
        for (int j = 0; j < len; j++)
            hexStr.append(byteToHex((char) byteArray[j]));
        return hexStr.toString();
    }

    private String byteToHex(char val) {
        int hi = (val & 0xF0) >> 4;
        int lo = (val & 0x0F);
        return "" + HEX.charAt(hi) + HEX.charAt(lo);
    }

}
