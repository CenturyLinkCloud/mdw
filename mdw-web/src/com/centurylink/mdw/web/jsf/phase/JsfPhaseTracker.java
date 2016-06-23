/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.phase;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

public class JsfPhaseTracker implements PhaseListener
{
  private static final String PHASE_PARAM = "com.centurylink.mdw.web.util.phaseTracker.phase";
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  private static String phase = null;

  public PhaseId getPhaseId()
  {

    PhaseId phaseId = PhaseId.ANY_PHASE;

    if (phase == null)
    {
      FacesContext context = FacesContext.getCurrentInstance();
      if (context == null)
        return phaseId;
      phase = (String) context.getExternalContext().getInitParameter(PHASE_PARAM);
    }

    if (phase != null)
    {
      if ("RESTORE_VIEW".equals(phase))
        phaseId = PhaseId.RESTORE_VIEW;
      else if ("APPLY_REQUEST_VALUES".equals(phase))
        phaseId = PhaseId.APPLY_REQUEST_VALUES;
      else if ("PROCESS_VALIDATIONS".equals(phase))
        phaseId = PhaseId.PROCESS_VALIDATIONS;
      else if ("UPDATE_MODEL_VALUES".equals(phase))
        phaseId = PhaseId.UPDATE_MODEL_VALUES;
      else if ("INVOKE_APPLICATION".equals(phase))
        phaseId = PhaseId.INVOKE_APPLICATION;
      else if ("RENDER_RESPONSE".equals(phase))
        phaseId = PhaseId.RENDER_RESPONSE;
      else if ("ANY_PHASE".equals(phase))
        phaseId = PhaseId.ANY_PHASE;
    }
    return phaseId;
  }

  public void beforePhase(PhaseEvent e)
  {
    logger.info("BEFORE " + e.getPhaseId());
  }

  public void afterPhase(PhaseEvent e)
  {
    logger.info("AFTER " + e.getPhaseId());
  }
}
