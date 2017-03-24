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
import java.awt.Graphics;

/**
 *
 * QBPM Source File
 */
public class IconSendMsg extends Icon {

    protected void draw(Graphics g, int x, int y, int w, int h) {
		g.setColor(Color.BLUE);
		g.drawRect(x, y, w*2/3, h);
		drawMsgArrow(g, x+w/3, y+h/5, w*2/3, h*3/5, false);
    }
    
}
