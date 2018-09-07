/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.image;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.centurylink.mdw.export.ProcessExporter;
import com.centurylink.mdw.model.workflow.Process;

public class PngProcessExporter implements ProcessExporter{
    private String output;

    public void setOutput(String output) {
        this.output = output;
    }

    @Override
    public String export(Process process) throws IOException {
        ImageIO.write(new ImageExportHelper().printImage(process), "png", new File(output));
        return null;
    }

}
