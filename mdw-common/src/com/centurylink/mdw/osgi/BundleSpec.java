/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class BundleSpec {

    private String symbolicName;
    public String getSymbolicName() { return symbolicName; }

    private String versionSpec;
    public String getVersionSpec() { return versionSpec; }

    public BundleSpec(String symbolicName) {
        this(symbolicName, null);
    }

    public BundleSpec(String symbolicName, String versionSpec) {
        this.symbolicName = symbolicName;
        this.versionSpec = versionSpec;
    }

    public boolean meetsSpec(Bundle bundle) {
        return bundle.getSymbolicName() != null && bundle.getSymbolicName().equals(symbolicName) && meetsVersionSpec(bundle.getVersion());
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
    public boolean meetsVersionSpec(Version version) {
        if (versionSpec == null || versionSpec.equals("0"))
            return true;

        if (versionSpec.startsWith("[")) {
            int comma = versionSpec.indexOf(',');
            if (comma == -1) {
                String min = versionSpec.substring(1);
                return Version.parseVersion(min).compareTo(version) <= 0;
            }
            else {
                String min = versionSpec.substring(1, comma);
                String maxExcl = versionSpec.substring(comma + 1, versionSpec.lastIndexOf(')'));
                return Version.parseVersion(min).compareTo(version) <= 0 && Version.parseVersion(maxExcl).compareTo(version) > 0;
            }
        }
        else {
            return Version.parseVersion(versionSpec).compareTo(version) == 0;
        }
    }

    public String toString() {
        String str = symbolicName;
        if (versionSpec != null && !versionSpec.equals("0"))
            str += " v" + versionSpec;
        return str;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BundleSpec))
            return false;
        return toString().equals(obj.toString());
    }
}
