/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.variable;

import java.io.Serializable;

import org.apache.xmlbeans.XmlObject;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.HtmlDocument;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.Value.Display;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengDocumentClass;
import com.qwest.mbeng.MbengTableArray;
import com.qwest.mbeng.MbengVariable;

public class VariableVO implements Serializable, Comparable<VariableVO>, MbengVariable, Jsonable {

    public static final String[] VariableCategories =
        {"Local", "Input", "Output", "Input/Output", "Static"};
    public static final String[] DisplayModes =
    	{"Required", "Optional", "Read Only", "Hidden", "Excluded"};

    public static final int CAT_LOCAL = 0;
    public static final int CAT_INPUT = 1;
    public static final int CAT_OUTPUT = 2;
    public static final int CAT_INOUT = 3;
    public static final int CAT_STATIC = 4;

    // built-in variable names
    public static final String MASTER_REQUEST_ID = "MasterRequestID";
    public static final String PROCESS_INSTANCE_ID = "ProcessInstanceID";
    public static final String ACTIVITY_INSTANCE_ID = "ActivityInstanceID";

    // the data sources is only for backward compatibility
    public static final String DATA_SOURCE_READONLY = "workflow";
    public static final String DATA_SOURCE_OTHERS = "task_user";

    public static final Integer DATA_REQUIRED = new Integer(0);
    public static final Integer DATA_OPTIONAL =  new Integer(1);
    public static final Integer DATA_READONLY =  new Integer(2);
    public static final Integer DATA_HIDDEN =  new Integer(3);
    public static final Integer DATA_EXCLUDED =  new Integer(4);

    private Long variableId;
    private String variableType;
    private String variableName;
    private String variableReferredAs;
    private Boolean isChecked;
//    private String valueLocator;
    private Integer displaySequence;
    private Integer optionality;
    private String description;

    public VariableVO() {
    }

    public VariableVO(String name, String type) {
        this.variableId = 0L;
        this.variableName = name;
        this.variableType = type;
    }

    public VariableVO(Long pVariableId, String pVariableName, String pVariableType,
            String pVarRefAs, Integer pDisplayMode, Integer pSeq) {
        this.variableId = pVariableId;
        this.variableType = pVariableType;
        this.variableName = pVariableName;
        this.isChecked = new Boolean(false);
        this.variableReferredAs = pVarRefAs;
        this.optionality = pDisplayMode;
        this.displaySequence = pSeq;
    }

    /**
     * @return returns the VariableId
     */
    public Long getVariableId() {
        return this.variableId;
    }

    /**
     * @param pVariableId The VariableId to set.
     */
    public void setVariableId(Long pVariableId) {
        this.variableId = pVariableId;
    }

    public String getVariableType() {
    	return variableType;
    }

    public void setVariableType(String vartype) {
        this.variableType = vartype;
    }

    /**
     * @return Returns the VariableName
     */

    public String getVariableName() {
        return this.variableName;
    }

    /**
     *  @param pVariableName The VariableName to set.
     */

    public void setVariableName(String pVariableName) {
        this.variableName = pVariableName;
    }

    /**
     * @return Returns the variableReferredAs
     */

    public String getVariableReferredAs() {
        return this.variableReferredAs;
    }

    /**
     *  @param pVariableName The variableReferredAs to set.
     */

    public void setVariableReferredAs(String pVariableName) {
        this.variableReferredAs = pVariableName;
    }

    /**
     * @return  Returns  IsChecked
     */
    public Boolean getIsChecked() {
        return this.isChecked;
    }

    /**
     *  @param pisChecked The IsChecked to set.
     */
    public void setIsChecked(Boolean pIsChecked) {
        this.isChecked = pIsChecked;
    }

//    /**
//     * returns the value Locator
//     */
//    public String getValueLocator() {
//        return this.valueLocator;
//    }
//
//    /**
//     * Sets the value fetcher
//     * @param pValueFetcher
//     */
//    public void setValueLocator(String pValueLocator) {
//        this.valueLocator = pValueLocator;
//    }

    /**
     *
     * @return
     * @author
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("VariableVO[");
        buffer.append("isChecked = ").append(isChecked);
        buffer.append(" variableId = ").append(variableId);
        buffer.append(" variableName = ").append(variableName);
        buffer.append(" variableType = ").append(variableType);
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * @return
     */
    public Integer getDisplaySequence() {
        return displaySequence;
    }

    /**
     * @param displaySequence
     */
    public void setDisplaySequence(Integer displaySequence) {
        this.displaySequence = displaySequence;
    }

    /**
     * Important note:  for backward compatibility and other historical reasons,
     * variables stored in relational form (5.0 and prior) uses
     * two fields in VARIABLE_MAPPING table to control display mode:
	 * 	- Read Only:  VARIABLE_DATA_SOURCE is 'workflow'
	 *  - Optional: VARIABLE_DATA_SOURCE is 'task_user', and VARIABLE_DATA_OPT_IND is 1
	 *	- Required: VARIABLE_DATA_SOURCE is 'task_user', and VARIABLE_DATA_OPT_IND is 0
	 *  - Not Displayed: no variable mapping exist
	 * Additionally, display mode is only used for variables mapped to tasks;
	 * for variables mapped to processes, VARIABLE_DATA_OPT_IND is used to store
	 * variable category.
	 *
     * @return  Returns display mode code
     */
    public Integer getDisplayMode() {
        return this.optionality;
    }

    public boolean isRequired() {
        return getDisplayMode().equals(VariableVO.DATA_REQUIRED);
    }

    /**
     * See important note in getDisplayMode()
     * @param displayMode
     */
    public void setDisplayMode(Integer displayMode) {
        this.optionality = displayMode;
    }

    /**
     * See important note in getDisplayMode()
     * @return variable category, a.k.a variable mode (Input/Output/etc)
     */
    public Integer getVariableCategory() {
        return optionality;
    }

    /**
     * See important note in getDisplayMode()
     * @param variableCategory
     */
    public void setVariableCategory(Integer variableCategory) {
        this.optionality = variableCategory;
    }

    public int compareTo(VariableVO other) {
        if (other == null)
          return 1;

        return this.getVariableName().compareTo(other.getVariableName());
    }

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * This method is needed by MbengVariable
	 */
	@Override
	public String getName() {
		return this.variableName;
	}

	/**
	 * This method is needed by MbengVariable
	 */
	@Override
	public int getKind() {
		String type = variableType;
		if (type.equals(XmlObject.class.getName())) return MbengVariable.KIND_DOCUMENT;
		if (type.equals(Document.class.getName())) return MbengVariable.KIND_DOCUMENT;
		if (type.equals(FormDataDocument.class.getName())) return MbengVariable.KIND_DOCUMENT;
		if (type.equals(JSONObject.class.getName())) return MbengVariable.KIND_DOCUMENT;
		if (type.equals(MbengDocument.class.getName())) return MbengVariable.KIND_DOCUMENT;
		if (type.equals(StringDocument.class.getName())) return MbengVariable.KIND_DOCUMENT;
		if (type.equals(MbengTableArray.class.getName())) return MbengVariable.KIND_TABLE;
		if (type.equals(HtmlDocument.class.getName())) return MbengVariable.KIND_DOCUMENT;
		return MbengVariable.KIND_STRING;
	}

	/**
	 * This method is needed by MbengVariable
	 */
	@Override
	public Object newInstance() throws InstantiationException {
		String type = variableType;
		if (type.equals(XmlObject.class.getName())) {
			return new DomDocument();
		}
		if (type.equals(Document.class.getName())) {
			return new DomDocument();
		}
		if (type.equals(FormDataDocument.class.getName())) {
			return new FormDataDocument();
		}
		if (type.equals(MbengDocument.class.getName())) {
			return new MbengDocumentClass();
		}
		if (type.equals(JSONObject.class.getName())) {
			return new JSONObject();	// should return a MbengDocument wrapper
		}
		if (type.equals(MbengTableArray.class.getName())) {
			return new MbengTableArray();
		}
		return null;
	}

	public boolean isDocument() {
	    return VariableTranslator.isDocumentReferenceVariable(getVariableType());
	}

	public boolean isXmlDocument() {
	    return VariableTranslator.isXmlDocumentTranslator(getVariableType());
	}

    public boolean isInput() {
        return getVariableCategory() == CAT_INPUT || getVariableCategory() == CAT_INOUT;
    }

    public boolean isOutput() {
        return getVariableCategory() == CAT_OUTPUT || getVariableCategory() == CAT_INOUT;
    }

    public boolean isString() {
        return String.class.getName().equals(getVariableType()) || StringDocument.class.getName().equals(getVariableType());
    }

    public boolean isJavaObject() {
        return Object.class.getName().equals(getVariableType());
    }

    public String getCategory() {
        int cat = getVariableCategory() == null ? 0 : getVariableCategory();
        if (cat == CAT_INPUT)
            return "INPUT";
        else if (cat == CAT_OUTPUT)
            return "OUTPUT";
        else if (cat == CAT_INOUT)
            return "INOUT";
        else if (cat == CAT_STATIC)
            return "STATIC";
        else
            return "LOCAL";
    }

    // for json (instead of old optionality )
    private Display display;
    public Display getDisplay() { return display; }
    public void setDisplay(Display display) { this.display = display; }

    public int getCategoryCode(String category) {
        if ("INPUT".equals(category))
            return CAT_INPUT;
        else if ("OUTPUT".equals(category))
            return CAT_OUTPUT;
        else if ("INOUT".equals(category))
            return CAT_INOUT;
        else if ("STATIC".equals(category))
            return CAT_STATIC;
        else
            return CAT_LOCAL;
    }

    public VariableVO(JSONObject json) throws JSONException {
        if (json.has("name"))
            this.variableName = json.getString("name");
        this.variableType = json.getString("type");
        if (json.has("category"))
            this.setVariableCategory(getCategoryCode(json.getString("category")));
        if (json.has("label"))
            this.variableReferredAs = json.getString("label");
        if (json.has("sequence"))
            this.displaySequence = json.getInt("sequence");
        if (json.has("display"))
            this.display = Display.valueOf(json.getString("display"));
        this.setVariableId(0L);
    }

    /**
     * Serialized as an object, so name is not included.
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", variableType);
        json.put("category", getCategory());
        if (variableReferredAs != null && !variableReferredAs.isEmpty())
            json.put("label", variableReferredAs);
        if (displaySequence != null && displaySequence > 0)
            json.put("sequence", displaySequence);
        if (display != null)
            json.put("display", display.toString());
        return json;
    }

    public String getJsonName() {
        return getName();
    }
}
