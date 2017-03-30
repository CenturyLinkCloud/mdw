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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.codegen.CodeGenWizard;
import com.centurylink.mdw.plugin.codegen.meta.EventHandler;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;

public class EventHandlerWizard extends CodeGenWizard {
    public static final String WIZARD_ID = "mdw.codegen.event.handler";

    public enum HandlerAction {
        launchProcess, notifyProcess
    }

    private HandlerAction handlerAction = HandlerAction.launchProcess;

    public HandlerAction getHandlerAction() {
        return this.handlerAction;
    }

    public void setHandlerAction(HandlerAction handlerAction) {
        this.handlerAction = handlerAction;
    }

    public EventHandler getEventHandler() {
        return (EventHandler) getCodeElement();
    }

    public void setEventHandler(EventHandler eventHandler) {
        setCodeElement(eventHandler);
    }

    // wizard pages
    EventHandlerPage eventHandlerPage;

    public EventHandlerPage getEventHandlerPage() {
        return eventHandlerPage;
    }

    CustomEventHandlerPage customEventHandlerPage;

    public CustomEventHandlerPage getCustomEventHandlerPage() {
        return customEventHandlerPage;
    }

    public EventHandlerWizard() {
        setWindowTitle("New MDW External Event Handler");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection, new EventHandler());
    }

    @Override
    public void addPages() {
        eventHandlerPage = (EventHandlerPage) addPage(new EventHandlerPage());
        addJavaImplCodeGenPages();
        customEventHandlerPage = (CustomEventHandlerPage) addPage(new CustomEventHandlerPage());
    }

    protected void createExternalEvent() {
        String handler = null;
        if (getEventHandler().isCustom()) {
            String javaPackage = getEventHandler().getJavaPackage();
            String packagePrefix = javaPackage == null || javaPackage.length() == 0 ? ""
                    : javaPackage + ".";
            handler = packagePrefix + getEventHandler().getClassName();
        }
        else {
            if (getHandlerAction() == HandlerAction.launchProcess)
                handler = "START_PROCESS?ProcessName=" + getEventHandler().getProcess();
            else if (getHandlerAction() == HandlerAction.notifyProcess)
                handler = "NOTIFY_PROCESS?EventName=" + getEventHandler().getEvent();
        }
        ExternalEvent externalEvent = getEventHandler().createExternalEvent(handler);
        externalEvent.setDynamicJava(getCodeGenType() == CodeGenType.dynamicJavaCode);
        getEventHandler().getProject().getDesignerProxy()
                .registerExternalEventHandler(externalEvent);
        externalEvent.addElementChangeListener(externalEvent.getProject());
        externalEvent.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, externalEvent);
        if (externalEvent.getProject().checkRequiredVersion(6, 0)) {
            IFile file = externalEvent.getProject().getAssetFolder()
                    .getFolder(externalEvent.getPackage().getName().replace('.', '/'))
                    .getFile(externalEvent.getName()+".evth");
            IWorkbenchPage activePage = MdwPlugin.getActivePage();
            try {
                externalEvent.getProject().getAssetFolder()
                .getFolder(externalEvent.getPackage().getName().replace('.', '/'))
                .refreshLocal(IResource.DEPTH_ONE, null);
                IDE.openEditor(activePage, file, true);
            }
            catch (PartInitException ex) {
                PluginMessages.uiError(ex, "Open File", externalEvent.getProject());
            }
            catch (CoreException ex) {
                PluginMessages.uiError(ex, "Open File", externalEvent.getProject());
            }
        }
    }

    /**
     * Delegates the source code generation to a <code>JetAccess</code> object.
     * Then the generated Java source code file is opened in an editor.
     */
    public void generate(IProgressMonitor monitor) throws InterruptedException, CoreException {
        setModel(getEventHandler());

        monitor.beginTask("Creating EventHandler -- ", 100);

        if (getEventHandler().isCustom() && getCodeGenType() != CodeGenType.registrationOnly) {
            // create the java code
            String jetFile = "source/src/eventHandlers/ExternalEventHandler.javajet";
            if (!getEventHandler().getProject().checkRequiredVersion(5, 5))
                jetFile = "source/52/src/eventHandlers/ExternalEventHandler.javajet";

            if (!generateCode(jetFile, monitor))
                return;
        }

        createExternalEvent();
    }

    @Override
    public boolean canFinish() {
        if (getEventHandler().isCustom())
            return super.canFinish();
        else
            return eventHandlerPage.isPageComplete();
    }

    protected String validate() {
        // TODO
        return null;
    }

    @Override
    public String getInfoLabelLabel() {
        return "Message Pattern";
    }

    @Override
    public String getInfoLabelValue() {
        return getEventHandler().getMessagePattern();
    }

    @Override
    public WizardPage getPageAfterJavaImplCodeGenPage() {
        if (!getEventHandler().isCustom() || getCodeGenType() == CodeGenType.registrationOnly) {
            return null;
        }
        else {
            customEventHandlerPage.initializeInfo();
            return customEventHandlerPage;
        }
    }

}
