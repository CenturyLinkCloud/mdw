package com.centurylink.mdw.bpmn;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

import com.centurylink.mdw.procimport.ProcessImporter;
import com.centurylink.mdw.model.workflow.Process;

public class BpmnProcessImporter implements ProcessImporter{
    @Override
    public Process importProcess(File process) throws IOException {
        return new BpmnImportHelper().importProcess(process);
    }
}
