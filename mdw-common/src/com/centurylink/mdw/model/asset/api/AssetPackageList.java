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
import com.centurylink.mdw.util.DateHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * List of packages that reflect the result of filtering (eg: by type of asset).
 * Assets are not populated with VCS info.
 */
public class AssetPackageList implements Jsonable {

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    private List<PackageAssets> packageAssetList;
    public List<PackageAssets> getPackageAssetList() { return packageAssetList; }
    public void setPackageAssetList(List<PackageAssets> packageAssetList) { this.packageAssetList = packageAssetList; }

    public PackageAssets getPackageAssets(String packageName) {
        if (packageAssetList != null) {
            for (PackageAssets pkgAssets : packageAssetList) {
                if (pkgAssets.getPackageName().equals(packageName))
                    return pkgAssets;
            }
        }
        return null;
    }

    public AssetPackageList(List<PackageAssets> packageAssetList) {
        this.packageAssetList = packageAssetList;
        packageAssetList.forEach(pal -> pal.getAssets().forEach(a -> count++));
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("retrieveDate", DateHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        JSONArray array = new JSONArray();
        if (packageAssetList != null) {
            for (PackageAssets pkgAssets : packageAssetList)
                array.put(pkgAssets.getJson());
        }
        json.put("packages", array);
        return json;
    }

    public String getJsonName() {
        return "AssetPackageList";
    }

    public void sort() {
        Collections.sort(getPackageAssetList());
        for (PackageAssets pkgAssets : getPackageAssetList()) {
            if (pkgAssets.getAssets() != null)
                Collections.sort(pkgAssets.getAssets());
        }
    }

}
