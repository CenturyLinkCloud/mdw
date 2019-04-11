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

import com.beust.jcommander.Parameters;

@Parameters(commandNames="status", commandDescription="Project status", separators="=")
public class Status extends Setup {

    @Override
    public Status run(ProgressMonitor... progressMonitors) throws IOException {
        status();
        return this;
    }

    @Override
    protected boolean needsConfig() { return false; }
}
