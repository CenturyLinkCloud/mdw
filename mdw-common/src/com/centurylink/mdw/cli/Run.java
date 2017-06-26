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

import com.beust.jcommander.Parameter;

public class Run {

    private File projectDir;
    public File getProjectDir() { return this.projectDir; }
    protected void setProjectDir(File projectDir) { this.projectDir = projectDir; }

    @Parameter(names="--binaries-url", description="MDW Binaries")
    private String binariesUrl = "https://github.com/CenturyLinkCloud/mdw/releases";
    public String getBinariesUrl() { return binariesUrl; }
    public void setBinariesUrl(String url) { this.binariesUrl = url; }

    @Parameter(names="--vmArgs", description="Java VM Arguments (enclose in quotes)")
    private String vmArgs;
    public String getVmArgs() { return vmArgs; }
    public void setVmArgs(String args) { this.vmArgs = args; }

    Run() {
      // cli use only
    }

    public Run(File projectDir) {
        this.projectDir = projectDir;
    }
}
