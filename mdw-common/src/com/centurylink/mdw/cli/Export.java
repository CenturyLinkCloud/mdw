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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.bpmn.BpmnProcessExporter;
import com.centurylink.mdw.export.ProcessExporter;
import com.centurylink.mdw.html.HtmlProcessExporter;
import com.centurylink.mdw.image.PngProcessExporter;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Project;
import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.pdf.PdfProcessExporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Exports process into specific format. If --format is specified with html/png,
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

    @Parameter(names = "--format", description = "Format to be exported (bpmn, png or html)")
    private String format;
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

        int index = process.lastIndexOf('/');
        String pkg = process.substring(0, index);
        String pkgFile = getAssetRoot() + "/" + pkg.replace('.', '/') + "/";
        String procName = process.substring(index + 1);
        String content = new String(Files.readAllBytes(Paths.get(pkgFile + procName)));

        if (output == null) {
            output = new File(pkgFile + procName.substring(0, procName.length() - 5) + "." + format);
        }
        else {
            if (format == null) {
                int lastDot = output.getName().lastIndexOf('.');
                if (lastDot > 0 && lastDot < output.getName().length() - 1) {
                    format = output.getName().substring(lastDot + 1);
                }
                else {
                    format = "bpmn";
                }
            }
            File fileDir = output.getParentFile();
            if (!fileDir.exists() && !fileDir.mkdirs())
                throw new IOException("Unable to create directory: " + fileDir);
        }

        ProcessExporter exporter = getProcessExporter();
        List<Dependency> dependencies = exporter.getDependencies();
        if (dependencies != null) {
            for (Dependency dependency : dependencies) {
                dependency.run(monitors);
            }
        }

        Process proc = new Process(new JsonObject(content));
        proc.setName(procName.substring(0, procName.length() - 5));

        if (exporter instanceof PdfProcessExporter) {
            ((PdfProcessExporter) exporter).setOutputDir(output);
        }else if (exporter instanceof HtmlProcessExporter) {
            ((HtmlProcessExporter) exporter).setOutputDir(output.getParentFile());
        }

        byte[] exported = exporter.export(proc);

        if (exported != null)
            Files.write(Paths.get(output.getPath()), exported);

        return this;
    }

    protected ProcessExporter getProcessExporter() throws IOException {
        if ("bpmn2".equals(format) || "bpmn".equals(format))
            return new BpmnProcessExporter();
        else {
            final Setup setup = this;
            Project project = new Project() {
                public File getAssetRoot() {
                    try {
                        return setup.getAssetRoot();
                    }
                    catch (IOException ex) {
                        throw new RuntimeException(ex.getMessage(), ex);
                    }
                }
                public String getHubRootUrl() {
                    try {
                        return new Props(setup).get(Props.DISCOVERY_URL);
                    }
                    catch (IOException ex) {
                        throw new RuntimeException(ex.getMessage(), ex);
                    }
                }
                public MdwVersion getMdwVersion() {
                    try {
                        return new MdwVersion(setup.getMdwVersion());
                    }
                    catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
            if ("html".equals(format))
                return new HtmlProcessExporter(project);
            else if ("png".equals(format))
                return new PngProcessExporter(project);
            else if ("pdf".equals(format))
                return new PdfProcessExporter(project);
        }

        return null;
    }
}
