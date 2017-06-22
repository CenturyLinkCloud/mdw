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
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.beust.jcommander.Parameters;

@Parameters(commandNames="version", commandDescription="MDW CLI Version")
public class Version {

    public void run() throws IOException {

        String name = getClass().getSimpleName() + ".class";
        String path = getClass().getResource(name).toString();
        if (path.startsWith("jar")) {
            String mf = path.substring(0, path.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            Manifest manifest = new Manifest(new URL(mf).openStream());
            Attributes attr = manifest.getMainAttributes();
            System.out.println("MDW CLI " + attr.getValue("MDW-Version") + " (" + attr.getValue("MDW-Build") + ")");
        }
    }

}
