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
 * Handle War download.
 */
@Parameters(commandNames="install", commandDescription="Install MDW", separators="=")
public class Install implements Operation {

    private static final String WEBTOOLS = "com/centurylink/mdw/mdw/5.5.40/mdw-webtools-5.5.40.war";

    private static final long MDW_WAR_SIZE = 50000000L; // roughly
    private static final long MDW_BOOT_SIZE = 65000000L; // roughly

    private File projectDir;
    public File getProjectDir() { return projectDir; }

    @Parameter(names="--webtools", description="Include webtools")
    private boolean webtools;
    public boolean isWebtools() { return webtools; }
    public void setWebtools(boolean webtools) { this.webtools = webtools; }

    Install() {
        // cli only
        this.projectDir = new File(".");
    }

    public Install(File projectDir) {
        this.projectDir = projectDir;
    }

    @Parameter(names="--mdw-version", description="MDW Version")
    private String mdwVersion;
    protected String getMdwVersion() throws IOException {
        if (mdwVersion == null)
            mdwVersion = new Version().getMdwVersion(getProjectDir());
        return mdwVersion;
    }
    public void setMdwVersion(String version) { this.mdwVersion = version; }

    @Parameter(names="--webapps-dir", description="Webapps dir for Tomcat or Jetty installation")
    private File webappsDir;
    public File getWebappsDir() { return webappsDir; }
    public void setWebappsDir(File dir) { this.webappsDir = dir; }

    @Parameter(names="--binaries-url", description="MDW Binaries URL")
    private String binariesUrl = "https://github.com/CenturyLinkCloud/mdw/releases";
    public String getBinariesUrl() { return binariesUrl; }
    public void setBinariesUrl(String url) { this.binariesUrl = url; }

    @Parameter(names="--releases-url", description="MDW Releases Maven Repo URL")
    private String releasesUrl = "http://repo.maven.apache.org/maven2";
    public String getReleasesUrl() { return releasesUrl; }
    public void setReleasesUrl(String url) { this.releasesUrl = url; }

    public Install run(ProgressMonitor... progressMonitors) throws IOException {
        String mdwVer = getMdwVersion();
        Download[] downloads = null;
        if (webappsDir != null) {
            // download war from maven releases-url
            // clean out old installs
            File warFile = new File(webappsDir + "/mdw.war");
            if (warFile.isFile()) {
                Files.delete(Paths.get(warFile.getPath()));
            }
            File webtoolsWar = new File(webappsDir + "/webtools.war");
            if (webtools) {
                if (webtoolsWar.isFile()) {
                    Files.delete(Paths.get(webtoolsWar.getPath()));
                }
            }
            File warDir = new File(webappsDir + "/mdw");
            if (warDir.isDirectory()) {
                new Delete(warDir).run(progressMonitors);
            }
            if (webtools) {
                File webtoolsDir = new File(webappsDir + "/webtools");
                if (webtoolsDir.isDirectory()) {
                    new Delete(webtoolsDir).run(progressMonitors);
                }
            }
            // download from releases-url
            String releasesUrl = getReleasesUrl();
            if (!releasesUrl.endsWith("/"))
                releasesUrl += "/";
            URL url = new URL(releasesUrl + "com/centurylink/mdw/mdw/" + mdwVer + "/mdw-" + mdwVer + ".war");
            if (webtools) {
                URL webtoolsUrl = new URL(releasesUrl + WEBTOOLS);
                downloads = new Download[]{new Download(url, warFile, MDW_WAR_SIZE), new Download(webtoolsUrl, webtoolsWar)};
            }
            else {
                downloads = new Download[]{new Download(url, warFile, MDW_WAR_SIZE)};
            }
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
                    if (relObj.optString("name").equals(mdwVer)) {
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
                if (!binariesUrl.endsWith("/"))
                    binariesUrl += "/";
                downloads = new Download[]{new Download(new URL(binariesUrl + "download/v" + mdwVer + "/mdw-boot-" + mdwVer + ".jar"), jarFile, MDW_BOOT_SIZE)};
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
}
