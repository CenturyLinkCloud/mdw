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

/**
 * Asset headline info that can come from definition caches, or from parsing comment strings.
 */
public class AssetHeader {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String version;
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    private String packageName;
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    private String packageVersion;
    public String getPackageVersion() { return packageVersion; }
    public void setPackageVersion(String packageVersion) { this.packageVersion = packageVersion; }

    public AssetHeader() {
    }

    public AssetHeader(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public AssetHeader(String comments) {
        AssetVersionSpec spec = AssetVersionSpec.parse(comments);
        name = spec.getName();
        version = spec.getVersion();
        String pkgNameVer = spec.getPackageName();
        if (pkgNameVer != null) {
            int spaceV = pkgNameVer.indexOf(" v");
            if (spaceV > 0 && pkgNameVer.length() > spaceV + 2) {
                packageName = pkgNameVer.substring(0, spaceV);
                packageVersion = pkgNameVer.substring(spaceV + 1);
            }
            else {
                packageName = spec.getPackageName();
            }
        }

    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (packageName != null)
            sb.append(packageName).append("/");
        if (name != null)
            sb.append(name);
        if (version != null)
            sb.append(" v").append(version);
        return sb.toString();
    }
}
