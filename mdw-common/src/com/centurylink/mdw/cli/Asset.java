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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.dataaccess.AssetRef;

@Parameters(commandNames="asset", commandDescription="Show Asset Info")
public class Asset extends Setup {

    /**
     * Arguments = list of asset specs.
     */
    @Parameter(names="args", description="pass-thru jgit arguments", variableArity = true)
    public List<String> args = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        JCommander cmd = new JCommander();
        Asset asset = new Asset();
        cmd.addCommand("asset", asset);
        String[] assetArgs = new String[args.length + 1];
        assetArgs[0] = "asset";
        assetArgs[1] = "args";
        for (int i = 1; i < args.length; i++) {
            assetArgs[i+1] = args[i];
        }
        cmd.parse(assetArgs);
        asset.run(Main.getMonitor());
    }

    @Override
    public Asset run(ProgressMonitor... progressMonitors) throws IOException {
        File projectDir = getProjectDir();

        Props props = new Props(projectDir, this);
        VcInfo vcInfo = new VcInfo(projectDir, props);
        DbInfo dbInfo = new DbInfo(props);
        Checkpoint checkpoint = new Checkpoint(getReleasesUrl(), vcInfo, getAssetLoc(), dbInfo);
        try {
            checkpoint.run(progressMonitors);
            System.out.println("Asset info:");
            for (String arg : args) {
                AssetRef ref = null;
                if (arg.matches(".* v[0-9\\.\\[,\\)]*$")) {
                    ref = checkpoint.retrieveRef(arg);
                    System.out.println("  db ref: " + ref);
                }
                if (ref == null) {
                    ref = checkpoint.getCurrentRef(arg);
                    System.out.println("  current ref: " + ref);
                }
                // TODO: show git content
            }
        }
        catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }

        return this;
    }

}
