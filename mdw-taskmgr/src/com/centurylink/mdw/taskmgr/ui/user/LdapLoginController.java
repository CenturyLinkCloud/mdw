/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.user;

import java.net.MalformedURLException;
import java.net.URL;

import com.centurylink.mdw.common.ApplicationContext;

public class LdapLoginController extends com.centurylink.mdw.web.ldap.LdapLoginController
{
  @Override
  public URL getWelcomePath() throws MalformedURLException
  {
    return new URL(ApplicationContext.getTaskManagerUrl() + ApplicationContext.getTaskWelcomePath());
  }  
}
