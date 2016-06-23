/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.centurylink.mdw.web.util.WebUtil;
import com.qwest.appsec.actrl.AccessControl;
import com.qwest.appsec.actrl.AccessControlFactory;
import com.qwest.appsec.actrl.BasicCredential;

public class LdapLoginController implements ActionListener
{
  private static final String DEFAULT_FILTER_CONFIG_FILE = "CTAPPFilter.config";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  private static boolean allowAnyAuthenticatedUser;

  static
  {
    InputStream inStream = null;

    try
    {
      inStream = FileHelper.openConfigurationFile(DEFAULT_FILTER_CONFIG_FILE, LdapLoginController.class.getClassLoader());
      Properties props = new Properties();
      props.load(inStream);
      allowAnyAuthenticatedUser = Boolean.valueOf(props.getProperty(MiscConstants.AllowAnyAuthenticatedUser));
    }
    catch (IOException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }
    finally
    {
      if (inStream != null)
      {
        try
        {
          inStream.close();
        }
        catch (IOException e)
        {
          logger.severeException(e.getMessage(), e);
        }
      }
    }
  }

  public void processAction(ActionEvent actionEvent) throws AbortProcessingException
  {
    LdapLogin ldapLogin = (LdapLogin) FacesVariableUtil.getValue("ldapLogin");
    if (actionEvent.getComponent().getId().equals("loginButton"))
    {
      try
      {
        String ctEnv = WebUtil.getRuntimeEnv();
        if (!"prod".equals(ctEnv))
          ctEnv = "test";
        System.setProperty("com.qwest.appsec.actrl.ctenv", "employee." + ctEnv);
        System.setProperty("com.qwest.appsec.actrl.applName", ApplicationContext.getApplicationName());
        AccessControl accessControl = AccessControlFactory.getInstance();
        BasicCredential basicCred = new BasicCredential(ldapLogin.getUser(), ldapLogin.getPassword());
        accessControl.authenticate(basicCred);

        AuthenticatedUser user = RemoteLocator.getUserManager().loadUser(ldapLogin.getUser());
        if (user == null && allowAnyAuthenticatedUser)
          user = new AuthenticatedUser(ldapLogin.getUser());
        FacesVariableUtil.setValue("authenticatedUser", user);
        logger.info("Authenticated User: " + user.getCuid());
        logger.mdwDebug("Auth User Details:\n" + user);
        FacesVariableUtil.navigate(getWelcomePath());
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        FacesVariableUtil.addMessage(ex.toString());
      }
    }
  }

  public URL getWelcomePath() throws MalformedURLException
  {
    return new URL(ApplicationContext.getMdwWebUrl() + ApplicationContext.getMdwWebWelcomePath());
  }
}
