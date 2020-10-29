package com.centurylink.mdw.image;

import com.centurylink.mdw.draw.Diagram;
import com.centurylink.mdw.draw.Display;
import com.centurylink.mdw.draw.model.DrawProps;
import com.centurylink.mdw.model.project.Project;
import com.centurylink.mdw.model.workflow.Process;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ProcessCanvas extends JPanel {
    private Diagram diagram;
    private int zoom;
    private Project project;
    private Process process;
    private Color backgroundColor;

    private Display getInitDisplay() {
        return new Display(0, 0, ProcessCanvas.this.getSize().width - 1,
                ProcessCanvas.this.getSize().height);
    }

    public ProcessCanvas(Project project, Process process) {
        super(new BorderLayout());
        this.project = project;
        this.process = process;
        this.zoom = 100;
        this.setBackground(Color.WHITE);
        this.setFocusable(true);
        this.setAutoscrolls(true);
    }

    public void prepare() {
        Display.Companion.setDEFAULT_COLOR(new Color(29, 29, 29));
        Display.Companion.setOUTLINE_COLOR(Color.BLACK);
        backgroundColor = Display.Companion.getBACKGROUND_COLOR();
        Display.Companion.setBACKGROUND_COLOR(Color.WHITE);
    }

    public void dispose() {
        Display.Companion.setDEFAULT_COLOR(UIManager.getColor("EditorPane.foreground"));
        Display.Companion.setOUTLINE_COLOR(UIManager.getColor("EditorPane.foreground"));
        Display.Companion.setBACKGROUND_COLOR(backgroundColor);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        if (this.zoom != 100) {
            double scale = (double) this.zoom / 100.0D;
            g2d.scale(scale, scale);
        }
        try {
            final Diagram d = new Diagram(g2d, getInitDisplay(), project, process, project.getActivityImplementors(),
                    new DrawProps(true, false, null));
            diagram = d;
            d.draw();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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
