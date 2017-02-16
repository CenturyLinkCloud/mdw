/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.designer.utils.MarkdownRenderer;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.EmbeddedSubProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.properties.editor.ArtifactEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.properties.value.ArtifactEditorValueProvider;
import com.centurylink.mdw.plugin.designer.properties.value.DocumentationEditorValueProvider;

public class DocumentationSection extends PropertySection implements IFilter {
    private static final String ATTR = WorkAttributeConstant.DOCUMENTATION;

    private WorkflowElement element;

    public WorkflowElement getElement() {
        return element;
    }

    private ArtifactEditor artifactEditor;
    private PropertyEditor referenceIdEditor;
    private PropertyEditor sequenceIdEditor;
    private PropertyEditor webEditor;

    private String language = DocumentationEditorValueProvider.MARKDOWN;

    public void setSelection(WorkflowElement selection) {
        element = selection;
        String attrVal = element.getAttribute(ATTR);
        if (attrVal != null && !attrVal.isEmpty()) {
            if (attrVal.length() >= 8) {
                byte[] first4 = RuleSetVO.decode(attrVal.substring(0, 8));
                if (first4[0] == 68 && first4[1] == 35 && first4[2] == 17 && first4[3] == 0)
                    language = DocumentationEditorValueProvider.MS_WORD;
            }
        }

        if (artifactEditor != null) {
            artifactEditor.dispose();
            artifactEditor = null;
        }
        if (referenceIdEditor != null) {
            referenceIdEditor.dispose();
            referenceIdEditor = null;
        }
        if (sequenceIdEditor != null) {
            sequenceIdEditor.dispose();
            sequenceIdEditor = null;
        }
        if (webEditor != null) {
            webEditor.dispose();
            webEditor = null;
        }

        // artifact editor
        ArtifactEditorValueProvider valueProvider = new DocumentationEditorValueProvider(
                selection) {
            @Override
            public void languageChanged(String newLanguage) {
                super.languageChanged(newLanguage);
                boolean proceed = true;
                String attrVal = element.getAttribute(ATTR);
                if (attrVal != null && !attrVal.isEmpty() && !language.equals(newLanguage))
                    proceed = MessageDialog.openConfirm(getShell(), "Confirm Format",
                            "Proceed with switch to " + newLanguage + " format? (" + language
                                    + " formatted content will be lost.)");

                if (proceed) {
                    language = newLanguage;
                    element.setAttribute(getAttributeName(), " ");
                    setSelection(element);
                }
                else {
                    artifactEditor.setLanguage(language);
                }
            }

            @Override
            public String getLanguage() {
                return language;
            }
        };
        artifactEditor = new ArtifactEditor(selection, valueProvider, "Format");
        artifactEditor.render(composite);

        artifactEditor.setElement(selection);
        artifactEditor.setEditable(!selection.isReadOnly());
        artifactEditor.setLanguage(language);

        if (element instanceof Activity || element instanceof EmbeddedSubProcess) {
            // reference ID text field
            sequenceIdEditor = new PropertyEditor(element, PropertyEditor.TYPE_TEXT);
            sequenceIdEditor.setLabel("Sequence Number");
            sequenceIdEditor.setWidth(100);
            sequenceIdEditor.setVerticalIndent(5);
            sequenceIdEditor.render(composite);
            sequenceIdEditor.setElement(selection);
            sequenceIdEditor.setValue(element instanceof EmbeddedSubProcess
                    ? ((EmbeddedSubProcess) element).getSequenceId()
                    : ((Activity) element).getSequenceId());
            sequenceIdEditor.setEditable(false);

            // reference ID text field
            referenceIdEditor = new PropertyEditor(element, PropertyEditor.TYPE_TEXT);
            referenceIdEditor.setLabel("Reference ID");
            referenceIdEditor.setWidth(100);
            referenceIdEditor
                    .setComment("Optional (select Reference ID element order when exporting)");
            referenceIdEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    element.setAttribute(WorkAttributeConstant.REFERENCE_ID, (String) newValue);
                }
            });
            referenceIdEditor.render(composite);
            referenceIdEditor.setElement(selection);
            referenceIdEditor.setEditable(!selection.isReadOnly());
            referenceIdEditor.setValue(element.getAttribute(WorkAttributeConstant.REFERENCE_ID));
        }

        if (DocumentationEditorValueProvider.MARKDOWN.equals(language)
                && element.getProject().checkRequiredVersion(6, 0)) {
            webEditor = new PropertyEditor(element, PropertyEditor.TYPE_WEB);
            webEditor.render(composite);

            webEditor.setElement(element);
            MarkdownRenderer renderer = new MarkdownRenderer(attrVal);
            webEditor.setValue(renderer.renderHtml());
            webEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    MarkdownRenderer renderer = new MarkdownRenderer(
                            newValue == null ? null : newValue.toString());
                    String html = renderer.renderHtml();
                    webEditor.setValue(html);
                }
            });
        }

        composite.layout(true);
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        element = selection;
        // widget creation is deferred until setSelection()
    }

    public boolean select(Object toTest) {
        if (!(toTest instanceof Activity) && !(toTest instanceof WorkflowProcess)
                && !(toTest instanceof EmbeddedSubProcess))
            return false;

        if (toTest instanceof Activity && ((Activity) toTest).isForProcessInstance())
            return false;
        if (toTest instanceof EmbeddedSubProcess
                && ((EmbeddedSubProcess) toTest).isForProcessInstance())
            return false;

        if (((WorkflowElement) toTest).hasInstanceInfo())
            return false;

        return true;
    }
}