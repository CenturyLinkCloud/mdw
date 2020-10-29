package com.centurylink.mdw.model.system;

import com.centurylink.mdw.util.ClasspathUtil;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * An MDW release version, or an MDW asset package version.
 */
public class MdwVersion implements Comparable<MdwVersion> {
    private String version;

    private int major;
    public int getMajor() { return major; }

    private int minor;
    public int getMinor() { return minor; }

    private int build;
    public int getBuild() { return build; }

    private boolean snapshot;
    public boolean isSnapshot() { return snapshot; }

    public MdwVersion(String version) throws BadVersionException {
        if (version == null)
            throw new NullPointerException(); // cannot be null
        if (version.startsWith("v"))
            version = version.substring(1);

        this.version = version;
        if (!version.equals("0")) {
            int firstDot = version.indexOf('.');
            if (firstDot < 0)
                throw new BadVersionException("Missing dot: " + version);
            try {
                major = Integer.parseInt(version.substring(0, firstDot));
                int secondDot = version.indexOf('.', firstDot + 1);
                if (secondDot < 0)
                    throw new BadVersionException("Missing second dot: " + version);
                minor = Integer.parseInt(version.substring(firstDot + 1, secondDot));
                if (version.endsWith("-SNAPSHOT")) {
                    snapshot = true;
                    build += Integer.parseInt(version.substring(secondDot + 1, version.length() - 9));
                }
                else {
                    build += Integer.parseInt(version.substring(secondDot + 1));
                }
            }
            catch (NumberFormatException ex) {
                throw new BadVersionException(version, ex);
            }
        }
    }

    public MdwVersion(int intVersion) {
        if (intVersion == 0) {
            this.version = "0";
        }
        else {
            major = Math.abs(intVersion / 1000);
            int remainder = Math.abs(intVersion % 1000);
            minor = remainder / 100;
            build = remainder % 100;
            this.version = major + "." + minor + "." + (build >= 10 ? build : "0" + build);
        }
    }

    @SuppressWarnings("unused")
    public boolean checkRequiredVersion(int major) {
        return checkRequiredVersion(major, 0);
    }

    public boolean checkRequiredVersion(int major, int minor) {
        return checkRequiredVersion(major, minor, 0);
    }

    public boolean checkRequiredVersion(int major, int minor, int build) {
        if (version == null || version.length() == 0)
            return false;

        if (this.major > major)
            return true;
        if (this.major < major)
            return false;

        // major versions are equal
        if (this.minor > minor)
            return true;
        if (this.minor < minor)
            return false;

        // major and minor are equal
        return this.build >= build;
    }

    /**
     * Checks whether this version meets the spec according to semantic versioning.
     * Currently spec must be an actual version (no wildcards, etc).
     */
    public boolean meets(String spec) {
        MdwVersion reqVer = new MdwVersion(spec);
        if (major != reqVer.major)
            return false;
        return (minor >= reqVer.minor);
    }

    public String getLabel() {
        return " v" + version;
    }

    public String toString() {
        return version;
    }

    /**
     * Returns old-style int version.
     */
    public int getIntVersion() throws BadVersionException {
        if (version == null || version.equals("0"))
            return 0;
        int ver = major * 1000 + minor * 100 + build;
        if (version.startsWith("-"))
            return -1 * ver;
        else
            return ver;
    }

    @Override
    public int compareTo(MdwVersion other) {
        if (this.major != other.major)
            return this.major - other.major;
        if (this.minor != other.minor)
            return this.minor - other.minor;
        if (this.build != other.build)
            return this.build - other.build;
        if (this.isSnapshot() != other.isSnapshot())
            return this.isSnapshot() ? -1 : 1;
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MdwVersion && ((MdwVersion)obj).version.equals(version);
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }

    /**
     * Includes build timestamp.
     * Finds info from META-INF/manifest.mf via classloader.  Does not work from
     * within certain containers.  For comprehensive lookup, use ApplicationContext.getMdwVersion().
     */
    @SuppressWarnings("unused")
    public static String getRuntimeVersion() throws IOException {
        String classLoc = ClasspathUtil.locate(MdwVersion.class.getName());
        int dotJarBang = classLoc.lastIndexOf(".jar!");
        if (dotJarBang > 0) {
            String jarFilePath = classLoc.substring(0, dotJarBang + 4);
            if (jarFilePath.startsWith("file:/")) {
                jarFilePath = jarFilePath.substring(6).replaceAll("%20", " ");
            }
            if (!jarFilePath.startsWith("/"))
                jarFilePath = "/" + jarFilePath;
            try (JarFile jarFile = new JarFile(new File(jarFilePath))) {
                Manifest manifest = jarFile.getManifest();
                String version = manifest.getMainAttributes().getValue("MDW-Version");
                String buildTimestamp = manifest.getMainAttributes().getValue("MDW-Build");
                return version + " " + buildTimestamp;
            }
        }
        return null;
    }
}
