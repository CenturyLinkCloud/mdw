/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;

import javax.swing.ImageIcon;

public abstract class Shape implements Drawable {
    public Graphics2D g2d;
    public Display display;
    /**
     * @return the display
     */
    public Display getDisplay() {
        return display;
    }

    /**
     * @param display the display to set
     */
    public void setDisplay(Display display) {
        this.display = display;
    }

    public static final int MIN_SIZE = 4;

    public Shape(Graphics2D g2d, Display display) {
        this.g2d = g2d;
        this.display = display;
    }

    public void drawRect(int x, int y, int w, int h, Color border, Color fill, Integer radius) {
        this.g2d.setColor(border);
        if (radius != null) {
            this.g2d.drawRoundRect(x, y, w, h, radius, radius);
            if (fill != null) {
                this.g2d.setPaint((Paint) fill);
                this.g2d.fillRoundRect(x, y, w, h, radius, radius);
            }
        }
        else {
            this.g2d.drawRect(x, y, w, h);
            if (fill != null) {
                this.g2d.setPaint((Paint) fill);
                this.g2d.fillRect(x, y, w, h);
            }
        }

        this.g2d.setColor(Display.DEFAULT_COLOR);
        this.g2d.setPaint((Paint) Display.DEFAULT_COLOR);
    }

    public void drawIcon(ImageIcon icon, int x, int y, float opacity) {
        this.drawImage(icon.getImage(), x, y, opacity);
    }

    public void drawImage(Image image, int x, int y, float opacity) {
        Composite origComposite = g2d.getComposite();
        if (opacity != 1.0F) {
            this.g2d.setComposite(
                    (Composite) AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }

        this.g2d.drawImage(image, x, y, null);
        this.g2d.setComposite(origComposite);
    }

    public void drawOval(int x, int y, int w, int h, Color fill, Color fadeTo, Graphics2D g2d) {
        if (fill != null) {
            g2d.setColor(Display.OUTLINE_COLOR);
            g2d.fillOval(x, y, w, h);
            if (fadeTo != null) {
                g2d.setPaint((Paint) (new GradientPaint((float) x, (float) y, fill,
                        (float) (x + w - 2), (float) (y + h - 2), fadeTo)));
            }
            else {
                g2d.setPaint((Paint) fill);
            }
            g2d.fillOval(x + 1, y + 1, w - 2, h - 2);
        }
        else {
            g2d.setColor(Display.OUTLINE_COLOR);
            g2d.drawOval(x, y, w, h);
        }

        g2d.setPaint((Paint) Display.DEFAULT_COLOR);
    }

    public void drawDiamond(int x, int y, int w, int h, Color border) {
        int x1 = x + w / 2;
        int y1 = y + h / 2;
        this.g2d.setColor(border);
        this.g2d.drawLine(x, y1, x1, y);
        this.g2d.drawLine(x1, y, x + w, y1);
        this.g2d.drawLine(x + w, y1, x1, y + h);
        this.g2d.drawLine(x1, y + h, x, y1);
        this.g2d.setColor(Display.DEFAULT_COLOR);
    }

    public final void drawText(String text, int x, int y, Font font, Color color) {
        this.g2d.setFont(font);
        this.g2d.setColor(color);
        this.g2d.drawString(text, x, y);
        this.g2d.setFont(Display.DEFAULT_FONT);
        this.g2d.setColor(Display.DEFAULT_COLOR);
    }

    public final void clearRect(int x, int y, int w, int h) {
        drawRect(x, y, w, h, Display.BACKGROUND_COLOR, Display.BACKGROUND_COLOR, null);
    }
}
