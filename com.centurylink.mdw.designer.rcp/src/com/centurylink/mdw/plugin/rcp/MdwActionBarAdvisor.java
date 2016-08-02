/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.rcp;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

public class MdwActionBarAdvisor extends ActionBarAdvisor
{
  private IWorkbenchAction newAction;
  private IWorkbenchAction importAction;
  private IWorkbenchAction exportAction;
  private IWorkbenchAction closeAction;
  private IWorkbenchAction closeAllAction;
  private IWorkbenchAction saveAction;
  private IWorkbenchAction saveAllAction;
  private IWorkbenchAction exitAction;
  private IWorkbenchAction copyAction;
  private IWorkbenchAction pasteAction;
  private IWorkbenchAction deleteAction;
  private IWorkbenchAction preferencesAction;
  private IWorkbenchAction resetPerspectiveAction;
  private IWorkbenchAction helpContentsAction;
  private IWorkbenchAction aboutAction;
  private IContributionItem viewShortcutsContribItem;

  public MdwActionBarAdvisor(IActionBarConfigurer configurer)
  {
    super(configurer);
  }

  protected void makeActions(IWorkbenchWindow window)
  {
    newAction = ActionFactory.NEW.create(window);
    newAction.setText("New");
    register(newAction);

    importAction = ActionFactory.IMPORT.create(window);
    register(importAction);

    exportAction = ActionFactory.EXPORT.create(window);
    register(exportAction);

    closeAction = ActionFactory.CLOSE.create(window);
    register(closeAction);

    closeAllAction = ActionFactory.CLOSE_ALL.create(window);
    register(closeAllAction);

    saveAction = ActionFactory.SAVE.create(window);
    register(saveAction);

    saveAllAction = ActionFactory.SAVE_ALL.create(window);
    register(saveAllAction);

    exitAction = ActionFactory.QUIT.create(window);
    register(exitAction);

    copyAction = ActionFactory.COPY.create(window);
    register(copyAction);

    pasteAction = ActionFactory.PASTE.create(window);
    register(pasteAction);

    deleteAction = ActionFactory.DELETE.create(window);
    register(deleteAction);

    preferencesAction = ActionFactory.PREFERENCES.create(window);
    register(preferencesAction);

    viewShortcutsContribItem = new ShowViewMenu(window);

    resetPerspectiveAction = ActionFactory.RESET_PERSPECTIVE.create(window);
    register(resetPerspectiveAction);

    helpContentsAction = ActionFactory.HELP_CONTENTS.create(window);
    register(helpContentsAction);

    aboutAction = ActionFactory.ABOUT.create(window);
    register(aboutAction);
  }

  protected void fillMenuBar(IMenuManager menuBar)
  {
    MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
    menuBar.add(fileMenu);
    fileMenu.add(newAction);
    fileMenu.add(new Separator());
    fileMenu.add(closeAction);
    fileMenu.add(closeAllAction);
    fileMenu.add(new Separator());
    fileMenu.add(saveAction);
    fileMenu.add(saveAllAction);
    fileMenu.add(new Separator());
    fileMenu.add(importAction);
    fileMenu.add(exportAction);
    fileMenu.add(new Separator());
    fileMenu.add(exitAction);

    MenuManager editMenu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
    menuBar.add(editMenu);
    editMenu.add(copyAction);
    editMenu.add(pasteAction);
    editMenu.add(new Separator());
    editMenu.add(deleteAction);
    editMenu.add(new GroupMarker(IWorkbenchActionConstants.FIND_EXT));
    editMenu.add(new GroupMarker(ActionFactory.SELECT_ALL.getId()));

    menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
    menuBar.add(windowMenu);

    MenuManager showViewMenu = new MenuManager("Show View", "mdw.rcp.show.view");
    showViewMenu.add(viewShortcutsContribItem);
    windowMenu.add(showViewMenu);

    windowMenu.add(resetPerspectiveAction);
    windowMenu.add(new Separator());
    windowMenu.add(preferencesAction);

    MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
    menuBar.add(helpMenu);
    helpMenu.add(helpContentsAction);
    helpMenu.add(new Separator());
    helpMenu.add(aboutAction);

  }

  @Override
  protected void fillCoolBar(ICoolBarManager coolBar)
  {
    IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
    coolBar.add(new ToolBarContributionItem(toolbar, "mdwMain"));

    toolbar.add(newAction);
    toolbar.add(saveAction);
  }
}
