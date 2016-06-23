/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.taskmgr.ui.detail.ModelWrapper;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;

public class FullProcessInstance extends MDWProcessInstance implements ModelWrapper
{
  private ProcessInstanceVO _processInstanceVO;
  public ProcessInstanceVO getProcessInstanceVO() { return _processInstanceVO; }
  public void setProcessInstanceVO(ProcessInstanceVO piVO)
  {
    _processInstanceVO = piVO;
    super.setProcessInstanceVO(_processInstanceVO);
  }
  
  private List<VariableDataItem> _variableDataItems;
  public List<VariableDataItem> getVariableDataItems() { return _variableDataItems; }
  
  public FullProcessInstance()
  {
    _processInstanceVO = new ProcessInstanceVO(null, null);
    initializeVariableDataItems();
    super.setProcessInstanceVO(_processInstanceVO);
  }
  
  /**
   * Constructor used by process start page.
   * @param processVO
   */
  public FullProcessInstance(ProcessVO processVO)
  {
    _processInstanceVO = new ProcessInstanceVO(processVO.getProcessId(), processVO.getProcessName());
    initializeVariableDataItems();
    super.setProcessInstanceVO(_processInstanceVO);
  }
  
  public Long getProcessId()
  {
    return _processInstanceVO.getProcessId();
  }
  public void setProcessId(Long processId)
  {
    // initialize if form was submitted due to changing of process selection
    if (FacesVariableUtil.getRequestParamValue("mainDetailForm:launchProcessButton") == null
        && !checkArrayAddButtonSubmit())
    {
      if (processId == null || processId.longValue() == 0)
      {
        _processInstanceVO = new ProcessInstanceVO(null, null);
      }
      else
      {
        ProcessVO processVO = ProcessVOCache.getProcessVO(processId);
        _processInstanceVO = new ProcessInstanceVO(processId, processVO.getProcessName());
      }

      initializeVariableDataItems();
    }    
  }
  
  public List<VariableDataItem> getInputParameters()
  {
    List<VariableDataItem> subList = new ArrayList<VariableDataItem>();
    for (VariableDataItem variableDataItem : getVariableDataItems())
    {
      if (variableDataItem.isInputParam())
      {
        if (!variableDataItem.isSupportedVariableType())
          throw new UnsupportedOperationException("Variable Type not supported: " + variableDataItem.getVariableType());

        subList.add(variableDataItem);
      }
    }
    return subList;
  }
  
  public boolean isHasInputParameters()
  {
    return getInputParameters().size() != 0;
  }
  
  protected void initializeVariableDataItems()
  {
    _variableDataItems = new ArrayList<VariableDataItem>();
    if (_processInstanceVO.getProcessId() == null)
      return;
    
    ProcessVO processVO = ProcessVOCache.getProcessVO(_processInstanceVO.getProcessId());
    if (processVO.getVariables() != null)
    {
      for (VariableVO variableVO : processVO.getVariables())
      {
        _variableDataItems.add(new VariableDataItem(this, variableVO));
      }
    }
    Collections.sort(_variableDataItems);
    // set the sequence id for input variables
    List<VariableDataItem> inputItems = getInputParameters();
    for (int i = 0; i < inputItems.size(); i++)
    {
      inputItems.get(i).setSequenceId(i);
    }
  }
  
  public String getName()
  {
    return _processInstanceVO.getProcessName();
  }
  
  public String getMasterRequestId()
  {
    return _processInstanceVO.getMasterRequestId();
  }
  public void setMasterRequestId(String masterReqId)
  {
    _processInstanceVO.setMasterRequestId(masterReqId);
  }
  
  public String getOwner()
  {
    return _processInstanceVO.getOwner();
  }
  public void setOwner(String owner)
  {
    _processInstanceVO.setOwner(owner);
  }
  
  public Long getOwnerId()
  {
    return _processInstanceVO.getOwnerId();
  }
  public void setOwnerId(Long ownerId)
  {
    _processInstanceVO.setOwnerId(ownerId);
  }
  
  public String getSecondaryOwner()
  {
    return _processInstanceVO.getSecondaryOwner();
  }
  public void setSecondaryOwner(String secondaryOwner)
  {
    _processInstanceVO.setSecondaryOwner(secondaryOwner);
  }
  
  public Long getSecondaryOwnerId()
  {
    return _processInstanceVO.getSecondaryOwnerId();
  }
  public void setSecondaryOwnerId(Long secondaryOwnerId)
  {
    _processInstanceVO.setSecondaryOwnerId(secondaryOwnerId);
  }

  public String getWrappedId()
  {
    if (_processInstanceVO == null || _processInstanceVO.getId() == null)
      return null;
      
    return _processInstanceVO.getId().toString();
  }

  public Object getWrappedInstance()
  {
    return _processInstanceVO;
  }
  
  public boolean isLaunchEnabled()
  {
    return _processInstanceVO.getProcessId() != null;
  }

  public boolean checkArrayAddButtonSubmit()
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
    {
      String paramName = iter.next();
      if (paramName.endsWith("arrayDataAddButton") && externalContext.getRequestParameterMap().get(paramName) != null)
        return true;
    }
    return false;
  }  
}
