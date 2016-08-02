/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;

/**
 * Special section for task workgroups.
 * For 5.5 this section is replaced by TaskTemplateEditor.
 */
public class TaskWorkgroupsSection extends DesignSection implements IFilter
{
  @Override
  public boolean selectForSection(PropertyEditor propertyEditor)
  {
    return PropertyEditor.SECTION_WORKGROUPS.equals(propertyEditor.getSection());
  }

  @Override
  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof Activity))
      return false;

    Activity activity = (Activity) toTest;

    if (activity.isForProcessInstance())
      return false;

    if (activity.isManualTask())
    {
      PropertyEditorList propEditorList = new PropertyEditorList(activity);
      for (PropertyEditor propertyEditor : propEditorList)
      {
        // return true if any widgets specify Workgroups section
        if (PropertyEditor.SECTION_WORKGROUPS.equals(propertyEditor.getSection()))
          return true;
      }
    }

    return false;
  }
}
