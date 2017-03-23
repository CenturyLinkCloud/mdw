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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.ImageObserver;

import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.pages.CanvasCommon;

public class WorkflowImage extends CanvasCommon implements ImageObserver {

    private Graph process;

    public WorkflowImage(Graph process, DesignerDataAccess dao) {
        super(new IconFactory(dao));

        Dimension size = process.getGraphSize();
        size.width += 40;
        size.height += 40;
        setPreferredSize(size);
        setOpaque(false);
        // setMaximumSize(size); does not work
        this.process = process;
    }

    public void paintComponent(Graphics g) {
        // antialiasing
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }

        super.paintComponent(g);
        draw_graph(g, process, true);

        // handle selection
        if (selected_obj != null) {
            if (selected_obj instanceof Node)
                drawSelectionBox(g, (Node)selected_obj);
            else if (selected_obj instanceof Link)
                drawSelectionBox(g, (Link)selected_obj);
            else if (selected_obj instanceof SubGraph)
                drawSelectionBox(g, (SubGraph)selected_obj);
            else if (selected_obj instanceof Graph)
                drawSelectionBox(g, (Graph)selected_obj);
        }
    }
}