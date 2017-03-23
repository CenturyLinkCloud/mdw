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
package com.centurylink.mdw.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.file.GitDiffs;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.util.timer.ProgressMonitor;
import com.centurylink.mdw.util.timer.SystemOutProgressMonitor;

public class GitImporter {

    private static final String CMD_DIFF = "diff";

    public static void main(String[] args) {

        String cmd = null;

        if (args.length == 1 && args[0].equals("-h")) {
            System.out.println("Example Usage: ");
            System.out.println("java com.centurylink.mdw.common.utilities.GitImporter /path/to/mdw.properties gituser gitpassword");
            System.exit(0);
        }
        else if (args.length == 4 && (args[3]).startsWith("--")) {
            cmd = args[3].substring(2);
        }
        else if (args.length != 3) {
            System.err.println("arguments: <path_to_mdw_properties> <gituser> <gitpassword>");
            System.err.println(" or: <path_to_mdw_properties> <gituser> <gitpassword> --diff");
            System.err.println("(-h for example usage)");
            System.exit(-1);
        }

        try {
            Properties mdwProps = new Properties();
            mdwProps.load(new FileInputStream(args[0]));

            String gitLocalPath = mdwProps.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH); // mdw.git.local.path
            if (gitLocalPath == null)
                throw new IllegalArgumentException("Missing property: " + PropertyNames.MDW_GIT_LOCAL_PATH);
            File gitLocalDir = new File(gitLocalPath);
            String gitRemoteUrl = mdwProps.getProperty(PropertyNames.MDW_GIT_REMOTE_URL); // mdw.git.remote.url
            if (gitRemoteUrl == null)
                throw new IllegalArgumentException("Missing property: " + PropertyNames.MDW_GIT_REMOTE_URL);
            String assetLoc = mdwProps.getProperty(PropertyNames.MDW_ASSET_LOCATION);  // mdw.asset.location
            if (assetLoc == null)
                throw new IllegalArgumentException("Missing property: " + PropertyNames.MDW_ASSET_LOCATION);
            String assetPath = assetLoc.substring(gitLocalPath.length() + 1);
            String gitTrustedHost = mdwProps.getProperty(PropertyNames.MDW_GIT_TRUSTED_HOST);  // mdw.git.trusted.host
            if (gitTrustedHost != null)
                DesignatedHostSslVerifier.setupSslVerification(gitTrustedHost);
            String gitBranch = mdwProps.getProperty(PropertyNames.MDW_GIT_BRANCH); // mdw.git.branch
            if (gitBranch == null)
                throw new IllegalArgumentException("Missing property: " + PropertyNames.MDW_GIT_BRANCH);
            String tempLoc = mdwProps.getProperty(PropertyNames.MDW_TEMP_DIR);

            GitImporter importer = new GitImporter(gitRemoteUrl, gitBranch, args[1], args[2], gitLocalDir, assetPath, tempLoc);
            if (cmd == null) {
                long before = System.currentTimeMillis();
                importer.doImport();
                long afterImport = System.currentTimeMillis();
                System.out.println("Time taken for import: " + ((afterImport - before)/1000) + " s");
                System.out.println("Please restart your server or refresh the MDW asset cache.");
            }
            else {
                importer.doCommand(cmd);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    private VersionControlGit vcGit;
    private String branch;
    private File localDir;
    private String assetPath;
    private String tempLoc;

    public GitImporter(String remoteUrl, String branch, String user, String password, File localDir, String assetPath, String tempLoc) throws IOException {
        this.branch = branch;
        this.localDir = localDir;
        this.assetPath = assetPath;
        this.vcGit = new VersionControlGit();
        this.tempLoc = tempLoc;
        vcGit.connect(remoteUrl, user, password, localDir);
    }

    public void doImport() throws Exception {
        if (localDir.exists() && new File(localDir + "/.git").exists()) {
            System.out.println("Directory: " + localDir + " exists.  Updating...");
            File assetDir = new File(localDir + "/" + assetPath);
            File tempDir = new File(tempLoc);
            ProgressMonitor progressMonitor = new SystemOutProgressMonitor();
            progressMonitor.start("Archive existing assets");
            VcsArchiver archiver = new VcsArchiver(assetDir, tempDir, vcGit, progressMonitor);
            archiver.backup();

            System.out.println("Performing git checkout on branch: " + branch);
            vcGit.sparseCheckout(branch, assetPath);

            archiver.archive();
            progressMonitor.done();
        }
        else {
            System.out.println("Directory: " + localDir + " does not exist.  Cloning...");
            vcGit.cloneNoCheckout();
            System.out.println("Performing git checkout on branch: " + branch);
            vcGit.sparseCheckout(branch, assetPath);
        }
    }

    public void doCommand(String cmd) throws Exception {
        if (!localDir.exists()) {
            System.err.println("Directory: " + localDir + " does not exist.  Exiting...");
            System.exit(-1);
        }
        else if (cmd.equals(CMD_DIFF)) {
            GitDiffs diffs = vcGit.getDiffs(branch, assetPath);
            System.out.println(diffs.toString());
        }
        else {
            throw new IllegalArgumentException("Unsupported command: " + cmd);
        }
    }

}
