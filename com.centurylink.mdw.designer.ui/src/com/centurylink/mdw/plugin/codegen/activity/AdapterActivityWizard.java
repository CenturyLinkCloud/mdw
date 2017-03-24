/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.plugin.codegen.activity;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.codegen.meta.AdapterActivity;

public class AdapterActivityWizard extends ActivityWizard {
    public static final String WIZARD_ID = "mdw.codegen.adapter.activity";
    private static final String OLD_ADAPTER_BASE = "com.qwest.mdw.workflow.activity.types.AdapterActivity";

    AdapterActivityPage adapterActivityWizardPage;

    public AdapterActivityWizard() {
        setWindowTitle("New Adapter Activity");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection, new AdapterActivity());
        getActivity().setIcon("adapter.png");
        if (getActivity().getProject().checkRequiredVersion(5, 5))
            getActivity().setBaseClass(
                    com.centurylink.mdw.activity.types.AdapterActivity.class.getName());
        else
            getActivity().setBaseClass(OLD_ADAPTER_BASE);
    }

    @Override
    public void addPages() {
        super.addPages();
        adapterActivityWizardPage = (AdapterActivityPage) addPage(new AdapterActivityPage());
    }

    /**
     * Delegates the source code generation to a <code>JetAccess</code> object.
     * Then the generated Java source code file is opened in an editor.
     */
    public void generate(IProgressMonitor monitor) throws InterruptedException, CoreException {
        AdapterActivity adapterActivity = (AdapterActivity) getActivity();
        setModel(adapterActivity);

        monitor.beginTask("Creating Adapter Activity -- ", 100);

        if (getCodeGenType() != CodeGenType.registrationOnly) {
            String jetFile = "source/src/activities/adapters/";
            if (!adapterActivity.getProject().checkRequiredVersion(5, 5))
                jetFile = "source/52/src/activities/adapters/";
            if (adapterActivity.getAdapterType().equals(AdapterActivity.ADAPTER_TYPE_GENERAL))
                jetFile += "AdapterActivity.javajet";
            else if (adapterActivity.getAdapterType().equals(AdapterActivity.ADAPTER_TYPE_BUS))
                jetFile += "BusServiceAdapter.javajet";
            else if (adapterActivity.getAdapterType()
                    .equals(AdapterActivity.ADAPTER_TYPE_WEB_SERVICE))
                jetFile += "WebServiceAdapter.javajet";
            else if (adapterActivity.getAdapterType().equals(AdapterActivity.ADAPTER_TYPE_RESTFUL))
                jetFile += "RestfulServiceAdapter.javajet";
            else if (adapterActivity.getAdapterType().equals(AdapterActivity.ADAPTER_TYPE_JMS))
                jetFile += "JmsAdapter.javajet";

            if (!generateCode(jetFile, monitor))
                return;
        }

        createActivityImpl();
    }

    @Override
    public String getInfoLabelLabel() {
        return "Adapter Label";
    }

    @Override
    public WizardPage getPageAfterJavaImplCodeGenPage() {
        if (getCodeGenType() == CodeGenType.registrationOnly) {
            return null;
        }
        else {
            adapterActivityWizardPage.initializeInfo();
            return adapterActivityWizardPage;
        }
    }

}
