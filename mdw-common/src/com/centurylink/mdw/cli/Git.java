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
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.dataaccess.VersionControl;

/**
 * Uses reflection to avoid hard dependency on JGit.
 */
public class Git implements Operation {

    private static final String VERSION_CONTROL = "com.centurylink.mdw.dataaccess.file.VersionControlGit";

    private static final Map<String,Long> DEPENDENCIES = new HashMap<>();
    static {
        DEPENDENCIES.put("org/eclipse/jgit/org.eclipse.jgit/4.8.0.201706111038-r/org.eclipse.jgit-4.8.0.201706111038-r.jar", 2474713L);
        DEPENDENCIES.put("org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar", 41203L);
        DEPENDENCIES.put("org/slf4j/slf4j-simple/1.7.25/slf4j-simple-1.7.25.jar", 15257L);
        DEPENDENCIES.put("com/jcraft/jsch/0.1.54/jsch-0.1.54.jar", 280515L);
    };

    private String mavenRepoUrl;
    private VcInfo vcInfo;
    private String command;
    private Object[] params;

    public Git(String mavenRepoUrl, VcInfo vcInfo, String command, Object...params) {
        this.mavenRepoUrl = mavenRepoUrl;
        if (!this.mavenRepoUrl.endsWith("/"))
            this.mavenRepoUrl += "/";

        this.vcInfo = vcInfo;
        this.command = command;
        this.params = params;
    }

    @Override
    public Git run(ProgressMonitor... progressMonitors) throws IOException {

        String mdwHome = System.getenv("MDW_HOME");
        if (mdwHome == null)
            throw new IOException("Missing environment variable: MDW_HOME");
        File mdwDir = new File(mdwHome);
        if (!mdwDir.isDirectory())
            throw new IOException("MDW_HOME is not a directory: " + mdwDir.getAbsolutePath());
        File libDir = new File (mdwDir + "/lib");
        if (!libDir.exists() && !libDir.mkdirs())
            throw new IOException("Cannot create lib dir: " + libDir.getAbsolutePath());

        for (String dep : DEPENDENCIES.keySet()) {
            File depJar = new File(libDir + "/" + dep.substring(dep.lastIndexOf('/')));
            if (!depJar.exists()) {
                System.out.println("Downloading " + depJar + "...");
                new Download(new URL(mavenRepoUrl + dep), depJar, DEPENDENCIES.get(dep)).run(progressMonitors);
            }
        }

        try {
            invokeVersionControl();
        }
        catch (ReflectiveOperationException ex) {
            throw new IOException(ex.getMessage(), ex);
        }

        return this;
    }

    private void invokeVersionControl()
            throws ReflectiveOperationException {
        Class<? extends VersionControl> cls = Class.forName(VERSION_CONTROL).asSubclass(VersionControl.class);
        Object vcGit = cls.newInstance();
        Method connect = cls.getMethod("connect", String.class, String.class, String.class, File.class);
        connect.invoke(vcGit, vcInfo.getUrl(), vcInfo.getUser(), vcInfo.getPassword(), vcInfo.getLocalDir());
        Method exists = cls.getMethod("exists");
        Object gitExists = exists.invoke(vcGit);
        if (!"true".equalsIgnoreCase(gitExists.toString())) {
            System.out.println("Git local not found: " + vcInfo.getLocalDir() + " -- cloning from " + vcInfo.getUrl() + "...");
            cls.getMethod("cloneNoCheckout", boolean.class).invoke(vcGit, true);
        }

        // run the requested command
        List<Class<?>> types = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (params != null) {
            for (Object param : params) {
                types.add(param.getClass());
                args.add(param);
            }
        }
        Method method = cls.getMethod(command, types.toArray(new Class<?>[0]));
        method.invoke(vcGit, args.toArray(new Object[0]));
    }

}
