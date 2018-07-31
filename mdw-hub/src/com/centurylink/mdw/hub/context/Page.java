/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.hub.context;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.model.asset.AssetInfo;

/**
 * Represents a custom UI page to be loaded.
 */
public class Page {

    private Mdw mdw;
    public Mdw getMdw() { return mdw; }

    private String path;
    public String getPath() { return path; }

    public String getRelPath() {
        return path.substring(1);
    }

    private AssetInfo asset;
    public AssetInfo getAsset() {
        if (asset == null) {
            asset = new AssetInfo(mdw.getAssetRoot(), path);
        }
        return asset;
    }

    public String getAssetPath() {
        String relPath = getRelPath();
        return relPath.substring(0, relPath.length() - getAsset().getName().length() - 1).replace('/', '.')
                + "/" + getAsset().getName();
    }

    private String template;
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    private AssetInfo templateAsset;
    public AssetInfo getTemplateAsset() {
        if (templateAsset == null) {
            templateAsset = new AssetInfo(mdw.getAssetRoot(), template);
        }
        return templateAsset;
    }

    public boolean exists() {
        return getAsset().exists();
    }

    public File getFile() {
        return new File(mdw.getAssetRoot() + "/" + path);
    }

    public String getExt() {
        return getAsset().getExtension();
    }

    /**
     * Embedded means inside MDWHub tabs/frame.
     */
    private boolean embedded;
    public boolean isEmbedded() { return embedded; }
    public void setEmbedded(boolean embedded) { this.embedded = embedded; }

    /**
     * path begins with /
     */
    public Page(Mdw mdw, String path) {
        this.mdw = mdw;
        this.path = path;
    }

    /**
     * Locate closest page in ancestor package hierarchy (if any).
     */
    public Page findAncestor(String name) {
        Path rootPath = Paths.get(mdw.getAssetRoot().getPath()).normalize();
        Path pkgPath = Paths.get(getAsset().getFile().getPath()).normalize();
        while (pkgPath.startsWith(rootPath)) {
            if (new File(pkgPath + "/" + PackageDir.PACKAGE_JSON_PATH).exists() || new File(pkgPath + "/" + PackageDir.PACKAGE_YAML_PATH).exists() ) {
                Page p = new Page(mdw, "/" + rootPath.relativize(pkgPath) + "/" + name);
                if (p.exists())
                    return p;
            }
            pkgPath = pkgPath.getParent();
        }
        return null;
    }
}
