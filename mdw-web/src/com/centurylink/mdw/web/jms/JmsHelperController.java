/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jms;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;

public class JmsHelperController implements ActionListener
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void processAction(ActionEvent actionEvent) throws AbortProcessingException
  {
    JmsHelper jmsHelper = (JmsHelper) FacesVariableUtil.getValue("jmsHelper");
    if (actionEvent.getComponent().getId().equals("sendMessageButton"))
    {
      try
      {
        jmsHelper.sendMessage();
        FacesVariableUtil.addMessage("Message sent to queue: " + jmsHelper.getQueueName());
        String user = FacesVariableUtil.getCurrentUser().getCuid();
        UserActionVO userAction = new UserActionVO(user, Action.Send, Entity.Message, new Long(0), "JMS");
        userAction.setSource("MDW Web Tools");
        EventManager eventMgr = RemoteLocator.getEventManager();
        eventMgr.createAuditLog(userAction);
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        FacesVariableUtil.addMessage(ex.getMessage());
      }
    }
  }
}
