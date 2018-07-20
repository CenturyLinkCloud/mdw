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

import com.beust.jcommander.Parameters;

@Parameters(commandNames="stop", commandDescription="Stop the MDW server", separators="=")
public class Stop extends Run {

    @Override
    public Stop run(ProgressMonitor... progressMonitors) throws IOException {
        new Fetch(new URL("http://localhost:" + getServerPort() + "/" + getContextRoot()
                + "/Services/System/exit")).run().getData();
        return this;
    }
}
