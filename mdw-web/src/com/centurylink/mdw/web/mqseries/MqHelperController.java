/**
* Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
*/


package com.centurylink.mdw.web.mqseries;

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

public class MqHelperController implements ActionListener
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void processAction(ActionEvent actionEvent) throws AbortProcessingException
  {
    MqHelper mqHelper = (MqHelper) FacesVariableUtil.getValue("mqHelper");
    if (actionEvent.getComponent().getId().equals("sendMessageButton"))
    {
      try
      {
        mqHelper.sendMessage();
        FacesVariableUtil.addMessage("Message has been sent.  Correlation ID: " + mqHelper.getCorrelationId());
        String user = FacesVariableUtil.getCurrentUser().getCuid();
        UserActionVO userAction = new UserActionVO(user, Action.Send, Entity.Message, new Long(0), "MQ Series");
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