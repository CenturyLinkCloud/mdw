/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.activity;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.codegen.CodeGenWizard;
import com.centurylink.mdw.plugin.codegen.meta.Activity;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;

public class ActivityWizard extends CodeGenWizard
{
  public static final String WIZARD_ID = "mdw.codegen.general.activity";

  public Activity getActivity()
  {
    return (Activity)getCodeElement();
  }
  public void setActivity(Activity activity)
  {
    setCodeElement(activity);
  }

  // wizard pages
  ActivityPage activityPage;

  public ActivityWizard()
  {
    setWindowTitle("New MDW Activity");
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    super.init(workbench, selection, new Activity());
    getActivity().setIcon("shape:activity");
  }

  @Override
  public void addPages()
  {
    activityPage = new ActivityPage();
    addPage(activityPage);
    addJavaImplCodeGenPages();
  }

  /**
   * Delegates the source code generation to a <code>JetAccess</code> object.
   * Then the generated Java source code file is opened in an editor.
   */
  public void generate(IProgressMonitor monitor) throws InterruptedException, CoreException
  {
    setModel(getActivity());

    monitor.beginTask("Creating Activity -- ", 100);

    if (getCodeGenType() != CodeGenType.registrationOnly)
    {
      // create the java code
      String jetFile = "source/src/activities/GeneralActivity.javajet";
      if (!getActivity().getProject().checkRequiredVersion(5, 5))
        jetFile = "source/52/src/activities/GeneralActivity.javajet";
      if (!generateCode(jetFile, monitor))
        return;
    }

    createActivityImpl();
  }

  protected void createActivityImpl()
  {
    ActivityImpl activityImpl = getActivity().createActivityImpl();
    activityImpl.setDynamicJava(getCodeGenType() == CodeGenType.dynamicJavaCode);
    getActivity().getProject().getDesignerProxy().createActivityImpl(activityImpl);
    getActivity().getProject().addActivityImpl(activityImpl);
    activityImpl.addElementChangeListener(activityImpl.getProject());
    activityImpl.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, activityImpl);
  }

  protected String validate()
  {
    if (getCodeElement().getProject().activityImplClassExists(getActivity().getJavaPackage() + "." + getActivity().getClassName()))
      return "Activity implementor class already exists for project " + getCodeElement().getProject().getLabel() + ": '" + getActivity().getJavaPackage() + "." + getActivity().getClassName() + "'.";

    return null;
  }

  @Override
  public String getInfoLabelLabel()
  {
    return "Activity Label";
  }

  @Override
  public String getInfoLabelValue()
  {
    return getActivity().getLabel();
  }
}
