package com.centurylink.mdw.model.asset;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

public class AssetVersion implements Jsonable, Comparable<AssetVersion> {

    public AssetVersion(JSONObject json) {
        bind(json);
    }

    public AssetVersion(Long id, String path, String version) {
        this.id = id;
        this.path = path;
        this.name = path.substring(path.lastIndexOf("/") + 1);
        this.version = version;
    }

    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String path;
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    /**
     * Name is set to make JSON look like AssetInfo.
     */
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String version;
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    private String ref;
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    private CommitInfo commitInfo;
    public CommitInfo getCommitInfo() { return commitInfo; }
    public void setCommitInfo(CommitInfo commitInfo) { this.commitInfo = commitInfo; }

    private long count;
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    /**
     * Descending
     */
    @Override
    public int compareTo(AssetVersion otherVersion) {
        return parseVersion(otherVersion.version) - parseVersion(version);
    }

    public String toString() {
        return path + " v" + version;
    }

    public boolean equals(Object other) {
        return other instanceof AssetVersion && other.toString().equals(toString());
    }

    public int hashCode() {
        return Integer.parseInt(toString());
    }

    /**
     * Either a specific version number can be specified, or a Smart Version can be specified which designates an allowable range.
     *
     * Smart Version Ranges
     * This is similar to the OSGi version spec.  There are four supported syntaxes:
     *    - A specific version -- such as 1.2 -- can be specified.
     *    - Zero can be specified to always use the latest asset version.
     *    - A 'half-open' range -- such as [1.2,2) -- designates an inclusive lower limit and an exclusive upper limit,
     *      denoting version 1.2 and any version after this, up to, but not including, version 2.0.
     *    - An 'unbounded' version range -- such as [1.2 -- which denotes version 1.2 and all later versions.
     */
    public boolean meetsSpec(AssetVersionSpec spec) {
        int intVersion = parseVersionSpec(getVersion());
        if (spec.isRange()) {
            String versionSpec = spec.getVersion();
            int comma = versionSpec.indexOf(',');
            if (comma == -1) {
                String min = versionSpec.substring(1);
                return parseVersionSpec(min) <= intVersion;
            }
            else {
                String min = versionSpec.substring(1, comma);
                String maxExcl = versionSpec.substring(comma + 1, versionSpec.lastIndexOf(')'));
                return parseVersionSpec(min) <= intVersion && parseVersionSpec(maxExcl) > intVersion;
            }
        }
        else {
            return parseVersionSpec(spec.getVersion()) == intVersion || spec.getVersion().equals("0");
        }
    }

    public static int parseVersion(String versionString) throws NumberFormatException {
        if (versionString == null)
            return 0;
        if (versionString.startsWith("v"))
            versionString = versionString.substring(1);
        int dot = versionString.indexOf('.');
        int major, minor;
        if (dot > 0) {
            major = Integer.parseInt(versionString.substring(0, dot));
            minor = Integer.parseInt(versionString.substring(dot + 1));
        }
        else {
            major = 0;
            minor = Integer.parseInt(versionString);
        }
        return major * 1000 + minor;
    }

    public static String formatVersion(int version) {
        if (version == 0)
            return "0";
        else
            return version/1000 + "." + version%1000;
    }

    /**
     * single digit without decimal means a major version not minor
     */
    public static int parseVersionSpec(String versionString) throws NumberFormatException {
        if (versionString == null)
            return 0;
        int dot = versionString.indexOf('.');
        int major, minor;
        if (dot > 0) {
            major = Integer.parseInt(versionString.substring(0, dot));
            minor = Integer.parseInt(versionString.substring(dot + 1));
        }
        else {
            major = Integer.parseInt(versionString);
            minor = 0;
        }
        return major * 1000 + minor;
    }

}
