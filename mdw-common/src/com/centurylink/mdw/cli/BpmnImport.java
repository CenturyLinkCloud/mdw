package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.bpmn.BpmnProcessImporter;
import com.centurylink.mdw.model.Yamlable;
import com.centurylink.mdw.procimport.ProcessImporter;
import com.centurylink.mdw.model.workflow.Process;

/**
 * Imports bpmn2 process
 */
@Parameters(commandNames = "bpmnimport", commandDescription = "Imports bpmn process", separators = "=")
public class BpmnImport extends Setup {

    @Parameter(names = "--process", description = "Process to be imported")
    private File process;
    public File getProcess() {
        return process;
    }
    public void setProcess(File process) {
        this.process = process;
    }

    @Parameter(names="--yaml", description="Import .proc file as YAML")
    private boolean yaml;
    public boolean isYaml() { return yaml; }
    public void setYaml(boolean yaml) { this.yaml = yaml; }

    @Parameter(names = "--pkg", description = "Package to which process needs to be imported")
    private String pkg;
    public String getPkg() {
        return pkg;
    }
    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    BpmnImport() {

    }

    public BpmnImport run(ProgressMonitor... monitors) throws IOException {
        ProcessImporter importer = getProcessImporter();
        Process proc = importer.importProcess(process);
        String imported = isYaml() ? Yamlable.toString(proc, 2) : proc.getJson().toString(2);
        String filePath = process.getPath();
        int index = filePath.lastIndexOf('\\');
        String procName = filePath.substring(index+1);
        Files.write(Paths.get(getAssetRoot() + "/" + pkg.replace('.', '/') + "/" + procName.replace("bpmn", "proc")), imported.getBytes());
        return this;
    }

    protected ProcessImporter getProcessImporter() {
        return new BpmnProcessImporter();
    }
}