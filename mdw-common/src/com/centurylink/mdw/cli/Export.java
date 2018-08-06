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
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.bpmn.BpmnProcessExporter;
import com.centurylink.mdw.bpmn.HtmlProcessExporter;
import com.centurylink.mdw.export.ProcessExporter;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.workflow.Process;

/**
 * Exports process into specific format. If --format is specified wiht html/png,
 * exports into html/png format otherwise, exports into bpmn2 format.
 */
@Parameters(commandNames = "export", commandDescription = "Export process into supported formats", separators = "=")
public class Export extends Setup {
    @Parameter(names = "--process", description = "Process to be exported.")
    private String process;

    public String getProcess() {
        return process;
    }

    public void setProcess(String proc) {
        this.process = proc;
    }

    @Parameter(names = "--format", description = "Format to be exported")
    private String format = "bpmn2";

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Parameter(names = "--output", description = "Filename of the exported output")
    private File output;

    public File getOutput() {
        return output;
    }

    Export() {

    }

    public Export run(ProgressMonitor... monitors) throws IOException {

        ProcessExporter exporter = getProcessExporter();
        List<Dependency> dependencies = exporter.getDependencies();
        if (dependencies != null) {
            for (Dependency dependency : dependencies) {
                dependency.run(monitors);
            }
        }
        int index = process.lastIndexOf('/');
        String pkg = process.substring(0, index);
        String pkgFile = getAssetRoot() + "/" + pkg.replace('.', '/') + "/";
        String procName = process.substring(index + 1);
        String content = new String(Files.readAllBytes(Paths.get(pkgFile + procName)));
        Process proc = new Process(new JsonObject(content));
        proc.setName(procName.substring(0, procName.length() - 5));

        if (output == null) {
            output = new File(
                    pkgFile + procName.substring(0, procName.length() - 5) + "." + format);
        }
        else {
            File fileDir = output.getParentFile();
            if (!fileDir.mkdirs())
                throw new IOException("Unable to create directory: " + fileDir);
        }

        if (exporter instanceof HtmlProcessExporter)
           ((HtmlProcessExporter)exporter).setOutput(output.getPath());

        String exported = exporter.export(proc);

        Files.write(Paths.get(output.getPath()), exported.getBytes());

        return this;
    }

    protected ProcessExporter getProcessExporter() {
        if ("bpmn2".equals(format))
            return new BpmnProcessExporter();
        else if ("html".equals(format))
            return new HtmlProcessExporter();

        return null;
    }

    public static void main(String[] args) throws IOException {
        Export exp = new Export();
        exp.setConfigLoc("c:\\MdwBootTest\\config");
        exp.setAssetLoc("c:\\MdwBootTest\\assets");
        exp.setFormat("html");
        exp.setProcess("com\\centurylink\\mdw\\tests\\workflow/ActivityException.proc");
        exp.run(null);
    }
}
