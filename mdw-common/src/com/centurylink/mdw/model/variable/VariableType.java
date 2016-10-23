/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.variable;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class VariableType implements Serializable, Jsonable {

    private Long variableTypeId;
    private String variableType;
    private String translatorClass;

    public VariableType(Long variableTypeId, String variableType, String translatorClass){
        this.variableTypeId = variableTypeId;
        this.variableType = variableType;
        this.translatorClass = translatorClass;
    }

    public VariableType(JSONObject json) throws JSONException {
        this.variableTypeId = json.getLong("id");
        this.variableType = json.getString("name");
        this.translatorClass = json.getString("translator");
    }

    public Long getVariableTypeId(){
        return this.variableTypeId;
    }

    public String getVariableType(){
        return this.variableType;
    }
    public void setVariableType(String type) {
        this.variableType = type;
    }

    public String getTranslatorClass(){
        return this.translatorClass;
    }
    public void setTranslatorClass(String className){
       this.translatorClass = className;
    }

    public boolean isJavaObjectType() {
        return Object.class.getName().equals(variableType)
                || (translatorClass != null && translatorClass.endsWith("JavaObjectTranslator"));
    }

    // for Object types, true if SelfSerializable
    private boolean updateable = true;
    public boolean isUpdateable() { return updateable; }
    public void setUpdateable(boolean updateable) { this.updateable = updateable; }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("VariableTypeVO[");
        buffer.append("translatorClass = ").append(translatorClass);
        buffer.append(" variableType = ").append(variableType);
        buffer.append(" variableTypeId = ").append(variableTypeId);
        buffer.append("]");
        return buffer.toString();
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((variableType == null) ? 0 : variableType.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VariableType other = (VariableType) obj;
        if (variableType == null) {
            if (other.variableType != null)
                return false;
        }
        else if (!variableType.equals(other.variableType))
            return false;
        return true;
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", variableType);
        json.put("id", variableTypeId);
        json.put("translator", translatorClass);
        return json;
    }

    @Override
    public String getJsonName() {
        return "VariableType";
    }
}
