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

    /**
     * In case different from vcsBranch (switch scenario).
     */
    private String gitBranch;
    public String getGitBranch() { return gitBranch; }
    public void setGitBranch(String branch) { this.gitBranch = branch; }

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
        if (gitBranch != null)
            pkgs.put("gitBranch", gitBranch);
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
                pkg.put("format", "json");
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
