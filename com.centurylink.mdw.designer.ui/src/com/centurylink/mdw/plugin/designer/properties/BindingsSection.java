/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.MappingEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.qwest.mbeng.FormatXml;
import com.qwest.mbeng.MbengDocumentClass;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

/**
 * General bindings section for activity PAGELET elements which specify
 * SECTION='Bindings'.  Note: this section is not used for process start or
 * subprocess bindings (see StartProcessMappingSection and SubProcessMappingSection).
 */
public class BindingsSection extends PropertySection implements IFilter
{
  private Activity activity;
  public Activity getActivity() { return activity; }

  private MappingEditor mappingEditor;

  @Override
  public void setSelection(WorkflowElement selection)
  {
    activity = (Activity) selection;

    mappingEditor.setElement(activity);
    MbengNode bindingsNode = findBindingsNode(activity);
    mappingEditor.setValueAttr(bindingsNode.getAttribute("NAME"));
    mappingEditor.initValue();
    mappingEditor.setEditable(!activity.isReadOnly() && !mappingEditor.getProcessVariables().isEmpty());
  }

  @Override
  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    activity = (Activity) selection;

    mappingEditor = new MappingEditor(activity);
    mappingEditor.setOwningProcess(activity.getProcess());
    mappingEditor.setBindingColumnLabel(findBindingsNode(activity).getAttribute("LABEL"));

    mappingEditor.render(composite);
  }

  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof Activity))
      return false;

    Activity activity = (Activity) toTest;
    if (activity.isForProcessInstance())
      return false;

    return findBindingsNode(activity) != null;
  }

  private MbengNode findBindingsNode(Activity activity)
  {
    String attrXml = activity.getActivityImpl().getAttrDescriptionXml();

    FormatXml formatter = new FormatXml();
    MbengDocumentClass mbengDocument = new MbengDocumentClass();
    try
    {
      formatter.load(mbengDocument, attrXml);
      for (MbengNode currentNode = mbengDocument.getRootNode().getFirstChild(); currentNode != null; currentNode = currentNode.getNextSibling())
      {
        if (MappingEditor.TYPE_MAPPING.equals(currentNode.getName())
            && PropertyEditor.SECTION_BINDINGS.equals(currentNode.getAttribute("SECTION")))
          return currentNode;
      }
    }
    catch (MbengException ex)
    {
      PluginMessages.uiError(getShell(), ex, "Parse Attr XML");
    }

    return null;
  }
}
