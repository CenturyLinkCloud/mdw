package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.system.BadVersionException;
import com.centurylink.mdw.model.system.MdwVersion;

public class PackageDependency {

    private String pkg;
    public String getPackage() { return pkg; }
    public void setPackage(String packageName) { this.pkg = packageName; }

    private MdwVersion version;
    public MdwVersion getVersion() { return version; }
    public void setVersion(MdwVersion version) { this.version = version; }

    public PackageDependency(String packageName, MdwVersion version) {
        this.pkg = packageName;
        this.version = version;
    }

    public PackageDependency(String dependency) throws BadVersionException {
        String[] segments = dependency.split("\\s+");
        if (segments.length != 2 || !segments[1].startsWith("v"))
            throw new BadVersionException(dependency);
        this.pkg = segments[0];
        this.version = new MdwVersion(segments[1]);
    }

    @Override
    public String toString() {
        return pkg + version.getLabel();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PackageDependency))
            return false;
        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
