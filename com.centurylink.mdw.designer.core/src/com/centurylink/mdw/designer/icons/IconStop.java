/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.icons;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class IconStop extends Icon {

    public IconStop() {
        super();
        this.setIconWidth(32);
        this.setIconHeight(32);
    }

    protected void draw(Graphics g, int x, int y, int w, int h) {
        if (g instanceof Graphics2D) {
            g.setColor(SHADOW);
            g.fillOval(x + 2, y + 2, w, h);
            g.setColor(Color.black);
            g.fillOval(x, y, w, h);
            Graphics2D g2 = (Graphics2D)g;
            GradientPaint p = new GradientPaint(x, y, Color.red, x+w-6, y+h-6, Color.white);
            g2.setPaint(p);
            g2.fillOval(x+1, y+1, w-2, h-2);
            g2.setPaint(Color.black);
        } else {
            g.setColor(Color.RED);
            g.drawRoundRect(x, y, w, h, 3, 3);
            g.fillRoundRect(x+2,y+2,w-3,h-3, 2, 2);
        }
    }

    static protected void drawDesc(Graphics g, String desc, int x, int y, int w, int h) {
    }

}
