/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D.Double;

import javax.swing.border.AbstractBorder;

public class RoundedBorder extends AbstractBorder {
    private final Insets insets;
    private final BasicStroke stroke;
    private final RenderingHints hints;
    private final Color color;
    private final int thickness;
    private final int radii;
    private final int strokePad;

    public RoundedBorder(Color color, int thickness, int radii, int strokePad) {
        super();
        this.color = color;
        this.thickness = thickness;
        this.radii = radii;
        this.strokePad = strokePad;
        this.stroke = new BasicStroke((float) this.thickness);
        this.hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = this.radii + this.strokePad;
        this.insets = new Insets(pad, pad, pad, pad);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return this.insets;
     }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        return this.getBorderInsets(c);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;
        int bottomLineY = height - this.thickness;
        Double bubble = new Double((double) 0 + (double) this.strokePad,
                (double) 0 + (double) this.strokePad, (double) width - (double) this.thickness,
                (double) bottomLineY, (double) this.radii, (double) this.radii);
        Area area = new Area(bubble);
        g2d.setRenderingHints(this.hints);
        if (c.getParent() != null) {
            Rectangle rect = new Rectangle(0, 0, width, height);
            Area borderRegion = new Area(rect);
            borderRegion.subtract(area);
            g2d.setClip(borderRegion);
            g2d.setColor(c.getParent().getBackground());
            g2d.fillRect(0, 0, width, height);
            g2d.setClip(null);
        }

        g2d.setColor(this.color);
        g2d.setStroke(stroke);
        g2d.draw(area);
    }

}
