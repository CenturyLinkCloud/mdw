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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Import assets from Git.
 * Archiving is replaced by population of ASSET_REF db table.
 * Extends setup only so that Git info can be shared for template population.
 * TODO: If local .git already present, confirm existing branch matches requested
 */
@Parameters(commandNames="import", commandDescription="Import assets from Git", separators="=")
public class Import extends Setup {

    @Parameter(description="<project>", required=true)
    private String project;

    public Import run(ProgressMonitor... progressMonitors) throws IOException {
        System.out.println("Importing " + project + "...");
        projectDir = new File(project);

        Props props = new Props(projectDir, this);
        String gitUrl = props.get(Props.Git.REMOTE_URL);
        String gitUser = props.get(Props.Git.USER);
        String gitPassword = props.get(Props.Git.PASSWORD, false);
        String gitBranch = props.get(Props.Git.BRANCH, false);

        VcInfo vcInfo = new VcInfo(gitUrl, gitUser, gitPassword, projectDir, gitBranch);

        DbInfo dbInfo = null; // TODO
        Checkpoint checkpoint = new Checkpoint(getReleasesUrl(), vcInfo, getAssetLoc(), dbInfo);
        checkpoint.run(progressMonitors);

        Git git = new Git(getReleasesUrl(), vcInfo, "hardCheckout", vcInfo.getBranch());
        git.run(progressMonitors);

        return this;
    }

}
