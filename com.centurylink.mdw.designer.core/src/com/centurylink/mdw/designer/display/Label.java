/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
