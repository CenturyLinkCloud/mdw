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
package com.centurylink.mdw.plugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

public class MessageConsolePageParticipant implements IConsolePageParticipant {
    public static final String PREFS_KEY = "MdwShowWhenContentChanges";

    private ShowWhenContentChangesAction showWhenContentChangesAction;
    private TerminateAction terminateAction;

    private IConsole console;

    public void init(IPageBookViewPage page, IConsole console) {
        this.console = console;

        // contribute to toolbar
        IActionBars actionBars = page.getSite().getActionBars();
        showWhenContentChangesAction = new ShowWhenContentChangesAction();
        actionBars.getToolBarManager().appendToGroup(IConsoleConstants.OUTPUT_GROUP,
                showWhenContentChangesAction);

        if (((MessageConsole) console).isShowTerminate()) {
            terminateAction = new TerminateAction();
            actionBars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP,
                    terminateAction);
        }
    }

    public void activated() {
        if (terminateAction != null)
            terminateAction.update();
    }

    public void deactivated() {
        if (terminateAction != null)
            terminateAction.update();
    }

    public void dispose() {
        if (showWhenContentChangesAction != null)
            showWhenContentChangesAction.dispose();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object getAdapter(Class adapter) {
        return null;
    }

    private class ShowWhenContentChangesAction extends Action implements IPropertyChangeListener {
        public ShowWhenContentChangesAction() {
            super("Show Console When Output Changes", IAction.AS_CHECK_BOX);
            setId("com.centurylink.mdw.ShowWhenContentChangesAction");
            setToolTipText("Show Console When Output Changes");
            getPreferenceStore().addPropertyChangeListener(this);
            setImageDescriptor(MdwPlugin.getImageDescriptor("icons/write_out.gif"));
            update();
        }

        public void run() {
            IPreferenceStore store = getPreferenceStore();
            store.removePropertyChangeListener(this);
            store.setValue(getPrefsKey(), isChecked());
            store.addPropertyChangeListener(this);
        }

        public void propertyChange(PropertyChangeEvent event) {
            String property = event.getProperty();
            if (property.equals(getPrefsKey()))
                update();
        }

        private void update() {
            IPreferenceStore store = getPreferenceStore();
            setChecked(store.getBoolean(getPrefsKey()));
        }

        private IPreferenceStore getPreferenceStore() {
            return MdwPlugin.getDefault().getPreferenceStore();
        }

        public void dispose() {
            getPreferenceStore().removePropertyChangeListener(this);
        }

        private String getPrefsKey() {
            return PREFS_KEY + "_" + ((MessageConsole) console).getCoreName().replaceAll(" ", "");
        }
    }

    private class TerminateAction extends Action implements IPropertyChangeListener {
        public TerminateAction() {
            super("Stop Watching", MdwPlugin.getImageDescriptor("icons/stop.gif"));
            setId("com.centurylink.mdw.TerminateAction");
            setToolTipText("Stop Watching");
            update();
        }

        public void run() {
            ((MessageConsole) console).terminate();
            setEnabled(false);
        }

        private void update() {
            setEnabled(((MessageConsole) console).isRunning());
        }

        public void propertyChange(PropertyChangeEvent event) {
            update();
        }
    }
}