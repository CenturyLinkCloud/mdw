/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public final class Label extends Shape implements Drawable {
    private final String text;
    private final Font font;
    public static final int PAD = 2;
    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    public Label(Graphics2D g2d, Display display, String text, Font font) {
        super(g2d, display);
        this.g2d = g2d;
        this.display = display;
        this.text = text;
        this.font = font;
    }

    public Display draw() {
        return this.draw(Display.DEFAULT_COLOR);
    }

    public final Display draw(Color color) {
        this.g2d.setFont(this.font);
        display.setW(this.g2d.getFontMetrics().stringWidth(this.text) + PAD * 2);
        display.setH(this.g2d.getFontMetrics().getHeight() + PAD * 2);
        this.drawText(text, display.getX() + PAD, display.getY() + g2d.getFontMetrics().getAscent() + PAD, this.font, color);
        this.g2d.setFont(Display.DEFAULT_FONT);
        return display;
     }
}
