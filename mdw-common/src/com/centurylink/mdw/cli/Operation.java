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
import java.io.PrintStream;

public interface Operation {

    String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2";
    String SONATYPE_URL = "https://oss.sonatype.org/service/local/artifact/maven";

    /**
     * @param progressMonitors only a single long-running step is permitted
     * per operation since monitors cannot be reset.
     */
    Operation run(ProgressMonitor... progressMonitors) throws IOException;

    default PrintStream getOut() { return System.out; }
    default void setOut(PrintStream out) { }

    default PrintStream getErr() { return System.err; }
    default void setErr(PrintStream err) { }
}
