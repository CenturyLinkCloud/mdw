/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.actions.WebLaunchActions;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class PackagePermissionsSection extends PropertySection implements IFilter
{
  private WorkflowPackage workflowPackage;
  public WorkflowPackage getPackage() { return workflowPackage; }

  private PropertyEditor userEditor;
  private PropertyEditor editProcessesEditor;
  private PropertyEditor runProcessesEditor;
  private PropertyEditor adminPermissionsEditor;
  private PropertyEditor sysAdminPermissionsEditor;
  private PropertyEditor mdwHubLinkEditor;
  private PropertyEditor reloadButtonEditor;

  public void setSelection(WorkflowElement selection)
  {
    workflowPackage = (WorkflowPackage) selection;

    userEditor.setElement(workflowPackage);
    userEditor.setValue(workflowPackage.getProject().getUser().getUsername());

    editProcessesEditor.setElement(workflowPackage);
    editProcessesEditor.setValue(workflowPackage.isUserAuthorized(UserRoleVO.ASSET_DESIGN));

    runProcessesEditor.setElement(workflowPackage);
    runProcessesEditor.setValue(workflowPackage.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION));

    adminPermissionsEditor.setElement(workflowPackage);
    adminPermissionsEditor.setValue(workflowPackage.isUserAuthorized(UserRoleVO.USER_ADMIN));

    sysAdminPermissionsEditor.setElement(workflowPackage);
    sysAdminPermissionsEditor.setValue(workflowPackage.getProject().isUserAuthorizedForSystemAdmin());

    mdwHubLinkEditor.setElement(workflowPackage);

    reloadButtonEditor.setElement(workflowPackage);
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    workflowPackage = (WorkflowPackage) selection;

    // user text field
    userEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_TEXT);
    userEditor.setLabel("Current User");
    userEditor.setWidth(150);
    userEditor.setReadOnly(true);
    userEditor.render(composite);

    // edit processes checkbox
    editProcessesEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_CHECKBOX);
    editProcessesEditor.setLabel("Create/Edit Assets");
    editProcessesEditor.setReadOnly(true);
    editProcessesEditor.render(composite);

    // run processes checkbox
    runProcessesEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_CHECKBOX);
    runProcessesEditor.setLabel("Execute Processes");
    runProcessesEditor.setReadOnly(true);
    runProcessesEditor.render(composite);

    // admin checkbox
    adminPermissionsEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_CHECKBOX);
    adminPermissionsEditor.setLabel("Administrator Privileges");
    adminPermissionsEditor.setReadOnly(true);
    adminPermissionsEditor.render(composite);

    // sys admin checkbox
    sysAdminPermissionsEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_CHECKBOX);
    sysAdminPermissionsEditor.setLabel("System Admin Privileges");
    sysAdminPermissionsEditor.setReadOnly(true);
    sysAdminPermissionsEditor.render(composite);

    // MDW Hub link
    mdwHubLinkEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_LINK);
    mdwHubLinkEditor.setLabel("Launch MDW Hub to Change Role Assignments");
    mdwHubLinkEditor.setIndent(-100);
    mdwHubLinkEditor.setWidth(500);
    mdwHubLinkEditor.setHeight(20);
    mdwHubLinkEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        launchMdwHub();
      }
    });
    mdwHubLinkEditor.render(composite);

    // reload button
    reloadButtonEditor = new PropertyEditor(workflowPackage, PropertyEditor.TYPE_BUTTON);
    reloadButtonEditor.setLabel("Reload");
    reloadButtonEditor.setIndent(-100);
    reloadButtonEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        reloadPermissions();
      }
    });
    reloadButtonEditor.render(composite);
  }

  private void launchMdwHub()
  {
    WebLaunchActions.getLaunchAction(workflowPackage.getProject(), WebApp.MdwHub).launch(workflowPackage.getProject());
  }

  private void reloadPermissions()
  {
    try
    {
      workflowPackage.getDesignerDataModel().reloadPriviledges(workflowPackage.getProject().getDesignerDataAccess(),
          workflowPackage.getProject().getUser().getUsername());
      setSelection(workflowPackage);
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Reload Permissions", workflowPackage.getProject());
    }
  }

  @Override
  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof WorkflowPackage))
      return false;

    return ((WorkflowPackage) toTest).getProject().checkRequiredVersion(5, 5, 8);
  }

}
