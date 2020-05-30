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
import java.io.IOException;
import java.nio.file.Files;

public class Asset implements Comparable<Asset> {

    private Long id;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private String packageName;
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    private int version;
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }

    private File file;
    public File getFile() {
        return file;
    }
    public void setFile(File file) {
        this.file = file;
    }
    public File file() {
        return file;
    }

    private byte[] content;
    public byte[] getContent() { return content; }
    public void setContent(byte[] content) {
        this.content = content;
    }

    private boolean archived;
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public String getExtension() {
        int lastDot =  name.lastIndexOf('.');
        if (lastDot > 0)
            return name.substring(lastDot + 1);
        else
            return null;
    }

    public String getRootName() {
        int lastDot =  name.lastIndexOf('.');
        if (lastDot > 0)
            return name.substring(0, lastDot);
        else
            return name;
    }

    public String getContentType() {
        return ContentTypes.getContentType(getExtension());
    }

    protected Asset() {
    }

    public Asset(String packageName, String name, int version, File file) {
        this.packageName = packageName;
        this.name = name;
        this.version = version;
        this.id = new AssetVersion(packageName + "/" + name, version).getId();
        this.file = file;
    }

    /**
     * Instantiates an unloaded asset.
     */
    public Asset(File assetRoot, AssetVersion assetVersion) {
        this.id = assetVersion.getId();
        this.name = assetVersion.getName();
        this.version = AssetVersion.parseVersion(assetVersion.getVersion());
        AssetPath assetPath = new AssetPath(assetVersion.getPath());
        this.packageName = assetPath.pkg;
        this.file = new File(assetRoot + "/" + packageName.replace('.', '/') + "/" + assetVersion.getName());
    }

    public String getText() {
        if (content == null)
            return null;
        return new String(content);
    }
    public void setText(String string) {
        this.content = string == null ? null : string.getBytes();
    }

    public String getLabel() {
        return getName() + (getVersion() == 0 ? "" : " v" + getVersionString());
    }

    public boolean isLoaded() {
        return content != null;
    }

    public void load() throws IOException {
        this.content = Files.readAllBytes(getFile().toPath());
    }

    public String getVersionString() {
        return AssetVersion.formatVersion(version);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Asset))
            return false;

        return ((Asset)obj).getId().equals(getId());
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public int compareTo(Asset other) {
        if (this.getName().equals(other.getName()))
            return other.version - this.version;
        return this.getName().compareToIgnoreCase(other.getName());
    }

    public boolean meetsVersionSpec(String versionSpec) {
        AssetVersionSpec assetVersionSpec = new AssetVersionSpec(getPath(), versionSpec);
        return assetVersionSpec.isMatch(new AssetVersion(getPath(), getVersion()));
    }

    public String getPath() {
        return getPackageName() + "/" + getName();
    }

    public String getQualifiedLabel() {
        return getPath() + (getVersion() == 0 ? "" : " v" + getVersionString());
    }

    public boolean exists() {
        return getFile() != null && getFile().exists();
    }
    @SuppressWarnings("squid:S1845")
    public String text() {
        return getText();
    }

    /**
     * Removes windows newlines
     */
    public String getTextNormalized() {
        return text().replace("\r", "");
    }
}
