/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.views.properties.PropertySheet;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.codegen.activity.ActivityWizard;
import com.centurylink.mdw.plugin.codegen.activity.AdapterActivityWizard;
import com.centurylink.mdw.plugin.codegen.activity.EvaluatorActivityWizard;
import com.centurylink.mdw.plugin.codegen.activity.StartActivityWizard;
import com.centurylink.mdw.plugin.codegen.event.CamelNotifyHandlerWizard;
import com.centurylink.mdw.plugin.codegen.event.CamelProcessHandlerWizard;
import com.centurylink.mdw.plugin.codegen.event.EventHandlerWizard;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.TabbedPropertySheetPage;
import com.centurylink.mdw.plugin.designer.views.ProcessExplorerView;
import com.centurylink.mdw.plugin.designer.views.ProcessInstanceListView;
import com.centurylink.mdw.plugin.designer.views.AutomatedTestView;
import com.centurylink.mdw.plugin.designer.views.ToolboxView;
import com.centurylink.mdw.plugin.designer.wizards.NewCamelRouteWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewSpringConfigWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewJarFileWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewJavaWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewPackageWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewPageWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewProcessWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewReportWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewRuleWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewScriptWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTemplateWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTestCaseWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTextResourceWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewWebResourceWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewWordDocWizard;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.LocalCloudProjectWizard;
import com.centurylink.mdw.plugin.project.RemoteWorkflowProjectWizard;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class DesignerPerspective implements IPerspectiveFactory
{
  public void createInitialLayout(IPageLayout layout)
  {
    // view shortcuts
    layout.addShowViewShortcut(ProcessExplorerView.VIEW_ID);
    layout.addShowViewShortcut("org.eclipse.jdt.ui.PackageExplorer");
    layout.addShowViewShortcut(ToolboxView.VIEW_ID);
    layout.addShowViewShortcut("org.eclipse.ui.views.PropertySheet");
    layout.addShowViewShortcut(ProcessInstanceListView.VIEW_ID);
    layout.addShowViewShortcut(AutomatedTestView.VIEW_ID);
    layout.addShowViewShortcut("org.eclipse.gef.ui.palette_view");

    // new wizard shortcuts
    layout.addNewWizardShortcut(LocalCloudProjectWizard.WIZARD_ID);
    layout.addNewWizardShortcut(RemoteWorkflowProjectWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewProcessWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewPackageWizard.WIZARD_ID);
    layout.addNewWizardShortcut(ActivityWizard.WIZARD_ID);
    layout.addNewWizardShortcut(StartActivityWizard.WIZARD_ID);
    layout.addNewWizardShortcut(AdapterActivityWizard.WIZARD_ID);
    layout.addNewWizardShortcut(EvaluatorActivityWizard.WIZARD_ID);
    layout.addNewWizardShortcut(EventHandlerWizard.WIZARD_ID);
    layout.addNewWizardShortcut(CamelProcessHandlerWizard.WIZARD_ID);
    layout.addNewWizardShortcut(CamelNotifyHandlerWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewPageWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewReportWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewRuleWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewWordDocWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewScriptWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewJavaWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewCamelRouteWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewTemplateWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewWebResourceWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewSpringConfigWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewJarFileWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewTestCaseWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewTextResourceWizard.WIZARD_ID);

    // editor is placed by default (for process defs)
    String editorArea = layout.getEditorArea();

    // place process explorer to left of the editor area.
    IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, (float) 0.25, editorArea);
    left.addView(ProcessExplorerView.VIEW_ID);

    // place properties view below
    IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.75, editorArea);
    bottom.addView("org.eclipse.ui.views.PropertySheet");
    bottom.addView(ProcessInstanceListView.VIEW_ID);
    bottom.addView(AutomatedTestView.VIEW_ID);

    // place designer toolbox to the right
    IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, (float) 0.75, editorArea);
    right.addView(ToolboxView.VIEW_ID);
    right.addPlaceholder("org.eclipse.gef.ui.palette_view");

  }

  public static void openPerspective(IWorkbenchWindow activeWindow)
  {
    try
    {
      showPerspective(activeWindow);
    }
    catch (WorkbenchException ex)
    {
      PluginMessages.uiError(activeWindow.getShell(), ex, "Open Perspective");
    }
  }

  public static IWorkbenchPage showPerspective(IWorkbenchWindow activeWindow) throws WorkbenchException
  {
    if (MdwPlugin.isRcp())
      return PlatformUI.getWorkbench().showPerspective("MDWDesignerRCP.perspective", activeWindow);
    else
      return PlatformUI.getWorkbench().showPerspective("mdw.perspectives.designer", activeWindow);
  }

  public static void showPerspectiveAndSelectProjectPreferences(IWorkbenchWindow activeWindow, WorkflowProject project)
  {
    try
    {
      IWorkbenchPage page = showPerspective(activeWindow);
      ProcessExplorerView processExplorer = (ProcessExplorerView) page.findView(ProcessExplorerView.VIEW_ID);
      if (processExplorer != null)
      {
        processExplorer.setFocus();
        processExplorer.select(project);
        PropertySheet propSheet = (PropertySheet) page.showView("org.eclipse.ui.views.PropertySheet");
        if (propSheet != null)
        {
          TabbedPropertySheetPage tabbedPage = (TabbedPropertySheetPage) propSheet.getCurrentPage();
          if (tabbedPage != null)
            tabbedPage.setSelectedTab("mdw.properties.tabs.preferences");
        }
      }
    }
    catch (WorkbenchException ex)
    {
      PluginMessages.uiError(activeWindow.getShell(), ex, "Project Preferences", project);
    }
  }

  public static void promptForShowPerspective(IWorkbenchWindow activeWindow, WorkflowElement workflowElement)
  {
    if (MdwPlugin.isRcp())
      return;

    IPerspectiveDescriptor pd = activeWindow.getActivePage().getPerspective();
    if (!"mdw.perspectives.designer".equals(pd.getId()))
    {
      boolean switchPerspective = false;
      String switchPref = MdwPlugin.getStringPref(PreferenceConstants.PREFS_SWITCH_TO_DESIGNER_PERSPECTIVE);
      if (switchPref == null || switchPref.length() == 0)
      {
        String message = "Resources of type '" + workflowElement.getTitle() + "' are associated with MDW Designer Perspective.  Switch to this perspective now?";
        String toggleMessage = "Remember my answer and don't ask me again";
        MessageDialogWithToggle mdwt = MessageDialogWithToggle.openYesNoQuestion(activeWindow.getShell(), "Switch Perspective", message , toggleMessage, false, MdwPlugin.getDefault().getPreferenceStore(), PreferenceConstants.PREFS_SWITCH_TO_DESIGNER_PERSPECTIVE);
        switchPerspective = mdwt.getReturnCode() == IDialogConstants.YES_ID || mdwt.getReturnCode() == IDialogConstants.OK_ID;
      }
      else
      {
        switchPerspective = switchPref.equalsIgnoreCase("always");
      }

      if (switchPerspective)
      {
        try
        {
          showPerspective(activeWindow);
        }
        catch (Exception ex)
        {
          PluginMessages.uiError(activeWindow.getShell(), ex, "Show Perspective", workflowElement.getProject());
        }
      }
    }
  }
}
