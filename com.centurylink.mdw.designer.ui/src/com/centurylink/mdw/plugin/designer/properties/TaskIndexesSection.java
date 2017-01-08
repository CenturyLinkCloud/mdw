/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;

/**
 *  For 5.5 this section is replaced by TaskTemplateEditor.
 */
public class TaskIndexesSection extends DesignSection implements IFilter
{
  @Override
  public boolean selectForSection(PropertyEditor propertyEditor)
  {
    return PropertyEditor.SECTION_INDEXES.equals(propertyEditor.getSection());
  }

  @Override
  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof Activity))
      return false;

    Activity activity = (Activity) toTest;

    if (activity.isManualTask())
    {
      PropertyEditorList propEditorList = new PropertyEditorList(activity);
      for (PropertyEditor propertyEditor : propEditorList)
      {
        // return true if any widgets specify Indexes section
        if (PropertyEditor.SECTION_INDEXES.equals(propertyEditor.getSection()))
          return true;
      }
    }

    return false;
  }

  @Override
  public boolean shouldUseExtraSpace()
  {
    return true;
  }
}