/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;

public class ProcessInstanceLabelProvider extends LabelProvider implements ITableLabelProvider
{
  public String getColumnText(Object element, int columnIndex)
  {
    ProcessInstanceVO processInstanceInfo = (ProcessInstanceVO) element;

    switch (columnIndex)
    {
      case 0:
        return processInstanceInfo.getId().toString();
      case 1:
        return processInstanceInfo.getMasterRequestId();
      case 2:
        return processInstanceInfo.getOwner();
      case 3:
        return processInstanceInfo.getOwnerId().toString();
      case 4:
        return lookupStatus(processInstanceInfo.getStatusCode());
      case 5:
        if (processInstanceInfo.getStartDate() == null)
          return "";
        return processInstanceInfo.getStartDate();
      case 6:
        if (processInstanceInfo.getEndDate() == null)
          return "";
        return processInstanceInfo.getEndDate();
      default:
        return null;
    }
  }

  private String lookupStatus(int statusCode)
  {
    return WorkStatuses.getWorkStatuses().get(new Integer(statusCode));
  }

  public Image getColumnImage(Object element, int columnIndex)
  {
    return null;
  }
}
