/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.icons;

import java.awt.Color;
import java.awt.Graphics;

public class IconJava extends Icon {

    public IconJava() {
        super();
        this.setIconHeight(16);
    }

    protected void draw(Graphics g, int x, int y, int w, int h) {
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y+2, w, h+2, 6, 6);
    }

}
