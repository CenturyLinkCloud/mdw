/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.pages;

import javax.swing.*;
import javax.swing.table.JTableHeader;

import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.dialogs.FormWidgetDialog;
import com.centurylink.mdw.designer.utils.Constants;
import com.centurylink.mdw.designer.utils.SwingFormGenerator;
import com.centurylink.mdw.designer.utils.SwingFormGenerator.MenuButton;
import com.centurylink.mdw.model.FormDataDocument;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengNode;

import java.awt.*;
import java.awt.event.*;


/**
 *
 * Description of type.
 *
 * @version 1.0
 */
public class FormDesignCanvas extends JPanel
	implements MouseListener, MouseMotionListener, ActionListener {

    private static final long serialVersionUID = 1L;

    private Cursor default_cursor;
    private Cursor ne_resize_cursor, nw_resize_cursor;
    private JPopupMenu processesPopup, subprocessPopup;
    private MbengDocument desc_doc;
    private MbengNode selected_node = null;
    private Component dragging = null;
    private int at_anchor;
    private int delta_x, delta_y;
    private Window frame;
    private boolean atRuntime;
    private int gridSize = 4;
    private SwingFormGenerator generator;
//    private MenuEditor menuEditor;
    private int pagelet_w, pagelet_h;
    private boolean readonly;
    private FormPanel formpanel;	// runtime only; null for design time

//    private DragGestureRecognizer recognizer;

	public FormDesignCanvas(Window frame, FormPanel formmain, DesignerDataAccess dao) {
		Dimension size = new Dimension(640, 480);
        this.setMinimumSize(size);
        this.atRuntime = formmain!=null;
//        setPreferredSize(size);
		this.frame = frame;
		this.formpanel = formmain;
		this.readonly = false;
		if (!atRuntime) {
			setBackground(Color.WHITE);
			addMouseListener(this);
			this.addMouseMotionListener(this);
		}
//		this.setToolTipText("please tell me!");
		selected_node = null;
		generator = new SwingFormGenerator(this, formpanel, dao);
//		shading = new Color(0.9f, 1.0f, 1.0f, 0.5f);
//		selectedColor = new Color(1.0f, 0.8f, 0.1f, 0.3f);
		createPopupMenus();

//        menuEditor = new MenuEditor(generator, this);
//        add(menuEditor);

		default_cursor =
				Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
//		hand_cursor =
//			Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        nw_resize_cursor =
                Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
        ne_resize_cursor =
                Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
//        crosshair_cursor =
//            Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

    private void createPopupMenus() {
        processesPopup = new JPopupMenu();
        processesPopup.setInvoker(this);

        JMenuItem menuitem;

        subprocessPopup = new JPopupMenu();
        subprocessPopup.setInvoker(this);
        menuitem = new JMenuItem("open subprocess");
        menuitem.setActionCommand(Constants.ACTION_SUBVIEW);
//        menuitem.addActionListener(page);
        subprocessPopup.add(menuitem);
    }

    public void setReadonly(boolean v) {
    	this.readonly = v;
		setBackground(readonly? new Color(240,255,250):Color.white);
    }

    private void getRootCoord(Container container, Rectangle r) {
        // container can be null when it is a menu
        while (container!=this && container!=null) {
            r.x += container.getX();
            r.y += container.getY();
            container = container.getParent();
        }
    }

    private Point getRootCoord(MouseEvent e) {
        Component src = e.getComponent();
        Point ret = e.getPoint();
        // src can be null when it is a menu
        while (src!=this && src!=null) {
            Point pp = src.getLocation();
//            System.out.println("  add loc: " + src.getClass().getName() + pp);
            ret.x += pp.x;
            ret.y += pp.y;
            src = src.getParent();
//            if (parent instanceof JViewport) parent = parent.getParent();
        }
        return ret;
    }

	protected void paintComponent(Graphics g) {
//        if (gridSize>1) {
//            int w = this.getWidth();
//            int h = this.getHeight();
//            int k;
//            g.setColor(Color.LIGHT_GRAY);
//            for (k=gridSize; k<w; k+=gridSize) {
//                g.drawLine(k, 0, k, h);
//            }
//            for (k=gridSize; k<h; k+=gridSize) {
//                g.drawLine(0, k, w, k);
//            }
//        }

        super.paintComponent(g);

//        if (gridSize>1) {
//            int w = this.getWidth();
//            int h = this.getHeight();
//            int k;
//            g.setColor(Color.LIGHT_GRAY);
//            for (k=gridSize; k<w; k+=gridSize) {
//                g.drawLine(k, 0, k, h);
//            }
//            for (k=gridSize; k<h; k+=gridSize) {
//                g.drawLine(0, k, w, k);
//            }
//        }
	}

    public void paint(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }

        super.paint(g);

        if (!atRuntime) {
        	g.setColor(Color.CYAN);
        	g.drawLine(0,pagelet_h, pagelet_w, pagelet_h);
        	g.drawLine(pagelet_w, 0, pagelet_w, pagelet_h);
        }

        if (!atRuntime && selected_node!=null) {
            JLabel label = generator.getLabel(selected_node);
            Component widget = generator.getWidget(selected_node);
            if (widget!=null && !(widget instanceof JMenuItem)) {
                Rectangle r = widget.getBounds();
                this.getRootCoord(widget.getParent(), r);
                g.setColor(Color.RED);
                g.fillRect(r.x-2,r.y-2,4,4);
                g.fillRect(r.x+r.width-2,r.y-2,4,4);
                g.fillRect(r.x+r.width-2,r.y+r.height-2,4,4);
                g.fillRect(r.x-2,r.y+r.height-2,4,4);
            }
            if (label!=null) {
                Rectangle r = label.getBounds();
                this.getRootCoord(label.getParent(), r);
                g.setColor(Color.RED);
                g.fillRect(r.x-2,r.y-2,4,4);
                g.fillRect(r.x+r.width-2,r.y-2,4,4);
                g.fillRect(r.x+r.width-2,r.y+r.height-2,4,4);
                g.fillRect(r.x-2,r.y+r.height-2,4,4);
            }
        }
    }

	public void showWidgetDialog(MbengNode node) {
        FormWidgetDialog dialog = new FormWidgetDialog((JFrame)frame, node, generator, this.readonly);
        dialog.setVisible(true);
        repaint();
	}

    public void mouseClicked(MouseEvent arg0) {
        if (arg0.getClickCount()<2) return;
        if (selected_node==null) return;
        Component src = (Component)arg0.getSource();
        if (src instanceof JMenu) {
//            menuEditor.show(selected_node, (JMenu)src);
            showWidgetDialog(selected_node);
        } else {
            showWidgetDialog(selected_node);
        }
    }

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
//		page.frame.setCursor(default_cursor);
	}

	public void mousePressed(MouseEvent arg0) {
        Object src = arg0.getSource();
//        System.out.println("SOURCE: " + src.getClass().getName());
        if (selected_node!=null && at_anchor>=0) {
            if (at_anchor>=4) {
                dragging = generator.getLabel(selected_node);
            } else {
                dragging = generator.getWidget(selected_node);
            }
            Point p = getRootCoord(arg0);
            Rectangle r = dragging.getBounds();
            if (at_anchor==0 || at_anchor==4) {
                delta_x = p.x - r.x;
                delta_y = p.y - r.y;
            } else if (at_anchor==1 || at_anchor==5) {
                delta_x = p.x - r.x - r.width;
                delta_y = p.y - r.y;
            } else if (at_anchor==2 || at_anchor==6) {
                delta_x = p.x - r.x - r.width;
                delta_y = p.y - r.y - r.height;
            } else {
                delta_x = p.x - r.x;
                delta_y = p.y - r.y - r.height;
            }
//          System.out.println("Delta: ("+delta_x+","+delta_y+")");
        } else if (src instanceof Component && src!=this) {
            if (src instanceof JTableHeader) {
                JTable table = ((JTableHeader)src).getTable();
                Point p = new Point(arg0.getPoint());
                int c = table.columnAtPoint(p);
//                System.out.println("Column " + c);
//                System.out.println("Table coord " + arg0.getX() + "," + arg0.getY());
                Component widget = generator.getWidgetFromEventSource(table);
                MbengNode tableNode = generator.getNode(widget);
                selected_node = tableNode.getFirstChild();
                int k = 0;
                while (k<c) {
                    selected_node = selected_node.getNextSibling();
                    k++;
                }
                if (selected_node!=null)
                	selected_node.setAttribute("INDEX", Integer.toString(c));
//                System.out.println("Column: " + selected_node.getAttribute(FormConstants.FORMATTR_LABEL));
            } else {
                dragging = generator.getWidgetFromEventSource((Component)src);
                Point p = getRootCoord(arg0);
    //            System.out.println("Root: " + p);
                delta_x = p.x-dragging.getX();
                delta_y = p.y-dragging.getY();
    //            System.out.println("Delta: ("+delta_x+","+delta_y+")");
                selected_node = generator.getNode(dragging);
            }
        } else {
            dragging = null;
            selected_node = null;
        }
        repaint();
	}

	public void mouseReleased(MouseEvent arg0) {
		if (readonly) return;
	    if (dragging!=null) {
	        if (selected_node!=null && at_anchor>=0) {
	            Rectangle rect = dragging.getBounds();
	            if (at_anchor==0 || at_anchor==4) {
	                rect.x = toGrid(rect.x);
	                rect.y = toGrid(rect.y);
	                rect.width = toGrid(rect.width);
	                rect.height = toGrid(rect.height);
	            } else if (at_anchor==1 || at_anchor==5) {
                    rect.y = toGrid(rect.y);
                    rect.width = toGrid(rect.width);
                    rect.height = toGrid(rect.height);
	            } else if (at_anchor==2 || at_anchor==6) {
                    rect.width = toGrid(rect.width);
                    rect.height = toGrid(rect.height);
	            } else {
                    rect.x = toGrid(rect.x);
                    rect.width = toGrid(rect.width);
                    rect.height = toGrid(rect.height);
	            }
	            dragging.setBounds(rect);
	            repaint();
	        } else {
	            Point p = dragging.getLocation();
                p.x = toGrid(p.x);
                p.y = toGrid(p.y);
	            dragging.setLocation(p);
	            repaint();
	        }
	        generator.saveGeoInfo(dragging);
	    }
        dragging = null;
        at_anchor = -1;
	}

	private int toGrid(int k) {
	    return (k+gridSize/2)/gridSize*gridSize;
	}

	public void mouseDragged(MouseEvent arg0) {
		if (readonly) return;
	    if (dragging!=null && selected_node!=null && at_anchor>=0) {
	        Point p = getRootCoord(arg0);
	        Rectangle oldr = dragging.getBounds();
	        Rectangle newr = new Rectangle();
            if (at_anchor==0 || at_anchor==4) {
                newr.x = p.x - delta_x;
                newr.y = p.y - delta_y;
                newr.width = oldr.width + oldr.x + delta_x - p.x;
                newr.height = oldr.height + oldr.y + delta_y - p.y;
            } else if (at_anchor==1 || at_anchor==5) {
                newr.x = oldr.x;
                newr.y = p.y - delta_y;
                newr.width = p.x-oldr.x-delta_x;
                newr.height = oldr.height + oldr.y + delta_y - p.y;
            } else if (at_anchor==2 || at_anchor==6) {
                newr.x = oldr.x;
                newr.y = oldr.y;
                newr.width = p.x-oldr.x-delta_x;
                newr.height = p.y-oldr.y-delta_y;
            } else {
                newr.x = p.x-delta_x;
                newr.y = oldr.y;
                newr.width = oldr.width + oldr.x + delta_x - p.x;
                newr.height = p.y-oldr.y-delta_y;
            }
            applyResizeConstraint(dragging, newr);
            dragging.setBounds(newr);
            repaint();
	    } else if (dragging!=null) {
	        Point p = getRootCoord(arg0);
//	        System.out.println("root coord = ("+p.x+","+p.y+")");
    	    p.x -= delta_x;
    	    p.y -= delta_y;
//            System.out.println("drag to ("+x+","+y+")");
    	    applyMoveConstraint(dragging, p);
            dragging.setLocation(p);
    	    repaint();
	    }
	}

	private void applyMoveConstraint(Component comp, Point p) {
	    Container parent = comp.getParent();
	    Rectangle r = parent.getBounds();
	    if (p.x<0) p.x = 0;
	    else if (p.x > r.width-20) p.x = r.width - 20;
	    if (p.y<0) p.y = 0;
	    else if (p.y > r.height-20) p.y = r.height - 10;
	    if (comp instanceof JMenu) p.y = 0;
	    else if (comp instanceof JMenuBar) p.x = p.y = 0;
	}

    private void applyResizeConstraint(Component comp, Rectangle r) {
        Point p = new Point(r.x, r.y);
        applyMoveConstraint(comp, p);
        r.x = p.x;
        r.y = p.y;
        if (r.height<6) r.height = 6;
        if (r.width<6) r.width = 6;
    }

    private boolean onSelectableAnchor(int x, int y, int x1, int y1, int w1, int h1) {
        if (Math.abs(x1-x)<=3 && Math.abs(y1-y)<=3) {
            at_anchor = 0;
            return true;
        } else if (Math.abs(x1+w1-x)<=3 && Math.abs(y1-y)<=3) {
            at_anchor = 1;
            return true;
        } else if (Math.abs(x1+w1-x)<=3 && Math.abs(y1+h1-y)<=3) {
            at_anchor = 2;
            return true;
        } else if (Math.abs(x1-x)<=3 && Math.abs(y1+h1-y)<=3) {
            at_anchor = 3;
            return true;
        } else return false;
    }

	public void mouseMoved(MouseEvent arg0) {
        if (selected_node==null) return;
        if (dragging!=null) return;
        Point p = getRootCoord(arg0);
        Component widget = generator.getWidget(selected_node);
        JLabel label = generator.getLabel(selected_node);
        at_anchor = -1;
        if (widget!=null) {
            Rectangle r = widget.getBounds();
            this.getRootCoord(widget.getParent(), r);
            if (onSelectableAnchor(p.x, p.y, r.x, r.y, r.width, r.height)) {
            }
        }
        if (at_anchor<0 && label!=null) {
            Rectangle r = label.getBounds();
            this.getRootCoord(label.getParent(), r);
            if (onSelectableAnchor(p.x, p.y, r.x, r.y, r.width, r.height)) {
                at_anchor+= 4;
            }
        }
        if (at_anchor>=0) {
            if (at_anchor%2==0) setCursor(nw_resize_cursor);
            else setCursor(ne_resize_cursor);
        } else setCursor(default_cursor);
	}

    public boolean isFocusable() {
        return true;
    }

    private MbengNode objectAt(MbengNode node, int x, int y) {
        MbengNode found = null;
        Component widget = generator.getWidget(node);
        if (widget==null) return null;
        Rectangle r = widget.getBounds();
        if (!widget.isVisible()) return null;
        if (widget==this) {
        	r.x = 0;
        	r.y = 0;
        }
        if (x<r.x || x>r.x+r.width || y<r.y || y>r.y+r.height) return null;
        for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
            found = objectAt(child, x-r.x, y-r.y);
            if (found!=null) return found;
        }
        return node;
    }

    private boolean isContainer(MbengNode node) {
        String type = node.getName();
        if (type.equals(FormConstants.WIDGET_PAGELET) ||
                type.equals(FormConstants.WIDGET_PANEL) ||
                type.equals(FormConstants.WIDGET_MENUBAR) ||
                type.equals(FormConstants.WIDGET_MENU) ||
                type.equals(FormConstants.WIDGET_TABBEDPANE) ||
                type.equals(FormConstants.WIDGET_TABLE) ||
                type.equals(FormConstants.WIDGET_TAB))
            return true;
        return false;
    }

    private boolean checkChildCompatible(String parentType, String childType) {
        if (childType.equals(FormConstants.WIDGET_MENUBAR)) {
            if (FormConstants.WIDGET_PAGELET.equals(parentType)) return true;
            else return false;
        } else if (childType.equals(FormConstants.WIDGET_MENU)) {
            if (FormConstants.WIDGET_MENUBAR.equals(parentType)) return true;
            else if (FormConstants.WIDGET_MENU.equals(parentType)) return true;
            else if (FormConstants.WIDGET_PANEL.equals(parentType)) return true;
            else if (FormConstants.WIDGET_TAB.equals(parentType)) return true;
            else if (FormConstants.WIDGET_PAGELET.equals(parentType)) return true;
            else return false;
        } else if (childType.equals(FormConstants.WIDGET_MENUITEM)) {
            if (FormConstants.WIDGET_MENU.equals(parentType)) return true;
            else return false;
        } else if (childType.equals(FormConstants.WIDGET_TAB)) {
            if (FormConstants.WIDGET_TABBEDPANE.equals(parentType)) return true;
            else return false;
        } else if (childType.equals(FormConstants.WIDGET_COLUMN)) {
            if (FormConstants.WIDGET_TABLE.equals(parentType)) return true;
            else return false;
        } else if (childType.equals(FormConstants.WIDGET_TEXT) ||
                childType.equals(FormConstants.WIDGET_TEXTAREA) ||
                childType.equals(FormConstants.WIDGET_BUTTON) ||
                childType.equals(FormConstants.WIDGET_LISTPICKER) ||
                childType.equals(FormConstants.WIDGET_DROPDOWN) ||
                childType.equals(FormConstants.WIDGET_RADIOBUTTONS) ||
                childType.equals(FormConstants.WIDGET_CHECKBOX) ||
                childType.equals(FormConstants.WIDGET_LIST) ||
                childType.equals(FormConstants.WIDGET_DATE) ||
                childType.equals(FormConstants.WIDGET_HYPERLINK)) {
            if (FormConstants.WIDGET_PANEL.equals(parentType)) return true;
            else if (FormConstants.WIDGET_PAGELET.equals(parentType)) return true;
            else if (FormConstants.WIDGET_TAB.equals(parentType)) return true;
            else return false;
        } else if (childType.equals(FormConstants.WIDGET_PANEL)||
                childType.equals(FormConstants.WIDGET_INCLUDE) ||
                childType.equals(FormConstants.WIDGET_TABBEDPANE) ||
                childType.equals(FormConstants.WIDGET_TABLE)) {
            if (FormConstants.WIDGET_PANEL.equals(parentType)) return true;
            else if (FormConstants.WIDGET_PAGELET.equals(parentType)) return true;
            else if (FormConstants.WIDGET_TAB.equals(parentType)) return true;
            else return false;
        } else return false;
    }

    public void addNode(String node_type, int x, int y) throws Exception {
        MbengNode node = desc_doc.newNode(node_type, "", " ", ' ');
        node.setAttribute(FormConstants.FORMATTR_LABEL, "New " + node_type);
        MbengNode parent;
//        if (menuEditor.isVisible()) {
//            System.out.println("In menu editor up");
//            Rectangle rect = menuEditor.getBounds();
//            if (x<rect.x || x>rect.x+rect.width || y<rect.y || y>rect.y+rect.height) {
//                throw new Exception("You must drop into menu dialog when it is up");
//            }
//            parent = menuEditor.getMenuNode();
//        }
//        else
        {
            parent = objectAt(desc_doc.getRootNode(), x, y);
            while (!isContainer(parent)) parent = parent.getParent();
        }

        if (!checkChildCompatible(parent.getName(), node_type)) {
            throw new Exception(node_type + " cannot be in a " + parent.getName());
        }

        parent.appendChild(node);
        selected_node = node;
//        System.out.println("add this to " + parent.getAttribute("NAME") + " " + x + " " + y);
        Container container = (Container)generator.getWidget(parent);
        Container p = container;
        while (p!=this) {
            x -= p.getX();
            y -= p.getY();
            p = p.getParent();
        }
//        System.out.println("Translated xy " + x + " " +y);
        generator.create_component(node, x-60, x, y, container);
        at_anchor = -1;
        requestFocus();
        repaint();
    }

    public void deleteNode(MbengNode node) throws Exception {
        MbengNode parent = node.getParent();
        parent.removeChild(node);
        selected_node = null;
        at_anchor = -1;
        generator.deleteNode(parent, node);
        repaint();
    }

    public void setFormXml(MbengDocument doc, FormDataDocument dataxml) throws Exception {
        // clean first
    	for (int k=this.getComponentCount(); k>0; k--) {
    		this.remove(k-1);
    	}
        // then set up
        selected_node = null;
        at_anchor = -1;
        desc_doc = doc;
        java.awt.Rectangle aRect = new java.awt.Rectangle(0,0,64,64);
        scrollRectToVisible(aRect);
        Rectangle vr = generator.generate(desc_doc, dataxml);
        pagelet_w = vr.width;
        pagelet_h = vr.height;
        setPreferredSize(new Dimension(pagelet_w, pagelet_h));
    }

    public void setPageSize(int w, int h) {
    	pagelet_w = w;
    	pagelet_h = h;
    	desc_doc.getRootNode().setAttribute(FormConstants.FORMATTR_VW, Integer.toString(w));
    	desc_doc.getRootNode().setAttribute(FormConstants.FORMATTR_VW, Integer.toString(w));
    	repaint();
    }

    public MbengDocument getFormXml() {
        return desc_doc;
    }

    MbengNode getSelectedNode() {
        return selected_node;
    }

    void setSelectedNode(MbengNode node) {
        selected_node = node;
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        Component src = (Component)e.getSource();
        MbengNode node = generator.getNode(src);
        if (cmd.equals(FormConstants.ACTION_MENU)) {
        	MenuButton button = (MenuButton)src;
        	JPopupMenu menu = button.getMenu();
        	menu.show(src, src.getWidth()-10, src.getHeight()-10);
        } else {
        	showWidgetDialog(node);
    	}
    }

    public SwingFormGenerator getGenerator() {
    	return generator;
    }


}
