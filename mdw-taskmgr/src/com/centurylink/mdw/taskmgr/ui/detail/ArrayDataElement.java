/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.detail;

import java.util.Iterator;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.taskmgr.ui.EditableItem;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * Represents a single line item in a DataItem whose type is Array.
 * Limitations: Currently only String arrays support update operations.
 * Only a single Array dataItem is supported per Detail/InstanceData object.
 */
public class ArrayDataElement extends ListItem implements EditableItem
{
  public ArrayDataElement()
  {

  }

  public ArrayDataElement(int sequenceId, DataItem dataItem, int rowIndex)
  {
    _sequenceId = sequenceId;
    _dataItem = dataItem;
    _rowIndex = rowIndex;
    _name = getObjValue().getClass().getName();
  }

  private int _sequenceId = -1;
  public int getSequenceId() { return _sequenceId; }
  public void setSequenceId(int sid) { _sequenceId = sid; }

  private DataItem _dataItem;
  public DataItem getDataItem() { return _dataItem; }
  public void setDataItem(DataItem di) { _dataItem = di; }

  private int _rowIndex = -1;
  public int getRowIndex() { return _rowIndex; }
  public void setRowIndex(int i) { _rowIndex = i; }

  public String getStringValue()
  {
    if (getObjValue() == null)
      return null;

    return getObjValue().toString();
  }
  public Object getObjValue()
  {
    if (_dataItem == null || _rowIndex == -1)
      return null;

    Object[] itemArray = (Object[]) _dataItem.getDataValue();
    return itemArray[_rowIndex];
  }

  public void setStringValue(String s)
  {
    if (s == null || _dataItem == null || isSaveRequest() || isTaskAction())
      return;

    Object[] itemArray = (Object[]) _dataItem.getDataValue();

    if (itemArray instanceof MapElement[])
    {
      String name = getItemNameFromRequestParam();
      setObjValue(new MapElement(name, s));
    }
    else if (itemArray instanceof Integer[])
    {
      try
      {
        setObjValue(new Integer(s));
      }
      catch (NumberFormatException ex)
      {
        // formatting is caught in validation
      }
    }
    else if (itemArray instanceof Long[])
    {
      try
      {
        setObjValue(new Long(s));
      }
      catch (NumberFormatException ex)
      {
        // formatting is caught in validation
      }
    }
    else
    {
      setObjValue(s);
    }
  }
  
  public void setObjValue(Object o)
  {
    if (_dataItem == null || _rowIndex == -1)
      return;

    Object[] itemArray = (Object[]) _dataItem.getDataValue();
    itemArray[_rowIndex] = o;
  }

  private String _comment;
  public String getComment() { return _comment; }
  public void setComment(String comment) { _comment = comment; }

  private String _name;
  public String getName() { return _name; }
  public void setName(String name) { _name = name; }

  private Long _id;
  public Long getId() { return _id; }
  public void setId(Long id) { _id = id; }

  /**
   * Clears the current row selection.
   */
  public void clearRowIndex()
  {
    setRowIndex(-1);
  }

  /**
   * Add a new element to the array and save it.
   */
  public void add() throws UIException
  {
    setSequenceId(getSequenceIdFromRequestParam());
    _dataItem = ((InstanceDataItem)_dataItem).findDataItem(getSequenceId());
    String newElementValue = getItemValueFromRequestParam();
    
    if (newElementValue != null)
    {
      Object[] oldItemArray = (Object[]) _dataItem.getDataValue();
      Object[] newItemArray = null;
      
      int oldSize = 0;
      if (oldItemArray != null)
        oldSize = oldItemArray.length;
      
      if (_dataItem.getDataType().equals("java.util.Map"))
      {
        newItemArray = new MapElement[oldSize + 1];
        String name = getItemNameFromRequestParam();
        newItemArray[oldSize] = new MapElement(name, newElementValue);
      }
      else if (_dataItem.getDataType().equals("java.lang.Integer[]"))
      {
        newItemArray = new Integer[oldSize + 1];
        try
        {
          newItemArray[oldSize] = new Integer(newElementValue);
        }
        catch (NumberFormatException ex)
        {
          // formatting is caught in validator
          newItemArray = oldItemArray;
        }
      }
      else if (_dataItem.getDataType().equals("java.lang.Long[]"))
      {
        newItemArray = new Long[oldSize + 1];
        try
        {
          newItemArray[oldSize] = new Long(newElementValue);
        }
        catch (NumberFormatException ex)
        {
          // formatting is caught in validator
          newItemArray = oldItemArray;
        }
      }
      else
      {
        newItemArray = new String[oldSize + 1];
        newItemArray[oldSize] = newElementValue;
      }
        
      for (int i = 0; i < oldSize; i++)
      {
        newItemArray[i] = oldItemArray[i];
      }
      
      _dataItem.setDataValue(newItemArray);
      ((InstanceDataItem)_dataItem).resetArrayValues();
      ((InstanceDataItem)_dataItem).saveImmediate();
    }
  }

  /**
   * Delete the current element from the array.
   */
  public void delete() throws UIException
  {
    Object[] oldItemArray = (Object[]) _dataItem.getDataValue();
    Object[] newItemArray = null;

    if (oldItemArray instanceof MapElement[])
    {
      newItemArray = new MapElement[oldItemArray.length - 1];
    }
    else if (oldItemArray instanceof Integer[])
    {
      newItemArray = new Integer[oldItemArray.length - 1];
    }
    else if (oldItemArray instanceof Long[])
    {
      newItemArray = new Long[oldItemArray.length - 1];
    }
    else
    {
      newItemArray = new String[oldItemArray.length - 1];
    }


    for (int i = 0; i < oldItemArray.length; i++)
    {
      if (i < _rowIndex)
        newItemArray[i] = oldItemArray[i];
      if (i > _rowIndex)
        newItemArray[i - 1] = oldItemArray[i];
    }

    _dataItem.setDataValue(newItemArray);
    ((InstanceDataItem)_dataItem).resetArrayValues();
    ((InstanceDataItem)_dataItem).saveImmediate();
  }

  /**
   * Save the array element at the appropriate location in the array.
   */
  public void save() throws UIException
  {
    setStringValue(getItemValueFromRequestParam());
    ((InstanceDataItem)_dataItem).saveImmediate();
  }

  private String getItemValueFromRequestParam()
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
    {
      String paramName = iter.next();
      int endIdx = paramName.indexOf(":arrayDataAddText");
      if (endIdx == -1)
        endIdx = paramName.indexOf(":arrayDataAddSelect");
      if (endIdx > 0)
      {
        int startIdx = paramName.substring(0, endIdx).lastIndexOf(':') + 1;
        if (Integer.parseInt(paramName.substring(startIdx, endIdx)) == getSequenceId())
          return (String) externalContext.getRequestParameterMap().get(paramName);
      }
    }

    return null;
  }

  private String getItemNameFromRequestParam()
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
    {
      String paramName = iter.next();
      int endIdx = paramName.indexOf(":arrayDataAddNameText");
      if (endIdx > 0)
      {
        int startIdx = paramName.substring(0, endIdx).lastIndexOf(':') + 1;
        if (Integer.parseInt(paramName.substring(startIdx, endIdx)) == getSequenceId())
          return (String) externalContext.getRequestParameterMap().get(paramName);
      }
    }

    return null;
  }

  private int getSequenceIdFromRequestParam()
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
    {
      String paramName = iter.next();
      int endIdx = paramName.indexOf(":arrayDataAddButton");
      if (endIdx > 0)
      {
        int startIdx = paramName.substring(0, endIdx).lastIndexOf(':') + 1;
        return Integer.parseInt(paramName.substring(startIdx, endIdx));
      }
    }
    return -1;
  }

  private boolean isSaveRequest()
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
    {
      String paramName = iter.next();
      if (paramName != null && paramName.endsWith("taskDetailSave")
          && "Save".equals(externalContext.getRequestParameterMap().get(paramName)))
      {
        return true;
      }
    }
    return false;
  }

  private boolean isTaskAction()
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
    {
      String paramName = iter.next();
      if ((paramName.endsWith("taskAction_go") && "Go".equals(externalContext.getRequestParameterMap().get(paramName)))
          || paramName.endsWith("taskActionMenu_go"))
      {
        return true;
      }
    }
    return false;
  }

  public boolean isEditableByCurrentUser()
  {
    return true;
  }
}
