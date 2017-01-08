/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.WorkflowAssetEditor;

public class ListViewLabelProvider extends LabelProvider implements ITableLabelProvider
{
  public static final String TYPE_CHECKBOX = "BOOLEAN";
  public static final String TYPE_COMBO = "DROPDOWN";
  public static final String TYPE_ASSET = "ASSET";
  
  public List<ColumnSpec> columnSpecs;
  private PropertyUtilsBean propUtilsBean = new PropertyUtilsBean();
  private Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

  public ListViewLabelProvider(List<ColumnSpec> columnSpecs)
  {
    this.columnSpecs = columnSpecs;
  }
  
  public String getColumnText(Object element, int columnIndex)
  {
    ColumnSpec colspec = columnSpecs.get(columnIndex);
    if (colspec.type.equals(TYPE_CHECKBOX))
    {
      return null;
    }
    else if (colspec.type.equals(TYPE_COMBO))
    {
      return null; // TODO
    }
    else if (colspec.type.equals(WorkflowAssetEditor.TYPE_ASSET))
    {
      String stringVal = (String)getValue(element, columnIndex);
      if (stringVal != null && stringVal.indexOf('/') > 0)
      {
        return stringVal.substring(stringVal.indexOf('/') + 1);
      }
      else
      {
        return stringVal;
      }
    }
    else
    {
      Object val = getValue(element, columnIndex);
      if (val instanceof Date && colspec.dateFormat != null)
        return colspec.dateFormat.format((Date)val);
      
      return val == null ? "" : String.valueOf(val);
    }
  }

  public Image getColumnImage(Object element, int columnIndex)
  {
    ColumnSpec colspec = columnSpecs.get(columnIndex);
    if (colspec.type.equals(TYPE_CHECKBOX))
    {
      ImageDescriptor descriptor = null;
      Boolean value = (Boolean) getValue(element, columnIndex);
      if (value.booleanValue())
      {
        descriptor = MdwPlugin.getImageDescriptor("icons/checked.gif");
      }
      else
      {
        descriptor = MdwPlugin.getImageDescriptor("icons/unchecked.gif");          
      }
      Image image = (Image) imageCache.get(descriptor);
      if (image == null)
      {
        image = descriptor.createImage();
        imageCache.put(descriptor, image);
      }        
      return image;
    }
    else
    {
      return null;
    }
  }
  
  private Object getValue(Object element, int columnIndex)
  {
    if (element == null)
      return null;
    
    try
    {
      String propName = columnSpecs.get(columnIndex).property;
      return propUtilsBean.getProperty(element, propName);
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
      return null;
    }
  }

}
