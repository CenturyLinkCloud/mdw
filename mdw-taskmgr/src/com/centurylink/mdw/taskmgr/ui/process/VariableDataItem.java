/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.process;

import java.io.Serializable;

import com.centurylink.mdw.common.constant.VariableConstants;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.taskmgr.ui.detail.DataItem;
import com.centurylink.mdw.taskmgr.ui.detail.InstanceDataItem;

public class VariableDataItem extends InstanceDataItem implements Comparable<VariableDataItem>, Serializable
{
  private static final long serialVersionUID = 1L;

  private FullProcessInstance _processInstance;
  public FullProcessInstance getProcessInstance() { return _processInstance; }
  public void setProcessInstance(FullProcessInstance fpi) { _processInstance = fpi; }

  private VariableVO _variableVO;
  public VariableVO getVariableVO() { return _variableVO; }
  public void setVariableVO(VariableVO varVO) { _variableVO = varVO; }

  private int _sequenceId;
  public int getSequenceId() { return _sequenceId; }
  public void setSequenceId(int sequenceId) { _sequenceId = sequenceId; }

  private Object _variableData;
  public Object getVariableData() { return _variableData; }
  public void setVariableData(Object value)
  {
    if (value == null)
      _variableData = null;
    else if (value instanceof String)
      _variableData = ((String)value).trim();
    else
      _variableData = value;
  }

  public String getVariableType()
  {
    return _variableVO.getVariableType();
  }

  public VariableDataItem(FullProcessInstance fpi, VariableVO varVO)
  {
    _processInstance = fpi;
    _variableVO = varVO;
    if (isArray() || isMap())
      initializeArrayDataElement();
  }

  @Override
  public String getDataType()
  {
    return getVariableType();
  }

  public boolean isSupportedVariableType()
  {
    return getVariableType().equals("java.lang.String")
        || getVariableType().equals("java.lang.Long")
        || getVariableType().equals("java.lang.Integer")
        || getVariableType().equals("java.lang.Boolean")
        || getVariableType().equals("java.util.Date")
        || getVariableType().equals("java.lang.String[]")
        || getVariableType().equals("java.lang.Integer[]")
        || getVariableType().equals("java.lang.Long[]")
        || getVariableType().equals("java.util.Map")
        || getVariableType().equals("java.net.URI")
        || getVariableType().equals("org.w3c.dom.Document")
        || getVariableType().equals("org.apache.xmlbeans.XmlObject")
        || getVariableType().equals("com.qwest.mbeng.MbengDocument")
        || getVariableType().equals("java.lang.Object")
        || getVariableType().equals("org.json.JSONObject")
        || getVariableType().equals("groovy.util.Node")
        || getVariableType().equals("com.centurylink.mdw.xml.XmlBeanWrapper")
        || getVariableType().equals("com.centurylink.mdw.model.StringDocument")
        || getVariableType().equals("javax.xml.bind.JAXBElement");
  }

  public boolean isInputParam()
  {
    return getCategory().equals(VariableConstants.VARIABLE_CATEGORY_INPUT)
      || getCategory().equals(VariableConstants.VARIABLE_CATEGORY_IN_OUT);
  }

  public Integer getCategory()
  {
    return getVariableVO().getVariableCategory();
  }

  public String getLabel()
  {
    if (!StringHelper.isEmpty(getVariableVO().getVariableReferredAs()))
      return getVariableVO().getVariableReferredAs();
    else
      return getVariableVO().getVariableName();
  }

  @Override
  public String getName()
  {
    return getVariableVO().getVariableName();
  }

  @Override
  public boolean isRendered()
  {
    return true;
  }

  private boolean editable = true;
  public boolean isValueEditable()
  {
    return editable && !isJavaObject();
  }
  public void setValueEditable(boolean editable)
  {
    this.editable = editable;
  }

  private boolean required;
  public boolean isValueRequired()
  {
    return required;
  }
  public void setValueRequired(boolean required)
  {
    this.required = required;
  }

  public int compareTo(VariableDataItem other)
  {
    // display according to display sequence
    Integer thisSeq = getVariableVO().getDisplaySequence() == null ? 0 : getVariableVO().getDisplaySequence();
    Integer otherSeq = other.getVariableVO().getDisplaySequence() == null ? 0 : other.getVariableVO().getDisplaySequence();
    int seqCompare = thisSeq.compareTo(otherSeq);
    if (seqCompare != 0)
      return seqCompare;
    // tie is resolved alphabetically
    return getLabel().compareTo(((VariableDataItem)other).getLabel());
  }

  public boolean isAllowArrayResize()
  {
    return true;
  }

  public String getDataKeyName()
  {
    if (!StringHelper.isEmpty(_variableVO.getVariableReferredAs()))
    {
      return _variableVO.getVariableReferredAs();
    }
    return _variableVO.getVariableName();
  }

  public DataItem findDataItem(int sequenceId)
  {
    return _processInstance.getInputParameters().get(sequenceId);
  }

  public Long getId()
  {
    return _variableVO.getVariableId();
  }

}
