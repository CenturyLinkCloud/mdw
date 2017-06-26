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
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import com.beust.jcommander.Parameter;

/**
 * Handle War download.
 */
public class Install {

    @Parameter(names="--mdw-version", description="MDW Version")
    private String mdwVersion;
    /**
     * TODO: Read mdw version from plugin.xml if present, and compare
     * with gradle.properties.
     * TODO: Support mdw version in pom.xml as alternative to gradle.properties.
     */
    protected String getMdwVersion() throws IOException {
        if (mdwVersion == null) {
            File gradleProps = new File(getProjectDir() + "/gradle.properties");
            if (!gradleProps.exists())
                throw new IOException("Missing: " + gradleProps.getAbsolutePath());
            Properties props = new Properties();
            props.load(new FileInputStream(gradleProps));
            return props.getProperty("mdwVersion");
        }
        return mdwVersion;
    }
    public void setMdwVersion(String version) { this.mdwVersion = version; }

    @Parameter(names="--binaries-url", description="MDW Binaries URL")
    private String binariesUrl = "https://github.com/CenturyLinkCloud/mdw/releases";
    public String getBinariesUrl() { return binariesUrl; }
    public void setBinariesUrl(String url) { this.binariesUrl = url; }

    Install() {
        // cli only
    }

    public void run() throws IOException {
        String mdwVer = getMdwVersion();
        File binDir = new File(getProjectDir() + "/bin");
        File jarFile = new File(binDir + "/mdw-" + mdwVer + ".jar");
        if (binDir.isDirectory()) {
            if (jarFile.exists() && !mdwVer.endsWith("-SNAPSHOT")) {
                System.out.println("Already up-to-date.");
                return;
            }

            for (File file : binDir.listFiles()) {
                if (file.isFile() && file.getName().startsWith("mdw-") && file.getName().endsWith(".jar")) {
                    // remove any mdw jars
                    Files.delete(Paths.get(file.getPath()));
                }
            }
        }
        else {
            Files.createDirectories(Paths.get(binDir.getPath()));
        }

        // https://github.com/CenturyLinkCloud/mdw/releases/download/v6.0.05-SNAPSHOT/mdw-6.0.05-SNAPSHOT.jar
        if (!binariesUrl.endsWith("/"))
            binariesUrl += "/";
        URL jarDownloadUrl = new URL(binariesUrl + "download/v" + mdwVer + "/mdw-" + mdwVer + ".jar");
        System.out.println("Downloading " + jarDownloadUrl + "...");
        new Download(jarDownloadUrl, jarFile).run();
        System.out.println("Done.");
    }


    private File getProjectDir() {
        return new File(".");
    }
}
