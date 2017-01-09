/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.EmbeddedSubProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.ArtifactEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.RichTextEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.properties.value.ArtifactEditorValueProvider;
import com.centurylink.mdw.plugin.designer.properties.value.DocumentationEditorValueProvider;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;

public class DocumentationSection extends PropertySection implements IFilter
{
  private static final String ATTR = WorkAttributeConstant.DOCUMENTATION;

  private WorkflowElement element;
  public WorkflowElement getElement() { return element; }

  private PropertyEditor richTextEditor;
  private ArtifactEditor artifactEditor;
  private PropertyEditor referenceIdEditor;
  private PropertyEditor sequenceIdEditor;

  boolean alreadyHtml;
  boolean alreadyWord;

  public void setSelection(WorkflowElement selection)
  {
    boolean epfSupport = false;
    try
    {
      Class.forName("org.eclipse.epf.richtext.RichTextEditor");
      epfSupport = true;
    }
    catch (ClassNotFoundException ex)
    {
    }
    catch (Throwable t)
    {
      PluginMessages.log(t);
    }

    element = selection;
    alreadyHtml = false;
    alreadyWord = false;
    String attrVal = element.getAttribute(ATTR);
    if (attrVal != null && !attrVal.isEmpty())
    {
      if (attrVal.startsWith("<html>"))
        alreadyHtml = true;
      else
        alreadyWord = true;
    }

    if (artifactEditor != null)
    {
      artifactEditor.dispose();
      artifactEditor = null;
    }
    if (referenceIdEditor != null)
    {
      referenceIdEditor.dispose();
      referenceIdEditor = null;
    }
    if (sequenceIdEditor != null)
    {
      sequenceIdEditor.dispose();
      sequenceIdEditor = null;
    }
    if (richTextEditor != null)
    {
      richTextEditor.dispose();
      richTextEditor = null;
    }

    if ((MdwPlugin.getSettings().isMsWordDocumentationEditing() && !alreadyHtml) || alreadyWord)
    {

      // artifact editor
      ArtifactEditorValueProvider valueProvider = new DocumentationEditorValueProvider(selection, epfSupport)
      {
        @Override
        public void languageChanged(String newLanguage)
        {
          super.languageChanged(newLanguage);
          if ("HTML".equals(newLanguage))
          {
            boolean proceed = true;
            if (alreadyWord)
              proceed = MessageDialog.openConfirm(getShell(), "Confirm Format", "Proceed with switch to HTML format? (Word-formatted content will be lost.)");
            if (proceed)
            {
              element.setAttribute(getAttributeName(), "<html></html>");
              setSelection(element);
            }
            else
            {
              artifactEditor.setLanguage("MS Word");
            }
          }
        }
      };
      artifactEditor = new ArtifactEditor(selection, valueProvider, "Format");
      artifactEditor.render(composite);

      artifactEditor.setElement(selection);
      artifactEditor.setEditable(!selection.isReadOnly());

      if (element instanceof Activity || element instanceof EmbeddedSubProcess)
      {
        // reference ID text field
        sequenceIdEditor = new PropertyEditor(element, PropertyEditor.TYPE_TEXT);
        sequenceIdEditor.setLabel("Sequence Number");
        sequenceIdEditor.setWidth(100);
        sequenceIdEditor.setVerticalIndent(5);
        sequenceIdEditor.render(composite);
        sequenceIdEditor.setElement(selection);
        sequenceIdEditor.setValue(element instanceof EmbeddedSubProcess ? ((EmbeddedSubProcess)element).getSequenceId() : ((Activity)element).getSequenceId());
        sequenceIdEditor.setEditable(false);

        // reference ID text field
        referenceIdEditor = new PropertyEditor(element, PropertyEditor.TYPE_TEXT);
        referenceIdEditor.setLabel("Reference ID");
        referenceIdEditor.setWidth(100);
        referenceIdEditor.setComment("Optional (select Reference ID element order when exporting)");
        referenceIdEditor.addValueChangeListener(new ValueChangeListener()
        {
          public void propertyValueChanged(Object newValue)
          {
            element.setAttribute(WorkAttributeConstant.REFERENCE_ID, (String)newValue);
          }
        });
        referenceIdEditor.render(composite);
        referenceIdEditor.setElement(selection);
        referenceIdEditor.setEditable(!selection.isReadOnly());
        referenceIdEditor.setValue(element.getAttribute(WorkAttributeConstant.REFERENCE_ID));
      }
    }
    else
    {
      richTextEditor = new RichTextEditor(element);
      richTextEditor.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          String value = (String) newValue;
          if (element instanceof WorkflowProcess)
            ((WorkflowProcess)element).setAttribute(ATTR, value);
          else if (element instanceof Activity)
            ((Activity)element).setAttribute(ATTR, value);
          else if (element instanceof EmbeddedSubProcess)
            ((EmbeddedSubProcess)element).setAttribute(ATTR, value);
        }
      });
      richTextEditor.render(composite);

      richTextEditor.setElement(element);
      richTextEditor.setValue(element.getAttribute(ATTR));
      richTextEditor.setEditable(!element.isReadOnly());
    }

    composite.layout(true);
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    element = selection;
    // widget creation is deferred until setSelection()
  }

  public boolean select(Object toTest)
  {
    if (!(toTest instanceof Activity) && !(toTest instanceof WorkflowProcess) && !(toTest instanceof EmbeddedSubProcess))
      return false;

    if (toTest instanceof Activity && ((Activity)toTest).isForProcessInstance())
      return false;
    if (toTest instanceof EmbeddedSubProcess && ((EmbeddedSubProcess)toTest).isForProcessInstance())
      return false;

    if (((WorkflowElement)toTest).hasInstanceInfo())
      return false;

    return true;
  }
}