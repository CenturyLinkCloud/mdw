/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.actions.WebLaunchActions;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProjectPermissionsSection extends PropertySection implements IFilter
{
  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }

  private PropertyEditor userEditor;
  private PropertyEditor editProcessesEditor;
  private PropertyEditor runProcessesEditor;
  private PropertyEditor adminPermissionsEditor;
  private PropertyEditor sysAdminPermissionsEditor;
  private PropertyEditor taskManagerLinkEditor;
  private PropertyEditor reloadButtonEditor;

  public void setSelection(WorkflowElement selection)
  {
    project = (WorkflowProject) selection;

    userEditor.setElement(project);
    userEditor.setValue(project.getUser().getUsername());

    editProcessesEditor.setElement(project);
    editProcessesEditor.setValue(project.isUserAuthorizedInAnyGroup(UserRoleVO.PROCESS_DESIGN));

    runProcessesEditor.setElement(project);
    runProcessesEditor.setValue(project.isUserAuthorizedInAnyGroup(UserRoleVO.PROCESS_EXECUTION));

    adminPermissionsEditor.setElement(project);
    adminPermissionsEditor.setValue(project.isUserAuthorizedInAnyGroup(UserRoleVO.USER_ADMIN));

    // TODO task execution

    sysAdminPermissionsEditor.setElement(project);
    sysAdminPermissionsEditor.setValue(project.isUserAuthorizedForSystemAdmin());

    taskManagerLinkEditor.setElement(project);

    reloadButtonEditor.setElement(project);
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    project = (WorkflowProject) selection;

    // user text field
    userEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
    userEditor.setLabel("Current User");
    userEditor.setWidth(150);
    userEditor.setReadOnly(true);
    userEditor.render(composite);

    // edit processes checkbox
    editProcessesEditor = new PropertyEditor(project, PropertyEditor.TYPE_CHECKBOX);
    editProcessesEditor.setLabel("Design/Edit Processes");
    editProcessesEditor.setReadOnly(true);
    editProcessesEditor.render(composite);

    // run processes checkbox
    runProcessesEditor = new PropertyEditor(project, PropertyEditor.TYPE_CHECKBOX);
    runProcessesEditor.setLabel("Execute Processes");
    runProcessesEditor.setReadOnly(true);
    runProcessesEditor.render(composite);

    // admin checkbox
    adminPermissionsEditor = new PropertyEditor(project, PropertyEditor.TYPE_CHECKBOX);
    adminPermissionsEditor.setLabel("Administrator Privileges");
    adminPermissionsEditor.setReadOnly(true);
    adminPermissionsEditor.render(composite);

    // sys admin checkbox
    sysAdminPermissionsEditor = new PropertyEditor(project, PropertyEditor.TYPE_CHECKBOX);
    sysAdminPermissionsEditor.setLabel("System Admin Privileges");
    sysAdminPermissionsEditor.setReadOnly(true);
    sysAdminPermissionsEditor.render(composite);

    // task manager link
    taskManagerLinkEditor = new PropertyEditor(project, PropertyEditor.TYPE_LINK);
    taskManagerLinkEditor.setLabel("Launch Task Manager to Change Role Assignments");
    taskManagerLinkEditor.setIndent(-100);
    taskManagerLinkEditor.setHeight(20);
    taskManagerLinkEditor.setWidth(500);
    taskManagerLinkEditor.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          launchTaskManager();
        }
      });
    taskManagerLinkEditor.render(composite);

    // reload button
    reloadButtonEditor = new PropertyEditor(project, PropertyEditor.TYPE_BUTTON);
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

  private void launchTaskManager()
  {
    WebLaunchActions.getLaunchAction(project, WebApp.TaskManager).launch(project);
  }

  private void reloadPermissions()
  {
    project.getDesignerProxy().getPluginDataAccess().reloadUserPermissions();
    setSelection(project);
  }

  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof WorkflowProject))
      return false;

    if (!((WorkflowProject)toTest).isInitialized())
      return false;  // wait until project has been initialized

    return !((WorkflowProject)toTest).getProject().checkRequiredVersion(5, 5, 8);
  }
}
