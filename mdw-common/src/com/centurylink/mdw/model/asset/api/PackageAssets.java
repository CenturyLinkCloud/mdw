package com.centurylink.mdw.model.asset.api;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class PackageAssets implements Jsonable, Comparable<PackageAssets> {

    private final String packageName;
    public String getPackageName() { return packageName; }

    private List<AssetInfo> assets;
    public List<AssetInfo> getAssets() { return assets; }
    public void setAssets(List<AssetInfo> assets) { this.assets = assets; }

    public PackageAssets(String packageName) {
        this.packageName = packageName;
    }

    public PackageAssets(String packageName, List<AssetInfo> assets) {
        this.packageName = packageName;
        this.assets = assets;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject pkg = create();
        pkg.put("name", packageName);
        JSONArray assetArray = new JSONArray();
        if (assets != null) {
            for (AssetInfo asset : assets)
                assetArray.put(asset.getJson());
        }
        pkg.put("assets", assetArray);
        return pkg;
    }

    public String getJsonName() {
        return "Package";
    }

    public void sort() {
        Collections.sort(assets);
    }

    public int compareTo(PackageAssets other) {
        return this.packageName.compareTo(other.packageName);
    }
}