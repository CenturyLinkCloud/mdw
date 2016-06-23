/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.phase;

import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

/**
 * Phase listener for logging JSF validation messages.
 */
public class ValidationLoggerPhaseListener implements PhaseListener
{
  private static final long serialVersionUID = 1L;
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  public PhaseId getPhaseId()
  {
    return PhaseId.RENDER_RESPONSE;
  }
  
  public void beforePhase(PhaseEvent event)
  {
    logValidationMessages(event.getFacesContext());
  }
  
  public void afterPhase(PhaseEvent event)
  {
    // do nothing
  }
  
  private void logValidationMessages(FacesContext facesContext)
  {
    if (logger.isInfoEnabled())
    {
      Map<String,List<FacesMessage>> messages = FacesVariableUtil.getMessages();
      if (messages != null && !messages.isEmpty())
      {
        for (String clientId : messages.keySet())
        {
          logger.info("JSF Validation Messages for Client ID='" + clientId + "':");
          for (FacesMessage message : messages.get(clientId))
            logger.info("  " + message.getSummary() + ": " + message.getDetail());
        }
      }
    }
  }
}
