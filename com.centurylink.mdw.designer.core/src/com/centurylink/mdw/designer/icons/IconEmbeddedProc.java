/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.icons;

import java.awt.Color;
import java.awt.Graphics;

public class IconEmbeddedProc extends Icon {

    private static Color LIGHT_BLUE = new Color(120, 140, 220);

    protected void draw(Graphics g, int x, int y, int w, int h) {
		g.setColor(LIGHT_BLUE);
        g.drawRoundRect(x, y, w, h-2, 3, 3);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x+2, y-1, 12, 3);
    }

}
