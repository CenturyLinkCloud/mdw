/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.awt.FontMetrics;
import java.awt.Graphics;

public class Label {
    public int width, height;
    public String[] lines;
    public int fontHeight;
    public int fontAscent;
    
    public Label(Graphics g, String label) {
        FontMetrics fm = g.getFontMetrics();
        lines = label.split("\n");
        fontHeight = fm.getHeight();
        fontAscent = fm.getAscent();
        height = lines.length*fontHeight;
        width = 0;
        for (int i=0; i<lines.length; i++) {
            int lw = fm.stringWidth(lines[i]);
            if (lw>width) width = lw;
        }
    }
    
    public void draw(Graphics g, int lx, int ly) {
        for (int i=0; i<lines.length; i++) {
            int lw = g.getFontMetrics().stringWidth(lines[i]);
            g.drawString(lines[i], lx+(width-lw)/2, ly+i*fontHeight+fontAscent);
        }
    }
}
