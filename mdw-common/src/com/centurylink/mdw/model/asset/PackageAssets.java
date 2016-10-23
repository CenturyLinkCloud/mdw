/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.asset;

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.dataaccess.file.PackageDir;

public class PackageAssets implements Jsonable {

    private PackageDir packageDir;
    public PackageDir getPackageDir() { return packageDir; }

    private List<AssetInfo> assets;
    public List<AssetInfo> getAssets() { return assets; }
    public void setAssets(List<AssetInfo> assets) { this.assets = assets; }

    public PackageAssets(PackageDir pkgDir) {
        this.packageDir = pkgDir;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject pkg = new JSONObject();
        pkg.put("id", packageDir.getId());
        pkg.put("name", packageDir.getPackageName());
        pkg.put("version", packageDir.getPackageVersion());
        if (packageDir.getVcsDiffType() != null)
            pkg.put("vcsDiff", packageDir.getVcsDiffType().toString());
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
}