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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Handle War download.  TODO: Allow specify war name
 */
@Parameters(commandNames="install", commandDescription="Install MDW", separators="=")
public class Install extends Setup {

    private File projectDir;
    public File getProjectDir() { return projectDir; }

    Install() {
        // cli only
        this.projectDir = new File(".");
    }

    public Install(File projectDir) {
        this.projectDir = projectDir;
    }

    @Parameter(names="--webapp", description="Install MDW WAR")
    private boolean webapp = false;
    public boolean isWebapp() { return webapp; }
    public void setWebapp(boolean webapp) { this.webapp = webapp; }

    @Parameter(names="--webapps-dir", description="Webapps dir for Tomcat or Jetty installation")
    private File webappsDir;
    public File getWebappsDir() { return webappsDir; }
    public void setWebappsDir(File dir) { this.webappsDir = dir; }

    @Parameter(names="--binaries-url", description="MDW Binaries URL")
    private String binariesUrl = "https://github.com/CenturyLinkCloud/mdw/releases";
    public String getBinariesUrl() { return binariesUrl; }
    public void setBinariesUrl(String url) { this.binariesUrl = url; }

    public Install run(ProgressMonitor... progressMonitors) throws IOException {
        String mdwVer = new Props(this).get(Props.Gradle.MDW_VERSION);
        Download[] downloads = null;

        if (webapp && webappsDir == null) {
            webappsDir = new File(getProjectDir() + "/deploy/webapps");
            if (!webappsDir.isDirectory() && !webappsDir.mkdirs())
                throw new IOException("Unable to create directory: " + webappsDir.getAbsolutePath());
        }

        if (webappsDir != null) {
            // download war from maven releases-url
            // clean out old installs
            File warFile = new File(webappsDir + "/mdw.war");
            if (warFile.isFile()) {
                Files.delete(Paths.get(warFile.getPath()));
            }
            File warDir = new File(webappsDir + "/mdw");
            if (warDir.isDirectory()) {
                new Delete(warDir).run(progressMonitors);
            }
            // download from releases-url
            URL url = null;
            if (isSnapshots())
                url = new URL("https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.centurylink.mdw&a=mdw&v=LATEST&p=war");
            else
                url = new URL(getReleasesUrl() + "/com/centurylink/mdw/mdw/" + mdwVer + "/mdw-" + mdwVer + ".war");
            downloads = new Download[]{new Download(url, warFile)};
        }
        else {
            // download spring boot from binaries-url
            File binDir = new File(getProjectDir() + "/bin");
            File jarFile = new File(binDir + "/mdw-boot-" + mdwVer + ".jar");
            if (binDir.isDirectory()) {
                if (jarFile.exists() && !mdwVer.endsWith("-SNAPSHOT")) {
                    System.out.println("Already up-to-date: " + jarFile.getAbsolutePath());
                    return this;
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

            if (binariesUrl.startsWith("https://github.com/")) {
                // use the github api to find the boot jar size
                URL releasesApiUrl = new URL("https://api.github.com/repos/" + binariesUrl.substring(19));
                JSONArray releasesArr = new JSONArray(new Fetch(releasesApiUrl).run().getData());
                JSONObject releaseJson = null;
                for (int i = 0; i < releasesArr.length(); i++) {
                    JSONObject relObj = releasesArr.getJSONObject(i);
                    if (relObj.optString("name").equals(mdwVer) || relObj.optString("tag_name").equals(mdwVer)) {
                        releaseJson = relObj;
                        break;
                    }
                }
                if (releaseJson == null)
                    throw new FileNotFoundException("Release not found: " + mdwVer);
                if (releaseJson.has("assets")) {
                    JSONArray assetsArr = releaseJson.getJSONArray("assets");
                    for (int i = 0; i < assetsArr.length(); i++) {
                        JSONObject assetObj = assetsArr.getJSONObject(i);
                        if (assetObj.optString("name").equals(jarFile.getName()) && assetObj.has("browser_download_url")) {
                            if (assetObj.has("size"))
                                downloads = new Download[]{new Download(new URL(assetObj.getString("browser_download_url")), jarFile, assetObj.getInt("size"))};
                            else
                                downloads = new Download[]{new Download(new URL(assetObj.getString("browser_download_url")), jarFile)};
                        }
                    }
                }
            }
            else {
                downloads = new Download[]{new Download(new URL(getBinariesUrl() + "/download/v" + mdwVer + "/mdw-boot-" + mdwVer + ".jar"), jarFile)};
            }
            if (downloads == null) {
                throw new FileNotFoundException("Release artifact not found: " + jarFile.getName());
            }
        }

        if (downloads != null) {
            for (Download download : downloads) {
                System.out.println("Downloading " + download.getTo() + "...");
                download.run(progressMonitors);
            }
        }
        return this;
    }

    @Override
    public boolean validate() throws IOException {
        if (!super.validate())
            return false;
        if (getMdwVersion() == null) {
            File gradleProps = new File(getProjectDir() + "/gradle.properties");
            if (!gradleProps.isFile()) {
                System.err.println("Option --mdw-version required or should be readable from: " + gradleProps.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    protected boolean needsConfig() { return false; }
}
