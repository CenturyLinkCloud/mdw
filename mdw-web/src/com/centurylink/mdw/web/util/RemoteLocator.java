/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.util;

import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;

/**
 * May be used in the future to wrap a different mechanism for accessing
 * service beans from webapps.
 */
public class RemoteLocator
{
  public static TaskManager getTaskManager()
  {
    return ServiceLocator.getTaskManager();
  }

  public static UserManager getUserManager()
  {
    return ServiceLocator.getUserManager();
  }

  public static EventManager getEventManager()
  {
	return ServiceLocator.getEventManager();
  }
}
