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
package com.centurylink.mdw.adapter;

import com.centurylink.mdw.model.attribute.Attribute;

public class SimulationResponse {

    private Boolean selected;
    private String returnCode;
    private Integer chance;
    private String response;
    private Attribute attr;
    
    public SimulationResponse(Attribute attr) {
        this(attr.getAttributeValue());
        this.attr = attr;
    }

    public SimulationResponse(String value) {
        attr = null;
        int firstDelim = value.indexOf(',');
        int secondDelim = (firstDelim>=0)?value.indexOf(',', firstDelim+1):-1;
        if (secondDelim>0) {
            returnCode = value.substring(0,firstDelim);
            chance = new Integer(value.substring(firstDelim+1,secondDelim));
            response = value.substring(secondDelim+1);
        } else if (firstDelim>=0) {
            returnCode = value.substring(0,firstDelim);
            chance = new Integer(value.substring(firstDelim+1));
            response = "";
        } else {
            returnCode = value;
            chance = 0;
            response = "";
        }
        selected = false;
    }
    
    public SimulationResponse(String returnCode, int chance, String response) {
        this.returnCode = returnCode;
        this.chance = chance;
        this.response = response;
        selected = false;
        attr = null;
    }
        
    private void updateAttr() {
        attr.setAttributeValue(
                (returnCode!=null?returnCode:"")
                +','
                +(chance!=null?Integer.toString(chance):"0")
                +','
                +(response!=null?response:""));
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public String getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
        this.updateAttr();
    }

    public Integer getChance() {
        return chance;
    }

    public void setChance(Integer chance) {
        this.chance = chance;
        this.updateAttr();
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
        this.updateAttr();
    }

    public Attribute getAttribute() {
        return attr;
    }

    public void setAttribute(Attribute attr) {
        this.attr = attr;
        this.updateAttr();
    }
}
