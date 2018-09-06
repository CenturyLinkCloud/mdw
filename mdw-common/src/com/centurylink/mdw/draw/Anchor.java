/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import java.awt.Color;
import java.awt.Graphics2D;

public class Anchor extends Shape {
    public static final Color ANCHOR_COLOR = new Color(15483002);
    public static final int ANCHOR_W = 3;
    public static final int ANCHOR_HIT_W = 8;

    public Anchor(Graphics2D g2d, int x, int y) {
        super(g2d, new Display(x - 3, y - 3, 6, 6));
        this.g2d = g2d;
    }

    public Display draw() {
        this.g2d.setColor(ANCHOR_COLOR);
        this.g2d.fillRect(this.getDisplay().getX(), this.getDisplay().getY(),
                this.getDisplay().getW(), this.getDisplay().getH());
        this.g2d.setColor(Display.DEFAULT_COLOR);
        return this.getDisplay();
    }

}
