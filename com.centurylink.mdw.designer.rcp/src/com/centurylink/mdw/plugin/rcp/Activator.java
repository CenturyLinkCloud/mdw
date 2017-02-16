/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.rcp;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "com.centurylink.mdw.plugin.rcp";

    private static Activator plugin;

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * Convenience method for determining the active page.
     *
     * @return the active page
     */
    public static IWorkbenchPage getActivePage() {
        IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return null;
        }
        return activeWorkbenchWindow.getActivePage();
    }

    /**
     * Convenience method for determining the active workbench window
     *
     * @return the active window
     */
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        if (plugin == null) {
            return null;
        }
        IWorkbench workBench = plugin.getWorkbench();
        if (workBench == null) {
            return null;
        }
        return workBench.getActiveWorkbenchWindow();
    }
}
