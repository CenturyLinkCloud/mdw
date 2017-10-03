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

import org.json.JSONArray;
import org.json.JSONObject;

import com.beust.jcommander.Parameters;

@Parameters(commandNames="update", commandDescription="Update MDW assets locally via Discovery", separators="=")
public class Update extends Setup {

    public Update(Setup cloneFrom) {
        super(cloneFrom);
    }

    Update() {
        // cli use only
    }

    public Update run(ProgressMonitor... progressMonitors) throws IOException {
        Props props = new Props(projectDir, this);

        if (getBaseAssetPackages() == null) {
            initBaseAssetPackages();
        }

        List<String> discovered = new ArrayList<>();
        String discoveryUrl = props.get(Props.DISCOVERY_URL);
        System.out.println("Discovering assets from: " + discoveryUrl);
        String assetsJson = new Fetch(new URL(discoveryUrl + "/services/Assets")).run().getData();
        JSONObject json = new JSONObject(assetsJson);
        if (json.has("packages")) {
            JSONArray pkgArr = json.getJSONArray("packages");
            for (int i = 0; i < pkgArr.length(); i++) {
                discovered.add(pkgArr.getJSONObject(i).getString("name"));
            }
        }

        List<String> toDownload = new ArrayList<>();
        System.out.println("Import asset packages:");
        for (String pkg : getBaseAssetPackages()) {
            if (discovered.contains(pkg)) {
                System.out.println("  - " + pkg);
                toDownload.add(pkg);
            }
            else {
                System.err.println("  - " + pkg + " not found for import");
            }
        }

        if (toDownload.isEmpty()) {
            System.out.println(" - no packages selected");
        }
        else {
            importPackages(discoveryUrl, toDownload, progressMonitors);
        }

        return this;
    }

    protected void importPackages(String discoveryUrl, List<String> packages, ProgressMonitor... monitors) throws IOException {

        // download packages temp zip
        System.out.println("Downloading packages...");

        File tempZip = Files.createTempFile("mdw-discovery", ".zip").toFile();

        String pkgsParam = "[";
        for (int i = 0; i < packages.size(); i++) {
            pkgsParam += packages.get(i);
            if (i < packages.size() - 1)
                pkgsParam += ",";
        }
        pkgsParam += "]";
        new Download(new URL(discoveryUrl + "/asset/packages?recursive=false&packages=" + pkgsParam), tempZip).run(monitors);

        // import packages
        File assetDir = getAssetRoot();
        Archive archive = new Archive(assetDir, packages);
        archive.backup();
        System.out.println("Unzipping into: " + assetDir);
        new Unzip(tempZip, assetDir, true).run();
        archive.archive(true);
        if (!tempZip.delete())
            throw new IOException("Failed to delete: " + tempZip.getAbsolutePath());
    }
}