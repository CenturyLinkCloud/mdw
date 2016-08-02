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
public class IconTask extends Icon {

    protected void draw(Graphics g, int x, int y, int w, int h) {
        g.setColor(Color.BLUE);
		g.drawOval(x, y, w, h);
    }

}
