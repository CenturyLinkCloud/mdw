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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.beust.jcommander.Parameters;

@Parameters(commandDescription="Update an MDW project")
public class Update {

    private File projectDir;
    public File getProjectDir() { return this.projectDir; }
    protected void setProjectDir(File projectDir) { this.projectDir = projectDir; }

    public Update(File projectDir) {
        this.projectDir = projectDir;
    }

    Update() {
        // cli use only
    }

    public void run() throws IOException {
        File propFile = new File(projectDir + "/config/mdw.properties");
        if (!propFile.exists())
            throw new IOException("Missing: " + propFile.getAbsolutePath());
        Properties props = new Properties();
        props.load(new FileInputStream(propFile));
        String discoveryUrl = props.getProperty("mdw.discovery.url");
        System.out.println("Discovery URL: " + discoveryUrl);
    }

}