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
package com.centurylink.mdw.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.centurylink.mdw.export.ExportHelper;
import com.centurylink.mdw.model.Project;
import com.centurylink.mdw.model.workflow.Process;

public class ImageExportHelper extends ExportHelper {

    public ImageExportHelper(Project project) {
        super(project);
    }

    public BufferedImage printImage(Process process) {
        int hMargin = 72;
        int vMargin = 72;
        Dimension graphsize = getGraphSize(process);
        BufferedImage image = new BufferedImage(graphsize.width + hMargin,
                graphsize.height + vMargin, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, image.getWidth(), image.getHeight());
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        ProcessCanvas canvas = new ProcessCanvas(project, process);
        Color bgsave = canvas.getBackground();
        canvas.paintComponent(g2d);
        canvas.setBackground(bgsave);
        g2d.dispose();
        Runtime r = Runtime.getRuntime();
        r.gc();
        return image;
    }
}
