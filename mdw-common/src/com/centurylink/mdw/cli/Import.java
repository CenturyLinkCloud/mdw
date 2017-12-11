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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Import extends Setup {

    private String groupId;

    public String getGroupId() {
        return groupId;
    }

    private String artifactId;

    public String getArtifactId() {
        return artifactId;
    }

    private String version;

    public String getVersion() {
        return version;
    }

    private List<String> artifacts = new ArrayList<>();

    public List<String> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<String> artifacts) {
        this.artifacts = artifacts;
    }

    public Import(String groupId, List<String> artifacts) {
        this.groupId = groupId;
        this.artifacts = artifacts;
    }

    public Import(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public Import run(ProgressMonitor... progressMonitors) throws IOException {
        if (groupId != null && !groupId.isEmpty() && artifactId != null && !artifactId.isEmpty()
                && version != null && !version.isEmpty())
            importPackage(progressMonitors);
        if (artifacts != null && !artifacts.isEmpty()) {
            for (String pkg : artifacts) {
                int index = pkg.indexOf('-');
                artifactId = pkg.substring(0, index);
                version = pkg.substring(index + 1);
                importPackage(progressMonitors);
            }
        }
        return this;
    }

    protected void importPackage(ProgressMonitor... monitors) throws IOException {
        String url = "http://search.maven.org/remotecontent?filepath=";
        File assetDir = new File(getAssetLoc());
        List<String> pkgs = new ArrayList<>();
        pkgs.add(groupId.replace("assets", "") + "." + artifactId.replace('-', '.'));
        File tempZip = Files.createTempFile("central-discovery", ".zip").toFile();
        new Download(new URL(url + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/"
                + artifactId + "-" + version + ".zip"), tempZip).run(monitors);

        Archive archive = new Archive(assetDir, pkgs);
        archive.backup();
        System.out.println("Unzipping into: " + assetDir);
        new Unzip(tempZip, assetDir, true).run();
        archive.archive(true);
        if (!tempZip.delete())
            throw new IOException("Failed to delete: " + tempZip.getAbsolutePath());
    }

}
