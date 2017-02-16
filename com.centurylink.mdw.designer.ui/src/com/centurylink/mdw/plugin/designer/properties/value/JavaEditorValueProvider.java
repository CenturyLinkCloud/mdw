/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.codegen.jet.JETEmitter;
import org.eclipse.emf.codegen.jet.JETException;
import org.eclipse.jface.dialogs.Dialog;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.FrameworkUpdateDialog;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;

public class JavaEditorValueProvider extends ArtifactEditorValueProvider {
    private Activity activity;

    public JavaEditorValueProvider(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    public byte[] getArtifactContent() {
        String text = activity.getAttribute(getAttributeName());
        if (text == null || text.trim().length() == 0) {
            try {
                // initialize from template
                String template = "/templates/source/executors/JavaExecutor.javajet";
                if (!getProject().checkRequiredVersion(6, 0))
                    template = "/templates/source/55/executors/JavaExecutor.javajet";
                if (!getProject().checkRequiredVersion(5, 5))
                    template = "/templates/source/52/executors/JavaExecutor.javajet";
                String templateUri = Platform.getBundle(MdwPlugin.getPluginId()).getEntry("/")
                        .toString() + template;
                JETEmitter emitter = new JETEmitter(templateUri, getClass().getClassLoader());
                emitter.addVariable(null, MdwPlugin.getPluginId());
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("model", this);
                map.put("settings", MdwPlugin.getSettings());
                text = emitter.generate(new NullProgressMonitor(), new Object[] { map });
                activity.setAttribute(getAttributeName(), text);
                activity.fireDirtyStateChanged(true);
            }
            catch (JETException ex) {
                PluginMessages.uiError(ex, "Generate Java", getProject());
            }
        }
        return text.getBytes();
    }

    public String getArtifactTypeDescription() {
        return "Java Code";
    }

    @Override
    public IFolder getTempFolder() {
        IFolder tempFolder = getProject().getSourceProject()
                .getFolder(MdwPlugin.getSettings().getTempResourceLocation());
        if (!activity.isInDefaultPackage()) {
            StringTokenizer st = new StringTokenizer(getPackageName(), ".");
            while (st.hasMoreTokens())
                tempFolder = tempFolder.getFolder(st.nextToken());
        }

        return tempFolder;
    }

    @Override
    public String getTempFileName() {
        return getClassName() + WorkflowElement.getArtifactFileExtensions().get(getLanguage());
    }

    public String getClassName() {
        String raw = activity.getName() + "_" + activity.getLogicalId();
        return JavaNaming.getValidClassName(raw);
    }

    public String getPackageName() {
        return JavaNaming.getValidPackageName(activity.getPackage().getName());
    }

    @Override
    public boolean beforeTempFileOpened() {
        ProjectConfigurator projConf = new ProjectConfigurator(getProject(),
                MdwPlugin.getSettings());

        projConf.setJava(new NullProgressMonitor());
        try {
            if (getProject().isRemote() && projConf.isJavaCapable()
                    && !projConf.hasFrameworkJars()) {
                FrameworkUpdateDialog updateDlg = new FrameworkUpdateDialog(MdwPlugin.getShell(),
                        MdwPlugin.getSettings(), getProject());
                if (updateDlg.open() == Dialog.OK) {
                    String origVer = getProject().getMdwVersion(); // as
                                                                   // reported
                                                                   // by server
                                                                   // or db
                    getProject().setMdwVersion(updateDlg.getMdwVersion()); // for
                                                                           // downloading
                    projConf.initializeFrameworkJars();
                    getProject().setMdwVersion(origVer);
                }
            }
        }
        catch (CoreException ex) {
            PluginMessages.uiError(ex, "Framework Jars", getProject());
        }

        return true;
    }

    @Override
    public void afterTempFileSaved() {
        if (!activity.getProject().checkRequiredVersion(5, 5)) {
            // activity level bsn
            String bsn = getBundleSymbolicName();
            if (bsn != null)
                activity.setAttribute(WorkAttributeConstant.OSGI_BSN, bsn);
        }
    }

    public String getEditLinkLabel() {
        return activity.isReadOnly() ? "View Java Code" : "Edit Java Code";
    }

    public List<String> getLanguageOptions() {
        List<String> languages = new ArrayList<String>();
        languages.add("Java");
        return languages;
    }

    public String getDefaultLanguage() {
        return "Java";
    }

    public String getLanguage() {
        return "Java";
    }

    public void languageChanged(String newLanguage) {
    }

    public String getAttributeName() {
        return "Java";
    }
}