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
public class IconRule extends Icon {

    protected void draw(Graphics g, int x, int y, int w, int h) {
        g.setColor(Color.BLUE);
		g.draw3DRect(x, y, w, h, true);
		for (int k=4; k<h; k+=4) {
			g.drawLine(x+3, y+k, x+w-3, y+k);
		}
    }

}
