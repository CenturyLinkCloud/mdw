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
package com.centurylink.mdw.designer.icons;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class IconStart extends Icon {

    private static Color dark_green = null;

    public IconStart() {
        super();
        this.setIconWidth(32);
        this.setIconHeight(32);
    }

    protected void draw(Graphics g, int x, int y, int w, int h) {
        if (g instanceof Graphics2D) {
            g.setColor(SHADOW);
            g.fillOval(x + 2, y + 2, w, h);
            g.setColor(Color.black);
            g.fillOval(x,y,w,h);
            Graphics2D g2 = (Graphics2D)g;
            if (dark_green==null) dark_green = new Color(0.0f, 0.5f, 0.f);
            GradientPaint p = new GradientPaint(x, y, dark_green, x+w-2, y+h-2, Color.white);
            g2.setPaint(p);
            g2.fillOval(x+1, y+1, w-2, h-2);
            g2.setPaint(Color.black);
        } else {
            g.drawOval(x,y,w,h);
            g.fillOval(x+2,y+2,w-3,h-3);
        }
    }

    static protected void drawDesc(Graphics g, String desc, int x, int y, int w, int h) {
    }

}
