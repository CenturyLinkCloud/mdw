/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.detail;

import java.net.URL;

import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;

public class MasterTaskActionController
{
  public String displayMasterTaskInstance() throws UIException
  {
    Long masterTaskInstanceId = DetailManager.getInstance().getTaskDetail().getFullTaskInstance().getMasterTaskInstanceId();
    try
    {
      FacesVariableUtil.navigate(new URL(RemoteLocator.getTaskManager().getTaskInstanceUrl(masterTaskInstanceId)));
      return null;  //redirect above
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }
}
