/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.events;

import java.util.Date;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.model.value.event.ExternalEventInstanceVO;
import com.centurylink.mdw.taskmgr.ui.detail.ModelWrapper;
import com.centurylink.mdw.taskmgr.ui.process.ProcessStatuses;
import com.centurylink.mdw.web.ui.list.ListItem;

public class ExternalEventItem extends ListItem implements ModelWrapper
{
  private ExternalEventInstanceVO mExternalEventInstanceVO;
  public ExternalEventInstanceVO getExternalEventInstanceVO() { return mExternalEventInstanceVO; }
  public void setExternalEventInstanceVO(ExternalEventInstanceVO vo) { mExternalEventInstanceVO = vo; }

  public ExternalEventItem(ExternalEventInstanceVO externalEventInstanceVO)
  {
    mExternalEventInstanceVO = externalEventInstanceVO;
  }

  public Long getInstanceId()
  {
    return mExternalEventInstanceVO.getExternalEventInstanceId();
  }

  public Long getEventId()
  {
    return mExternalEventInstanceVO.getEventId();
  }

  public String getName()
  {
    return mExternalEventInstanceVO.getEventName();
  }

  public String getData()
  {
    if (mExternalEventInstanceVO.getEventData() == null)
      return null;
    try
    {
      // assume XML
      return XmlObject.Factory.parse(mExternalEventInstanceVO.getEventData()).xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2));
    }
    catch (XmlException ex)
    {
      // guess not
    }
    return mExternalEventInstanceVO.getEventData();
  }

  public String getMasterRequestId()
  {
    return mExternalEventInstanceVO.getMasterRequestId();
  }

  public String getProcessName()
  {
    return mExternalEventInstanceVO.getProcessName();
  }

  public Date getCreateDate()
  {
    return mExternalEventInstanceVO.getCreatedDate();
  }

  public Long getProcessInstanceId()
  {
    return mExternalEventInstanceVO.getProcessInstanceId();
  }

  public Long getProcessId()
  {
    return mExternalEventInstanceVO.getProcessId();
  }

  public Integer getProcessInstanceStatus()
  {
    return mExternalEventInstanceVO.getProcessInstanceStatus();
  }

  public String getProcessInstanceStatusString()
  {
      if (this.getProcessInstanceStatus()==null) return null;
      return ProcessStatuses.decodeProcessStatus(this.getProcessInstanceStatus());
  }

  public String getWrappedId()
  {
    return getInstanceId().toString();
  }

  public Object getWrappedInstance()
  {
    return getExternalEventInstanceVO();
  }
}
