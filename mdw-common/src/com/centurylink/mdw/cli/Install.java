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

    private File projectDir;
    public File getProjectDir() { return projectDir; }

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

    @Parameter(names="--binaries-url", description="MDW Binaries URL")
    private String binariesUrl = "https://github.com/CenturyLinkCloud/mdw/releases";
    public String getBinariesUrl() { return binariesUrl; }
    public void setBinariesUrl(String url) { this.binariesUrl = url; }

    public Install run(ProgressMonitor... progressMonitors) throws IOException {
        String mdwVer = getMdwVersion();
        File binDir = new File(getProjectDir() + "/bin");
        File jarFile = new File(binDir + "/mdw-boot-" + mdwVer + ".jar");
        if (binDir.isDirectory()) {
            if (jarFile.exists() && !mdwVer.endsWith("-SNAPSHOT")) {
                System.out.println("Already up-to-date.");
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

        Download download = null;
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
                            download = new Download(new URL(assetObj.getString("browser_download_url")), jarFile, assetObj.getInt("size"));
                        else
                            download = new Download(new URL(assetObj.getString("browser_download_url")), jarFile);
                    }
                }
            }
        }
        else {
            if (!binariesUrl.endsWith("/"))
                binariesUrl += "/";
            download = new Download(new URL(binariesUrl + "download/v" + mdwVer + "/mdw-boot-" + mdwVer + ".jar"), jarFile);
        }
        System.out.println("Downloading " + jarFile + "...");
        if(download!=null)
            download.run(progressMonitors);

        return this;
    }
}
