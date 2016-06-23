/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.runtime;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.ImageObserver;

import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.pages.CanvasCommon;

public class RunTimeDesignerImage extends CanvasCommon
	implements ImageObserver {

    private static final long serialVersionUID = 1L;

    Graph process;
    DesignerDataAccess dao;

    public RunTimeDesignerImage(Graph process,DesignerDataAccess dao){
        super(new IconFactory(dao));

        Dimension size = process.getGraphSize();
        size.width += 40;
        size.height += 40;
		setPreferredSize(size);
		// setMaximumSize(size); does not work
		setBackground(new Color(0.97f,1.0f,0.97f));
        this.process = process;
        this.dao = dao;
    }

    /* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		draw_graph(g, process, true);
	}

}
