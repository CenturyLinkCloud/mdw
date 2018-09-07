/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import java.awt.Color;
import java.awt.Graphics2D;

import com.centurylink.mdw.model.workflow.Activity;

public class Step extends Shape implements Drawable {
    private final Activity activity;
    private final Implementor implementor;
    private final boolean boxStyle;
    public static final int MIN_SIZE = 4;

    public Step(Graphics2D g2d, Activity activity, Implementor implementor, boolean boxStyle) {
        super(g2d, new Display(activity.getAttribute("WORK_DISPLAY_INFO")));
        this.g2d = g2d;
        this.activity = activity;
        this.implementor = implementor;
        this.boxStyle = boxStyle;
    }

    public final Activity getActivity() {
        return this.activity;
    }

    public Display draw() {
        Display extents = new Display(0, 0, this.getDisplay().getX() + this.getDisplay().getW(),
                this.getDisplay().getY() + this.getDisplay().getH());
        int yAdjust = -3;
        Color textColor = Display.DEFAULT_COLOR;
        int iconX;
        int w;
        int y;
        if (implementor.getIcon() != null) {
            if (boxStyle) {
                drawRect(this.getDisplay().getX(), this.getDisplay().getY(),
                        this.getDisplay().getW(), this.getDisplay().getH(), null, null, null);
            }
            iconX = display.getX() + display.getW() / 2 - 12;
            int iconY = display.getY() + 5;
            extents.setW(Math.max(extents.getW(), iconX + implementor.getIcon().getIconWidth()));
            extents.setH(Math.max(extents.getH(), iconY + implementor.getIcon().getIconHeight()));
            drawIcon(implementor.getIcon(), iconX, iconY, 1.0F);
        }
        else if (this.implementor.getIconName() != null) {
            String shape = this.implementor.getIconName().substring(6);
            if (shape.equals("stop")) {
                drawOval(this.getDisplay().getX(), this.getDisplay().getY(),
                        this.getDisplay().getW(), this.getDisplay().getH(), Display.STOP_COLOR,
                        null, g2d);
                textColor = Display.SHAPE_TEXT_COLOR;
            }
            else if (shape.equals("start")) {
                drawOval(this.getDisplay().getX(), this.getDisplay().getY(),
                        this.getDisplay().getW(), this.getDisplay().getH(), Display.START_COLOR,
                        null, g2d);
                textColor = Display.SHAPE_TEXT_COLOR;
            }
            else if (shape.equals("decision")) {
                drawDiamond(this.getDisplay().getX(), this.getDisplay().getY(),
                        this.getDisplay().getW(), this.getDisplay().getH(), null);
            }
            else
                drawRect(this.getDisplay().getX(), this.getDisplay().getY(),
                        this.getDisplay().getW(), this.getDisplay().getH(), null, null, null);
        }
        else
            drawRect(this.getDisplay().getX(), this.getDisplay().getY(), this.getDisplay().getW(),
                    this.getDisplay().getH(), null, null, null);

        if (this.activity.getName() != null) {
            String[] lines = this.activity.getName().split("\r\n");
            w = 0;
            y = this.getDisplay().getY() + this.getDisplay().getH() / 2;
            if (lines.length == 1) {
                y += g2d.getFontMetrics().getHeight() / 2;
            }

            if (this.implementor.getIcon() != null) {
                y += this.implementor.getIcon().getIconHeight() / 2;
            }

            if (y < 0) {
                y = 0;
            }

            for (String line : lines) {
                int lw = this.g2d.getFontMetrics().stringWidth(line);
                if (lw > w) {
                    w = lw;
                }

                int x = this.getDisplay().getX() + this.getDisplay().getW() / 2 - lw / 2;
                extents.setW(Math.max(extents.getW(), x + w));
                extents.setH(
                        Math.max(extents.getH(), y + yAdjust + g2d.getFontMetrics().getHeight()));
                drawText(line, x, y + yAdjust, null, textColor);
                y += g2d.getFontMetrics().getHeight() - 1;
            }
        }

        if (this.activity.getId() > 0L) {
            extents.setW(Math.max(extents.getW(), this.display.getX() + 2
                    + g2d.getFontMetrics().stringWidth(activity.getId().toString())));
            extents.setH(Math.max(extents.getH(),
                    this.display.getY() - 2 + g2d.getFontMetrics().getHeight()));
            drawText(activity.getLogicalId(), this.display.getX() + 2, this.display.getY() - 2,
                    null, Display.META_COLOR);
        }

        return extents;
    }
}
