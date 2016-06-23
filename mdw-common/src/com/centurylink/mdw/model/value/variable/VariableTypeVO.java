/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.variable;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class VariableTypeVO implements Serializable, Jsonable {

    private Long variableTypeId;
    private String variableType;
    private String translatorClass;

    public VariableTypeVO(Long variableTypeId, String variableType, String translatorClass){
        this.variableTypeId = variableTypeId;
        this.variableType = variableType;
        this.translatorClass = translatorClass;
    }

    public VariableTypeVO(JSONObject json) throws JSONException {
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
