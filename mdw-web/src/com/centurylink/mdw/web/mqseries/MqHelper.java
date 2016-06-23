/**
* Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
*/

package com.centurylink.mdw.web.mqseries;

import java.util.Hashtable;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;

public class MqHelper
{
  public static final String PROP_QUEUE_MANAGER_NAME = "QueueManagerName";

  public static final String PROP_REQUEST_QUEUE_NAME = "QueueName";

  public static final String PROP_REPLY_QUEUE_NAME = "ReplyQueueName";

  public static final String PROP_HOST_NAME = "HostName";

  public static final String PROP_CHANNEL_NAME = "ChannelName";

  public static final String PROP_PORT = "Port";

  private StandardLogger logger;

  // per-call meta data
  public static final String PROP_CORRELATION_ID = "CorrelationId";

  private String replyQueueName;

  private boolean isSynchronous;

  private static final int defaultTimeout = 120;

  private MQQueueManager qmgr;

  private String hostName = "suomt70i.dev.qintra.com";

  public String getHost()
  {
    return hostName;
  }

  public void setHost(String hostName)
  {
    this.hostName = hostName;
  }

  private String port = "1414";

  public String getPort()
  {
    return port;
  }

  public void setPort(String port)
  {
    this.port = port;
  }

  private String qmgr_name = "SUOMT70IQM2";

  public String getQueueManager()
  {
    return qmgr_name;
  }

  public void setQueueManager(String qmgr_name)
  {
    this.qmgr_name = qmgr_name;
  }

  private String queue_name = "QL_OCOD_IOM_REQUEST";

  public String getQueue()
  {
    return queue_name;
  }

  public void setQueue(String queue_name)
  {
    this.queue_name = queue_name;
  }

  private String channelName = "SYSTEM.DEF.SVRCONN";

  public String getChannel()
  {
    return channelName;
  }

  public void setChannel(String channelName)
  {
    this.channelName = channelName;
  }

  private String message;

  public String getMessage()
  {
    return message;
  }

  public void setMessage(String message)
  {
    this.message = message;
  }

  private String correlationId;

  public String getCorrelationId()
  {
    return correlationId;
  }

  public void sendMessage() throws Exception
  {
    int timeout = 300;

    openConnection();

    try
    {
      int msglen;
      MQMessage msg = new MQMessage();
      msg.format = MQConstants.MQFMT_STRING;
      msg.writeString(message);
      int openQueueOptions = getOpenQueueOptions();
      MQPutMessageOptions putOptions = new MQPutMessageOptions();
      putOptions.options = putOptions.options | getPutQueueOptions();

      this.correlationId = String.valueOf(System.currentTimeMillis() % 1000);

      if (this.correlationId != null)
        msg.correlationId = this.correlationId.getBytes();
      if (replyQueueName != null)
        msg.replyToQueueName = replyQueueName;

      MQQueue replyQueue;
      if (isSynchronous)
      {
        if (replyQueueName == null)
          replyQueueName = "REPLY_MODEL_QUEUE";
        String dynamicQueueName = replyQueueName + "*";
        replyQueue = qmgr.accessQueue(replyQueueName, openQueueOptions, null, dynamicQueueName,
            null);
        msg.replyToQueueName = replyQueue.getName();
      }
      else
        replyQueue = null;
      qmgr.put(queue_name, msg);
      logger.trace("Sent Message: "+msg.readLine());
      if (replyQueue != null)
      {
        /* read reply */
        MQGetMessageOptions getOptions = new MQGetMessageOptions();
        getOptions.options = getOptions.options | getGetQueueuOptions();
        getOptions.matchOptions = getOptions.matchOptions | getMatchQueueOptions();
        if (timeout <= 0)
          timeout = defaultTimeout;
        getOptions.waitInterval = timeout * 1000;
        MQMessage rmsg = new MQMessage();
        rmsg.messageId = msg.messageId;
        rmsg.correlationId = msg.correlationId;
        replyQueue.get(rmsg, getOptions, 1024 * 1024);
        msglen = rmsg.getTotalMessageLength();
        String result = rmsg.readStringOfCharLength(msglen);
        logger.trace("Sent message:\\n" + message);
        logger.trace("Return message:\\n" + result);
      }
      qmgr.disconnect();
      qmgr = null;
    }
    catch (Exception e)
    {
      logger.severe(e.getMessage());
      e.printStackTrace();
    }
  }

  public void openConnection() throws ConnectionException
  {
    try
    {
      logger = LoggerUtil.getStandardLogger();
      Hashtable<String, Object> props = new Hashtable<String, Object>();

      logger.trace("hostName=" + hostName);
      logger.trace("channelName=" + channelName);
      logger.trace("port=" + port);

      if (!StringHelper.isEmpty(hostName))
      {
        props.put(MQConstants.HOST_NAME_PROPERTY, hostName);
        props.put(MQConstants.CHANNEL_PROPERTY, channelName);
        props.put(MQConstants.PORT_PROPERTY, Integer.valueOf(port));
      }
      qmgr = new MQQueueManager(qmgr_name, props);
    }
    catch (Exception e)
    {
      throw new ConnectionException(ConnectionException.CONNECTION_DOWN,
          "Failed to open connection to MQ server", e);
    }
  }

  /**
   * Default options used by MDW is MQC.MQGMO_WAIT Override this method if client application wants
   * to set different get queue options.
   *
   * @return
   */
  protected int getGetQueueuOptions()
  {
    return MQConstants.MQGMO_WAIT;
  }

  /**
   * Default options used by MDW is MQC.MQPMO_SET_IDENTITY_CONTEXT Override this method if client
   * application wants to set different put queue options.
   *
   * @return
   */
  protected int getPutQueueOptions()
  {
    return MQConstants.MQPMO_SET_IDENTITY_CONTEXT;
  }

  /**
   * Default options used by MDW is MQC.MQOO_INPUT_AS_Q_DEF|MQC.MQOO_OUTPUT|MQC.MQOO_BROWSE Override
   * this method if client application wants to set different open queue options.
   *
   * @return
   */
  protected int getOpenQueueOptions()
  {
    return MQConstants.MQOO_INPUT_AS_Q_DEF | MQConstants.MQOO_OUTPUT | MQConstants.MQOO_BROWSE;
  }

  /**
   * Default options used by MDW is MQC.MQMO_MATCH_MSG_ID Override this method if client application
   * wants to set different matched queue options.
   *
   * @return
   */
  protected int getMatchQueueOptions()
  {
    return MQConstants.MQMO_MATCH_MSG_ID;
  }
}
