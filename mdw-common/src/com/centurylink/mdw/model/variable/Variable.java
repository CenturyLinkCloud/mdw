/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.model.variable;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.Value.Display;
import com.centurylink.mdw.translator.VariableTranslator;

public class Variable implements Serializable, Comparable<Variable>, Jsonable {

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

    public Variable() {
    }

    public Variable(String name, String type) {
        this.variableId = 0L;
        this.variableName = name;
        this.variableType = type;
    }

    public Variable(Long pVariableId, String pVariableName, String pVariableType,
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
     *     - Read Only:  VARIABLE_DATA_SOURCE is 'workflow'
     *  - Optional: VARIABLE_DATA_SOURCE is 'task_user', and VARIABLE_DATA_OPT_IND is 1
     *    - Required: VARIABLE_DATA_SOURCE is 'task_user', and VARIABLE_DATA_OPT_IND is 0
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
        return getDisplayMode().equals(Variable.DATA_REQUIRED);
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

    public int compareTo(Variable other) {
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

    public String getName() {
        return this.variableName;
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

    public Value toValue() {
        Value value = new Value(getName());
        value.setType(getVariableType());
        if (getDisplayMode() != null)
            value.setDisplay(Value.getDisplay(getDisplayMode()));
        if (getDisplaySequence() != null)
            value.setSequence(getDisplaySequence());
        if (getVariableReferredAs() != null)
            value.setLabel(getVariableReferredAs());
        return value;
    }

    public Variable(JSONObject json) throws JSONException {
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
        JSONObject json = create();
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

    @Deprecated
    public boolean isDocument() {
        return VariableTranslator.isDocumentReferenceVariable(getVariableType());
    }

    @Deprecated
    public boolean isXmlDocument() {
        return VariableTranslator.isXmlDocumentTranslator(getVariableType());
    }

}
