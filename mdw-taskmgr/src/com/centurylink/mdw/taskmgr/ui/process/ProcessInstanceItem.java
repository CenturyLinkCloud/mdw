/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import java.util.Date;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.web.ui.list.ListItem;

public class ProcessInstanceItem extends ListItem
{
  private ProcessInstanceVO instanceInfo;
  public ProcessInstanceVO getInstanceInfo() { return instanceInfo; }
  public void setInstanceInfo(ProcessInstanceVO ii) { this.instanceInfo = ii; }

  public ProcessInstanceItem(ProcessInstanceVO instanceInfo)
  {
    this.instanceInfo = instanceInfo;
  }

  public Long getInstanceId()
  {
    return instanceInfo.getId();
  }

  public Long getProcessInstanceId()
  {
    return getInstanceId();
  }

  public String getMasterRequestId()
  {
    return instanceInfo.getMasterRequestId();
  }

  public String getOwner()
  {
    return instanceInfo.getOwner();
  }

  public Long getOwnerId()
  {
    return instanceInfo.getOwnerId();
  }

  public String getSecondaryOwner()
  {
    return instanceInfo.getSecondaryOwner();
  }

  public Long getSecondaryOwnerId()
  {
    return instanceInfo.getSecondaryOwnerId();
  }

  public String getStatus()
  {
    return WorkStatuses.getWorkStatuses().get(instanceInfo.getStatusCode());
  }

  public String getProcessStatus()
  {
    return getStatus();
  }

  public Date getStartDate()
  {
    return StringHelper.stringToDate(instanceInfo.getStartDate());
  }

  public Date getEndDate()
  {
    return StringHelper.stringToDate(instanceInfo.getEndDate());
  }

  public String getCompletionCode()
  {
    return instanceInfo.getCompletionCode();
  }

  public String getProcessName()
  {
    return instanceInfo.getProcessName();
  }

  /**
   * Special values for process instances are queried variables.
   */
  @Override
  protected Object getAttributeValueSpecial(String name)
  {
    if (instanceInfo == null || instanceInfo.getVariables() == null)
      return null;
    for (VariableInstanceInfo varInstInfo : instanceInfo.getVariables())
    {
      if (name.equals(varInstInfo.getName()))
        return varInstInfo.getStringValue();
    }
    return null;
  }
  /**
   * @return the blvLink
   */
  public String getBlvLink()
  {
    return blvLink;
  }
  /**
   * @param blvLink the blvLink to set
   */
  public void setBlvLink(String blvLink)
  {
    this.blvLink = blvLink;
  }
  private String blvLink;
}
