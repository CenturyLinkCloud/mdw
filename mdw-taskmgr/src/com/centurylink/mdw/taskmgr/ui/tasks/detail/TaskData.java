/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.detail;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.taskmgr.ui.detail.DataItem;
import com.centurylink.mdw.web.ui.UIDocument;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;

public class TaskData
{
  private TaskDetail taskDetail;
  private MDWProcessInstance processInstance;
  private Map<String,TaskDataItem> dataItems;
  private Map<String,DataItem> dataItemsFacad;
  
  public TaskData()
  {
  }
  
  public TaskData(TaskDetail taskDetail, MDWProcessInstance processInstance)
  {
    this.taskDetail = taskDetail;
    this.processInstance = processInstance;
  }
  
  public Map<String,DataItem> getItems()
  {
    if (dataItemsFacad == null)
    {
      dataItemsFacad = new HashMap<String,DataItem>()
      {
        private static final long serialVersionUID = 1L;

        @Override
        public DataItem get(Object key)
        {
          return getItem(key == null ? null : key.toString());
        }

        @Override
        public DataItem put(String key, DataItem value)
        {
          return dataItems.put(key, (TaskDataItem)value);
        }
      };
    }
    return dataItemsFacad;
  }
  
  public TaskDataItem getItem(String name)
  {
    if (dataItems == null)
      dataItems = new HashMap<String,TaskDataItem>();
    TaskDataItem dataItem = dataItems.get(name);
    if (dataItem == null && processInstance != null)
    {
      for (VariableVO variable : processInstance.getProcessVO().getVariables())
      {
        if (variable.getName().equals(name))
        {
          dataItem = new TaskDataItem(name, variable.getVariableType());
          dataItems.put(name, dataItem);
        }
      }
    }
    
    return dataItem;
  }
  
  public Object getDocument(String docName) throws UIException
  {
    TaskDataItem dataItem = getItem(docName);
    if (dataItem == null)
      return null;
    else
      return dataItem.getDocument();
  }
  
  public DataItem getRequiredItem(String name)
  {
    TaskDataItem dataItem = (TaskDataItem)getItem(name);
    if (dataItem != null)
      dataItem.setRequired(true);
    return dataItem;
  }
  
  public DataItem getReadOnlyItem(String name)
  {
    TaskDataItem dataItem = (TaskDataItem)getItem(name);
    if (dataItem != null)
      dataItem.setReadOnly(true);
    return dataItem;
  }
  
  public class TaskDataItem extends DataItem
  {
    private String name;
    private String type;
    private boolean required;
    private boolean readOnly;
    private boolean rendered = true;
    
    public TaskDataItem(String name, String type)
    {
      this.name = name;
      this.type = type;
    }

    public String getName()
    {
      return name;
    }

    public Object getValue()
    {
      return getDataValue();
    }
    public Object getDataValue()
    {
      return processInstance.getVariables().get(name);
    }
    public void setValue(Object value)
    {
      setDataValue(value);
    }
    public void setDataValue(Object value)
    {
      processInstance.getVariables().put(name, value);
      processInstance.setDirty(name);
    }
    
    public String getDataType()
    {
      return type;
    }

    public boolean isValueRequired()
    {
      return required;
    }
    public void setRequired(boolean required)
    {
      this.required = required;
    }
    
    public boolean isValueEditable()
    {
      return taskDetail.isInstanceDataEditable() && !readOnly;
    }
    public void setReadOnly(boolean readOnly)
    {
      this.readOnly = readOnly;
    }

    public boolean isRendered()
    {
      return rendered;
    }
    public void setRendered(boolean rendered)
    {
      this.rendered = rendered;
    }
    
    public Object getDocument() throws UIException
    {
      DocumentReference docRef = (DocumentReference) processInstance.getVariables().get(name);
      UIDocument uiDoc = processInstance.getDocument(docRef);
      return uiDoc.getObject();
    }
  }  
}
