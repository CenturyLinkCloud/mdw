/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import com.centurylink.mdw.plugin.project.model.ServerSettings;

public class JBossServerConfigurator extends ServerConfigurator
{
  public JBossServerConfigurator(ServerSettings serverSettings)
  {
    super(serverSettings);
  }

  public void doConfigure(Shell shell)
  {
    MessageDialog.openWarning(shell, "TODO", "Not yet implemented for JBoss");
  }

  public void doDeploy(Shell shell)
  {
    MessageDialog.openWarning(shell, "TODO", "Not yet implemented for JBoss");
  }

  public String launchNewServerCreation(Shell shell)
  {

    MessageDialog.openWarning(shell, "TODO", "Not yet implemented for JBoss");
    return null;
  }

  public void parseServerAdditionalInfo()
  throws IOException, SAXException, ParserConfigurationException
  {
    // TODO
  }

  @Override
  public String[] getEnvironment(boolean debug)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getCommandDir()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getStartCommand()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getStopCommand()
  {
    // TODO Auto-generated method stub
    return null;
  }

}
