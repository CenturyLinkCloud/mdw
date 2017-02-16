/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
