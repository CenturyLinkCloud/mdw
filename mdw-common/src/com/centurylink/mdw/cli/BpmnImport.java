/*
 * Copyright (C) 2018 CenturyLink, Inc.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.bpmn.BpmnProcessImporter;
import com.centurylink.mdw.procimport.ProcessImporter;

/**
 * Imports bpmn2 process
 */
@Parameters(commandNames = "bpmnimport", commandDescription = "Imports bpmn process", separators = "=")
public class BpmnImport extends Setup {

    @Parameter(names = "--process", description = "Process to be imported.")
    private File process;

    public File getProcess() {
        return process;
    }

    public void setProcess(File process) {
        this.process = process;
    }

    @Parameter(names = "--pkg", description = "Package to which process needs to be imported.")
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
        String imported = importer.importProcess(process);
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