/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.faces.model.SelectItem;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.bpm.EventMessageDocument;
import com.centurylink.mdw.bpm.EventTypeDocument;
import com.centurylink.mdw.bpm.WorkTypeDocument;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.JMSDestinationNames;
import com.centurylink.mdw.common.exception.ServiceLocatorException;
import com.centurylink.mdw.common.utilities.JMSServices;

public class TransitionTrigger
{
  private static final String JMS_QUEUE = JMSDestinationNames.PROCESS_HANDLER_QUEUE;

  private String event;
  public String getEvent() { return event; }
  public void setEvent(String event) { this.event = event; }

  private String workType = "ACTIVITY";
  public String getWorkType() { return workType; }
  public void setWorkType(String workType) { this.workType = workType; }

  private String completionCode;
  public String getCompletionCode() { return completionCode; }
  public void setCompletionCode(String completionCode) { this.completionCode = completionCode; }

  private String contextUrl = "t3://<cluser address>";
  public String getContextUrl() { return contextUrl; }
  public void setContextUrl(String contextUrl) { this.contextUrl = contextUrl; }

  private String transitionInstances;
  public String getTransitionInstances() { return transitionInstances; }
  public void setTransitionInstances(String transitionInstances) { this.transitionInstances = transitionInstances; }

  private int instanceCount;
  public int getInstanceCount() { return instanceCount; }


  public void sendMessages() throws NamingException, SQLException, JMSException, ServiceLocatorException
  {
    DataSource ds = ApplicationContext.getMdwDataSource();

    JMSServices jmsServices = JMSServices.getInstance();
    QueueConnectionFactory factory = jmsServices.getQueueConnectionFactory(contextUrl);
    Queue queue = jmsServices.getQueue(contextUrl, JMS_QUEUE);
    Connection conn = ds.getConnection();
    String transitions = this.transitionInstances.replaceAll("\r", "");
    StringTokenizer tk = new StringTokenizer(transitions, "\n");
    instanceCount = 0;
    while (tk.hasMoreTokens())
    {
      String token = tk.nextToken();
      if (token.length() == 0)
        continue;
      String msg = createMessage(new Long(token), conn,
          WorkTypeDocument.WorkType.Enum.forString(workType.toUpperCase()), EventTypeDocument.EventType.Enum.forString(event), completionCode);
      sendMessage(factory, queue, msg);
      instanceCount++;
    }
    conn.close();
  }

  private String createMessage(Long wtInstanceId, Connection conn, WorkTypeDocument.WorkType.Enum workType,
      EventTypeDocument.EventType.Enum event, String completionCode) throws SQLException
  {
    EventMessageDocument doc = EventMessageDocument.Factory.newInstance();
    doc.addNewEventMessage();
    String[] ids = getWorkTransitionProcessInstanceIds(wtInstanceId, conn);
    if (ids[0] == null || ids[1] == null || ids[2] == null)
        throw new SQLException("Matching data not found for work transition instance id: " + wtInstanceId);
    long activityId = Long.parseLong(ids[1]);
    long processInstId = Long.parseLong(ids[0]);
    String masterRequestId = ids[2];
    doc.getEventMessage().setEventType(event);
    doc.getEventMessage().setWorkId(activityId);
    doc.getEventMessage().setWorkType(workType);
    doc.getEventMessage().setWorkOwnerId(processInstId);
    doc.getEventMessage().setWorkOwnerType("PROCESS_INSTANCE");
    doc.getEventMessage().setMasterRequestId(masterRequestId);
    doc.getEventMessage().setWorkTransitionInstanceId(wtInstanceId.longValue());
    doc.getEventMessage().setWorkCompletionCode(completionCode);
    return doc.xmlText();
  }

  private String[] getWorkTransitionProcessInstanceIds(Long wtInstanceId, Connection conn) throws SQLException
  {
    String[] ids = { null, null, null };
    String sql = "SELECT PROCESS_INST_ID, wt.TO_WORK_ID, pi.MASTER_REQUEST_ID FROM WORK_TRANSITION_INSTANCE wti, WORK_TRANSITION wt, PROCESS_INSTANCE pi "
        + " WHERE wti.WORK_TRANS_ID = wt.WORK_TRANS_ID AND wti.WORK_TRANS_INST_ID = ? AND wti.PROCESS_INST_ID = pi.PROCESS_INSTANCE_ID";
    PreparedStatement ps = conn.prepareStatement(sql);
    ps.setLong(1, wtInstanceId.longValue());
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      ids[0] = Long.toString(rs.getLong(1));
      ids[1] = Long.toString(rs.getLong(2));
      ids[2] = (rs.getString(3));
    }
    rs.close();
    ps.close();
    return ids;
  }

  private void sendMessage(QueueConnectionFactory factory, Queue queue, String message) throws JMSException
  {
    QueueConnection qConnection = factory.createQueueConnection();
    QueueSession qSession = qConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

    QueueSender qSender = qSession.createSender(queue);
    TextMessage jmsMessage = qSession.createTextMessage();
    qConnection.start();

    jmsMessage.setText(message);
    qSender.send(jmsMessage, DeliveryMode.PERSISTENT, TextMessage.DEFAULT_DELIVERY_MODE, TextMessage.DEFAULT_TIME_TO_LIVE);

    qSender.close();
    qSession.close();
    qConnection.close();
  }


  public List<SelectItem> getEventNames()
  {
    List<SelectItem> items = new ArrayList<SelectItem>();
    items.add(new SelectItem("START"));
    items.add(new SelectItem("FINISH"));
    items.add(new SelectItem("ERROR"));
    items.add(new SelectItem("DELAY"));
    items.add(new SelectItem("HOLD"));
    items.add(new SelectItem("ABORT"));
    items.add(new SelectItem("CORRECT"));
    return items;
  }

  public List<SelectItem> getQueueNames()
  {
    List<SelectItem> items = new ArrayList<SelectItem>();

    return items;
  }

}
