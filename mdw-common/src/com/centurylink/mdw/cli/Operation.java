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
