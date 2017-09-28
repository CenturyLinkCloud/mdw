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
import java.util.List;

import com.centurylink.mdw.dataaccess.file.VersionControlGit;

/**
 * Uses reflection to allow arbitrary Git commands.
 */
public class Git implements Operation {

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

        // TODO: arbitrary git commands (eg: "mdw git status")
        invokeVersionControl();
        return this;
    }

    private void invokeVersionControl() throws IOException {
        try {
            VersionControlGit versionControl = new VersionControlGit();
            versionControl.connect(vcInfo.getUrl(), vcInfo.getUser(), vcInfo.getPassword(), vcInfo.getLocalDir());
            List<Class<?>> types = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            if (params != null) {
                for (Object param : params) {
                    types.add(param.getClass());
                    args.add(param);
                }
            }
            Method method = VersionControlGit.class.getMethod(command, types.toArray(new Class<?>[0]));
            method.invoke(versionControl, args.toArray(new Object[0]));
        }
        catch (Exception ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

}
