/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class FrameworkUpdateDialog extends TrayDialog
{
  private MdwSettings settings;
  private WorkflowProject project;

  private String mdwVersion;
  public String getMdwVersion() { return mdwVersion; }

  private Combo versionCombo;

  public FrameworkUpdateDialog(Shell shell, MdwSettings settings, WorkflowProject project)
  {
    super(shell);
    this.settings = settings;
    this.project = project;
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite composite = (Composite) super.createDialogArea(parent);
    composite.getShell().setText("Update MDW Libraries");

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    composite.setLayout(layout);

    Label msgLabel = new Label(composite, SWT.NONE);
    if (project.isRemote() || project.isOsgi())
      msgLabel.setText("The project '" + project.getSourceProjectName() + "' will be updated with Maven dependencies during this required one-time step.");
    else
      msgLabel.setText("The MDW Framework Jar files will be downloaded locally\nand added to your classpath during this required one-time step.");
    msgLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

    new Label(composite, SWT.NONE).setText("MDW Version:");
    versionCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
    for (String version : settings.getMdwVersions())
      versionCombo.add(version);

    versionCombo.setText(getCompatibleVersion());

    return composite;
  }

  private String getCompatibleVersion()
  {
    List<String> versions = settings.getMdwVersions();
    String projVer = project.getMdwVersion();
    if (versions == null || versions.isEmpty() || projVer == null || projVer.indexOf('.') <= 0)
      return "";

    for (String version : versions)
    {
      if (version.equals(projVer))
        return version;
    }
    // no exact match
    int firstDot = projVer.indexOf('.');
    String projMajorVer = projVer.substring(0, firstDot);
    int secondDot = projVer.indexOf('.', projMajorVer.length()+1);
    if (secondDot == -1)
      return "";
    String projMinorVer = projVer.substring(projMajorVer.length()+1, secondDot);
    for (String version : versions)
    {
      if (version.startsWith(projMajorVer + "." + projMinorVer))
        return version;
    }
    for (String version : versions)
    {
      if (version.startsWith(projMajorVer))
        return version;
    }

    return "";
  }

  @Override
  protected void okPressed()
  {
    mdwVersion = versionCombo.getText();
    super.okPressed();
  }
}
