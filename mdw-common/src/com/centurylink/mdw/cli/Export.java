/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.json.JSONObject;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.model.workflow.Process;

/**
 * Exports process into bpmn2 format.
 * If --format is specified wiht html/png, exports into html/png format
 * otherwise, exports into bpmn2 format.
 */
@Parameters(commandNames = "export", commandDescription = "Export process into bpmn2 format", separators = "=")
public class Export extends Setup {
    @Parameter(names = "--process", description = "Process name to be exported.")
    private String process;

    public String getProcess() {
        return process;
    }

    @Parameter(names = "--format", description = "Format to which process needs to be exported")
    private String format = "bpmn";

    public String getFormat() {
        return format;
    }

    @Parameter(names = "--file-name", description = "Filename of the exported process")
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    Export() {

    }

    public Export run(ProgressMonitor... monitors) throws IOException {
        new Dependency(
                "https://github.com/CenturyLinkCloud/mdw/blob/master/mdw/libs/bpmn-schemas.jar?raw=true",
                "./bpmn-schemas.jar", 2011745L).run(monitors);
        new Dependency(
                getReleasesUrl(),
                "io/limberest/limberest/1.2.4/limberest-1.2.4.jar", 144562L).run(monitors);
        new Dependency(
                getReleasesUrl(),
                "org/apache/xmlbeans/xmlbeans/2.4.0/xmlbeans-2.4.0.jar", 2694049L).run(monitors);
        int index = process.lastIndexOf('/');
        String pkg = process.substring(0, index);
        String pkgFile = getAssetRoot() + "/" + pkg.replace('.', '/') + "/";
        String procName = process.substring(index+1);
        String content = new String(
                Files.readAllBytes(Paths.get(pkgFile + procName)));
        JSONObject json = new JSONObject(content);
        Process proc = new Process(json);
        proc.setName(procName.substring(0, procName.length() - 5));
        BpmnHelper helper = new BpmnHelper();
        Properties props = new Properties();
        props.load(new FileInputStream(new File(pkgFile + ".mdw/versions")));
        proc.setVersion(Integer.parseInt(props.getProperty(procName)));
        if (fileName == null)
            fileName = pkgFile + procName.substring(0, procName.length() - 5);
        else
        {
            int slashIndex = fileName.lastIndexOf('/');
            if (slashIndex > 0) {
                File fileDir = new File(fileName.substring(0, slashIndex));
                if (!fileDir.mkdirs())
                    throw new IOException("Unable to create file directory: " + fileDir);
            }
        }
        helper.exportProcess(proc, fileName + "." + format);
        return this;
    }
}
