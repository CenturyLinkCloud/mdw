/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.centurylink.mdw.model.workflow.TextNote;

public class Note extends Shape implements Drawable {
    private final TextNote textNote;
    private static final Color BOX_OUTLINE_COLOR = new Color(8421504);
    private static final Color BOX_FILL_COLOR = new Color(16777164);
    private static final int BOX_ROUNDING_RADIUS = 2;
    private static final Font FONT = new Font("Monospace", 0, 13);
    private static final String WORK_DISPLAY_INFO = "WORK_DISPLAY_INFO";

    public Note(Graphics2D g2d, TextNote textNote) {
        super(g2d, new Display(textNote.getAttribute(WORK_DISPLAY_INFO)));
        this.g2d = g2d;
        this.textNote = textNote;
    }

    public final TextNote getTextNote() {
        return this.textNote;
    }

    public Display draw() {
        Display extents = new Display(0, 0, this.getDisplay().getX() + this.getDisplay().getW(),
                this.getDisplay().getY() + this.getDisplay().getH());
        drawRect(this.getDisplay().getX(), this.getDisplay().getY(), this.getDisplay().getW(),
                this.getDisplay().getH(), BOX_OUTLINE_COLOR, BOX_FILL_COLOR, BOX_ROUNDING_RADIUS);

        if (this.textNote.getContent() != null) {
            this.g2d.setFont(FONT);
            int h = this.g2d.getFontMetrics().getHeight();
            int y = this.getDisplay().getY();
            for (String line : textNote.getContent().split("\r\n")) {
                y += h;
                extents.setW(Math.max(extents.getW(), display.getX() + 4 + g2d.getFontMetrics().stringWidth(line)));
                extents.setH(Math.max(extents.getH(), y));
                drawText(line, display.getX() + 4, y, FONT,  Display.DEFAULT_COLOR);
            }

        }

        return extents;
    }
}
