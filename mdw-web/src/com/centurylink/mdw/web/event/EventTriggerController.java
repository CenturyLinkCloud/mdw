/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.event;

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


public class EventTriggerController implements ActionListener
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  public void processAction(ActionEvent actionEvent) throws AbortProcessingException
  {
    EventTrigger eventTrigger = (EventTrigger) FacesVariableUtil.getValue("eventTrigger");
    if (actionEvent.getComponent().getId().equals("sendMessageButton"))
    {
      try
      {
        eventTrigger.sendMessage();
        FacesVariableUtil.addMessage("Message has been sent");
        String user = FacesVariableUtil.getCurrentUser().getCuid();
        UserActionVO userAction = new UserActionVO(user, Action.Trigger, Entity.Event, new Long(0), "External Event");
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
