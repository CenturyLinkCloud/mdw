/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.pages;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.ImageObserver;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import com.centurylink.mdw.designer.icons.Icon;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;

/**
 */
public class NodeChoicePane extends JPanel
	implements ImageObserver, MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 1L;
    private static final int hidden_count = 0;
    private static final int row_height = 32;

	private int selected_node;
	private DesignerPage page;
	private static final int node_x = 10;
	private int node_y[];
	private javax.swing.Icon icon[];
	private int next_choice_y;


	NodeChoicePane(DesignerPage page) {
		 //this.setSize(500,500);
        //this.setMinimumSize(new Dimension(500,500));
		this.page = page;
		addMouseListener(this);
		addMouseMotionListener(this);
		setToolTipText("please tell me!");
		init();
	}

	private void init() {
	    next_choice_y = 10;
        int i, n = page.getDataModel().getNodeMetaInfo().count();
        ActivityImplementorVO nmi;
        node_y = new int[n];
        icon = new javax.swing.Icon[n];
        for (i=hidden_count; i<n; i++) {
            nmi = page.getDataModel().getNodeMetaInfo().get(i);
            if (nmi!=null) {
                node_y[i] = next_choice_y;
//                next_choice_y += 10 + nmi.shape_h;
                next_choice_y += row_height;
                IconFactory iconFactory = page.frame.getIconFactory();
                icon[i] = iconFactory.getIcon(nmi.getIconName(), page);
                icon[i] = iconFactory.scaleIconToMax(icon[i], 24, 24);
            }
        }
        setPreferredSize(new Dimension(300,next_choice_y));
        selected_node = -1;
	}


    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = null;
        if (g instanceof Graphics2D) {
            g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
        super.paintComponent(g);
        ActivityImplementorVO nmi;
        int i, n = node_y.length;
        for (i=hidden_count; i<n; i++) {
            nmi = page.getDataModel().getNodeMetaInfo().get(i);
            if (selected_node==i) {
                g.setColor(Color.GRAY);
                if (g2d != null) {
                    g2d.fillRoundRect(0, node_y[i]-4, getWidth(), row_height, 4, 4);
                }
                else {
                    g.fillRect(0, node_y[i]-4, 200, row_height-4);
                }
            }
            if (icon[i] instanceof ImageIcon) {
                Icon.drawImage(g, nmi, node_x, node_y[i], (ImageIcon)icon[i]);
            } else ((Icon)icon[i]).draw(g, nmi.getLabel(), node_x, node_y[i]);

        }
    }

    public void paint(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
        super.paint(g);
    }

    public void reload() {
    	init();
        this.updateUI();
        this.repaint();
    }

    public void reload(boolean sortAtoZ, int dbversion) {
        page.getDataModel().getNodeMetaInfo().init(
        		page.getDataModel().getActivityImplementors(), sortAtoZ, dbversion);
        init();
        this.updateUI();
        this.repaint();
    }

    //AK..added on 03/27/2011
    public void filter() {
    	init();
        this.updateUI();
        this.repaint();
    }

	public void mouseClicked(MouseEvent arg0) {
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
	}

	private boolean onPoint(int i, int x, int y) {
		return (y>=node_y[i] && y<node_y[i]+(row_height-4));
	}

	public int nodeAt(int x, int y) {
		int i;
		for (i=node_y.length-1; i>=hidden_count; i--) {
			if (onPoint(i, x, y)) return i;
		}
		return -1;
	}

	public void mousePressed(MouseEvent arg0) {
		int x = arg0.getX();
		int y = arg0.getY();
		selected_node = nodeAt(x, y);
		repaint();
	}



	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent arg0) {
//		selected_node = -1;
//		repaint();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent arg0) {
		if (selected_node>=0) {
			repaint();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent arg0) {

	}

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        return super.getToolTipLocation(event);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (!(page instanceof FlowchartPage) || !FlowchartPage.showtip) return null;
        int x = event.getX();
        int y = event.getY();
        int node = nodeAt(x, y);
        if (node>=0) {
            ActivityImplementorVO nmi = page.getDataModel().getNodeMetaInfo().get(node);
            return nmi.getImplementorClassName();
        } else return null;
    }

    public int getSelectedNode() {
        return selected_node;
    }

}
