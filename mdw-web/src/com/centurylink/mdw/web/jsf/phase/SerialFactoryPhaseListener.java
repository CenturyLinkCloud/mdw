/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.phase;

import java.util.Map;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class SerialFactoryPhaseListener implements PhaseListener
{
  private static final long serialVersionUID = 1L;
  private static final String serialFactoryKey = "org.apache.myfaces.SERIAL_FACTORY";

  public PhaseId getPhaseId()
  {
    return PhaseId.ANY_PHASE;
  }
  
  public void beforePhase(PhaseEvent event)
  {
    Map<String,Object> appMap = event.getFacesContext().getExternalContext().getApplicationMap();
    if (appMap.get(serialFactoryKey) == null)
      appMap.put(serialFactoryKey, new org.apache.myfaces.shared_impl.util.serial.DefaultSerialFactory());
  }

  @Override
  public void afterPhase(PhaseEvent event)
  {
  }
}
