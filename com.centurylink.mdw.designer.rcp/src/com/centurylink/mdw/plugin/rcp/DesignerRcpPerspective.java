/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.rcp;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.centurylink.mdw.plugin.codegen.event.CamelProcessHandlerWizard;
import com.centurylink.mdw.plugin.codegen.event.EventHandlerWizard;
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
import com.centurylink.mdw.plugin.designer.wizards.NewXmlDocWizard;
import com.centurylink.mdw.plugin.project.RemoteWorkflowProjectWizard;

public class DesignerRcpPerspective implements IPerspectiveFactory
{
  public void createInitialLayout(IPageLayout layout)
  {
    layout.setFixed(true);  // to avoid non-functional perspective shortcuts in toolbar

    // view shortcuts
    layout.addShowViewShortcut(ProcessExplorerView.VIEW_ID);
    layout.addShowViewShortcut(ToolboxView.VIEW_ID);
    layout.addShowViewShortcut("org.eclipse.ui.views.PropertySheet");
    layout.addShowViewShortcut(ProcessInstanceListView.VIEW_ID);
    layout.addShowViewShortcut(AutomatedTestView.VIEW_ID);
    layout.addShowViewShortcut("org.eclipse.birt.report.designer.ui.views.data.DataView");
    layout.addShowViewShortcut("org.eclipse.birt.report.designer.ui.attributes.AttributeView");
    layout.addShowViewShortcut("org.eclipse.gef.ui.palette_view");

    // new wizard shortcuts
    layout.addNewWizardShortcut(RemoteWorkflowProjectWizard.WIZARD_ID + ".rcp");
    layout.addNewWizardShortcut(NewProcessWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewPackageWizard.WIZARD_ID);
    layout.addNewWizardShortcut(EventHandlerWizard.WIZARD_ID);
    layout.addNewWizardShortcut(CamelProcessHandlerWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewPageWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewReportWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewRuleWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewScriptWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewJavaWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewCamelRouteWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewTemplateWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewWebResourceWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewXmlDocWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewSpringConfigWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewJarFileWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewTestCaseWizard.WIZARD_ID);
    layout.addNewWizardShortcut(NewTextResourceWizard.WIZARD_ID);

    // editor is placed by default (for process defs)
    String editorArea = layout.getEditorArea();

    // place process explorer to left of the editor area.
    IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, (float) 0.25, editorArea);
    left.addView(ProcessExplorerView.VIEW_ID);
    left.addPlaceholder("org.eclipse.birt.report.designer.ui.views.data.DataView");

    // place properties view below
    IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.75, editorArea);
    bottom.addView("org.eclipse.ui.views.PropertySheet");
    bottom.addView(ProcessInstanceListView.VIEW_ID);
    bottom.addView(AutomatedTestView.VIEW_ID);
    bottom.addPlaceholder("org.eclipse.birt.report.designer.ui.attributes.AttributeView");

    // place designer toolbox to the right
    IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, (float) 0.75, editorArea);
    right.addView(ToolboxView.VIEW_ID);
    right.addPlaceholder("org.eclipse.gef.ui.palette_view");
  }
}
