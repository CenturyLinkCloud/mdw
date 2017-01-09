/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.icons;

import java.awt.Color;
import java.awt.Graphics;

/**
 *
 * QBPM Source File
 */
public class IconDecision extends Icon {

    public IconDecision() {
        super();
        this.setIconWidth(32);
        this.setIconHeight(24);
    }

    protected void draw(Graphics g, int x, int y, int w, int h) {
        int x1 = x + w/2;
		int y1 = y + h/2;
		g.setColor(Color.DARK_GRAY);
		g.drawLine(x, y1, x1, y);
		g.drawLine(x1, y, x+w, y1);
		g.drawLine(x+w, y1, x1, y+h);
		g.drawLine(x1, y+h, x, y1);
    }

    static protected void drawDesc(Graphics g, String desc, int x, int y, int w, int h) {
        g.setColor(Color.BLACK);
	    g.drawString(desc, x+2, y+h/2);
    }

}
