/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.icons;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.ImageIcon;

import com.centurylink.mdw.designer.display.Label;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;

/**
 *
 */
public class Icon implements javax.swing.Icon {

    public static final int JUST_LEFT = 0;
    public static final int JUST_CENTER = 1;
    public static final int JUST_RIGHT = 2;

    protected static final Color SHADOW = new Color(0, 0, 0, 50);

    protected static final float MAC_FONT_SIZE = 12;

    public String errmsg = null;
    private int w, h;

    public Icon() {
        errmsg = null;
        w = h = 24;
    }

    /**
     * This is a method javax.swing.Icon needs to implement
     */
	public void paintIcon(Component c, Graphics g, int x, int y) {
	    if (g instanceof Graphics2D)
	        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        draw(g, x, y, this.getIconWidth(), this.getIconHeight());
    }

	/**
	 * This is typically overriden by subclass
	 * @param g
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
    protected void draw(Graphics g, int x, int y, int w, int h) {
	    g.setColor(Color.BLUE);
		g.drawRect(x, y, w, h);
	}

    /**
     * Draw node in process canvas
     * @param g
     * @param node
     */
	public void draw(Graphics g, Node node) {
		draw(g, node.x, node.y, node.w, node.h);
		String desc = node.getName();
		String nodeIdType = node.getMainGraph().getNodeIdType();
		if (Node.ID_DATABASE.equals(nodeIdType)) {
			drawId(g, node.getId().toString(), node.x, node.y-2, Color.GRAY);
		} else if (Node.ID_LOGICAL.equals(nodeIdType)) {
			drawId(g, node.getLogicalId(), node.x, node.y-2, Color.GRAY);
		}
		else if (Node.ID_REFERENCE.equals(nodeIdType)) {
            drawId(g, node.getReferenceId(), node.x, node.y-2, Color.GRAY);
        }
        else if (Node.ID_SEQUENCE.equals(nodeIdType)) {
            drawId(g, String.valueOf(node.getSequenceId()), node.x, node.y-2, Color.GRAY);
        }
		if (desc!=null) drawLabel(g, node, desc);
	}

	/**
	 * Draw icon and label for activity implementor pane
	 * @param g
	 * @param label
	 * @param x
	 * @param y
	 */
	public void draw(Graphics g, String label, int x, int y) {
	    int w = getIconWidth();
	    int h = getIconHeight();
		draw(g, x, y, w, h);
		g.setColor(Color.BLACK);
		if (isMacOsX())
		    g.setFont(g.getFont().deriveFont(MAC_FONT_SIZE));
        g.drawString(label, x + 32, y + 12);
    }

	static protected void drawId(Graphics g, String name, int x, int y) {
	    drawId(g, name, x, y, Color.LIGHT_GRAY);
	}

    static protected void drawId(Graphics g, String name, int x, int y, Color color) {
        g.setColor(color);
        g.drawString(name, x, y);
    }

	/**
	 * Draw arrow of send message, receive message, etc.
	 * @param g
	 * @param x bounding box x
	 * @param y bounding box y
	 * @param w bounding box w
	 * @param h bounding box h
	 * @param toLeft  true if arrow points left, false if arrow points right
	 */
	public void drawMsgArrow(Graphics g, int x, int y, int w, int h,
			boolean toLeft) {
		if (toLeft) {
			int x1 = x+w/3;
			int y1 = y+h/4;
			int y2 = y+h*3/4;
			int xs[] = {x+w, x1, x1, x, x1, x1, x+w};
			int ys[] = {y1, y1, y, y+h/2, y+h, y2, y2};
			g.fillPolygon(xs, ys, 7);
		} else {
			int x1 = x+w*2/3;
			int y1 = y+h/4;
			int y2 = y+h*3/4;
			int xs[] = {x, x1, x1, x+w, x1, x1, x};
			int ys[] = {y1, y1, y, y+h/2, y+h, y2, y2};
			g.fillPolygon(xs, ys, 7);
		}
	}

    public static void drawImage(Graphics g, int x, int y, int w, int h, ImageIcon icon) {
        if (icon!=null) {
            g.drawImage(icon.getImage(),x,y,null);
        }
    }

    private static void drawLabel(Graphics g, Node node, String desc) {
        node.label = new Label(g, desc);
        node.lx = node.x+node.w/2-node.label.width/2;
        node.ly = node.y+node.h;
        g.setColor(Color.BLACK);
        node.label.draw(g, node.lx, node.ly);
    }

	public static void drawImage(Graphics g, Node node, ImageIcon icon, IconFactory factory) {
	    int h = icon.getIconHeight();
	    int w = icon.getIconWidth();
	    if (h!=node.h || w!=node.w) {
	        icon = (ImageIcon)factory.scaleIcon(icon, node.w, node.h);
	        node.setIconName(icon.getDescription());
	    }
		drawImage(g, node.x, node.y, node.w, node.h, icon);
		String desc = node.getName();
		String nodeIdType = node.getMainGraph().getNodeIdType();
		if (Node.ID_DATABASE.equals(nodeIdType)) {
			drawId(g, node.getId().toString(), node.x, node.y-2, Color.GRAY);
		} else if (Node.ID_LOGICAL.equals(nodeIdType)) {
			drawId(g, node.getLogicalId(), node.x, node.y-2, Color.GRAY);
		} else if (Node.ID_REFERENCE.equals(nodeIdType)) {
            drawId(g, node.getReferenceId(), node.x, node.y-2, Color.GRAY);
        } else if (Node.ID_SEQUENCE.equals(nodeIdType)) {
            drawId(g, String.valueOf(node.getSequenceId()), node.x, node.y-2, Color.GRAY);
        }
		if (desc!=null) drawLabel(g, node, desc);
	}

	public static void drawImage(Graphics g, ActivityImplementorVO nmi, int x, int y, ImageIcon icon) {
        int h = icon.getIconHeight();
        int w = icon.getIconWidth();
        drawImage(g, x, y, w, h, icon);
        g.setColor(Color.BLACK);
        if (isMacOsX())
            g.setFont(g.getFont().deriveFont(MAC_FONT_SIZE));
        g.drawString(nmi.getLabel(), x + 32, y + 12);
	}

	public static void drawBox(Graphics g, Node node, Object icon, boolean drawImageAlso) {
	    int image_h = 0;
	    if (icon instanceof ImageIcon) {
	    	if (node.isNew()) g.setColor(Color.MAGENTA);
	    	else g.setColor(Color.DARK_GRAY);
	        g.drawRoundRect(node.x, node.y, node.w, node.h, 12, 12);
	        if (drawImageAlso) {
	            ImageIcon imageIcon = (ImageIcon)icon;
	            int w = imageIcon.getIconWidth();
	            image_h = imageIcon.getIconHeight();
	            g.drawImage(imageIcon.getImage(), node.x+node.w/2-w/2, node.y+4, null);
	        }
	    } else {
	        ((Icon)icon).draw(g, node.x, node.y, node.w, node.h);
	        if (node.isNew()) {
            	g.setColor(Color.red);
    	        g.drawRoundRect(node.x-2, node.y-2, node.w+4, node.h+4, 12, 12);
            }
	    }
		String nodeIdType = node.getMainGraph().getNodeIdType();
		if (Node.ID_DATABASE.equals(nodeIdType)) {
			drawId(g, node.getId().toString(), node.x+2, node.y-2, Color.GRAY);
		} else if (Node.ID_LOGICAL.equals(nodeIdType)) {
			drawId(g, node.getLogicalId(), node.x+2, node.y-2, Color.GRAY);
		} else if (Node.ID_REFERENCE.equals(nodeIdType)) {
            drawId(g, node.getReferenceId(), node.x+2, node.y-2, Color.GRAY);
        } else if (Node.ID_SEQUENCE.equals(nodeIdType)) {
            drawId(g, String.valueOf(node.getSequenceId()), node.x+2, node.y-2, Color.GRAY);
        }
	    String desc = node.getName();
        if (desc!=null) {
          	if (node.isDeleted()) g.setColor(Color.LIGHT_GRAY);
        	else if (node.isNew()) g.setColor(Color.MAGENTA);
        	else g.setColor(Color.BLACK);
            node.label = new Label(g, desc);
            node.lx = node.x+node.w/2-node.label.width/2;
            if (image_h>0) node.ly = node.y + 8 + image_h;
            else if (node.label.height>node.h-8) node.ly = node.y + 4;
            else node.ly = node.y + (node.h - node.label.height)/2;
            node.label.draw(g, node.lx, node.ly);
	    }
	}

	public final int getIconWidth() {
	    return w;
	}

	public final int getIconHeight() {
	    return h;
	}

	public void setIconWidth(int w) {
	    this.w = w;
	}

	public void setIconHeight(int h) {
	    this.h = h;
	}

	/**
	 * TODO: Better place for this in Designer Core.
	 */
	private static boolean isMacOsX() {
	    return "Mac OS X".equals(System.getProperty("os.name"));
	}
}
