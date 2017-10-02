/**
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

import com.beust.jcommander.Parameters;

/**
 * Import assets from Git.
 * Archiving is replaced by population of ASSET_REF db table.
 * TODO: If local .git already present, confirm existing branch matches requested
 */
@Parameters(commandNames="import", commandDescription="Import assets from Git", separators="=")
public class Import extends Setup {

    public Import run(ProgressMonitor... progressMonitors) throws IOException {
        File projectDir = getProjectDir();
        System.out.println("Importing " + projectDir + "...");

        Props props = new Props(projectDir, this);
        VcInfo vcInfo = new VcInfo(projectDir, props);
        DbInfo dbInfo = new DbInfo(props);
        Checkpoint checkpoint = new Checkpoint(getReleasesUrl(), vcInfo, getAssetLoc(), dbInfo);
        try {
            checkpoint.run(progressMonitors).updateRefs();
        }
        catch (SQLException ex) {
            throw new IOException(ex.getMessage(), ex);
        }

        Git git = new Git(getReleasesUrl(), vcInfo, "hardCheckout", vcInfo.getBranch());
        git.run(progressMonitors);

        return this;
    }

}
