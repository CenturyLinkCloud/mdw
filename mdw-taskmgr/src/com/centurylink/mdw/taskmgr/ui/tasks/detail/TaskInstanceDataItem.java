/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.detail;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.variable.VariableInstanceVO;
import com.centurylink.mdw.taskmgr.ui.detail.DataItem;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.detail.InstanceDataItem;
import com.centurylink.mdw.web.ui.UIException;

/**
 * Wraps a model TaskInstanceData object and provides utility
 * methods like isValueModifiable().
 */
public class TaskInstanceDataItem extends InstanceDataItem
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private VariableInstanceVO _taskInstanceData;

  public TaskInstanceDataItem(VariableInstanceVO taskInstanceData, TaskDetail taskDetail)
  {
    super(taskDetail);
    _taskInstanceData = taskInstanceData;
    if (isArray() || isMap())
      initializeArrayDataElement();    
  }
  
  public TaskInstanceDataItem(VariableInstanceVO taskInstanceData, TaskDetail taskDetail, int sequence)
  {
	this(taskInstanceData,taskDetail);
	setSequence(sequence);
  }

  public VariableInstanceVO getTaskInstanceData()
  {
    return _taskInstanceData ;
  }

  public Long getId()
  {
    return _taskInstanceData.getInstanceId();
  }

  public String getDataKeyName()
  {
    if (!StringHelper.isEmpty(_taskInstanceData.getVariableReferredName()))
    {
      return _taskInstanceData.getVariableReferredName();
    }
    return _taskInstanceData.getName();
  }

  public boolean isValueRequired()
  {
    return _taskInstanceData.isRequired();
  }

  public boolean isValueEditable()
  {
    if (isJavaObject() || isHtml())
      return false;
    
    return getTaskDetail().isInstanceDataEditable() && _taskInstanceData.isEditable();
  }
  
  public TaskDetail getTaskDetail()
  {
    return (TaskDetail) getDetail();
  }

  public boolean isSelect()
  {
    return _taskInstanceData.isSelect();
  }
  
  public boolean isAllowArrayResize()
  {
    // default is true
    return _taskInstanceData.allowArrayResize();
  }  

  public List<SelectItem> getSelectList()
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    selectItems.add(new SelectItem(""));
    for (int i = 0; i < _taskInstanceData.getSelectValues().length; i++)
    {
      selectItems.add(new SelectItem(_taskInstanceData.getSelectValues()[i]));
    }
    return selectItems;
  }

  public String getDataType()
  {
    return _taskInstanceData.getType();
  }

  public void saveImmediate() throws UIException
  {
    // save workgroup comments immediately
    if (getName().equals("Workgroup Comments" ))
    {
      logger.info("Saving TaskInstanceDataItem:\n" + this);
      _taskInstanceData.setData(getDataValue());

      TaskDetail taskDetail = DetailManager.getInstance().getTaskDetail();
      Long taskInstId = new Long(taskDetail.getInstanceId());

      TaskInstanceActionController controller = new TaskInstanceActionController();
      try
      {
        controller.createOrUpdateTaskInstanceData(taskInstId, _taskInstanceData, _taskInstanceData.getData());
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new UIException(ex.getMessage(), ex);
      }
    }
  }
  
  public DataItem findDataItem(int sequenceId)
  {
    TaskDetail taskDetail = (com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail) getDetail();
    return (DataItem) taskDetail.getInstanceDataItems().get(sequenceId);
  }
  
  public Object getVariableData()
  {
    if (_taskInstanceData == null)
      return null;
    return _taskInstanceData.getRealData();
  }
  
  public void setVariableData(Object o)
  {
    _taskInstanceData.setData(o);
  }
}
