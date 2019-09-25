package com.centurylink.mdw.model.asset;

import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

public class AssetVersion implements Jsonable, Comparable<AssetVersion> {

    public AssetVersion(JSONObject json) {
        bind(json);
    }

    public AssetVersion(AssetRef assetRef) {
        String full = assetRef.getName();
        int sp = full.indexOf(' ');
        this.path = full.substring(0, sp);
        this.version = full.substring(sp + 2);
        this.id = assetRef.getDefinitionId();
        this.ref = assetRef.getRef();
    }

    public AssetVersion(Long id, String path, String version) {
        this.id = id;
        this.path = path;
        this.version = version;
    }

    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String path;
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    private String version;
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    private String ref;
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    private CommitInfo commitInfo;
    public CommitInfo getCommitInfo() { return commitInfo; }
    public void setCommitInfo(CommitInfo commitInfo) { this.commitInfo = commitInfo; }

    /**
     * Descending
     */
    @Override
    public int compareTo(AssetVersion otherVersion) {
        return Asset.parseVersion(otherVersion.version) - Asset.parseVersion(version);
    }
}
