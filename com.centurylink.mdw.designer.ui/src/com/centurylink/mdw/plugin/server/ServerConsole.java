/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.views.JavaProcessConsole;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.preferences.model.ServerConsoleSettings;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Console view for MDW Server.
 */
public class ServerConsole extends JavaProcessConsole implements IPropertyChangeListener, PreferenceConstants
{
  // override console id
  public static String CONSOLE_ID = "com.centurylink.mdw.server.ServerConsole";

  private boolean fontChange;
  private Font font;
  public Font getFont() { return font; }
  public void setFont(Font f)
  {
    font = f;
    fontChange = true;
  }
  private Color fontColor;
  public Color getFontColor() { return fontColor; }
  public void setFontColor(Color c)
  {
    fontColor = c;
    fontChange = true;
  }
  private Color backgroundColor;
  public Color getBackgroundColor() { return backgroundColor; }
  public void setBackgroundColor(Color c)
  {
    backgroundColor = c;
    fontChange = true;
  }

  // actions
  private Action startAction;
  private Action deployAction;
  private Action stopAction;
  private Action clientShellAction;

  public void setDeployText(String dt)
  {
    if (deployAction != null)
      deployAction.setToolTipText(dt);
  }

  private boolean hasClientShell;
  public void setHasClientShell(boolean hasClientShell)
  {
    this.hasClientShell = hasClientShell;
    if (clientShellAction != null)
    {
      clientShellAction.setEnabled(hasClientShell);
      clientShellAction.setToolTipText(hasClientShell ? "Client Shell" : "");
    }
  }

  public void setStartText(String st)
  {
    if (startAction != null)
      startAction.setToolTipText(st);
  }

  public static ServerConsole find()
  {
    return (ServerConsole)find(CONSOLE_ID);
  }

  /**
   * @see org.eclipse.ui.IWorkbenchPart#createPartControl(Composite)
   */
  public void createPartControl(Composite parent)
  {
    addToMenuAndToolbar();
    super.createPartControl(parent);

    setBufferSize(getPrefsBufferSize());
    setFont(getPrefsFont());
    setFontColor(getPrefsFontColor());
    setBackgroundColor(getPrefsBackgroundColor());

    // add the property change listener
    IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
    store.addPropertyChangeListener(this);
  }

  public void addToMenuAndToolbar()
  {
    // start action
    startAction = new Action("Start")
    {
      public void run()
      {
        WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
        actionHandler.run(getRunnableObject());
      }
    };
    startAction.setImageDescriptor(MdwPlugin.getImageDescriptor("icons/run.gif"));
    Object runnableObj = getRunnableObject();
    String startText = "Start";
    if (runnableObj instanceof WorkflowProject)
      startText += " " + ((WorkflowProject)runnableObj).getName();
    else if (runnableObj instanceof ServerSettings)
      startText += " " + ((ServerSettings)runnableObj).getServerName();
    startAction.setToolTipText(startText);
    startAction.setEnabled(runnableObj != null);

    // client shell action
    clientShellAction = new Action("Client Shell")
    {
      public void run()
      {
        WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
        actionHandler.clientShell(getRunnableObject());
      }
    };
    clientShellAction.setImageDescriptor(MdwPlugin.getImageDescriptor("icons/client_shell.gif"));
    clientShellAction.setToolTipText("Client Shell");
    clientShellAction.setEnabled(false);

    // deploy action
    deployAction = new Action("Deploy")
    {
      public void run()
      {
        WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
        actionHandler.deploy(getRunnableObject());
      }
    };
    deployAction.setImageDescriptor(MdwPlugin.getImageDescriptor("icons/deploy.gif"));
    deployAction.setToolTipText("Deploy");
    deployAction.setEnabled(false);

    // stop action
    stopAction = new Action("Stop")
    {
      public void run()
      {
        WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
        actionHandler.stop(getRunnableObject());
      }
    };
    stopAction.setImageDescriptor(MdwPlugin.getImageDescriptor("icons/stop.gif"));
    stopAction.setToolTipText("Stop");
    stopAction.setEnabled(false);

    // create menu
    getViewSite().getActionBars().getMenuManager().add(startAction);
    //getViewSite().getActionBars().getMenuManager().add(clientShellAction);
    getViewSite().getActionBars().getMenuManager().add(deployAction);
    getViewSite().getActionBars().getMenuManager().add(stopAction);
    getViewSite().getActionBars().getMenuManager().add(new Separator());

    // create toolbar
    getViewSite().getActionBars().getToolBarManager().add(startAction);
    getViewSite().getActionBars().getToolBarManager().add(clientShellAction);
    getViewSite().getActionBars().getToolBarManager().add(deployAction);
    getViewSite().getActionBars().getToolBarManager().add(stopAction);
    getViewSite().getActionBars().getToolBarManager().add(new Separator());
  }

  public void print(String text, int lineType)
  {
    if (fontChange)
    {
      getViewer().getTextWidget().setFont(getFont());
      getViewer().getTextWidget().setForeground(getFontColor());
      getViewer().getTextWidget().setBackground(getBackgroundColor());
      fontChange = false;
    }
    super.print(text, lineType);
  }

  /**
   * for PropertyChangeListener
   */
  public void propertyChange(PropertyChangeEvent e)
  {
    if (e.getProperty().equals(PREFS_SERVER_CONSOLE_BUFFER_SIZE))
    {
      setBufferSize(getPrefsBufferSize());
    }
    if (e.getProperty().equals(PREFS_SERVER_CONSOLE_FONT))
    {
      setFont(getPrefsFont());
      getViewer().getTextWidget().setFont(font);
    }
    else if (e.getProperty().equals(PREFS_SERVER_CONSOLE_FONT_RED)
             || e.getProperty().equals(PREFS_SERVER_CONSOLE_FONT_GREEN)
             || e.getProperty().equals(PREFS_SERVER_CONSOLE_FONT_BLUE))
    {
      setFontColor(getPrefsFontColor());
      getViewer().getTextWidget().setForeground(fontColor);
    }
    else if (e.getProperty().equals(PREFS_SERVER_CONSOLE_BG_RED)
             || e.getProperty().equals(PREFS_SERVER_CONSOLE_BG_GREEN)
             || e.getProperty().equals(PREFS_SERVER_CONSOLE_BG_BLUE))
    {
     setBackgroundColor(getPrefsBackgroundColor());
     getViewer().getTextWidget().setBackground(backgroundColor);
    }
    else if (e.getProperty().equals(PREFS_SERVER_RUNNING))
    {
      boolean running = MdwPlugin.getDefault().getPreferenceStore().getBoolean(PREFS_SERVER_RUNNING);
      String serverName = MdwPlugin.getDefault().getPreferenceStore().getString(PREFS_RUNNING_SERVER);
      startAction.setEnabled(!running);
      if (serverName.isEmpty())
      {
        deployAction.setEnabled(running);
      }
      else
      {
        ServerSettings serverSettings = getServerSettings(serverName);
        boolean hasProject = serverSettings != null && serverSettings.getProject() != null;
        deployAction.setToolTipText("Deploy to " + serverName);
        deployAction.setEnabled(running && hasProject);
      }

      clientShellAction.setEnabled(running ? hasClientShell : false);
      stopAction.setEnabled(running);
    }
    else if (e.getProperty().equals(PREFS_SERVER_WF_PROJECT))
    {
      String projName = MdwPlugin.getDefault().getPreferenceStore().getString(PREFS_SERVER_WF_PROJECT);
      startAction.setToolTipText("Start " + projName);
      clientShellAction.setToolTipText(hasClientShell ? "Client Shell" : "");
      deployAction.setToolTipText("Deploy " + " " + projName);
      stopAction.setToolTipText("Stop " + projName);
    }
    else if (e.getProperty().equals(PREFS_RUNNING_SERVER))
    {
      String serverName = MdwPlugin.getDefault().getPreferenceStore().getString(PREFS_RUNNING_SERVER);
      startAction.setToolTipText("Start " + serverName);
      deployAction.setToolTipText("Deploy to " + serverName);
    }
  }

  private ServerConsoleSettings getSettings()
  {
    return MdwPlugin.getSettings().getServerConsoleSettings();
  }

  private int getPrefsBufferSize()
  {
    return getSettings().getBufferSize();
  }

  private Font getPrefsFont()
  {
    return new Font(getViewer().getControl().getDisplay(), getSettings().getFontData());
  }

  private Color getPrefsFontColor()
  {
    return new Color(getViewer().getControl().getDisplay(), getSettings().getFontRgb());
  }

  private Color getPrefsBackgroundColor()
  {
    return new Color(getViewer().getControl().getDisplay(), getSettings().getBackgroundRgb());
  }

  /**
   * dispose of the text viewer and its resources
   */
  public void dispose()
  {
    super.dispose();
    IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
    store.removePropertyChangeListener(this);
  }

  /**
   * @return whether or not this console can be written to
   */
  public boolean isActive()
  {
    return getViewer().getTextWidget() != null;
  }

  private IServer getServer(String name)
  {
    for (IServer server : ServerCore.getServers())
    {
      if (server.getName().equals(name))
      {
        return server;
      }
    }
    return null;
  }

  private ServerSettings getServerSettings(String serverName)
  {
    IServer server = getServer(serverName);
    if (server == null)
      return null;

    ServiceMixServerBehavior serverBehave = null;
    if (server.getServerType().getId().startsWith(ServiceMixServer.ID_PREFIX))
      serverBehave = (ServiceMixServerBehavior) server.loadAdapter(ServiceMixServerBehavior.class, null);
    else if (server.getServerType().getId().startsWith(FuseServer.ID_PREFIX))
      serverBehave = (FuseServerBehavior) server.loadAdapter(FuseServerBehavior.class, null);

    if (serverBehave == null)
      return null;

    return serverBehave.getServerSettings();
  }

  private IPreferenceStore getPrefStore()
  {
    return MdwPlugin.getDefault().getPreferenceStore();
  }

  private Object getRunnableObject()
  {
    String runningServer = getPrefStore().getString(PREFS_RUNNING_SERVER);
    if (runningServer.isEmpty())
    {
      String proj = getPrefStore().getString(PREFS_SERVER_WF_PROJECT);
      return WorkflowProjectManager.getInstance().getWorkflowProject(proj);
    }
    else
    {
      return getServerSettings(runningServer);
    }
  }

}