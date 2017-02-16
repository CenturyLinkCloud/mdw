/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset.AssetWorkbenchListener;
import com.centurylink.mdw.plugin.designer.wizards.NewJarFileWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewJavaWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewJsonWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewPageWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewReportWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewRuleWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewScriptWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewSpringConfigWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTaskTemplateWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTemplateWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTestCaseWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewTextResourceWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewWebResourceWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewWordDocWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewXmlDocWizard;
import com.centurylink.mdw.plugin.designer.wizards.NewYamlWizard;

public class WorkflowAssetFactory {
    public static WorkflowAsset createAsset(RuleSetVO ruleSetVO, WorkflowPackage workflowPackage) {
        String language = ruleSetVO.getLanguage();
        if (language == null)
            return null;

        if (language.equals(RuleSetVO.GROOVY) || language.equals(RuleSetVO.JAVASCRIPT)) {
            return new Script(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.JAVA)) {
            return new JavaSource(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.DROOLS) || language.equals(RuleSetVO.GUIDED)
                || language.equals(RuleSetVO.EXCEL) || language.equals(RuleSetVO.EXCEL_2007)) {
            return new Rule(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.MS_WORD)) {
            return new WordDoc(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.CAMEL_ROUTE)) {
            return new CamelRoute(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.FACELET) || language.equals(RuleSetVO.JSP)
                || language.equals(RuleSetVO.HTML) || language.equals(RuleSetVO.FORM)
                || language.equals(RuleSetVO.PAGELET)) {
            return new Page(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.CSS) || language.equals(RuleSetVO.WEBSCRIPT)
                || language.equals(RuleSetVO.JAVASCRIPT) || language.equals(RuleSetVO.IMAGE_GIF)
                || language.equals(RuleSetVO.IMAGE_JPEG) || language.equals(RuleSetVO.IMAGE_PNG)) {
            return new WebResource(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.VELOCITY)) {
            try {
                return new Template(ruleSetVO, workflowPackage);
            }
            catch (NoClassDefFoundError er) {
                // don't prevent everything from loading
                PluginMessages.log(er);
            }
        }
        else if (language.equals(RuleSetVO.BIRT)) {
            return new Report(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.SPRING)) {
            return new SpringConfig(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.JAR)) {
            return new JarFile(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.TEXT)) {
            return new TextResource(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.XML) || language.equals(RuleSetVO.XSD)
                || language.equals(RuleSetVO.WSDL) || language.equals(RuleSetVO.XSL)) {
            return new XmlDoc(ruleSetVO, workflowPackage);
        }
        else if (language.startsWith("ATTRIBUTE_OVERFLOW")) {
            return null;
        }
        else if (language.equals(RuleSetVO.PROCESS)) {
            return null;
        }
        else if (language.equals(RuleSetVO.TEST) || language.equals(RuleSetVO.FEATURE)) {
            if (workflowPackage != null) // && !workflowPackage.isArchived()) We
                                         // will pick latest version anyways -
                                         // Bigger problems if latest version is
                                         // the one from Archive folder
            {
                TestCase testCase;
                if (workflowPackage.getProject().isFilePersist())
                    testCase = new TestCase(workflowPackage.getName(), ruleSetVO.getRawFile());
                else
                    testCase = new TestCase(workflowPackage.getName(), ruleSetVO);
                return new AutomatedTestCase(ruleSetVO, workflowPackage, testCase);
            }
        }
        else if (language.equals(RuleSetVO.YAML)) {
            // TODO: should not assume all YAML assets are test results
            return new AutomatedTestResults(ruleSetVO, workflowPackage);
        }
        else if (language.equals(RuleSetVO.JSON)) {
            return new Json(ruleSetVO, workflowPackage);
        }

        return new WorkflowAsset(ruleSetVO, workflowPackage);
    }

    public static Folder createAssetFolder(WorkflowAsset asset) {
        Folder folder = new Folder(asset.getName(), asset.getProject());
        folder.setChildren(new ArrayList<WorkflowElement>());

        if (asset instanceof Script)
            folder.setIcon("script_folder.gif");
        else if (asset instanceof Page)
            folder.setIcon("page_folder.gif");
        else
            folder.setIcon("resources_folder.gif");

        folder.getChildren().add(asset);
        return folder;
    }

    public static void registerAsset(WorkflowAsset asset) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.addResourceChangeListener(asset, IResourceChangeEvent.POST_CHANGE);
    }

    public static void deRegisterAsset(WorkflowAsset asset) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.removeResourceChangeListener(asset);
    }

    private static Map<IFile, AssetWorkbenchListener> workbenchListeners;

    public static Map<IFile, AssetWorkbenchListener> getWorkbenchListeners() {
        return workbenchListeners;
    }

    public static AssetWorkbenchListener getWorkbenchListener(IFile file) {
        if (workbenchListeners == null)
            return null;
        return workbenchListeners.get(file);
    }

    public static void registerWorkbenchListener(IFile file, AssetWorkbenchListener listener) {
        PlatformUI.getWorkbench().addWorkbenchListener(listener);
        if (workbenchListeners == null)
            workbenchListeners = new HashMap<IFile, AssetWorkbenchListener>();
        workbenchListeners.put(file, listener);
    }

    public static void deRegisterWorkbenchListener(IFile file) {
        if (workbenchListeners != null) {
            IWorkbenchListener listener = workbenchListeners.get(file);
            if (listener != null) {
                PlatformUI.getWorkbench().removeWorkbenchListener(listener);
                workbenchListeners.remove(file);
            }
        }
    }

    private static Map<String, String> wizardIds = new HashMap<String, String>();
    static {
        wizardIds.put(RuleSetVO.BIRT, NewReportWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.CSS, NewWebResourceWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.DROOLS, NewRuleWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.CAMEL_ROUTE, NewRuleWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.EXCEL, NewRuleWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.EXCEL_2007, NewRuleWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.MS_WORD, NewWordDocWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.FACELET, NewPageWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.FEATURE, NewTestCaseWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.FORM, NewPageWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.GROOVY, NewScriptWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.GUIDED, NewRuleWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.HTML, NewPageWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.IMAGE_GIF, NewWebResourceWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.IMAGE_JPEG, NewWebResourceWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.IMAGE_PNG, NewWebResourceWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.JAR, NewJarFileWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.JAVA, NewJavaWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.JAVASCRIPT, NewScriptWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.JSON, NewJsonWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.JSP, NewPageWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.MAGICBOX, NewScriptWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.PAGELET, NewPageWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.SPRING, NewSpringConfigWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.TASK, NewTaskTemplateWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.TEXT, NewTextResourceWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.TEST, NewTestCaseWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.VELOCITY, NewTemplateWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.WEBSCRIPT, NewWebResourceWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.XML, NewXmlDocWizard.WIZARD_ID);
        wizardIds.put(RuleSetVO.YAML, NewYamlWizard.WIZARD_ID);
    }

    public static String getWizardId(String ruleSetLanguage) {
        return wizardIds.get(ruleSetLanguage);
    }
}
