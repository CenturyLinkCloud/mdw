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

    private String description;
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    private List<SysInfo> sysInfos;
    public List<SysInfo> getSysInfos() { return sysInfos; }
    public void setSysInfos(List<SysInfo> sysInfos) { this.sysInfos = sysInfos; }

    public SysInfoCategory(String name, List<SysInfo> sysInfos) {
        this.name = name;
        this.sysInfos = sysInfos;
    }

    public SysInfoCategory(JSONObject json) throws JSONException {
        this.name = json.getString("name");
        if (json.has("description"))
            this.description = json.getString("description");
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
        if (this.description != null)
            json.put("description", this.description);
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
