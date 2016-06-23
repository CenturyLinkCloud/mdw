/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jms;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import com.centurylink.mdw.common.constant.JMSDestinationNames;
import com.centurylink.mdw.common.exception.ServiceLocatorException;
import com.centurylink.mdw.common.utilities.JMSServices;
import com.centurylink.mdw.common.utilities.StringHelper;

public class JmsHelper
{
  private String endpoint = "<Internal>";
  public String getEndpoint() { return endpoint; }
  public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

  private String queueName;
  public String getQueueName() { return queueName; }
  public void setQueueName(String queueName) { this.queueName = queueName; }

  private String jmsMessage;
  public String getJmsMessage() { return jmsMessage; }
  public void setJmsMessage(String message) { this.jmsMessage = message; }

  private String response;
  public String getResponse() { return response; }

  public void sendMessage()
  throws NamingException, ServiceLocatorException, JMSException
  {
    String contextUrl = StringHelper.isEmpty(endpoint) || endpoint.equals("<Internal>") ? null : endpoint;

    JMSServices jmsServices = JMSServices.getInstance();
    QueueConnectionFactory factory = jmsServices.getQueueConnectionFactory(contextUrl);
    QueueConnection connection = factory.createQueueConnection();
    QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    connection.start();
    Queue queue = jmsServices.getQueue(session, queueName);

    QueueSender sender = session.createSender(queue);
    TextMessage msg = session.createTextMessage(jmsMessage);
    Queue replyQueue = session.createTemporaryQueue();
    msg.setJMSReplyTo(replyQueue);
    sender.send(msg);
    MessageConsumer consumer = session.createConsumer(replyQueue);
    TextMessage responseMsg = (TextMessage)consumer.receive(10000);
    if (responseMsg == null)
      response = null;
    else
      response = responseMsg.getText();
  }

  public List<SelectItem> getQueueNames()
  {
    List<SelectItem> queueNames = new ArrayList<SelectItem>();

    queueNames.add(new SelectItem(JMSDestinationNames.EXTERNAL_EVENT_HANDLER_QUEUE));
    queueNames.add(new SelectItem(JMSDestinationNames.INTRA_MDW_EVENT_HANDLER_QUEUE));
    queueNames.add(new SelectItem(JMSDestinationNames.PROCESS_HANDLER_QUEUE));
    queueNames.add(new SelectItem(JMSDestinationNames.CONFIG_HANDLER_TOPIC));

    return queueNames;
  }
}
