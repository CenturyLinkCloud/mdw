/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.value;

import java.util.List;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;

public class TransformEditorValueProvider extends ArtifactEditorValueProvider
{
  private Activity activity;

  public TransformEditorValueProvider(Activity activity)
  {
    super(activity);
    this.activity = activity;
  }

  public byte[] getArtifactContent()
  {
    String value = activity.getAttribute(getAttributeName());
    return value == null ? null : value.getBytes();
  }

  public String getArtifactTypeDescription()
  {
    return "Transform";
  }

  public String getEditLinkLabel()
  {
    return activity.isReadOnly() ? "View Transform" : "Edit Transform";
  }

  public List<String> getLanguageOptions()
  {
    return activity.getTransformLanguages();
  }

  public String getDefaultLanguage()
  {
    return "GPath";
  }

  public String getLanguage()
  {
    return activity.getTransformLanguage();
  }

  public void languageChanged(String newLanguage)
  {
    activity.setTransformLanguage(newLanguage);
  }

  public String getAttributeName()
  {
    PropertyEditorList propEditorList = new PropertyEditorList(activity);
    for (PropertyEditor propertyEditor : propEditorList)
    {
      if (propertyEditor.getType().equals(PropertyEditor.TYPE_SCRIPT))
      {
        return propertyEditor.getName();
      }
    }
    return "RULE";
  }

  public static boolean isTransformActivity(Activity activity)
  {
    PropertyEditorList propEditorList = new PropertyEditorList(activity);
    for (PropertyEditor propertyEditor : propEditorList)
    {
      if (propertyEditor.getType().equals(PropertyEditor.TYPE_SCRIPT)
          && propertyEditor.getScriptType() != null
          && propertyEditor.getScriptType().equals("TRANSFORM"))
        return true;
    }
    return false;
  }
}