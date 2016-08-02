/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;

/**
 * Wraps a VariableVO and also associates a value.  The value
 * is the string representation of the variable as returned by the
 * appropriate translator.  For documents the value is the content.
 */
public class VariableValue implements Comparable<VariableValue>
{
  private VariableVO variableVO;
  public VariableVO getVariableVO() { return variableVO; }
  public void setVariableVO(VariableVO variableVO) { this.variableVO = variableVO; }

  private VariableTypeVO type;
  public VariableTypeVO getType() { return type; }
  public void setType(VariableTypeVO type) { this.type = type; }

  private String value;
  public String getValue() { return value; }
  public void setValue(String value) { this.value = value; }

  private boolean readOnly;
  public boolean isReadOnly() { return readOnly; }
  public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }

  public VariableValue(VariableVO variableVO)
  {
    this.variableVO = variableVO;
  }

  public VariableValue(VariableVO variableVO, VariableTypeVO type, String value)
  {
    this.variableVO = variableVO;
    this.type = type;
    this.variableVO.setVariableType(type == null ? null : type.getVariableType());
    this.value = value;
  }

  public VariableValue(VariableInstanceInfo variableInstanceInfo, VariableTypeVO type, String value)
  {
    variableVO = new VariableVO();
    this.type = type;
    variableVO.setVariableType(type.getVariableType());
    variableVO.setVariableName(variableInstanceInfo.getName());
    this.value = value;
  }

  public String getName()
  {
    return variableVO.getVariableName();
  }

  public String toString()
  {
    if (value == null)
      return "";
    return value;
  }

  public int compareTo(VariableValue other)
  {
    return this.getVariableVO().getVariableName().compareTo(other.getVariableVO().getVariableName());
  }

  // doc translator for unknown document types (defaults to string value)
  public static class StringDocTranslator extends DocumentReferenceTranslator
  {
    public Object realToObject(String string) throws TranslationException
    {
      return string;
    }
    public String realToString(Object object) throws TranslationException
    {
      return object.toString();
    }
  }

  /**
   * Represents the variable type for a string doc
   */
  public static class StringDocument
  {

  }

}