/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.PreferenceFilterEntry;
import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.IProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileVersioner;
import org.eclipse.jdt.internal.ui.viewsupport.ProjectTemplateStore;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateReaderWriter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.centurylink.mdw.plugin.PluginMessages;

@SuppressWarnings("restriction")
public class WorkspaceConfigurator implements IWorkspaceRunnable {
    private WorkspaceConfig workspaceConfig;

    public WorkspaceConfig getWorkspaceConfig() {
        return workspaceConfig;
    }

    private Shell shell;

    public Shell getShell() {
        return shell;
    }

    List<IRuntimeWorkingCopy> runtimeWorkingCopies;

    public WorkspaceConfigurator(WorkspaceConfig workspaceConfig, Shell shell) {
        this.workspaceConfig = workspaceConfig;
        this.shell = shell;
    }

    private boolean accepted;

    public boolean isAccepted() {
        return accepted;
    }

    public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
        accepted = doConfigure(monitor);
    }

    @SuppressWarnings("deprecation")
    public boolean doConfigure(IProgressMonitor monitor) {
        monitor.beginTask("Configuring Workspace -- ", 100);

        // save the server runtimes so they don't get blown away on import
        stashRuntimeWorkingCopies();
        monitor.worked(5);

        // preferences
        monitor.subTask("Loading Preferences");
        if (!loadPreferences())
            return false;
        monitor.worked(50);

        // formatter
        monitor.subTask("Setting Formatter");
        setFormatter();
        monitor.worked(10);

        // templates
        monitor.subTask("Importing Templates");
        importTemplates();
        monitor.worked(25);

        // autobuild
        monitor.subTask("Setting Autobuild");
        ResourcesPlugin.getPlugin().getPluginPreferences().setValue(
                ResourcesPlugin.PREF_AUTO_BUILDING, getWorkspaceConfig().isEclipseAutobuild());
        monitor.worked(5);

        // restore the runtimes
        restoreRuntimeWorkingCopies(monitor);

        // save the current values
        workspaceConfig.save();

        monitor.done();

        return true;
    }

    private void stashRuntimeWorkingCopies() {
        runtimeWorkingCopies = new ArrayList<IRuntimeWorkingCopy>();
        for (IRuntime serverRuntime : ServerCore.getRuntimes()) {
            runtimeWorkingCopies.add(serverRuntime.createWorkingCopy());
        }

    }

    private void restoreRuntimeWorkingCopies(IProgressMonitor monitor) {
        try {
            for (IRuntimeWorkingCopy runtimeWorkingCopy : runtimeWorkingCopies) {
                runtimeWorkingCopy.save(true, monitor);
            }
            runtimeWorkingCopies = null;
        }
        catch (CoreException ex) {
            PluginMessages.uiError(getShell(), ex, "Workspace Setup");
        }
    }

    /**
     * Loads preferences from the workspace setup site.
     * 
     * @return <code>true</code> if the load was successful
     */
    protected boolean loadPreferences() {
        // import all
        IPreferenceFilter[] filters = new IPreferenceFilter[1];
        filters[0] = new IPreferenceFilter() {
            public String[] getScopes() {
                return new String[] { InstanceScope.SCOPE, ConfigurationScope.SCOPE };
            }

            public Map<String, PreferenceFilterEntry[]> getMapping(String scope) {
                return null;
            }
        };

        String baseUrl = getWorkspaceConfig().getMdwSettings().getWorkspaceSetupUrl();
        if (!baseUrl.endsWith("/"))
            baseUrl += "/";

        InputStream is = null;

        try {
            URL url = new URL(baseUrl + "MdwWorkspaceSetup.epf");
            URLConnection connection = url.openConnection();
            is = connection.getInputStream();

            IPreferencesService service = Platform.getPreferencesService();
            IExportedPreferences prefs = service.readPreferences(is);
            service.applyPreferences(prefs, filters);
        }
        catch (Exception ex) {
            PluginMessages.uiError(getShell(), ex, "Workspace Setup");
            return false;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                }
            }
        }
        return true;
    }

    protected void setFormatter() {
        IScopeContext instanceScope = InstanceScope.INSTANCE;
        IEclipsePreferences uiPrefs = instanceScope.getNode(JavaUI.ID_PLUGIN);
        if (getWorkspaceConfig().getCodeFormatter().equals(WorkspaceConfig.CODE_FORMATTERS[0]))
            uiPrefs.put("formatter_profile", "_CenturyLinkIT");
        else
            uiPrefs.put("formatter_profile", "_MDWCodeFormatter");

        try {
            uiPrefs.flush();
            uiPrefs.sync();

            IProfileVersioner profileVersioner = new ProfileVersioner();
            ProfileStore profileStore = new FormatterProfileStore(profileVersioner);

            List<ProfileManager.Profile> profiles = profileStore.readProfiles(instanceScope);
            if (profiles == null)
                profiles = profileStore.readProfiles(DefaultScope.INSTANCE);
            if (profiles == null)
                profiles = new ArrayList<ProfileManager.Profile>();
            WorkingCopyManager workingCopyManager = new WorkingCopyManager();
            PreferencesAccess access = PreferencesAccess
                    .getWorkingCopyPreferences(workingCopyManager);
            ProfileManager profileManager = new FormatterProfileManager(profiles, instanceScope,
                    access, profileVersioner);
            profileManager.commitChanges(instanceScope);
        }
        catch (Exception ex) {
            PluginMessages.uiError(getShell(), ex, "Set Code Formatter");
        }
    }

    protected void importTemplates() {
        String baseUrl = getWorkspaceConfig().getMdwSettings().getWorkspaceSetupUrl();
        if (!baseUrl.endsWith("/"))
            baseUrl += "/";
        String templatesUrl = baseUrl;
        if (getWorkspaceConfig().getCodeTemplates().equals(WorkspaceConfig.CODE_TEMPLATES[0]))
            templatesUrl += "CenturyLinkITCodeTemplates.xml";
        else
            templatesUrl += "MDWCodeTemplates.xml";

        InputStream is = null;

        try {
            URL url = new URL(templatesUrl);
            URLConnection connection = url.openConnection();
            is = connection.getInputStream();

            TemplateReaderWriter reader = new TemplateReaderWriter();
            TemplatePersistenceData[] dataElements = reader.read(is, null);
            for (int i = 0; i < dataElements.length; i++) {
                updateTemplate(dataElements[i]);
            }
        }
        catch (Exception ex) {
            PluginMessages.uiError(getShell(), ex, "Import Templates");
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                }
            }
        }
    }

    private void updateTemplate(TemplatePersistenceData data) {
        ProjectTemplateStore templateStore = new ProjectTemplateStore(null);
        TemplatePersistenceData[] dataElements = templateStore.getTemplateData();
        for (int i = 0; i < dataElements.length; i++) {
            String id = dataElements[i].getId();
            if (id != null && id.equals(data.getId())) {
                dataElements[i].setTemplate(data.getTemplate());
                break;
            }
        }
    }
}
