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
package com.centurylink.mdw.model.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.dataaccess.file.AssetFile;
import com.centurylink.mdw.dataaccess.file.PackageDir;

public class PackageAssets implements Jsonable, Comparable<PackageAssets> {

    private PackageDir packageDir;
    public PackageDir getPackageDir() { return packageDir; }

    private List<AssetInfo> assets;
    public List<AssetInfo> getAssets() { return assets; }
    public void setAssets(List<AssetInfo> assets) { this.assets = assets; }

    public PackageAssets(PackageDir pkgDir) {
        this.packageDir = pkgDir;
    }

    public PackageAssets(PackageDir pkgDir, JSONObject json) throws JSONException {
        this.packageDir = pkgDir;
        if (json.has("assets")) {
            this.assets = new ArrayList<>();
            JSONArray assetArr = json.getJSONArray("assets");
            for (int i = 0; i < assetArr.length(); i++) {
                JSONObject assetObj = assetArr.getJSONObject(i);
                AssetRevision rev = null;
                if (assetObj.has("version"))
                    rev = new AssetRevision(assetObj.getString("version"));
                AssetFile assetFile = new AssetFile(pkgDir, assetObj.getString("name"), rev);
                this.assets.add(new AssetInfo(assetFile));
            }
        }
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

    public int compareTo(PackageAssets other) {
        return this.packageDir.getPackageName().compareToIgnoreCase(other.packageDir.getPackageName());
    }
}