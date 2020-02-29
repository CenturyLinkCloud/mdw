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

import com.beust.jcommander.Parameters;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (getBaseAssetPackages() == null) {
            initBaseAssetPackages();
        }

        Import mport = new Import();
        mport.setAssetLoc(getAssetLoc());
        mport.setMdwVersion(getMdwVersion());
        mport.setConfigLoc(getConfigLoc());

        if (isSnapshots()) {
            for (String pkg : getBaseAssetPackages()) {
                getOut().println("Import asset packages:");
                mport.importSnapshotPackage(pkg, monitors);
            }
        }
        else {
            Props props = new Props(this);

            Map<String, String> discovered = new HashMap<>();
            Discover discover = new Discover();
            if (getMdwVersion() != null)
                discover.setMdwVersion(getMdwVersion());
            else
                discover.setLatest(true);
            discover.discoverMaven("com.centurylink.mdw.assets", monitors);
            JSONObject json = discover.getPackages();
            if (json.has("packages")) {
                JSONArray pkgs = json.getJSONArray("packages");
                for (int i = 0; i < pkgs.length(); i++) {
                    JSONObject pkgJson = pkgs.getJSONObject(i);
                    discovered.put(pkgJson.getString("name"), pkgJson.getString("artifact") + "-" + pkgJson.getString("version"));
                }
            }

            List<String> toDownload = new ArrayList<>();
            getOut().println("Import asset packages:");
            for (String pkg : getBaseAssetPackages()) {
                if (discovered.containsKey(pkg)) {
                    getOut().println("  - " + pkg);
                    toDownload.add(discovered.get(pkg));
                }
                else {
                    getErr().println("  - " + pkg + " not found for import");
                }
            }

            if (toDownload.isEmpty()) {
                getOut().println(" - no packages selected");
            }
            else {
                mport.importPackagesFromMaven("com.centurylink.mdw.assets", toDownload, monitors);
            }
        }
        return this;
    }
}