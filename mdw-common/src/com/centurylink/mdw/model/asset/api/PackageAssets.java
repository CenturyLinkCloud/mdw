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