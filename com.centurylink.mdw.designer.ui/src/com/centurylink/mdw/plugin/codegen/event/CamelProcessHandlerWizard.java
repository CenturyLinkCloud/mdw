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
package com.centurylink.mdw.plugin.codegen.event;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.WizardPage;

public class CamelProcessHandlerWizard extends EventHandlerWizard {
    public static final String WIZARD_ID = "mdw.codegen.camel.process.handler";

    // wizard pages
    CamelHandlerPage camelHandlerPage;

    public CamelHandlerPage getCamelHandlerPage() {
        return camelHandlerPage;
    }

    public CamelProcessHandlerWizard() {
        setWindowTitle("Camel Process Launch Handler");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        getEventHandler().setCustom(true);
    }

    @Override
    public void addPages() {
        camelHandlerPage = (CamelHandlerPage) addPage(new CamelHandlerPage());
        super.addJavaImplCodeGenPages();
    }

    @Override
    public void generate(IProgressMonitor monitor) throws InterruptedException, CoreException {
        setModel(getEventHandler());

        monitor.beginTask("Creating Camel Event Notify Handler -- ", 100);

        // create the java code
        String template = "CamelProcessLaunchHandler.javajet";
        String jetFile = "source/src/eventHandlers/" + template;
        if (!getEventHandler().getProject().checkRequiredVersion(5, 5))
            jetFile = "source/52/src/eventHandlers/" + template;

        generateCode(jetFile, monitor);
    }

    @Override
    public WizardPage getPageAfterJavaImplCodeGenPage() {
        return null;
    }
}
