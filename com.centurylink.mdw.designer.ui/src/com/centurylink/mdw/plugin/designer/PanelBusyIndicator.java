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
package com.centurylink.mdw.plugin.designer;

import java.awt.Component;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import com.centurylink.mdw.plugin.PluginUtil;

public class PanelBusyIndicator {
    Display display;
    Component awtComponent;

    public PanelBusyIndicator(Display display, Component awtComponent) {
        this.display = display;
        this.awtComponent = awtComponent;
        if (!checkComponent())
            throw new NullPointerException("Component ancestor cannot be null.");
    }

    /**
     * Must be called from the SWT event thread.
     */
    public void busyWhile(Runnable runnable) throws InvocationTargetException {
        if (PluginUtil.isMac()) {
            // https://bugs.openjdk.java.net/browse/JDK-8087465
            runnable.run();
            return;
        }

        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    awtComponent.setEnabled(false);
                }
            });

            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    awtComponent.getParent().getParent().setCursor(
                            java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                }
            });

            BusyIndicator.showWhile(display, runnable);
        }
        catch (InterruptedException ex) {
            throw new InvocationTargetException(ex);
        }
        finally {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    awtComponent.getParent().getParent().setCursor(
                            java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
                    awtComponent.setEnabled(true);
                }
            });
        }
    }

    private boolean checkComponent() {
        if (awtComponent == null || awtComponent.getParent() == null
                || awtComponent.getParent().getParent() == null)
            return false;

        return true;
    }

}
