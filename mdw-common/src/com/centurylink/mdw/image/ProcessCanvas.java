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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.UIManager;

import com.centurylink.mdw.draw.Diagram;
import com.centurylink.mdw.draw.Display;
import com.centurylink.mdw.model.Project;
import com.centurylink.mdw.model.workflow.Process;

public class ProcessCanvas extends JPanel {
    private Diagram diagram;
    private int zoom;
    private Project project;
    private Process process;

    private Display getInitDisplay() {
        return new Display(0, 0, ProcessCanvas.this.getSize().width - 1,
                ProcessCanvas.this.getSize().height);
    }

    public ProcessCanvas(Project project, Process process) {
        super((LayoutManager) (new BorderLayout()));
        this.project = project;
        this.process = process;
        this.zoom = 100;
        Display.Companion.setDEFAULT_COLOR(UIManager.getColor("EditorPane.foreground"));
        Display.Companion.setGRID_COLOR(Color.LIGHT_GRAY);
        Display.Companion.setOUTLINE_COLOR(UIManager.getColor("EditorPane.foreground"));
        Display.Companion.setSHADOW_COLOR(new Color(0, 0, 0, 50));
        Display.Companion.setMETA_COLOR(Color.GRAY);
        Display.Companion.setBACKGROUND_COLOR(UIManager.getColor("EditorPane.background"));
        this.setBackground(Display.Companion.getBACKGROUND_COLOR());
        this.setFocusable(true);
        this.setAutoscrolls(true);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        if (this.zoom != 100) {
            double scale = (double) this.zoom / 100.0D;
            g2d.scale(scale, scale);
        }
        final Diagram d = new Diagram(g2d, this.getInitDisplay(), project, process, new Implementors(project.getAssetRoot()), true);
        diagram = d;
        d.draw();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        super.paint(g);
    }

    @Override
    public Dimension getPreferredSize() {
        if (this.diagram != null) {
            return new Dimension(diagram.getDisplay().getW() + 25,
                    diagram.getDisplay().getH() + 25);
        }
        return super.getPreferredSize();
    }
}
