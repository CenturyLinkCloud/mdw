/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.util.StringHelper;

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
                if (pkgAssets.getPackageDir().getPackageName().equals(packageName))
                    return pkgAssets;
            }
        }
        return null;
    }

    public AssetPackageList() {
    }

    public AssetPackageList(List<PackageAssets> packageAssetList) {
        this.packageAssetList = packageAssetList;
        packageAssetList.forEach(pal -> pal.getAssets().forEach(a -> count++));
    }

    public AssetPackageList(JSONObject json) throws JSONException {
        if (json.has("retrieveDate"))
            this.retrieveDate = StringHelper.serviceStringToDate(json.getString("retrieveDate"));
        if (json.has("count"))
            this.count = json.getInt("count");
        if (json.has("packages")) {
            JSONArray pkgsArr = json.getJSONArray("packages");
            this.packageAssetList = new ArrayList<>();
            for (int i = 0; i < pkgsArr.length(); i++) {
                JSONObject pkgObj = pkgsArr.getJSONObject(i);
                PackageDir pkgDir = new PackageDir(ApplicationContext.getAssetRoot(), pkgObj.getString("name"), null);
                this.packageAssetList.add(new PackageAssets(pkgDir, pkgObj));
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("retrieveDate", StringHelper.serviceDateToString(getRetrieveDate()));
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
