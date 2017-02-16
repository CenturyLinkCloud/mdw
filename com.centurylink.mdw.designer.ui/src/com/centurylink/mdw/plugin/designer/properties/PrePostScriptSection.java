/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ArtifactEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.properties.value.ArtifactEditorValueProvider;
import com.centurylink.mdw.plugin.designer.properties.value.ScriptEditorValueProvider;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;

/**
 *
 */
public class PrePostScriptSection extends PropertySection implements IFilter {

    private Activity activity;

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity a) {
        this.activity = a;
    }

    private ArtifactEditor preScriptArtifactEditor;
    private ArtifactEditor postScriptArtifactEditor;
    private PropertyEditor helpPropertyEditor;
    private PropertyEditor outputDocsPropertyEditor;

    public void setSelection(WorkflowElement selection) {
        activity = (Activity) selection;
        if (null == activity.getAttribute(WorkAttributeConstant.PRE_SCRIPT_LANGUAGE))
            activity.setAttribute(WorkAttributeConstant.PRE_SCRIPT_LANGUAGE,
                    preScriptArtifactEditor.getValueProvider().getDefaultLanguage());

        preScriptArtifactEditor.setElement(activity);
        preScriptArtifactEditor.setValueProvider(getNewScriptValueProvider(true));
        preScriptArtifactEditor.setEditable(!activity.isReadOnly());

        if (null == activity.getAttribute(WorkAttributeConstant.POST_SCRIPT_LANGUAGE))
            activity.setAttribute(WorkAttributeConstant.POST_SCRIPT_LANGUAGE,
                    postScriptArtifactEditor.getValueProvider().getDefaultLanguage());

        postScriptArtifactEditor.setElement(activity);
        postScriptArtifactEditor.setValueProvider(getNewScriptValueProvider(false));
        postScriptArtifactEditor.setEditable(!activity.isReadOnly());

        if (activity.canWriteOutputDocs()) {
            outputDocsPropertyEditor.setElement(activity);
            outputDocsPropertyEditor.setValue(activity.getAttribute("Output Documents"));
            outputDocsPropertyEditor.setEditable(!activity.isReadOnly());
            outputDocsPropertyEditor.setVisible(true);
        }
        else {
            outputDocsPropertyEditor.setVisible(false);
        }

        helpPropertyEditor.setValue("/MDWHub/doc/scriptActivity.html");
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        activity = (Activity) selection;
        activity.setScriptLanguages(null);

        // artifact editor
        preScriptArtifactEditor = new ArtifactEditor(activity, getNewScriptValueProvider(true),
                "PreScript Language");
        preScriptArtifactEditor.render(composite);

        postScriptArtifactEditor = new ArtifactEditor(activity, getNewScriptValueProvider(false),
                "PostScript Language");
        postScriptArtifactEditor.render(composite);

        // output docs
        outputDocsPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_PICKLIST);
        outputDocsPropertyEditor.setLabel("Reference Vars:Read-Only~Writable");

        outputDocsPropertyEditor.setValueOptions(activity.getProcess().getDocRefVariableNames());
        outputDocsPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                activity.setAttribute("Output Documents", (String) newValue);
            }
        });
        outputDocsPropertyEditor.render(composite);
        outputDocsPropertyEditor.setVisible(activity.canWriteOutputDocs());

        // help link
        helpPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_LINK);
        helpPropertyEditor.setLabel("Script Activity Help");
        helpPropertyEditor.render(composite);
    }

    /**
     * For IFilter interface, determine which activities include this section in
     * their Design properties tab page.
     */
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        Activity activity = (Activity) toTest;
        if (activity.isForProcessInstance())
            return false;

        return activity.isAdapter() && activity.getProject().checkRequiredVersion(5, 2);
    }

    private ArtifactEditorValueProvider getNewScriptValueProvider(boolean preScript) {
        final String scriptLang = preScript ? WorkAttributeConstant.PRE_SCRIPT_LANGUAGE
                : WorkAttributeConstant.POST_SCRIPT_LANGUAGE;
        final String scriptRule = preScript ? WorkAttributeConstant.PRE_SCRIPT
                : WorkAttributeConstant.POST_SCRIPT;
        return new ScriptEditorValueProvider(activity) {
            @Override
            public void languageChanged(String newLanguage) {
                activity.setAttribute(scriptLang, newLanguage);
            }

            @Override
            public String getTempFileName() {
                String ext = WorkflowElement.getArtifactFileExtensions()
                        .get(activity.getAttribute(scriptLang));
                String id = activity.getLogicalId();
                return FileHelper.stripDisallowedFilenameChars(getElement().getName()) + "_"
                        + scriptRule + "_" + id + ext;
            }

            @Override
            public String getAttributeName() {
                return scriptRule;
            }

            @Override
            public String getLanguage() {
                return activity.getAttribute(scriptLang);
            }
        };
    }
}