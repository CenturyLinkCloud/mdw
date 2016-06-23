/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.centurylink.mdw.common.cache.impl.WebPageCache;

public class MDWStartupListener implements ServletContextListener
{
  @Override
  public void contextInitialized(ServletContextEvent contextEvent)
  {
    // need to clear web page cache because in local dev iterations
    // webapp classloader may be reinitialized while ear lib classloader was not
    // (causing annoying class mismatch inconsistencies)
    WebPageCache.clear();
  }

  @Override
  public void contextDestroyed(ServletContextEvent contextEvent)
  {
  }
  
}
