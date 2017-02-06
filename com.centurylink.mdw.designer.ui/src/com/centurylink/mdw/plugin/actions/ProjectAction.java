/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.actions;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.LocalCloudProjectWizard;
import com.centurylink.mdw.plugin.project.RemoteWorkflowProjectWizard;
import com.centurylink.mdw.plugin.workspace.WorkspaceConfig;
import com.centurylink.mdw.plugin.workspace.WorkspaceConfigWizard;

public class ProjectAction extends BasePulldownAction
{
  public static final String MENU_SEL_NEW_LOCAL_PROJECT = "New Local Project";
  public static final String MENU_SEL_NEW_REMOTE_PROJECT = "New Remote Project";
  public static final String MENU_SEL_NEW_CLOUD_PROJECT = "New Cloud Project";
  public static final String MENU_SEL_CONFIGURE_WORKSPACE = "Configure Workspace";

  /**
   * populates the plugin action menu (the mdw icon) with its items
   */
  public void populateMenu(Menu menu)
  {
    // new local project
    MenuItem item = new MenuItem(menu, SWT.NONE);
    item = new MenuItem(menu, SWT.NONE);
    item.setText(MENU_SEL_NEW_CLOUD_PROJECT + "...");
    item.setImage(MdwPlugin.getImageDescriptor("icons/cloud_project.gif").createImage());
    item.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          newCloudProject();
        }
    });

    // new remote project
    item = new MenuItem(menu, SWT.NONE);
    item.setText(MENU_SEL_NEW_REMOTE_PROJECT + "...");
    item.setImage(MdwPlugin.getImageDescriptor("icons/remote_project.gif").createImage());
    item.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          accessRemoteWorkflowProject();
        }
    });


    // separator
    item = new MenuItem(menu, SWT.SEPARATOR);

    // configure workspace
    item = new MenuItem(menu, SWT.NONE);
    item.setText(MENU_SEL_CONFIGURE_WORKSPACE + "...");
    item.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          configureWorkspace();
        }
    });
  }

  public void newCloudProject()
  {
    LocalCloudProjectWizard cloudProjectWizard = new LocalCloudProjectWizard();
    cloudProjectWizard.init(PlatformUI.getWorkbench(), null);
    new WizardDialog(getActiveWindow().getShell(), cloudProjectWizard).open();
  }

  public void accessRemoteWorkflowProject()
  {
    RemoteWorkflowProjectWizard remoteWorkflowProjectWizard = new RemoteWorkflowProjectWizard();
    new WizardDialog(getActiveWindow().getShell(), remoteWorkflowProjectWizard).open();
  }

  public void configureWorkspace()
  {
    Shell shell = getActiveWindow().getShell();

    WorkspaceConfig model = new WorkspaceConfig(MdwPlugin.getSettings());
    WorkspaceConfigWizard workspaceConfigWizard = new WorkspaceConfigWizard(model);
    workspaceConfigWizard.setNeedsProgressMonitor(true);
    WizardDialog dialog = new WizardDialog(shell, workspaceConfigWizard);
    dialog.create();
    dialog.open();
  }
}