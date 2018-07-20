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
package com.centurylink.mdw.model.system;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

import io.swagger.annotations.ApiModel;

@ApiModel(value="SysInfo", description="System Information (one or many values)")
public class SysInfo implements Jsonable {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String value;
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    private List<String> values;
    public List<String> getValues() { return values; }
    public void setValues(List<String> values) { this.values = values; }

    private List<SysInfo> sysInfos;
    public List<SysInfo> getSysInfos() { return sysInfos; }
    public void setSysInfos(List<SysInfo> sysInfos) { this.sysInfos = sysInfos; }
    public void addSysInfo(SysInfo sysInfo) {
        if (this.sysInfos == null)
            this.sysInfos = new ArrayList<SysInfo>();
        this.sysInfos.add(sysInfo);
    }

    public SysInfo(String name) {
        this.name = name;
    }

    public SysInfo(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public SysInfo(JSONObject json) throws JSONException {
        this.name = json.getString("name");
        if (json.has("values")) {
            JSONArray valuesArr = json.getJSONArray("values");
            this.values = new ArrayList<String>();
            for (int i = 0; i < valuesArr.length(); i++) {
                this.values.add(valuesArr.getString(i));
            }
        }
        else if (json.has("value")) {
            this.value = json.getString("value");
        }
        if (json.has("sysInfos")) {
            this.sysInfos = new ArrayList<SysInfo>();
            JSONArray subInfosArr = json.getJSONArray("sysInfos");
            for (int i = 0; i < subInfosArr.length(); i++) {
                JSONObject subItemJson = subInfosArr.getJSONObject(i);
                SysInfo subItem = new SysInfo(subItemJson);
                this.sysInfos.add(subItem);
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("name", this.name);
        if (this.values != null) {
           JSONArray valuesArr = new JSONArray();
           for (String value : this.values) {
               valuesArr.put(value);
           }
           json.put("values", values);
        }
        else if (this.value != null) {
            json.put("value", this.value);
        }
        if (this.sysInfos != null) {
            JSONArray subItemsArr = new JSONArray();
            for (SysInfo subItem : this.sysInfos) {
                subItemsArr.put(subItem.getJson());
            }
            json.put("sysInfos", subItemsArr);
        }
        return json;
    }

    public String getJsonName() {
        return "sysInfoItem";
    }

}
