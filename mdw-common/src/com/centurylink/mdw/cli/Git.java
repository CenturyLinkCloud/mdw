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
    private Object result;
    public Object getResult() { return result; }

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
            versionControl.connect(vcInfo.getUrl(), vcInfo.getUser(), vcInfo.getPassword(), vcInfo.getLocalDir());
            if (!versionControl.exists()) {
                System.out.println("Git local not found: " + vcInfo.getLocalDir() + " -- cloning from " + vcInfo.getUrl() + "...");
                vcClass.getMethod("cloneNoCheckout", boolean.class).invoke(versionControl, true);
            }
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
        Method method = vcClass.getMethod(command, types.toArray(new Class<?>[0]));
        result = method.invoke(versionControl, args.toArray(new Object[0]));
    }

}
