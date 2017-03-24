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
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.codegen.jet.JETEmitter;
import org.eclipse.emf.codegen.jet.JETException;
import org.eclipse.jface.dialogs.Dialog;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.codegen.meta.JavaCode;
import com.centurylink.mdw.plugin.designer.dialogs.FrameworkUpdateDialog;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class JavaSource extends WorkflowAsset {
    public JavaSource() {
        super();
    }

    public JavaSource(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        super(ruleSetVO, packageVersion);
    }

    public JavaSource(JavaSource cloneFrom) {
        super(cloneFrom);
    }

    @Override
    public String getTitle() {
        return "Java Source";
    }

    @Override
    public String getIcon() {
        return "java.gif";
    }

    @Override
    public String getDefaultExtension() {
        return ".java";
    }

    @Override
    public IFolder getTempFolder() {
        IFolder tempFolder = getProject().getSourceProject()
                .getFolder(MdwPlugin.getSettings().getTempResourceLocation());
        if (!isInDefaultPackage()) {
            StringTokenizer st = new StringTokenizer(getJavaPackageName(), ".");
            while (st.hasMoreTokens())
                tempFolder = tempFolder.getFolder(st.nextToken());
        }

        return tempFolder;
    }

    @Override
    public String getTempFileName() {
        return getJavaClassName() + getExtension();
    }

    @Override
    public String getDefaultContent() {
        try {
            // initialize from template
            String templateUri = Platform.getBundle(MdwPlugin.getPluginId()).getEntry("/")
                    .toString() + "/templates/source/JavaSource.javajet";
            JETEmitter emitter = new JETEmitter(templateUri, getClass().getClassLoader());
            emitter.addVariable(null, MdwPlugin.getPluginId());
            Map<String, Object> map = new HashMap<String, Object>();
            JavaCode javaCode = new JavaCode(getJavaPackageName(), getJavaClassName());
            map.put("model", javaCode);
            map.put("settings", MdwPlugin.getSettings());
            return emitter.generate(new NullProgressMonitor(), new Object[] { map });
        }
        catch (JETException ex) {
            PluginMessages.uiError(ex, "Generate Java", getProject());
            return null;
        }
    }

    @Override
    public String validate() {
        if (!isInDefaultPackage()
                && !"true".equals(System.getProperty("mdw.allow.nonstandard.naming"))) {
            String goodPkgName = getJavaPackageName();
            if (!goodPkgName.equals(getPackage().getName()))
                return "Packages with Java Source must comply with Java package naming restrictions.";
        }
        return super.validate();
    }

    @Override
    public void beforeFileOpened() {
        ProjectConfigurator projConf = new ProjectConfigurator(getProject(),
                MdwPlugin.getSettings());
        IProgressMonitor monitor = new NullProgressMonitor();
        projConf.setJava(monitor);
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
                    projConf.initializeWebAppJars();
                    getProject().setMdwVersion(origVer);
                }
            }
            else if (!getProject().isCustomTaskManager() && !projConf.hasWebAppJars()) {
                projConf.initializeWebAppJars();
            }
        }
        catch (CoreException ex) {
            PluginMessages.uiError(ex, "Framework Jars", getProject());
        }
    }

    private static List<String> languages;

    @Override
    public List<String> getLanguages() {
        if (languages == null) {
            languages = new ArrayList<String>();
            languages.add("Java");
        }
        return languages;
    }

    protected String getJavaPackageName() {
        return JavaNaming.getValidPackageName(getPackage().getName());
    }

    protected String getJavaClassName() {
        return JavaNaming.getValidClassName(getName());
    }

}