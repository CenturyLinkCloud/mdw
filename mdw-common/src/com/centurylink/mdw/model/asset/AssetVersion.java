package com.centurylink.mdw.model.asset;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.system.BadVersionException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AssetVersion implements Jsonable, Comparable<AssetVersion> {

    private final Long id;
    public Long getId() { return id; }

    private final String path;
    public String getPath() { return path; }

    private final String version;
    public String getVersion() { return version; }

    public AssetVersion(String path, String version) {
        this.id = getId(path, version);
        this.path = path;
        this.name = path.substring(path.lastIndexOf("/") + 1);
        this.version = version;
    }

    public AssetVersion(String path, int version) {
        this(path, formatVersion(version));
    }

    /**
     * Name is set to make JSON look like AssetInfo.
     */
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String ref;
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    private CommitInfo commitInfo;
    public CommitInfo getCommitInfo() { return commitInfo; }
    public void setCommitInfo(CommitInfo commitInfo) { this.commitInfo = commitInfo; }

    private long count;
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public String ext() {
        int lastDot =  name.lastIndexOf('.');
        if (lastDot > 0)
            return name.substring(lastDot + 1);
        else
            return null;
    }

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
     * Unique id based on asset path and version.  Uses git hashing algorithm.
     * http://stackoverflow.com/questions/7225313/how-does-git-compute-file-hashes
     */
    static long getId(String assetPath, String version) throws BadVersionException {
        String logicalPath = assetPath + " v" + version;
        String blob = "blob " + logicalPath.length() + "\0" + logicalPath;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(blob.getBytes());
            String h = byteArrayToHexString(bytes).substring(0, 7);
            return Long.parseLong(h, 16);
        }
        catch (NoSuchAlgorithmException ex) {
            throw new BadVersionException(ex.getMessage(), ex);
        }
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
