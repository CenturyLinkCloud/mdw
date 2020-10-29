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
