/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.activity;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.activity.types.EvaluatorActivity;
import com.centurylink.mdw.plugin.codegen.meta.Activity;

public class EvaluatorActivityWizard extends ActivityWizard
{
  public static final String WIZARD_ID = "mdw.codegen.evaluator.activity";
  private static final String OLD_EVAL_BASE = "com.qwest.mdw.workflow.activity.types.ControlledActivity";

  public EvaluatorActivityWizard()
  {
    setWindowTitle("New Evaluator Activity");
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    super.init(workbench, selection, new Activity());
    getActivity().setIcon("shape:decision");
    if (getActivity().getProject().checkRequiredVersion(5, 5))
      getActivity().setBaseClass(EvaluatorActivity.class.getName());
    else
      getActivity().setBaseClass(OLD_EVAL_BASE);
  }

  /**
   * Delegates the source code generation to a <code>JetAccess</code> object.
   * Then the generated Java source code file is opened in an editor.
   */
  public void generate(IProgressMonitor monitor)
    throws InterruptedException, CoreException
  {
    setModel(getActivity());

    monitor.beginTask("Creating Activity -- ", 100);

    if (getCodeGenType() != CodeGenType.registrationOnly)
    {
      // create the java code
      String jetFile = "source/src/activities/EvaluatorActivity.javajet";
      if (!getActivity().getProject().checkRequiredVersion(5, 5))
        jetFile = "source/52/src/activities/EvaluatorActivity.javajet";

      if (!generateCode(jetFile, monitor))
        return;
    }

    createActivityImpl();
  }
}
