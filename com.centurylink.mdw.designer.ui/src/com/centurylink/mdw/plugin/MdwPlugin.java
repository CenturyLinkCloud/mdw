/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin;

import java.io.File;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.centurylink.mdw.plugin.launch.LogWatcher;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * The activator class controls the plug-in life cycle
 */
public class MdwPlugin extends AbstractUIPlugin
{
  public static final String PLUGIN_ID = "com.centurylink.mdw.designer.ui";

  // the shared instance
  private static MdwPlugin plugin;

  /**
   * The constructor
   */
  public MdwPlugin()
  {
  }

  /**
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception
  {
    super.start(context);
    plugin = this;
    // log the Designer version
    System.out.println("Starting " + getBundle().getSymbolicName() + " v" + getBundle().getVersion());
    PlatformUI.getWorkbench().addWindowListener(new IWindowListener()
    {
      public void windowOpened(IWorkbenchWindow window)
      {
      }
      public void windowClosed(IWorkbenchWindow window)
      {
        String userHome = System.getProperty("user.home");
        if (userHome != null)
        {
          File oomphP2CacheDir = new File(userHome + "/.eclipse/org.eclipse.oomph.p2/cache");
          if (oomphP2CacheDir.isDirectory())
          {
            try
            {
              System.err.println("Deleting: " + oomphP2CacheDir + " (https://bugs.eclipse.org/bugs/show_bug.cgi?id=477246)");
              PluginUtil.deleteDirectory(oomphP2CacheDir);
            }
            catch (Throwable th)
            {
              th.printStackTrace();
            }
          }
        }
      }
      public void windowActivated(IWorkbenchWindow window)
      {
      }
      public void windowDeactivated(IWorkbenchWindow window)
      {
      }
    });
  }

  /**
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception
  {
    try
    {
      for (WorkflowProject project : WorkflowProjectManager.getInstance().getWorkflowProjects())
        project.clear();
      LogWatcher logWatcher = LogWatcher.instance;
      if (logWatcher != null && logWatcher.isRunning())
        logWatcher.shutdown();
      plugin = null;
    }
    finally
    {
      super.stop(context);
    }
  }

  /**
   * Returns the shared instance
   *
   * @return the shared instance
   */
  public static MdwPlugin getDefault()
  {
    return plugin;
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in relative path
   *
   * @param path the path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(String path)
  {
    return imageDescriptorFromPlugin(getPluginId(), path);
  }

  public static String getPluginId()
  {
    if (getDefault() == null)
      return null;
    return getDefault().getBundle().getSymbolicName();
  }

  private static MdwSettings settings;
  public static void flushSettings() { settings = null; }
  public static MdwSettings getSettings()
  {
    if (settings == null)
      settings = new MdwSettings();
    return settings;
  }

  /**
   * Convenience method for determining the active page.
   *
   * @return the active page
   */
  public static IWorkbenchPage getActivePage()
  {
    IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
    if (activeWorkbenchWindow == null)
    {
      return null;
    }
    return activeWorkbenchWindow.getActivePage();
  }

  public static IWorkbench getPluginWorkbench()
  {
    return plugin.getWorkbench();
  }

  public static IWorkbenchWindow getActiveWorkbenchWindow()
  {
    if (plugin == null)
    {
      return null;
    }
    IWorkbench workBench = getPluginWorkbench();
    if (workBench == null)
    {
      return null;
    }
    return workBench.getActiveWorkbenchWindow();
  }

  public static IPath getPluginStateLocation()
  {
    return plugin.getStateLocation();
  }

  public static String getVersionString()
  {
    return Platform.getBundle("com.centurylink.mdw.designer.ui").getHeaders().get("Bundle-Version");
  }

  public static IPath getMetaDataLoc(String path) throws CoreException
  {
    return plugin.getStateLocation().append("/" + path);
  }

  public static IWorkspaceRoot getWorkspaceRoot()
  {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  public static File getWorkspaceDirectory()
  {
    return MdwPlugin.getWorkspaceRoot().getLocation().toFile();
  }

  public static Display getDisplay()
  {
    Display display = Display.getCurrent();
    if (display == null)
    {
      display = Display.getDefault();
    }
    return display;
  }

  public static boolean isUiThread()
  {
    Display display = getDisplay();
    return display != null && (display.getThread() == Thread.currentThread());
  }

  public static Shell getShell()
  {
    Display display = getDisplay();
    if (display == null)
      return null;
    else
      return display.getActiveShell();
  }

  public static boolean workspaceHasGroovySupport()
  {
    try
    {
      Class.forName("org.codehaus.groovy.eclipse.core.model.GroovyRuntime");
      return true;
    }
    catch (ClassNotFoundException ex)
    {
      return false;
    }
  }

  public static boolean workspaceHasMavenSupport()
  {
    try
    {
      Class.forName("org.eclipse.m2e.core.project.MavenProjectInfo");
      return true;
    }
    catch (ClassNotFoundException ex)
    {
      return false;
    }
  }

  public static boolean workspaceHasGradleSupport()
  {
    try
    {
      Class.forName("org.springsource.ide.eclipse.gradle.core.GradleProject");
      return true;
    }
    catch (ClassNotFoundException ex)
    {
      return false;
    }
  }

  public static boolean workspaceHasBirtSupport()
  {
    try
    {
      Class.forName("org.eclipse.birt.report.data.oda.jdbc.JDBCDriverManager");
      return true;
    }
    catch (ClassNotFoundException ex)
    {
      return false;
    }
  }

  private static Boolean rcp;
  public static boolean isRcp()
  {
    if (rcp == null) {
      rcp = Platform.getProduct() != null
          && (Platform.getProduct().getId().equals("MDWDesignerRCP.product")
        || Platform.getProduct().getId().equals("com.centurylink.mdw.plugin.rcp.product"));
    }
    return rcp;
  }

  public static String getStringPref(String name)
  {
    IPreferenceStore prefsStore = getDefault().getPreferenceStore();
    return prefsStore.getString(name);
  }

  public static void setStringPref(String name, String value)
  {
    IPreferenceStore prefsStore = getDefault().getPreferenceStore();
    if (value == null || value.length() == 0)
      prefsStore.setToDefault(name);
    else
      prefsStore.setValue(name, value);
  }

  public static boolean isPreJuno()
  {
    if (isRcp())
      return false;

    String platformVer = Platform.getProduct().getDefiningBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
    return platformVer.startsWith("3");
  }
}
