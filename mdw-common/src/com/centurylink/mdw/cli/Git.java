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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.dataaccess.VersionControl;

/**
 * Uses reflection to avoid hard dependency on JGit.
 */
@Parameters(commandNames="git", commandDescription="Git commands")
public class Git implements Operation {

    public static void main(String[] args) throws IOException {
        JCommander cmd = new JCommander();
        Git git = new Git();
        cmd.addCommand("git", git);
        String[] gitArgs = new String[args.length + 1];
        gitArgs[0] = "git";
        gitArgs[1] = "args";
        for (int i = 1; i < args.length; i++) {
            gitArgs[i+1] = args[i];
        }
        cmd.parse(gitArgs);
        git.run(Main.getMonitor());
    }

    private static final String VERSION_CONTROL = "com.centurylink.mdw.dataaccess.file.VersionControlGit";

    private static final Map<String,Long> DEPENDENCIES = new HashMap<>();
    static {
        DEPENDENCIES.put("org/eclipse/jgit/org.eclipse.jgit/4.8.0.201706111038-r/org.eclipse.jgit-4.8.0.201706111038-r.jar", 2474713L);
        DEPENDENCIES.put("org/eclipse/jgit/org.eclipse.jgit.pgm/4.8.0.201706111038-r/org.eclipse.jgit.pgm-4.8.0.201706111038-r.jar", 251079L);
        DEPENDENCIES.put("org/eclipse/jgit/org.eclipse.jgit.http.apache/4.8.0.201706111038-r/org.eclipse.jgit.http.apache-4.8.0.201706111038-r.jar", 22168L);
        DEPENDENCIES.put("org/eclipse/jgit/org.eclipse.jgit.lfs/4.8.0.201706111038-r/org.eclipse.jgit.lfs-4.8.0.201706111038-r.jar", 46166L);
        DEPENDENCIES.put("org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar", 41203L);
        DEPENDENCIES.put("org/slf4j/slf4j-simple/1.7.25/slf4j-simple-1.7.25.jar", 15257L);
        DEPENDENCIES.put("com/jcraft/jsch/0.1.54/jsch-0.1.54.jar", 280515L);
        DEPENDENCIES.put("args4j/args4j/2.0.15/args4j-2.0.15.jar",  155379L);
        DEPENDENCIES.put("commons-logging/commons-logging/1.2/commons-logging-1.2.jar", 61829L);
        DEPENDENCIES.put("org/apache/httpcomponents/httpcore/4.4.7/httpcore-4.4.7.jar", 325123L);
        DEPENDENCIES.put("org/apache/httpcomponents/httpclient/4.5.3/httpclient-4.5.3.jar", 747794L);
    };

    private String mavenRepoUrl;
    private VcInfo vcInfo;
    private String command;
    private Object[] params;
    private Object result;
    public Object getResult() { return result; }

    /**
     * Arguments for pass-thru git command.
     */
    @Parameter(names="args", description="pass-thru jgit arguments", variableArity = true)
    public List<String> args = new ArrayList<>();

    // for CLI
    Git() {
        mavenRepoUrl = "http://repo.maven.apache.org/maven2";
        command = "git";
    }

    public Git(String mavenRepoUrl, VcInfo vcInfo, String command, Object...params) {
        this.mavenRepoUrl = mavenRepoUrl;
        this.vcInfo = vcInfo;
        this.command = command;
        this.params = params;
    }

    @Override
    public Git run(ProgressMonitor... progressMonitors) throws IOException {

        for (String dep : DEPENDENCIES.keySet()) {
            new Dependency(mavenRepoUrl, dep, DEPENDENCIES.get(dep)).run(progressMonitors);
        }

        if (command != null) {
            try {
                invokeVersionControl();
            }
            catch (ReflectiveOperationException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }

        return this;
    }

    private Class<? extends VersionControl> vcClass;
    private VersionControl versionControl;
    VersionControl getVersionControl() { return versionControl; }

    private void invokeVersionControl()
            throws ReflectiveOperationException, IOException {
        if (vcClass == null) {
            vcClass = Class.forName(VERSION_CONTROL).asSubclass(VersionControl.class);
            versionControl = vcClass.newInstance();
            if (!command.equals("git")) {
                versionControl.connect(vcInfo.getUrl(), vcInfo.getUser(), vcInfo.getPassword(), vcInfo.getLocalDir());
                if (!versionControl.exists()) {
                    System.out.println("Git local not found: " + vcInfo.getLocalDir() + " -- cloning from " + vcInfo.getUrl() + "...");
                    vcClass.getMethod("cloneNoCheckout", boolean.class).invoke(versionControl, true);
                }
            }
        }

        // run the requested command
        List<Class<?>> types = new ArrayList<>();
        List<Object> methodArgs = new ArrayList<>();
        if (params != null) {
            for (Object param : params) {
                types.add(param.getClass());
                methodArgs.add(param);
            }
        }
        Method method;
        if (command.equals("git")) {
            method = vcClass.getMethod("git", String[].class);
            result = method.invoke(versionControl, (Object)this.args.toArray(new String[0]));
        }
        else {
            method = vcClass.getMethod(command, types.toArray(new Class<?>[0]));
            result = method.invoke(versionControl, methodArgs.toArray(new Object[0]));
        }
    }

}
