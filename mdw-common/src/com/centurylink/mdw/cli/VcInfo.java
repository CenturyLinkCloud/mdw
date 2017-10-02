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
import java.util.HashMap;
import java.util.Map;

public class VcInfo {

    public VcInfo(String url, String user, String password, File localDir, String branch) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.localDir = localDir;
        this.branch = branch;
    }

    private String url;
    public String getUrl() { return url; }

    private String user;
    public String getUser() { return user; }

    private String password;
    String getPassword() { return password; }

    private File localDir;
    public File getLocalDir() { return localDir; }

    private String branch;
    public String getBranch() { return branch; }

    public String toString() {
        return "url=" + url + ", user=" + user + ", localDir=" + localDir + ", branch=" + branch;
    }

    public static Map<String,Long> getDependencies(String provider) {
        Map<String,Long> dependencies = new HashMap<>();
        if (provider.equalsIgnoreCase("git")) {
            dependencies.put("org/eclipse/jgit/org.eclipse.jgit/4.8.0.201706111038-r/org.eclipse.jgit-4.8.0.201706111038-r.jar", 2474713L);
            dependencies.put("org/eclipse/jgit/org.eclipse.jgit.pgm/4.8.0.201706111038-r/org.eclipse.jgit.pgm-4.8.0.201706111038-r.jar", 251079L);
            dependencies.put("org/eclipse/jgit/org.eclipse.jgit.http.apache/4.8.0.201706111038-r/org.eclipse.jgit.http.apache-4.8.0.201706111038-r.jar", 22168L);
            dependencies.put("org/eclipse/jgit/org.eclipse.jgit.lfs/4.8.0.201706111038-r/org.eclipse.jgit.lfs-4.8.0.201706111038-r.jar", 46166L);
            dependencies.put("org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar", 41203L);
            dependencies.put("org/slf4j/slf4j-simple/1.7.25/slf4j-simple-1.7.25.jar", 15257L);
            dependencies.put("com/jcraft/jsch/0.1.54/jsch-0.1.54.jar", 280515L);
            dependencies.put("args4j/args4j/2.0.15/args4j-2.0.15.jar",  155379L);
            dependencies.put("commons-logging/commons-logging/1.2/commons-logging-1.2.jar", 61829L);
            dependencies.put("org/apache/httpcomponents/httpcore/4.4.7/httpcore-4.4.7.jar", 325123L);
            dependencies.put("org/apache/httpcomponents/httpclient/4.5.3/httpclient-4.5.3.jar", 747794L);
        }
        return dependencies;
    }

    public static String getVersionControlClass(String provider) {
        if (provider.equalsIgnoreCase("git"))
            return "com.centurylink.mdw.dataaccess.file.VersionControlGit";
        return null;
    }
}
