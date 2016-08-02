/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

public class ActivityLaunchMainTab extends ProcessLaunchMainTab
{
  private ActivityVO activityVO;

  private Combo activityNameCombo;

  public void createControl(Composite parent)
  {
    Composite composite = new Composite(parent, SWT.NONE);
    setControl(composite);

    GridLayout topLayout = new GridLayout();
    topLayout.numColumns = 2;
    composite.setLayout(topLayout);

    createWorkflowProjectSection(composite);
    createProcessSection(composite);
    processVersionCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        if (getProcess() != null)
          refreshActivities();
      }
    });

    createInstanceInfoSection(composite);
    createActivitySection(composite);
    createServerStatusSection(composite);
    createLiveViewSection(composite);
  }

  public String getName()
  {
    return "Activity";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration launchConfig)
  {
    super.initializeFrom(launchConfig);
    try
    {
      refreshActivities();
      String activityIdStr =  launchConfig.getAttribute(ActivityLaunchConfiguration.ACTIVITY_ID, "");
      Long activityId = new Long(activityIdStr);
      ProcessVO processVO = getProcess().getProcessVO();
      activityVO = processVO.getActivityVO(activityId);
      int selIdx = 0;
      for (int i = 0; i < processVO.getActivities().size(); i++)
      {
        if (processVO.getActivities().get(i).getActivityId().equals(activityVO.getActivityId()))
        {
          selIdx = i;
          break;
        }
      }
      activityNameCombo.select(selIdx);
    }
    catch (CoreException ex)
    {
      PluginMessages.uiError(ex, "Launch Init", getProject());
    }
    validatePage();
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy launchConfig)
  {
    super.performApply(launchConfig);
    if (activityNameCombo.getSelectionIndex() != -1)
    {
      activityVO = getProcess().getProcessVO().getActivities().get(activityNameCombo.getSelectionIndex());
      launchConfig.setAttribute(ActivityLaunchConfiguration.ACTIVITY_ID, activityVO.getActivityId().toString());
    }
  }

  protected void createActivitySection(Composite parent)
  {
    Group activityGroup = new Group(parent, SWT.NONE);
    activityGroup.setText("Start from Activity");
    GridLayout gl = new GridLayout();
    gl.numColumns = 4;
    activityGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    activityGroup.setLayoutData(gd);

    new Label(activityGroup, SWT.NONE).setText("Activity: ");

    activityNameCombo = new Combo(activityGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 250;
    activityNameCombo.setLayoutData(gd);
    refreshActivities();
    activityNameCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });
  }

  private void refreshActivities()
  {
    BusyIndicator.showWhile(getControl().getShell().getDisplay(), new Runnable()
    {
      public void run()
      {
        activityNameCombo.removeAll();
        if (getProcess() != null && activityNameCombo != null)
        {
          for (ActivityVO activityVO : getProcess().getProcessVO().getActivities())
            activityNameCombo.add(activityVO.getActivityName());
        }
      }
    });
  }

  @Override
  protected void validatePage()
  {
    super.validatePage();
    if (getErrorMessage() == null && activityVO == null)
    {
      setErrorMessage("Please select an Activity to launch from");
      updateLaunchConfigurationDialog();
    }
  }
}
