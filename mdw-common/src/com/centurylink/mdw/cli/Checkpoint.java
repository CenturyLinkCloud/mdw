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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.dataaccess.VersionControl;

/**
 * Capture current asset version info to DB.
 */
public class Checkpoint implements Operation {

    private static final Map<String,Long> DEPENDENCIES = new HashMap<>();
    static {
        DEPENDENCIES.put("org/mariadb/jdbc/mariadb-java-client/1.2.2/mariadb-java-client-1.2.2.jar", 300713L);
    };

    private String mavenRepoUrl;
    private VcInfo vcInfo;
    private File assetRoot;
    private DbInfo dbInfo;
    private VersionControl versionControl;
    private String commit;

    public Checkpoint(String mavenRepoUrl, VcInfo vcInfo, String assetLoc, DbInfo dbInfo) {
        this.mavenRepoUrl = mavenRepoUrl;
        this.vcInfo = vcInfo;
        this.assetRoot = new File(vcInfo.getLocalDir() + "/" + assetLoc);
        this.dbInfo = dbInfo;
    }

    @Override
    public Checkpoint run(ProgressMonitor... progressMonitors) throws IOException {

        for (String dep : DEPENDENCIES.keySet()) {
            new Dependency(mavenRepoUrl, dep, DEPENDENCIES.get(dep)).run(progressMonitors);
        }

        Git git = new Git(mavenRepoUrl, vcInfo, "getCommit");
        git.run(progressMonitors); // connect
        commit = (String) git.getResult();
        versionControl = git.getVersionControl();

        for (AssetRef ref : getRefs()) {
            // TODO: insert in db
            System.out.println("   " + ref);
        }

        return this;
    }

    public List<AssetRef> getRefs() throws IOException {
        return getRefs(assetRoot);
    }

    public List<AssetRef> getRefs(File dir) throws IOException {
        List<AssetRef> refs = new ArrayList<>();
        String pkgName = null;
        if (new File(dir + "/.mdw").isDirectory()) {
            pkgName = dir.getAbsolutePath().substring(assetRoot.getAbsolutePath().length())
                    .replace('/', '.').replace('\\', '.');
        }
        for (File file : dir.listFiles()) {
            if (pkgName != null && file.isFile()) {
                AssetRevision rev = versionControl.getRevision(file);
                if (rev == null) {
                    rev = new AssetRevision();
                    rev.setVersion(0);
                    rev.setModDate(new Date());
                }
                // logical path
                String name = pkgName + "/" + file.getName() + " " + rev.getFormattedVersion();
                refs.add(new AssetRef(name, versionControl.getId(new File(name)), commit));
            }
            if (file.isDirectory()) {
                refs.addAll(getRefs(file));
            }
        }
        return refs;
    }
}
