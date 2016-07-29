/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.asset;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.dataaccess.file.PackageDir;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="PackageList", description="Asset package list")
public class PackageList implements Jsonable {

    private String serverInstance;
    public String getServerInstance() { return serverInstance; }

    private File assetRoot;
    @ApiModelProperty(dataType="string")
    public File getAssetRoot() { return assetRoot; }

    private File vcsRoot;
    @ApiModelProperty(dataType="string")
    public File getVcsRoot() { return vcsRoot; }

    private String vcsBranch;
    public String getVcsBranch() { return vcsBranch; }
    public void setVcsBranch(String branch) { this.vcsBranch = branch; }

    private String vcsRemoteUrl;
    public String getVcsRemoteUrl() { return vcsRemoteUrl; }
    public void setVcsRemoteUrl(String url) { this.vcsRemoteUrl = url; }

    private List<PackageDir> packageDirs;
    public List<PackageDir> getPackageDirs() { return packageDirs; }
    public void setPackageDirs(List<PackageDir> pkgDirs) { this.packageDirs = pkgDirs; }

    public PackageList(String serverInstance, File assetRoot, File vcsRoot) {
        this.serverInstance = serverInstance;
        this.assetRoot = assetRoot;
        this.vcsRoot = vcsRoot;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject pkgs = new JSONObject();
        pkgs.put("serverInstance", serverInstance);
        pkgs.put("assetRoot", assetRoot.toString().replace('\\', '/'));
        if (vcsRoot != null)
            pkgs.put("vcsRoot", vcsRoot);
        if (vcsBranch != null)
            pkgs.put("vcsBranch", vcsBranch);
        if (vcsRemoteUrl != null)
            pkgs.put("vcsRemoteUrl", vcsRemoteUrl);
        JSONArray pkgArray = new JSONArray();
        if (packageDirs != null) {
            for (PackageDir pkgDir : packageDirs) {
                JSONObject pkg = new JSONObject();
                pkg.put("id", pkgDir.getId());
                pkg.put("name", pkgDir.getPackageName());
                pkg.put("version", pkgDir.getPackageVersion());
                if (pkgDir.getVcsDiffType() != null)
                    pkg.put("vcsDiff", pkgDir.getVcsDiffType());
                pkgArray.put(pkg);
            }
        }
        pkgs.put("packages", pkgArray);
        return pkgs;
    }

    public String getJsonName() {
        return "Packages";
    }

    public void sort() {
        Collections.sort(getPackageDirs(), new Comparator<PackageDir>() {
            public int compare(PackageDir pd1, PackageDir pd2) {
                return pd1.getPackageName().compareToIgnoreCase(pd2.getPackageName());
            }
        });
    }
}
