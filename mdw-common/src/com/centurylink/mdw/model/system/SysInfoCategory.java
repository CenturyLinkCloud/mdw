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

@ApiModel(value="SysInfoCategory", description="System Information Category")
public class SysInfoCategory implements Jsonable {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private List<SysInfo> sysInfos;
    public List<SysInfo> getSysInfos() { return sysInfos; }
    public void setSysInfos(List<SysInfo> sysInfos) { this.sysInfos = sysInfos; }

    public SysInfoCategory(String name, List<SysInfo> sysInfos) {
        this.name = name;
        this.sysInfos = sysInfos;
    }

    public SysInfoCategory(JSONObject json) throws JSONException {
        this.name = json.getString("name");
        if (json.has("sysInfos")) {
            this.sysInfos = new ArrayList<SysInfo>();
            JSONArray sysInfoArr = json.getJSONArray("sysInfos");
            for (int i = 0; i < sysInfoArr.length(); i++) {
                this.sysInfos.add(new SysInfo(sysInfoArr.getJSONObject(i)));
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("name", this.name);
        if (this.sysInfos != null) {
            JSONArray sysInfoArr = new JSONArray();
            for (SysInfo sysInfo : this.sysInfos) {
                sysInfoArr.put(sysInfo.getJson());
            }
            json.put("sysInfos", sysInfoArr);
        }
        return json;
    }

    public String getJsonName() {
        return "sysInfoCategory";
    }
}
