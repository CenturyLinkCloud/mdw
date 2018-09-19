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
import java.util.List;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.Project;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;

public class ImageExportHelper {

    private Project project;

    public ImageExportHelper(Project project) {
        this.project = project;
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

    public Dimension getGraphSize(Process process) {
        int w = 0;
        int h = 0;
        List<Activity> activities = process.getActivities();
        for (Activity act : activities) {
            String[] attrs = act.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO).split(",");
            w = getWidth(attrs, w);
            h = getHeight(attrs, h);
        }
        List<Process> subProcesses = process.getSubprocesses();
        for (Process subProc : subProcesses) {
            String[] attrs = subProc.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO)
                    .split(",");
            w = getWidth(attrs, w);
            h = getHeight(attrs, h);
        }
        return new Dimension(w, h);
    }

    private int getWidth(String[] attrs, int w) {
        int localW = Integer.parseInt(attrs[0].substring(2))
                + Integer.parseInt(attrs[2].substring(2));
        if (localW > w)
            w = localW;
        return w;
    }

    private int getHeight(String[] attrs, int h) {
        int localH = Integer.parseInt(attrs[1].substring(2))
                + Integer.parseInt(attrs[3].substring(2));
        if (localH > h)
            h = localH;
        return h;
    }

}
