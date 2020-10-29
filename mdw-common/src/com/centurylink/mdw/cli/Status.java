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
