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

import com.beust.jcommander.Parameters;

@Parameters(commandDescription="Update an MDW project")
public class Update extends Common {

    public Update(File projectDir) {
        this.projectDir = projectDir;
    }

    public Update(Common cloneFrom) {
        super(cloneFrom);
    }

    Update() {
        // cli use only
    }

    public void run() throws IOException {
        String discoveryUrl = getProperty("mdw.discovery.url");

        System.out.println("Discovering assets from: " + discoveryUrl);
        System.out.println("Base asset packages:");
        if (getBaseAssetPackages() == null) {
            initBaseAssetPackages();
        }
        for (String pkg : getBaseAssetPackages()) {
            System.out.println("  - " + pkg);
        }
    }

}