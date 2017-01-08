/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.pages;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.ImageObserver;

import javax.swing.ToolTipManager;

import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.display.SubGraph;

/**
 *
 */
public class DesignerCanvas extends CanvasCommon
	implements ImageObserver, MouseListener, MouseMotionListener,
		KeyListener {

    private static final long serialVersionUID = 1L;

	private FlowchartPage page;

	public DesignerCanvas(FlowchartPage page) {
	    super(page.frame.getIconFactory());
		Dimension size = new Dimension(640, 480);
		setLayout(null);
        this.setMinimumSize(size);
//        setPreferredSize(size);
		setBackground(Color.white);
		this.page = page;
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		this.setToolTipText("please tell me!");
		ToolTipManager.sharedInstance().setDismissDelay(30000);
		initialize_editable(this);

		String[] activity_menu_labels = {"Mark as new", "Mark as deleted",
				"Mark as unchanged", "Documentation", "Run from this activity"};
		String[] activity_menu_actions = {FlowchartPage.ACTION_MARK_AS_NEW,
				FlowchartPage.ACTION_MARK_AS_DELETED, FlowchartPage.ACTION_MARK_AS_NOCHANGE,
				FlowchartPage.ACTION_ACTIVITY_POPUP, FlowchartPage.ACTION_RUN_ACTIVITY};
		popup_activity = createPopup(activity_menu_labels, activity_menu_actions, page);
		String[] transition_menu_labels = {"Mark as new", "Mark as deleted", "Mark as unchanged"};
		String[] transition_menu_actions = {FlowchartPage.ACTION_MARK_AS_NEW,
				FlowchartPage.ACTION_MARK_AS_DELETED, FlowchartPage.ACTION_MARK_AS_NOCHANGE};
		popup_transition = createPopup(transition_menu_labels, transition_menu_actions, page);
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	protected void paintComponent(Graphics g) {
        Graph process = page.getProcess();
        super.paintComponent(g);

        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            if (process.zoom!=100) {
                double scale = process.zoom / 100.0;
                g2d.scale(scale, scale);
            }
        }

		// draw graph itself
        draw_graph(g, process, false);

		// then draw dragging and selected stuff
        super.paintDraggingAndSelection(g);

	}

    public void paint(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
        super.paint(g);
    }

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent arg0) {
	    super.mouseClicked(arg0, page.getProcess(), page);
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
		page.frame.setCursor(default_cursor);
	}

	protected void resizeSubGraph(int x, int y) {
	    SubGraph subgraph = (SubGraph)selected_obj;
	    Rectangle rect = new Rectangle(subgraph.x,
	            subgraph.y, subgraph.w, subgraph.h);
	    resizeRectangle(rect, x, y, 80);
	    subgraph.resize(rect);
	    if (!page.getProcess().isReadonly())
	    	page.getProcess().setDirtyLevel(Graph.GEOCHANGE);
	}

	public void mousePressed(MouseEvent arg0) {
	    super.mousePressed(arg0, page.getProcess());
	}

	public void mouseReleased(MouseEvent arg0) {
	    super.mouseReleased(arg0, page.getProcess(), page.linktype, page, page.recordchange);
	}

	public void mouseDragged(MouseEvent arg0) {
	    super.mouseDragged(arg0, page.getProcess());
	}

	public void mouseMoved(MouseEvent arg0) {
	    super.mouseMoved(arg0, page.getProcess());
	}

    public boolean isFocusable() {
        return true;
    }

    public void keyPressed(KeyEvent arg0) {
        super.keyPressed(arg0, page.getProcess(), page, page.recordchange);
    }

    public void keyReleased(KeyEvent arg0) {
        super.keyReleased(arg0, page.getProcess(), page.recordchange);
    }

    public void keyTyped(KeyEvent arg0) {
    }

	public String getToolTipText(MouseEvent arg0) {
		if (!FlowchartPage.showtip) return null;
		return super.getToolTipText(arg0, page.getProcess());
	}

	@Override
    protected void handleActivityMenuShortCut(Node node, int keycode) {
		if (keycode==KeyEvent.VK_D) {
			ActionEvent event = new ActionEvent(page, 0, FlowchartPage.ACTION_ACTIVITY_POPUP);
			page.actionPerformed(event);
		}
	}

}
