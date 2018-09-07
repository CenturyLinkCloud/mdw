/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

public final class Display {
    private int x;
    private int y;
    private int w;
    private int h;
    public static final Font DEFAULT_FONT = new Font("SansSerif", 0, 12);
    public static final Font TITLE_FONT = new Font("SansSerif", 1, 18);
    public static final int MAC_FONT_SIZE = 12;
    public static  Color DEFAULT_COLOR = Color.BLACK;
    public static  Color GRID_COLOR = Color.LIGHT_GRAY;
    public static  Color OUTLINE_COLOR = Color.BLACK;
    public static  Color SHADOW_COLOR = new Color(0, 0, 0, 50);
    public static  Color META_COLOR = Color.GRAY;
    public static  Color BACKGROUND_COLOR = Color.WHITE;
    public static final Color START_COLOR = new Color(0x98fb98);
    public static final Color STOP_COLOR = new Color(0xff8c86);
    public static final Color SHAPE_TEXT_COLOR = Color.BLACK;
    public static final BasicStroke DEFAULT_STROKE = new BasicStroke();
    public static final BasicStroke LINK_STROKE = new BasicStroke(3.0f);
    public static final BasicStroke GRID_STROKE = new BasicStroke(0.2f);
    public static final int GRID_SIZE = 10;
    public static final int ROUNDING_RADIUS = 12;
    public static final int MIN_DRAG = 3;
    public static final int ICON_WIDTH = 24;
    public static final int ICON_HEIGHT = 24;
    public static final int ICON_PAD = 8;

    public final int getX() {
        return this.x;
    }

    public final void setX(int var1) {
        this.x = var1;
    }

    public final int getY() {
        return this.y;
    }

    public final void setY(int var1) {
        this.y = var1;
    }

    public final int getW() {
        return this.w;
    }

    public final void setW(int var1) {
        this.w = var1;
    }

    public final int getH() {
        return this.h;
    }

    public final void setH(int var1) {
        this.h = var1;
    }

    public Display(String attr) {
        if (attr != null) {
            for (String dim : attr.split(",")) {
                if (dim.startsWith("x=")) {
                    x = Integer.parseInt(dim.substring(2));
                }
                else if (dim.startsWith("y=")) {
                    y = Integer.parseInt(dim.substring(2));
                }
                else if (dim.startsWith("w=")) {
                    w = Integer.parseInt(dim.substring(2));
                }
                else if (dim.startsWith("h=")) {
                    h = Integer.parseInt(dim.substring(2));
                }
            }
        }

    }

    public Display(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public Display(Display display) {
        this.x = display.x;
        this.y = display.y;
        this.w = display.w;
        this.h = display.h;
    }
}
