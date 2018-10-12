/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.model.system;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.centurylink.mdw.util.ClasspathUtil;

public class MdwVersion implements Comparable<MdwVersion> {
    private String version;

    public MdwVersion(String mdwVersion) {
        this.version = mdwVersion;
    }

    public boolean checkRequiredVersion(String requiredVersion) throws NumberFormatException {
        String[] parts = requiredVersion.split("\\.");
        if (parts.length == 1)
            return checkRequiredVersion(Integer.parseInt(parts[0]));
        else if (parts.length == 2)
            return checkRequiredVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        else if (parts.length == 3)
            return checkRequiredVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]));
        else
            return false;
    }

    public boolean checkRequiredVersion(int major) {
        return checkRequiredVersion(major, 0);
    }

    public boolean checkRequiredVersion(int major, int minor) {
        return checkRequiredVersion(major, minor, 0);
    }

    public boolean checkRequiredVersion(int major, int minor, int build) {
        if (version == null || version.length() == 0)
            return false;

        int mdwMajor = getMajorVersion();
        if (mdwMajor > major)
            return true;
        if (mdwMajor < major)
            return false;

        // major versions are equal
        int mdwMinor = getMinorVersion();
        if (mdwMinor > minor)
            return true;
        if (mdwMinor < minor)
            return false;

        // major and minor are equal
        return getBuildId() >= build;
    }

    public int getMajorVersion() {
        int idxFirstDot = version.indexOf('.');
        return Integer.parseInt(version.substring(0, idxFirstDot));
    }

    public int getMinorVersion() {
        int idxFirstDot = version.indexOf('.');
        int idxSecondDot = version.indexOf('.', idxFirstDot + 1);
        return idxSecondDot == -1 ? Integer.parseInt(version.substring(idxFirstDot + 1))
                : Integer.parseInt(version.substring(idxFirstDot + 1, idxSecondDot));
    }

    public boolean isSnapshot() {
        return version.endsWith("-SNAPSHOT");
    }

    public int getBuildId() {
        int idxFirstDot = version.indexOf('.');
        int idxSecondDot = version.indexOf('.', idxFirstDot + 1);
        int lastDash = version.lastIndexOf('-');
        if (idxSecondDot == -1)
            return 0;
        else if (lastDash == -1)
            return Integer.parseInt(version.substring(idxSecondDot + 1));
        else // trim -SNAPSHOT
            return Integer.parseInt(version.substring(idxSecondDot + 1, lastDash));
    }

    public String toString() {
        return version;
    }

    @Override
    public int compareTo(MdwVersion other) {
        if (this.getMajorVersion() != other.getMajorVersion())
            return this.getMajorVersion() - other.getMajorVersion();
        if (this.getMinorVersion() != other.getMinorVersion())
            return this.getMinorVersion() - other.getMinorVersion();
        if (this.getBuildId() != other.getBuildId())
            return this.getBuildId() - other.getBuildId();
        if (this.isSnapshot() != other.isSnapshot())
            return this.isSnapshot() ? -1 : 1;
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MdwVersion && ((MdwVersion)obj).version.equals(version);
    }

    /**
     * Includes build timestamp.
     * Finds info from META-INF/manifest.mf via classloader.  Does not work from
     * within certain containers.  For comprehensive lookup, use ApplicationContext.getMdwVersion().
     */
    public static String getRuntimeVersion() throws IOException {
        String classLoc = ClasspathUtil.locate(MdwVersion.class.getName());
        int dotJarBang = classLoc.lastIndexOf(".jar!");
        if (dotJarBang > 0) {
            String jarFilePath = classLoc.substring(0, dotJarBang + 4);
            if (jarFilePath.startsWith("file:/"))
                jarFilePath = jarFilePath.substring(6);
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
