/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.variable;

import java.util.List;

import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.attribute.AttributeVO;

/**
 * This class represents variables associated with manual tasks.
 * The engine uses VariableInstanceInfo instead.
 * The class extends VariableInstanceInfo to include task manager
 * specific fields such as whether the variable is required and editable
 */


public class VariableInstanceVO extends VariableInstanceInfo implements Comparable<VariableInstanceInfo> {


    public static final long serialVersionUID = 1L;

    private boolean isRequired;
    private boolean isEditable;
    private String variableReferredName;
    private Long processInstanceId;
    private List<AttributeVO> attributes;
    private String documentContent;		// for supporting document vars in auto-form tasks
    private Object documentObject;		// for supporting document vars in auto-form tasks

    /**
     * This class is used by the task manager to represent a variable
     * in classic/autoform tasks. The engine does not use this class
     * and uses VariableInstanceInfo instead. The class contains additional
     * info for displaying purposes.
     */

    public VariableInstanceVO(){
    }

    public VariableInstanceVO(VariableVO varVO) {
        setName(varVO.getVariableName());
        setVariableId(varVO.getVariableId());
        setType(varVO.getVariableType());
        setRequired(varVO.getDisplayMode().equals(VariableVO.DATA_REQUIRED));
        setEditable(varVO.getDisplayMode().equals(VariableVO.DATA_REQUIRED)
        		|| varVO.getDisplayMode().equals(VariableVO.DATA_OPTIONAL));
        setVariableReferredName(varVO.getVariableReferredAs());
    }

	// PUBLIC AND PROTECTED METHODS -----------------------------------
        /**
       * @return Returns the isRequired.
       */
      public boolean isRequired() {
        return isRequired;
      }

      /**
       * @param isRequired The isRequired to set.
       */
      public void setRequired(boolean isRequired) {
        this.isRequired = isRequired;
      }

         /**
       * @return Returns the isEditable.
       */
      public boolean isEditable() {
        return isEditable;
      }

      /**
       * @param isRequired The isRequired to set.
       */
      public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
      }

      /**
       * @return Returns the processInstanceId.
       */
      public Long getProcessInstanceId() {
        return this.processInstanceId;
      }

      /**
       * @param variableInstanceOwnerId The processInstanceId to set.
       */
      public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
      }

       /**
       * @return Returns the variableReferredName.
       */
      public String getVariableReferredName() {
        return variableReferredName;
      }

      /**
       * @param variableName The variableReferredName to set.
       */
      public void setVariableReferredName(String variableReferredName) {
        this.variableReferredName = variableReferredName;
      }


    public String toString()
    {
      StringBuffer buffer = new StringBuffer();
      buffer.append("VariableInstanceVO[");
      buffer.append("isEditable = ").append(isEditable);
      buffer.append(" isRequired = ").append(isRequired);
      buffer.append(" processInstanceId = ").append(processInstanceId);
      buffer.append(" variableData = ").append(getStringValue());
      buffer.append(" variableId = ").append(getVariableId());
      buffer.append(" variableInstanceId = ").append(getInstanceId());
      buffer.append(" variableName = ").append(getName());
      buffer.append(" variableType = ").append(getType());
      buffer.append(" isSelect = ").append(isSelect());
      buffer.append(" selectValues = ").append(getSelectValues());
      buffer.append("]");
      return buffer.toString();
    }

    public boolean isSelect()
    {
      return getAttribute("SELECT_VALUES") != null;
    }

    public String[] getSelectValues()
    {
      String selectValues = getAttribute("SELECT_VALUES");
      if (selectValues == null)
        return null;

      return selectValues.split(",");
    }

    public boolean allowArrayResize()
    {
      String attrValue = getAttribute("ALLOW_ARRAY_RESIZE");
      if (attrValue != null && attrValue.equalsIgnoreCase("false"))
        return false;

      return true;
    }

    public List<AttributeVO> getAttributes()
    {
      return attributes;
    }

    public void setAttributes(List<AttributeVO> attributes)
    {
      this.attributes = attributes;
    }

    public String getAttribute(String name)
    {
      if (name == null || attributes == null)
        return null;

      for (int i = 0; i < attributes.size(); i++)
      {
        if (name.equals(attributes.get(i).getAttributeName()))
          return attributes.get(i).getAttributeValue();
      }

      return null;
    }

    public int compareTo(VariableInstanceInfo other)
    {
      String thisLabel = StringHelper.isEmpty(getVariableReferredName()) ? getName() : getVariableReferredName();
      String otherLabel = other.getName();
      if (other instanceof VariableInstanceVO)
      {
        VariableInstanceVO otherVO = (VariableInstanceVO) other;
        otherLabel = StringHelper.isEmpty(otherVO.getVariableReferredName()) ? otherVO.getName() : otherVO.getVariableReferredName();
      }

      return thisLabel.compareTo(otherLabel);
    }

    /**
     * This is for backward compatibility. Use getStringValue() or getData() instead.
     * @return
     */
    @Deprecated
    public Object getVariableData() {
    	return getData();
    }

    /**
     * This is for backward compatibility. Use getName() instead
     * @return
     */
    @Deprecated
    public String getVariableName() {
    	return getName();
    }

	public void setRealStringValue(String documentContent) {
		super.setData(new DocumentReference(0L));
		if (documentContent==null) documentContent = "";
		this.documentContent = documentContent;
		documentObject = null;
	}

	public Object getRealData() {
		if (documentContent==null) return getData();
		if (documentObject==null && documentContent.length()>0) {
			documentObject = VariableTranslator.realToObject(getType(), documentContent);
		}
		return documentObject;
	}

}
