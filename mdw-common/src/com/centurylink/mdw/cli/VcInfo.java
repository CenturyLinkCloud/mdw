package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VcInfo {

    public VcInfo(File localDir, String url, String user, String password, String branch) {
        this.localDir = localDir;
        this.url = url;
        this.user = user;
        this.password = password;
        this.branch = branch;
    }

    public VcInfo(File localDir, Props props) throws IOException {
        this.localDir = localDir;
        this.url = props.get(Props.Git.REMOTE_URL);
        this.user = props.get(Props.Git.USER);
        this.password = props.get(Props.Git.PASSWORD, false);
        this.branch = props.get(Props.Git.BRANCH, false);
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

    public static List<Dependency> getDependencies(String provider) {
        List<Dependency> dependencies = new ArrayList<>();
        if (provider.equalsIgnoreCase("git")) {
            dependencies.add(new Dependency("org/eclipse/jgit/org.eclipse.jgit/4.8.0.201706111038-r/org.eclipse.jgit-4.8.0.201706111038-r.jar", 2474713L));
            dependencies.add(new Dependency("org/eclipse/jgit/org.eclipse.jgit.pgm/4.8.0.201706111038-r/org.eclipse.jgit.pgm-4.8.0.201706111038-r.jar", 251079L));
            dependencies.add(new Dependency("org/eclipse/jgit/org.eclipse.jgit.http.apache/4.8.0.201706111038-r/org.eclipse.jgit.http.apache-4.8.0.201706111038-r.jar", 22168L));
            dependencies.add(new Dependency("org/eclipse/jgit/org.eclipse.jgit.lfs/4.8.0.201706111038-r/org.eclipse.jgit.lfs-4.8.0.201706111038-r.jar", 46166L));
            dependencies.add(new Dependency("org/eclipse/jgit/org.eclipse.jgit.ui/4.8.0.201706111038-r/org.eclipse.jgit.ui-4.8.0.201706111038-r.jar", 28476L));
            dependencies.add(new Dependency("org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar", 41203L));
            dependencies.add(new Dependency("org/slf4j/slf4j-simple/1.7.25/slf4j-simple-1.7.25.jar", 15257L));
            dependencies.add(new Dependency("com/jcraft/jsch/0.1.54/jsch-0.1.54.jar", 280515L));
            dependencies.add(new Dependency("args4j/args4j/2.0.15/args4j-2.0.15.jar",  155379L));
            dependencies.add(new Dependency("commons-logging/commons-logging/1.2/commons-logging-1.2.jar", 61829L));
            dependencies.add(new Dependency("org/apache/httpcomponents/httpcore/4.4.7/httpcore-4.4.7.jar", 325123L));
            dependencies.add(new Dependency("org/apache/httpcomponents/httpclient/4.5.3/httpclient-4.5.3.jar", 747794L));
        }
        return dependencies;
    }

    public static String getVersionControlClass(String provider) {
        if (provider.equalsIgnoreCase("git"))
            return "com.centurylink.mdw.git.VersionControlGit";
        return null;
    }
}
