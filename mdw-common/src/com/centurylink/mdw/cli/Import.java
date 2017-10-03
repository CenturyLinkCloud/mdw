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
import java.net.URL;
import java.sql.SQLException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Import assets from Git.  Note: this performs a Git
 * <a href="https://git-scm.com/docs/git-reset#git-reset---hard">HARD REST</a>,
 * overwriting all local changes.
 * Archiving is replaced by population of ASSET_REF db table.
 * TODO: If local .git already present, confirm existing branch matches requested
 */
@Parameters(commandNames="import", commandDescription="Import assets from Git (HARD RESET!)", separators="=")
public class Import extends Setup {

    @Parameter(names="--force", description="Force overwrite, even on localhost or when branch disagrees")
    private boolean force = false;
    public boolean isForce() { return force; }
    public void setForce(boolean force) { this.force = force; }

    public Import run(ProgressMonitor... progressMonitors) throws IOException {
        File projectDir = getProjectDir();
        Props props = new Props(projectDir, this);
        VcInfo vcInfo = new VcInfo(projectDir, props);

        if (!isForce()) {
            String serviceUrl = props.get(Props.SERVICES_URL, false);
            if (serviceUrl != null && new URL(serviceUrl).getHost().equals("localhost")) {
                System.err.println(Props.SERVICES_URL.getProperty() + " indicates 'localhost'; "
                        + "use --force to confirm (overwrites ALL local changes from Git remote)");
                return this;
            }
            String configuredBranch = props.get(Props.Git.BRANCH);
            Git git = new Git(props.get(Props.Gradle.MAVEN_REPO_URL), vcInfo, "getBranch");
            git.run(progressMonitors);
            String gitBranch = (String)git.getResult();
            if (!gitBranch.equals(configuredBranch)) {
                System.err.println(Props.Git.BRANCH.getProperty() + " (" + configuredBranch
                        + ") disagrees with local Git (" + gitBranch
                        + ");  use --force to confirm (overwrites ALL local changes from Git remote)");
                return this;
            }
        }

        System.out.println("Importing " + projectDir + "...");

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
