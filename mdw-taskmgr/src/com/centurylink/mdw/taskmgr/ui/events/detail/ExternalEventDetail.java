/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events.detail;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.value.event.ExternalEventInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.taskmgr.ui.detail.Detail;
import com.centurylink.mdw.taskmgr.ui.events.ExternalEventItem;
import com.centurylink.mdw.taskmgr.ui.layout.DetailUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWDataAccess;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Represents a Detail for the user interface for an external event instance.
 */
public class ExternalEventDetail extends Detail {
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  public ExternalEventDetail(DetailUI detailUI)
  {
    super(detailUI);
  }

  private String _ownerType;
  public String getOwnerType() { return _ownerType; }
  public void setOwnerType(String ot) { _ownerType = ot; }

  protected void retrieveInstance(String instanceId) throws UIException
  {
    try
    {
      Long docId;
      if (OwnerType.DOCUMENT.equals(_ownerType))
        docId = new Long(instanceId);
      else
        docId = getOwningDocumentId(new Long(instanceId));
      EventManager evMgr = RemoteLocator.getEventManager();
      ExternalEventInstanceVO instVO = evMgr.getExternalEventInstanceVO(OwnerType.DOCUMENT, docId);
      if (instVO == null)
      {
        instVO = new ExternalEventInstanceVO();
        instVO.setEventName("Event not found for document ID: " + docId);
      }
      setModelWrapper(new ExternalEventItem(instVO));
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      throw new UIException("Error retrieving Event Detail.", ex);
    }
  }
  
  public ExternalEventItem getExternalEventItem()
  {
    return (ExternalEventItem) getModelWrapper();
  }
  
  public String getEventName()
  {
    return getExternalEventItem().getName();
  }
  
  private Long getOwningDocumentId(Long processInstanceId) throws Exception
  {
    RuntimeDataAccess rda = ((MDWDataAccess)FacesVariableUtil.getValue("dataAccess")).getRuntimeDataAccess();
    ProcessInstanceVO topLevel = rda.getProcessInstanceBase(processInstanceId);
    while (topLevel != null)
    {
      if (OwnerType.PROCESS_INSTANCE.equals(topLevel.getOwner()))
        topLevel = rda.getProcessInstanceBase(topLevel.getOwnerId());
      else if (OwnerType.DOCUMENT.equals(topLevel.getOwner()))
        return topLevel.getOwnerId();
      else
        topLevel = null;
    }
    
    return null;
  }
  
}
