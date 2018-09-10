/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

import com.centurylink.mdw.canvas.ProcessCanvas;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;

public class ImageExportHelper {

    public BufferedImage printImage(Process processVO) {
        int hMargin = 72;
        int vMargin = 72;
        Dimension graphsize = getGraphSize(processVO);
        BufferedImage image = new BufferedImage(graphsize.width + hMargin,
                graphsize.height + vMargin, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, image.getWidth(), image.getHeight());
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        ProcessCanvas canvas = new ProcessCanvas(processVO);
        canvas.setSize(graphsize);
        Color bgsave = canvas.getBackground();
        canvas.paintComponent(g2d);
        canvas.setBackground(bgsave);
        g2d.dispose();
        Runtime r = Runtime.getRuntime();
        r.gc();
        return image;
    }

    public Dimension getGraphSize(Process processVO) {
        int w = 0;
        int h = 0;
        List<Activity> activities = processVO.getActivities();
        for (Activity act : activities) {
            String[] attrs = act.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO).split(",");
            w = getWidth(attrs, w);
            h = getHeight(attrs, h);
        }
        List<Process> subProcesses = processVO.getSubprocesses();
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
