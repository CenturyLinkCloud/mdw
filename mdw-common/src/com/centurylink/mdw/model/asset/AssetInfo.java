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

import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.dataaccess.file.AssetFile;
import com.centurylink.mdw.dataaccess.file.GitDiffs.DiffType;
import com.centurylink.mdw.model.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AssetInfo implements Jsonable, Comparable<AssetInfo> {

    private static Map<String,String> extToContentType = new HashMap<>();
    static {
        extToContentType.put("camel", "text/xml");
        extToContentType.put("css", "text/css");
        extToContentType.put("csv", "application/csv");
        extToContentType.put("docx", "application/vnd.ms-word");
        extToContentType.put("gif", "image/gif");
        extToContentType.put("html", "text/html");
        extToContentType.put("jpg", "image/jpeg");
        extToContentType.put("js", "application/javascript");
        extToContentType.put("jsx", "application/javascript");
        extToContentType.put("json", "application/json");
        extToContentType.put("pagelet", "text/xml");
        extToContentType.put("png", "image/png");
        extToContentType.put("svg", "image/svg+xml");
        extToContentType.put("rptdesign", "text/xml");
        extToContentType.put("spring", "text/xml");
        extToContentType.put("wsdl", "text/xml");
        extToContentType.put("xhtml", "text/xml");
        extToContentType.put("xlsx", "application/vnd.ms-excel");
        extToContentType.put("xml", "text/xml");
        extToContentType.put("xsd", "text/xml");
        extToContentType.put("yml", "text/yaml");
    }

    /**
     * If not mapped, content-type is application/octet-stream for binary and test/plain otherwise.
     * TODO: make this extensible
     */
    public String getContentType() {
        return getContentType(getExtension());
    }

    public boolean isBinary() {
        return isBinary(getExtension());
    }

    public boolean isImage() {
        return isImage(getExtension());
    }

    public boolean isMarkdown() {
        return "md".equals(getExtension());
    }

    private File file;
    public File getFile() { return file; }

    public boolean exists() {
        return file != null && file.isFile();
    }

    private CommitInfo commitInfo;
    public CommitInfo getCommitInfo() { return commitInfo; }
    public void setCommitInfo(CommitInfo info) { this.commitInfo = info; }

    public AssetInfo(File file) {
        this.file = file;
    }

    public AssetInfo(File root, String path) {
        int slash = path.indexOf('/');
        String pkg = path.substring(0, slash);
        String asset = path.substring(slash + 1);
        this.file = new File(root + "/" + pkg.replace('.', '/') + "/" + asset);
    }

    public String getName() {
        return file.getName();
    }

    private String rootName;
    public String getRootName() {
        if (rootName == null) {
            String filename = file.getName();
            int lastDot =  filename.lastIndexOf('.');
            if (lastDot > 0)
                rootName = filename.substring(0, lastDot);
            else
                rootName = file.getName();
        }
        return rootName;
    }

    private String extension;
    public String getExtension() {
        if (extension == null) {
            String filename = file.getName();
            int lastDot =  filename.lastIndexOf('.');
            if (lastDot > 0)
                extension = filename.substring(lastDot + 1);
        }
        return extension;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject asset = create();
        asset.put("name", file.getName());
        if (file instanceof AssetFile) {
            AssetFile assetFile = (AssetFile)file;
            asset.put("id", assetFile.getId());
            AssetRevision assetRev = assetFile.getRevision();
            asset.put("version", assetRev == null ? "0" : assetRev.getVersionString());
        }
        if (isImage())
            asset.put("isImage", true);
        if (isBinary())
            asset.put("isBinary", true);
        if (isMarkdown())
            asset.put("isMarkdown", true);
        if (getVcsDiffType() != null)
            asset.put("vcsDiffType", getVcsDiffType());
        if (vcsDiffType != null)
            asset.put("vcsDiff", vcsDiffType);
        if (commitInfo != null)
            asset.put("commitInfo", commitInfo.getJson());
        return asset;
    }

    public AssetRevision getRevision() {
        if (file instanceof AssetFile) {
            AssetFile assetFile = (AssetFile)file;
            return assetFile.getRevision();
        }
        return null;
    }

    public String getJsonName() {
        return "Asset";
    }

    public boolean isFormat(String assetFormat) {
        return Asset.getFileExtension(assetFormat).substring(1).equals(getExtension());
    }

    public boolean shouldCache(String ifNoneMatchHeader) {
        if (ifNoneMatchHeader == null)
            return false; // no cache
        try {
            long clientTime = Long.parseLong(ifNoneMatchHeader);
            return clientTime >= getFile().lastModified();
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * @return the ETag header for browser caching
     */
    public String getETag() {
        return String.valueOf(getFile().lastModified());
    }

    @Override
    public int compareTo(AssetInfo other) {
        return getName().compareToIgnoreCase(other.getName());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AssetInfo)
            return file.equals(((AssetInfo)other).file);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    public String toString() {
        return getName() + " v" + getRevision();
    }

    /**
     * For asset services.
     */
    private DiffType vcsDiffType;
    public DiffType getVcsDiffType() { return vcsDiffType; }
    public void setVcsDiffType(DiffType diffType) { this.vcsDiffType = diffType; }

    /**
     * TODO: extensibility
     */
    public static boolean isBinary(String ext) {
        if (isImage(ext))
            return true;
        return "xlsx".equals(ext) || "docx".equals(ext) || "jar".equals(ext) || "class".equals(ext)
                || "zip".equals(ext) || "eot".equals(ext) || "ttf".equals(ext) || "woff".equals(ext) || "woof2".equals(ext);
    }

    /**
     * TODO: extensibility
     */
    public static boolean isImage(String ext) {
        return "png".equals(ext) || "jpg".equals(ext) || "gif".equals(ext) || "svg".equals(ext);
    }

    /**
     * If not mapped, content-type is application/octet-stream for binary and test/plain otherwise.
     * TODO: extensibility
     */
    public static String getContentType(String ext) {
        String ct = extToContentType.get(ext);
        if (ct == null)
            ct = isBinary(ext) ? "application/octet-stream" : "text/plain";
        return ct;
    }
}
