package com.centurylink.mdw.model.asset;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssetVersionSpec implements Jsonable {

    public static final String VERSION_LATEST = "0";
    public static final Pattern VERSION_PATTERN = Pattern.compile(" v[0-9\\.\\[,\\)]*$");

    private String packageName;
    public String getPackageName() { return packageName; }

    /**
     * The asset name.
     */
    private String name;
    public String getName() { return name; }

    /**
     * The asset version spec.
     */
    private String version;
    public String getVersion() { return version; }

    public AssetVersionSpec(String packageName, String name, String version) {
        this.packageName = packageName;
        this.name = name;
        this.version = version;
    }

    public AssetVersionSpec(String packageProcess) {
        this(packageProcess, "0");
    }

    /**
     * For compatibility: handles with/without leading package path, and handles old-style version format.
     */
    public AssetVersionSpec(String assetPath, String version) {
        int slash = assetPath.indexOf('/');
        if (slash > 0) {
            this.packageName = assetPath.substring(0, slash);
            this.name = assetPath.substring(slash + 1);
        }
        else {
            this.name = assetPath;
        }
        try {
            int versionInt = Integer.parseInt(version);
            // old style
            this.version = AssetVersion.formatVersion(versionInt);
        }
        catch (NumberFormatException ex) {
            this.version = version;
        }
    }

    public String getQualifiedName() {
        if (packageName == null)
            return name;
        else
            return packageName + "/" + name;
    }

    public String getPath() {
        return packageName + "/" + name;
    }

    public boolean isRange() {
        return version != null && (version.indexOf('[') >= 0 || version.indexOf(',') >= 0
                || version.indexOf(')') >= 0);
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
    public boolean isMatch(AssetVersion assetVersion) {
        if (!getPath().equals(assetVersion.getPath())) {
            return false;
        }
        int intVersion = parseVersionSpec(assetVersion.getVersion());
        if (isRange()) {
            String versionSpec = getVersion();
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
            return parseVersionSpec(getVersion()) == intVersion || getVersion().equals("0");
        }
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

    public String toString() {
        if (version == null)
            return getQualifiedName();
        else
            return getQualifiedName() + " v" + version;
    }

    /**
     * Smart Version Ranges:
     * This is similar to the OSGi version spec.  There are four supported syntaxes:
     *    - A specific version -- such as 1.2 -- can be specified.
     *    - Zero can be specified to always use the latest asset version.
     *    - A 'half-open' range -- such as [1.2,2) -- designates an inclusive lower limit and an exclusive upper limit,
     *      denoting version 1.2 and any version after this, up to, but not including, version 2.0.
     *    - An 'unbounded' version range -- such as [1.2 -- which denotes version 1.2 and all later versions.
     */
    public static AssetVersionSpec parse(String versionSpec) {
        Matcher matcher = VERSION_PATTERN.matcher(versionSpec);
        String name = null;
        String ver = null;
        String pkg = null;
        if (matcher.find()) {
            int start = matcher.start();
            ver = versionSpec.substring(start + 2);
            if (!ver.equals(VERSION_LATEST) && ver.indexOf('.') == -1 && Character.isDigit(ver.charAt(0)))
                ver = "0." + ver;
            name = versionSpec.substring(0, start);
        }
        else {
            name = versionSpec; // no version
            ver = VERSION_LATEST;
        }

        int slash = name.indexOf('/');
        if (slash != -1) {
            pkg = name.substring(0, slash);
            name = name.substring(slash + 1);
        }
        return new AssetVersionSpec(pkg, name, ver);
    }

    public static String getDefaultSmartVersionSpec(String version) {
        int dot = version.indexOf('.');
        int major = dot > 0 ? Integer.parseInt(version.substring(0, dot)) : 0;
        return "[" + version + "," + ++major + ")";
    }

    public AssetVersionSpec(JSONObject json) {
        this.name = json.getString("name");
        if (json.has("packageName"))
            this.packageName = json.getString("packageName");
        if (json.has("version"))
            this.version = json.getString("version");
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        if (packageName != null)
            json.put("packageName", packageName);
        if (version != null && !"0".equals(version))
            json.put("version", version);
        return json;
    }
}
