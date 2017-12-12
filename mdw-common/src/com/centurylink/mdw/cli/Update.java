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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.beust.jcommander.Parameters;

@Parameters(commandNames="update", commandDescription="Update MDW assets locally via Discovery", separators="=")
public class Update extends Setup {

    private File projDir;
    @Override
    public File getProjectDir() {
        if (projDir != null)
            return projDir;
        else
            return super.getProjectDir();
    }

    public Update(File projectDir) {
        this.projDir = projectDir;
    }

    Update() {
        // cli use only
    }

    public Update run(ProgressMonitor... monitors) throws IOException {
        Props props = new Props(this);
        String discoveryUrl = props.get(Props.DISCOVERY_URL);

        if (getBaseAssetPackages() == null) {
            initBaseAssetPackages();
        }

        Map<String,String> discovered = new HashMap<>();
        Discover discover = new Discover();
        discover.setLatest(true);
        boolean isMdw = discoveryUrl.endsWith("/services") || discoveryUrl.endsWith("/Services");
        if (isMdw)
            discover.discoverMdw(discoveryUrl, monitors);
        else
            discover.discoverMaven("com.centurylink.mdw.assets", monitors);
        JSONObject json = discover.getPackages();
        if (json.has("packages")) {
            JSONArray pkgs = json.getJSONArray("packages");
            pkgs.forEach(pkg -> {
                JSONObject pkgJson = (JSONObject)pkg;
                discovered.put(pkgJson.getString("name"), isMdw ? null : pkgJson.getString("version"));
            });
        }

        List<String> toDownload = new ArrayList<>();
        System.out.println("Import asset packages:");
        for (String pkg : getBaseAssetPackages()) {
            if (discovered.containsKey(pkg)) {
                System.out.println("  - " + pkg);
                toDownload.add(isMdw ? pkg : (pkg.substring(pkg.lastIndexOf('.') + 1) + "-" + discovered.get(pkg)));
            }
            else {
                System.err.println("  - " + pkg + " not found for import");
            }
        }

        if (toDownload.isEmpty()) {
            System.out.println(" - no packages selected");
        }
        else {
            Import mport = new Import();
            mport.setAssetLoc(getProjectDir() + "/" + getAssetLoc());
            if (isMdw) {
                mport.importPackagesFromMdw(discoveryUrl, toDownload, monitors);
            }
            else {
                mport.importPackagesFromMaven("com.centurylink.mdw.assets", toDownload, monitors);
            }
        }

        return this;
    }
}