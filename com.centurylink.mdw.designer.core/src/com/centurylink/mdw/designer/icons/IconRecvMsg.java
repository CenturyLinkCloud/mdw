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
public class IconRecvMsg extends Icon {

    protected void draw(Graphics g, int x, int y, int w, int h) {
		g.setColor(Color.BLUE);
//		g.drawRect(x+w/3, y, w*2/3, h);
//		drawMsgArrow(g, x, y+h/5, w*2/3, h*3/5, false);
		g.drawRect(x, y, w*2/3, h);
		drawMsgArrow(g, x+w/3, y+h/5, w*2/3, h*3/5, true);
    }

}
