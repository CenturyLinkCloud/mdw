package com.centurylink.mdw.model.asset.api;

import com.centurylink.mdw.git.GitDiffs;
import com.centurylink.mdw.model.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

public class PackageInfo implements Jsonable, Comparable<PackageInfo> {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String version;
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    private GitDiffs.DiffType vcsDiffType;
    public GitDiffs.DiffType getVcsDiffType() { return vcsDiffType; }
    public void setVcsDiffType(GitDiffs.DiffType diffType) { this.vcsDiffType = diffType; }

    public PackageInfo(String name) {
        this.name = name;
    }

    public PackageInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("name", name);
        json.put("version", version);
        if (vcsDiffType != null)
            json.put("vcsDiff", vcsDiffType);

        return json;
    }

    @Override
    public int compareTo(PackageInfo other) {
        return this.name.compareToIgnoreCase(other.name);
    }
}
