package com.centurylink.mdw.export;

import java.io.IOException;
import java.util.List;

import com.centurylink.mdw.cli.Dependency;
import com.centurylink.mdw.model.workflow.Process;

public interface ProcessExporter {
    byte[] export(Process process) throws IOException;

    default List<Dependency> getDependencies() { return null; }
}
