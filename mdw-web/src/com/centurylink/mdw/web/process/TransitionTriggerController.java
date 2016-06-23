/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.process;

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

public class TransitionTriggerController implements ActionListener
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  public void processAction(ActionEvent actionEvent) throws AbortProcessingException
  {
    TransitionTrigger transitionTrigger = (TransitionTrigger) FacesVariableUtil.getValue("transitionTrigger");
    if (actionEvent.getComponent().getId().equals("sendMessagesButton"))
    {
      try
      {
        transitionTrigger.sendMessages();
        FacesVariableUtil.addMessage("Messages have been sent.  Instance count: " + transitionTrigger.getInstanceCount());
        String user = FacesVariableUtil.getCurrentUser().getCuid();
        UserActionVO userAction = new UserActionVO(user, Action.Trigger, Entity.Transition, new Long(0), transitionTrigger.getEvent());
        userAction.setSource("MDW Web Tools");
        EventManager eventMgr = RemoteLocator.getEventManager();
        eventMgr.createAuditLog(userAction);        
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        FacesVariableUtil.addMessage(ex.toString());
      }
    }    
  } 
}
