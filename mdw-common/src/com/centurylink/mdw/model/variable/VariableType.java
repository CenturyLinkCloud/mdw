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
