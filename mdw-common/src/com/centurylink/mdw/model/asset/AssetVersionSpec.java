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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssetVersionSpec {

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

    /**
     * For compatibility: handles with/without leading package path, and handles old-style version format.
     */
    public AssetVersionSpec(String packageProcess, String version) {
        int slash = packageProcess.indexOf('/');
        if (slash > 0) {
            this.packageName = packageProcess.substring(0, slash);
            this.name = packageProcess.substring(slash + 1);
        }
        else {
            this.name = packageProcess;
        }
        try {
            int versionInt = Integer.parseInt(version);
            // old style
            this.version = Asset.formatVersion(versionInt);
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
}
