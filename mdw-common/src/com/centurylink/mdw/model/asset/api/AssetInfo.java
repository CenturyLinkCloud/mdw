package com.centurylink.mdw.model.asset.api;

import com.centurylink.mdw.git.GitDiffs.DiffType;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.asset.CommitInfo;
import com.centurylink.mdw.model.asset.ContentTypes;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class AssetInfo implements Jsonable, Comparable<AssetInfo> {

    private long id;
    public long getId() { return id; }

    private final String name;
    public String getName() { return name; }

    private File file;
    public File getFile() { return file; }

    private String version;
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version;}

    public AssetInfo(String name) {
        this.name = name;
    }

    public AssetInfo(String name, File file, long id, String version) {
        this.name = name;
        this.file = file;
        this.id = id;
        this.version = version;
    }

    public boolean isBinary() {
        return ContentTypes.isBinary(getExtension());
    }

    public boolean isImage() {
        return ContentTypes.isImage(getExtension());
    }

    public boolean isMarkdown() {
        return "md".equals(getExtension());
    }

    private CommitInfo commitInfo;
    public CommitInfo getCommitInfo() { return commitInfo; }
    public void setCommitInfo(CommitInfo info) { this.commitInfo = info; }

    private String rootName;
    public String getRootName() {
        if (rootName == null) {
            String filename = getName();
            int lastDot =  filename.lastIndexOf('.');
            if (lastDot > 0)
                rootName = filename.substring(0, lastDot);
            else
                rootName = filename;
        }
        return rootName;
    }

    private String extension;
    public String getExtension() {
        if (extension == null) {
            String filename = getName();
            int lastDot =  filename.lastIndexOf('.');
            if (lastDot > 0)
                extension = filename.substring(lastDot + 1);
        }
        return extension;
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("name", name);
        if (version != null)
            json.put("version", version);
        if (id > 0)
            json.put("id", id);
        if (isImage())
            json.put("isImage", true);
        if (isBinary())
            json.put("isBinary", true);
        if (isMarkdown())
            json.put("isMarkdown", true);
        if (getVcsDiffType() != null)
            json.put("vcsDiffType", getVcsDiffType());
        if (vcsDiffType != null)
            json.put("vcsDiff", vcsDiffType);
        if (commitInfo != null)
            json.put("commitInfo", commitInfo.getJson());
        return json;
    }

    public String getJsonName() {
        return "Asset";
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
        return name + (version == null ? "" : (" v " + version));
    }

    /**
     * For asset services.
     */
    private DiffType vcsDiffType;
    public DiffType getVcsDiffType() { return vcsDiffType; }
    public void setVcsDiffType(DiffType diffType) { this.vcsDiffType = diffType; }

    public static String getContentType(String ext) {
        return ContentTypes.getContentType(ext);
    }
}
