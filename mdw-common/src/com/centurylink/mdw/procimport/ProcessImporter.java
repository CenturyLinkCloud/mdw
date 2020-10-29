package com.centurylink.mdw.procimport;

import java.io.File;
import java.io.IOException;
import com.centurylink.mdw.model.workflow.Process;

public interface ProcessImporter {
    Process importProcess(File from) throws IOException;
}
