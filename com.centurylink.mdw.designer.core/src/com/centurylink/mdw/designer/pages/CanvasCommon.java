/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.pages;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;

import com.centurylink.mdw.designer.display.EditableCanvasText;
import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.display.GraphCommon;
import com.centurylink.mdw.designer.display.GraphFragment;
import com.centurylink.mdw.designer.display.Label;
import com.centurylink.mdw.designer.display.Link;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.display.Selectable;
import com.centurylink.mdw.designer.display.SubGraph;
import com.centurylink.mdw.designer.display.TextNote;
import com.centurylink.mdw.designer.icons.Icon;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.utils.CanvasTextEditor;
import com.centurylink.mdw.designer.utils.GraphClipboard;
import com.centurylink.mdw.designer.utils.LabelEditor;
import com.centurylink.mdw.model.data.common.Changes;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.data.work.WorkStatus;

public abstract class CanvasCommon extends JPanel
{
	protected Object selected_obj;
	protected Rectangle marquee;
    protected int at_anchor;           // for node: -2 (label), -1,0,1,2,3
                                    // for link: -2 (label), -1, 0, ...
    protected int delta_x;             // used for node and anchor selection
    protected int delta_y;             // used for node and anchor selection
    protected Stroke normal_stroke=null, line_stroke=null, dash_stroke, grid_stroke;
    private Font title_font;
    protected Font normal_font;
    private static Color NEW_COLOR = Color.MAGENTA;
    private static Color LIGHT_BLUE = new Color(140, 155, 220);
    private boolean dragging;           // true if dragging for link or node
    private boolean drag_to_create_link;
    private int drag_x, drag_y;         // used when dragging for link
    private EditableCanvasText editing_obj;       // null if not in editing mode
    private LabelEditor labelEditor=null;
    private CanvasTextEditor textEditor;
    protected JPopupMenu popup_activity, popup_transition;
    private static boolean useLabelEditor = true;	// false does not yet work
    private static boolean updateLabelWhileTyping = false;
    public boolean editable;
    private int grid_size = 10;

    protected Cursor default_cursor;
    private Cursor ne_resize_cursor, nw_resize_cursor, crosshair_cursor, hand_cursor;

    private IconFactory iconFactory;
    protected IconFactory getIconFactory() { return iconFactory; }

    public CanvasCommon(IconFactory iconFactory) {
        this.iconFactory = iconFactory;
        normal_font = new Font("SansSerif", Font.PLAIN, 12);
        title_font = new Font("SansSerif", Font.BOLD, 18);
        drag_to_create_link = false;
        marquee = null;
        editable = false;
    }

    protected JPopupMenu createPopup(String[] labels, String[] actions,
    		ActionListener listener) {
        JPopupMenu popup = new JPopupMenu();
        popup.setInvoker(this);
        for (int i=0; i<actions.length; i++) {
        	JMenuItem menuItem = new JMenuItem(labels[i]);
        	menuItem.setActionCommand(actions[i]);
        	menuItem.addActionListener(listener);
            popup.add(menuItem);
        }
//        popup.addPopupMenuListener(this);
        return popup;
    }

    public int getAnchor() {
        return at_anchor;
    }

    public Rectangle getMarquee() {
        return marquee;
    }

    protected void drawConnector(Graphics g, Graph process, Link conn) {
        Graphics2D g2 = (Graphics2D)g;
        int n = conn.getNumberOfControlPoints();
        if (n<2) conn.calcLinkPosition(0, process.arrowstyle);
        String labelOrEventType = conn.getLabelAndEventType();
        if (conn.isNew()) g.setColor(NEW_COLOR);
        else g.setColor(conn.color);
        if (labelOrEventType!=null && labelOrEventType.length()==0) labelOrEventType = null;
        if (conn.isDeleted()) g2.setStroke(dash_stroke);
        else g2.setStroke(line_stroke);
        g2.draw(conn.getShape());
        if (conn.from!=conn.to) g2.fill(conn.getArrow(process.arrowstyle));
        g2.setStroke(normal_stroke);
        if (labelOrEventType!=null) {
            conn.label = new Label(g, labelOrEventType);
            if (conn.color != Color.LIGHT_GRAY)
              g.setColor(Color.DARK_GRAY);
            conn.label.draw(g, conn.lx, conn.ly);
            g.setColor(conn.color);
        } else conn.label = null;
    }

    private void drawMainProcess(Graphics g, Graph graph) {
        String proc_name = graph.getName();
        g.setFont(title_font);
        int lw = g.getFontMetrics().stringWidth(proc_name);
        int ascent = g.getFontMetrics().getAscent();
        int descent = g.getFontMetrics().getDescent();
        graph.lw = lw+4;
        graph.lh = ascent+descent+4;
        g.setColor(Color.BLACK);
        g.drawString(proc_name, graph.lx+2, graph.ly+2+ascent);
        g.setColor(Color.green);
        g.drawRoundRect(graph.lx, graph.ly, graph.lw, graph.lh, 5, 5);
        g.setFont(normal_font);
    }

    private boolean onSelectableAnchor(int x, int y, int x1, int y1, int w1, int h1) {
        if (Math.abs(x1-x)<=3 && Math.abs(y1-y)<=3) {
            at_anchor = 0;
            delta_x = x-x1;
            delta_y = y-y1;
            return true;
        } else if (Math.abs(x1+w1-x)<=3 && Math.abs(y1-y)<=3) {
            at_anchor = 1;
            delta_x = x-(x1+w1);
            delta_y = y-y1;
            return true;
        } else if (Math.abs(x1+w1-x)<=3 && Math.abs(y1+h1-y)<=3) {
            at_anchor = 2;
            delta_x = x-(x1+w1);
            delta_y = y-(y1+h1);
            return true;
        } else if (Math.abs(x1-x)<=3 && Math.abs(y1+h1-y)<=3) {
            at_anchor = 3;
            delta_x = x-x1;
            delta_y = y-(y1+h1);
            return true;
        } else return false;
    }

    public Object objectAt(Graph process, int x, int y, Graphics g) {
        Node node;
        Link link;
        if (selected_obj!=null) {
            if (selected_obj instanceof Node) {
                node = (Node)selected_obj;
                if (onSelectableAnchor(x, y, node.x, node.y, node.w, node.h))
                    return node;
                else if (node.labelOnPoint(g, x, y)) {
                    at_anchor = -2;
                    delta_x = x - node.x;
                    delta_y = y - node.y;
                    return node;
                }
            } else if (selected_obj instanceof Link) {
                link = (Link)selected_obj;
                int cx, cy, n = link.getNumberOfControlPoints();
                for (int i=0; i<n; i++) {
                    cx = link.getControlPointX(i);
                    cy = link.getControlPointY(i);
                    if (Math.abs(cx-x)<=3 && Math.abs(cy-y)<=3) {
                        at_anchor = i;
                        delta_x = x-cx;
                        delta_y = y-cy;
                        return link;
                    }
                }
                if (link.labelOnPoint(g, x, y)) {
                    at_anchor = -2;
                    delta_x = x-link.lx;
                    delta_y = y-link.ly;
                    return link;
                }
            } else if (selected_obj instanceof SubGraph) {
                SubGraph subgraph = (SubGraph)selected_obj;
                if (subgraph.isSwimLane()) {
                    if (onSelectableAnchor(x, y, subgraph.x, subgraph.y, SubGraph.SWIMLANE_LABEL_BAR_WIDTH, subgraph.h))
                        return subgraph;
                } else {
                    if (onSelectableAnchor(x, y, subgraph.x, subgraph.y, subgraph.w, subgraph.h))
                        return subgraph;
                }
            } else if (selected_obj instanceof Graph) {
                if (onSelectableAnchor(x, y, process.lx, process.ly, process.lw, process.lh))
                    return process;
            } else  if (selected_obj instanceof TextNote) {
            	TextNote note = (TextNote)selected_obj;
                if (onSelectableAnchor(x, y, note.x, note.y, note.w, note.h))
                    return note;
            }
        }
        at_anchor = -1;
        Object obj = process.objectAt(x, y, g);
        if (obj!=null) {
            if (obj instanceof Node) {
                delta_x = x - ((Node)obj).x;
                delta_y = y - ((Node)obj).y;
            } else if (obj instanceof SubGraph) {
                delta_x = x - ((SubGraph)obj).x;
                delta_y = y - ((SubGraph)obj).y;
            } else if (obj instanceof Graph) {
                delta_x = x - ((Graph)obj).lx;
                delta_y = y - ((Graph)obj).ly;
            } else if (obj instanceof TextNote) {
                delta_x = x - ((TextNote)obj).x;
                delta_y = y - ((TextNote)obj).y;
            }
            return obj;
        }
        return null;
    }

    private void drawNodeOrSubgraphStatus(String status,
            Graphics g, int x, int y, int w, int h, boolean exterior)
    {
        int thick0 = 8;                 // thickness of latest activity instance
        int thick = 4;                  // thickness of earlier activity instances
        int max = 10;                   // show only latest 10 entries
        int n = status.length();
        if (n>max) n = max;
        for(int j=0; j<n; j++) {
            int statusCode = Integer.parseInt(status.substring(j,j+1));
            if(statusCode == WorkStatus.STATUS_PENDING_PROCESS.intValue())
                g.setColor(Color.BLUE);
            else if(statusCode == WorkStatus.STATUS_IN_PROGRESS.intValue())
                g.setColor(Color.GREEN);
            else if(statusCode == WorkStatus.STATUS_COMPLETED.intValue())
                g.setColor(Color.DARK_GRAY);
            else if(statusCode == WorkStatus.STATUS_FAILED.intValue())
                g.setColor(Color.RED);
            else if(statusCode == WorkStatus.STATUS_CANCELLED.intValue())
                g.setColor(Color.LIGHT_GRAY);
            else if(statusCode == WorkStatus.STATUS_WAITING.intValue())
                g.setColor(Color.YELLOW);
            else if(statusCode == WorkStatus.STATUS_HOLD.intValue())
                g.setColor(Color.CYAN);
            else    // not possible
                g.setColor(Color.CYAN);
            if (exterior) {
                int k = n-j;
                if (j==0) {
                    g.fillRect(x-thick*k-(thick0-thick) , y-thick*k-(thick0-thick),
                            w+thick*2*k+2*(thick0-thick), h+thick*2*k+2*(thick0-thick));
                } else {
                    g.fillRect(x-thick*k , y-thick*k, w+thick*2*k, h+thick*2*k);
                }
                g.setColor(Color.WHITE);
                k = n-j-1;
                g.fillRect(x-thick*k-1 , y-thick*k-1, w+thick*2*k+2, h+thick*2*k+2);
            } else {
                int x1, y1, w1, h1;
                if (j==0) {
                    g.fillRect(x, y, w, h);
                    x1 = x + (thick0-thick);
                    y1 = y + (thick0-thick);
                    w1 = w - 2*(thick0-thick);
                    h1 = h - 2*(thick0-thick);
                } else {
                    x1 = x+thick*j+(thick0-thick);
                    y1 = y+thick*j+(thick0-thick);
                    w1 = w-thick*2*j-2*(thick0-thick);
                    h1 = h-thick*2*j-2*(thick0-thick);
                    if (w1>0 && h1>0) g.fillRect(x1, y1, w1, h1);
                }
                g.setColor(Color.WHITE);
                x1 += thick - 1;
                y1 += thick - 1;
                w1 -= 2*thick - 2;
                h1 -= 2*thick - 2;
                if (w1>0 && h1>0) g.fillRect(x1, y1, w1, h1);
            }
        }
    }

    protected void drawNode(Node node, Graphics g, boolean drawStatus,
            String nodestyle) {
        String iconname = node.getIconName();
        javax.swing.Icon icon = iconFactory.getIcon(iconname);
        if(drawStatus && node.getStatus()!=null)
            drawNodeOrSubgraphStatus(node.getStatus(), g, node.x, node.y, node.w, node.h,
                    nodestyle.equals(Node.NODE_STYLE_ICON));
        if (node.isDeleted()) ((Graphics2D)g).setStroke(dash_stroke);
        if (nodestyle.equals(Node.NODE_STYLE_BOX)) {
            Icon.drawBox(g, node, icon, false);
        } else if (nodestyle.equals(Node.NODE_STYLE_BOX_ICON)) {
            Icon.drawBox(g, node, icon, true);
        } else {
            if (icon instanceof ImageIcon) {
                Icon.drawImage(g, node, (ImageIcon)icon, iconFactory);
            } else ((Icon)icon).draw(g, node);
            if (node.isNew()) {
            	g.setColor(NEW_COLOR);
            	g.drawRoundRect(node.x-2, node.y-2, node.w+4, node.h+4, 12, 12);
            }
        }
        ((Graphics2D)g).setStroke(normal_stroke);
    }

    protected void draw_textnote(Graphics g, TextNote note) {
    	g.setColor(Color.yellow);
    	g.draw3DRect(note.x, note.y, note.w, note.h, true);
    	g.setColor(Color.black);
    	if (note.textarea!=null) {
    		g.translate(note.x+1, note.y+1);
    		note.textarea.paint(g);
    		g.translate(-note.x-1, -note.y-1);
    	} else {
    		String content = note.vo.getContent();
    		if (content!=null) g.drawString(content, note.x+2, note.y+25);
    	}
    }

    private void draw_subgraph(Graphics g, Graph process, SubGraph subgraph,
            boolean drawStatus) {
        if(drawStatus && subgraph.getStatus()!=null)
            drawNodeOrSubgraphStatus(subgraph.getStatus(), g,
                    subgraph.x, subgraph.y, subgraph.w, subgraph.h,
                    process.nodestyle.equals(Node.NODE_STYLE_ICON));
        if (subgraph.isNew()) g.setColor(NEW_COLOR);
        else if (subgraph.isDeleted()) g.setColor(Color.LIGHT_GRAY);
        else g.setColor(LIGHT_BLUE);
        g.drawRoundRect(subgraph.x, subgraph.y, subgraph.w, subgraph.h, 12, 12);
//        String subtype = subgraph.getAttribute(WorkAttributeConstant.EMBEDDED_PROCESS_TYPE);
//        if (subtype==null || subtype.equals(ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS))
//            g.setColor(Color..red);
//        else
        g.setColor(Color.BLACK);
        subgraph.lx = subgraph.x+10;
        subgraph.ly = subgraph.y+4;
        g.drawString(subgraph.getName(), subgraph.lx, subgraph.ly);
        if (Node.ID_DATABASE.equals(process.getNodeIdType())) {
            g.setColor(Color.GRAY);
            g.drawString("["+subgraph.getProcessVO().getProcessId().toString()+"]",
                    subgraph.x+10, subgraph.y+subgraph.h+4);
        } else if (Node.ID_LOGICAL.equals(process.getNodeIdType())) {
        	g.setColor(Color.GRAY);
            g.drawString("["+subgraph.getLogicalId()+"]",
                    subgraph.x+10, subgraph.y+subgraph.h+4);
        } else if (Node.ID_REFERENCE.equals(process.getNodeIdType())) {
            g.setColor(Color.GRAY);
            g.drawString("["+subgraph.getReferenceId()+"]",
                    subgraph.x+10, subgraph.y+subgraph.h+4);
        }  else if (Node.ID_SEQUENCE.equals(process.getNodeIdType())) {
            g.setColor(Color.GRAY);
            g.drawString("["+String.valueOf(subgraph.getSequenceId())+"]",
                    subgraph.x+10, subgraph.y+subgraph.h+4);
        }
        for (Node node : subgraph.nodes) {
            drawNode(node, g, drawStatus, process.nodestyle);
        }
        if (drawStatus) {
            for (Link conn : subgraph.links) {
                if (!conn.isHidden() && conn.getColor()==Color.LIGHT_GRAY) drawConnector(g, process, conn);
            }
            for (Link conn : subgraph.links) {
                if (!conn.isHidden() && conn.getColor()!=Color.LIGHT_GRAY) drawConnector(g, process, conn);
            }
        } else {
            for (Link link : subgraph.links) {
                if (!link.isHidden()) this.drawConnector(g, process, link);
            }
        }
    }

    protected void draw_graph(Graphics g, Graph process, boolean drawStatus) {
        if (g instanceof Graphics2D && normal_stroke==null) {
            normal_stroke = ((Graphics2D)g).getStroke();
            line_stroke = new BasicStroke(3.0f);
            float[] dash = {10.0f, 10.0f};
            dash_stroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
            		BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f);
            grid_stroke = new BasicStroke(0.2f);
        }
        if (editable) {
            Dimension canvas_size = this.getSize();
        	g.setColor(Color.GRAY);
        	((Graphics2D)g).setStroke(grid_stroke);
        	for (int x=grid_size; x<canvas_size.width; x+=grid_size) {
        		g.drawLine(x, 0, x, canvas_size.height);
        	}
        	for (int y=grid_size; y<canvas_size.height; y+=grid_size) {
        		g.drawLine(0, y, canvas_size.width, y);
        	}
        }
        g.setFont(normal_font);
        if (drawStatus) {
            for (Link conn : process.links) {
                if (!conn.isHidden() && conn.getColor()==Color.LIGHT_GRAY) drawConnector(g, process, conn);
            }
            for (Link conn : process.links) {
                if (!conn.isHidden() && conn.getColor()!=Color.LIGHT_GRAY) drawConnector(g, process, conn);
            }
        } else {
            for (Link conn : process.links) {
                if (!conn.isHidden()) drawConnector(g, process, conn);
            }
        }
        drawMainProcess(g, process);
        for (Node node : process.nodes) {
            drawNode(node, g, drawStatus, process.nodestyle);
        }
        for (SubGraph subgraph : process.subgraphs) {
            draw_subgraph(g, process, subgraph, drawStatus);
        }
        if (process.notes!=null) {
        	for (TextNote note : process.notes) {
        		draw_textnote(g, note);
        	}
        }
    }

    public Object getSelectedObject() {
        return selected_obj;
    }

    public void setSelectedObject(Object obj) {
    	closeLabelEditor();
        selected_obj = obj;
    }

    protected void drawSelectionBox(Graphics g, Node node) {
        g.setColor(Color.RED);
        g.fillRect(node.x-2,node.y-2,4,4);
        g.fillRect(node.x+node.w-2,node.y-2,4,4);
        g.fillRect(node.x+node.w-2,node.y+node.h-2,4,4);
        g.fillRect(node.x-2,node.y+node.h-2,4,4);
    }

    protected void drawSelectionBox(Graphics g, Link conn /*, boolean isSimplified*/) {
        int n=conn.getNumberOfControlPoints();
        g.setColor(Color.RED);
        for (int i=0; i<n; i++) {
            g.fillRect(conn.getControlPointX(i)-2,conn.getControlPointY(i)-2,4,4);
        }
    }

    protected void drawSelectionBox(Graphics g, SubGraph subgraph) {
        g.setColor(Color.RED);
        if (subgraph.isSwimLane()) {
            g.drawRect(subgraph.x,subgraph.y,
                    SubGraph.SWIMLANE_LABEL_BAR_WIDTH,subgraph.h);
        } else {
            g.fillRect(subgraph.x-2,subgraph.y-2,4,4);
            g.fillRect(subgraph.x+subgraph.w-2,subgraph.y-2,4,4);
            g.fillRect(subgraph.x+subgraph.w-2,subgraph.y+subgraph.h-2,4,4);
            g.fillRect(subgraph.x-2,subgraph.y+subgraph.h-2,4,4);
        }
    }

    protected void drawSelectionBox(Graphics g, Graph graph) {
        g.setColor(Color.RED);
        g.fillRect(graph.lx-2,graph.ly-2,4,4);
        g.fillRect(graph.lx+graph.lw-2,graph.ly-2,4,4);
        g.fillRect(graph.lx+graph.lw-2,graph.ly+graph.lh-2,4,4);
        g.fillRect(graph.lx-2,graph.ly+graph.lh-2,4,4);
    }

    protected void drawSelectionBox(Graphics g, TextNote note) {
        g.setColor(Color.RED);
        g.fillRect(note.x-2,note.y-2,4,4);
        g.fillRect(note.x+note.w-2,note.y-2,4,4);
        g.fillRect(note.x+note.w-2,note.y+note.h-2,4,4);
        g.fillRect(note.x-2,note.y+note.h-2,4,4);
    }

    protected void drawSelectionBox(Graphics g, GraphFragment graphfragment) {
    	for (Node node : graphfragment.nodes) {
    		drawSelectionBox(g, node);
    	}
    	for (Link link : graphfragment.links) {
    		drawSelectionBox(g, link);
    	}
    	for (SubGraph subgraph : graphfragment.subgraphs) {
    		drawSelectionBox(g, subgraph);
    	}
    }

    public void zoom(Graph process, int zoomLevel) {
        process.zoom = zoomLevel;
        Dimension size = process.getGraphSize();
        if (process.zoom==0) {
            Dimension panelSize = getParent().getSize();
            if (size.width>panelSize.width || size.height>panelSize.height) {
                int zoom1 = panelSize.width*100/(size.width+40);
                int zoom2 = panelSize.height*100/(size.height+40);
                process.zoom = Math.min(zoom1,zoom2);
            } else process.zoom = 100;
            size = getMinimumSize();
        } else {
            size.width = size.width*process.zoom/100 + 40;
            size.height = size.height*process.zoom/100 + 40;
        }
        setPreferredSize(size);
        setSize(size);
        repaint();
    }

    private void resizeCanvasToFit(int mx, int my, int zoom) {
        mx += 40;
        my += 40;
        Dimension canvas_size = this.getSize();
        if (zoom!=100) {
            mx = mx * zoom / 100;
            my = my * zoom / 100;
        }
        if (mx+40 > canvas_size.width || my+40 > canvas_size.height) {
            if (mx > canvas_size.width) canvas_size.width = mx;
            if (my > canvas_size.height) canvas_size.height = my;
            this.setPreferredSize(canvas_size);
            this.setSize(canvas_size);         // need both size and preferred size to be set
        }
    }

    protected void moveNode(int ex, int ey) {
        Node selected_node = (Node)selected_obj;
        GraphCommon graph = selected_node.graph;
        int x = selected_node.x;
        int y = selected_node.y;
        int w = selected_node.w;
        int h = selected_node.h;
        x = ex-delta_x;
        y = ey-delta_y;
        if (x<graph.x) x = graph.x;
        else if (x+w>graph.x+graph.w) x = graph.x+graph.w-w;
        if (y<graph.y) y = graph.y;
        else if (y+h>graph.y+graph.h) y = graph.y+graph.h-h;
        selected_node.x = x;
        selected_node.y = y;
        Graph process = (graph instanceof SubGraph)? ((SubGraph)graph).getGraph() : (Graph)graph;
        graph.recalcLinkPosition(selected_node, process.arrowstyle);
//      selected_node.save();
        if (!process.isReadonly()) process.setDirtyLevel(Graph.GEOCHANGE);
        if (graph instanceof SubGraph && process.isSwimLanes()) {
            if (graph.w < x+w+40) graph.w = x+w+40;
        }
        resizeCanvasToFit(x+w, y+h, process.zoom);
    }

    protected void moveSubgraph(int ex, int ey) {
        SubGraph subgraph = (SubGraph)selected_obj;
        Graph graph = subgraph.getGraph();
        int x = subgraph.x;
        int y = subgraph.y;
        int w = subgraph.w;
        int h = subgraph.h;
        x = ex-delta_x;
        y = ey-delta_y;
        if (x<graph.x) x = graph.x;
        else if (x+w>graph.x+graph.w) x = graph.x+graph.w-w;
        if (y<graph.y) y = graph.y;
        else if (y+h>graph.y+graph.h) y = graph.y+graph.h-h;
        subgraph.move(x, y, graph.arrowstyle);
        if (!graph.isReadonly()) graph.setDirtyLevel(Graph.GEOCHANGE);
        resizeCanvasToFit(x+w, y+h, graph.zoom);
    }

    protected void moveTextNote(int ex, int ey) {
    	TextNote selected_note = (TextNote)selected_obj;
    	Graph graph = selected_note.graph;
        int x = selected_note.x;
        int y = selected_note.y;
        int w = selected_note.w;
        int h = selected_note.h;
        x = ex-delta_x;
        y = ey-delta_y;
        if (x<graph.x) x = graph.x;
        else if (x+w>graph.x+graph.w) x = graph.x+graph.w-w;
        if (y<graph.y) y = graph.y;
        else if (y+h>graph.y+graph.h) y = graph.y+graph.h-h;
        selected_note.x = x;
        selected_note.y = y;
        if (!graph.isReadonly()) graph.setDirtyLevel(Graph.GEOCHANGE);
    }

    protected void moveGraphLabel(int ex, int ey) {
        Graph graph = (Graph)selected_obj;
        int x = graph.lx;
        int y = graph.ly;
        int w = graph.lw;
        int h = graph.lh;
        x = ex-delta_x;
        y = ey-delta_y;
        if (x<graph.x) x = graph.x;
        else if (x+w>graph.x+graph.w) x = graph.x+graph.w-w;
        if (y<graph.y) y = graph.y;
        else if (y+h>graph.y+graph.h) y = graph.y+graph.h-h;
        graph.lx = x;
        graph.ly = y;
        if (!graph.isReadonly()) graph.setDirtyLevel(Graph.GEOCHANGE);
    }

    private void moveBackFrom(Link link, String arrowstyle) {
        int newX = link.getControlPointX(0);
        int newY = link.getControlPointY(0);
        if (newX<link.from.x-Link.gap) newX = link.from.x-Link.gap;
        else if (newX>link.from.x+link.from.w+Link.gap) newX = link.from.x+link.from.w+Link.gap;
        if (newY<link.from.y-Link.gap) newY = link.from.y-Link.gap;
        else if (newY>link.from.y+link.from.h+Link.gap) newY = link.from.y+link.from.h+Link.gap;
        link.moveControlPoint(0, newX, newY, arrowstyle);
    }

    private void moveBackTo(Link link, String arrowstyle) {
        int k = link.getNumberOfControlPoints()-1;
        int newX = link.getControlPointX(k);
        int newY = link.getControlPointY(k);
        if (newX<link.to.x-Link.gap) newX = link.to.x-Link.gap;
        else if (newX>link.to.x+link.to.w+Link.gap) newX = link.to.x+link.to.w+Link.gap;
        if (newY<link.to.y-Link.gap) newY = link.to.y-Link.gap;
        else if (newY>link.to.y+link.to.h+Link.gap) newY = link.to.y+link.to.h+Link.gap;
        link.moveControlPoint(k, newX, newY, arrowstyle);
    }

	private void checkIfMoveIntoSubprocess(Node node, int x, int y, DesignerPage page, boolean recordchange) {
		if (node.graph instanceof SubGraph) return;
		Graph process = (Graph)node.graph;
		for (Link l : process.links) {
			if (l.from==node || l.to==node) return;
		}
		for (SubGraph subgraph : process.subgraphs) {
			if (subgraph.containsPoint(x, y)) {
		        boolean toSubproc = page.getConfirmation("Do you want to move the node into the subprocess?");
		        if (toSubproc) {
		        	node.save_temp_vars();
		        	process.removeNode(node, recordchange);
		        	selected_obj = process.addNode(subgraph, node.nodet, 0, 0, recordchange, true);
		        }
				break;
			}
		}
	}

    protected void mouseReleased(MouseEvent arg0, Graph main_graph, String curr_link_type,
    		DesignerPage page, boolean recordchange) {
//        if (main_graph.isReadonly()) return;
        int x = arg0.getX();
        int y = arg0.getY();
        if (main_graph.zoom!=100) {
            x = x * 100 / main_graph.zoom;
            y = y * 100 / main_graph.zoom;
        }
        Object obj = main_graph.objectAt(x, y, getGraphics());
        if (arg0.getButton()!=1) {
        	if (arg0.isPopupTrigger() && obj!=null) {
        		JPopupMenu popup;
        		if (obj instanceof Node) {
        			popup = this.popup_activity;
    				selected_obj = obj;
        		} else if (obj instanceof Link) {
        			popup = this.popup_transition;
    				selected_obj = obj;
        		} else popup = null;
        		if (popup!=null) {
        			popup.show(this, arg0.getX(), arg0.getY());
        		}
        	}
        	return;
    	}
        if (dragging) {
            if (drag_to_create_link) {
                Node startnode = (Node)selected_obj;
                Node endnode = (obj!=null&&(obj instanceof Node))?(Node)obj:null;
                if (endnode!=null && endnode.graph.equals(startnode.graph)) {
                    int ctrl_pts = 2;
                    String linktype = curr_link_type;
                    if (endnode==startnode) {
                        linktype = Link.CURVE;
                    } else if (linktype.equals(Link.CURVE)) {
                        ctrl_pts = 4;
                    }
                    Link conn = main_graph.addLink(startnode, endnode,
                            EventType.FINISH, linktype, ctrl_pts, recordchange);
                    selected_obj = conn;
                } else selected_obj = null;
            } else if (selected_obj instanceof Link && at_anchor>=0) {
                Link link = (Link)selected_obj;
                Node node = (obj!=null&&(obj instanceof Node))?(Node)obj:null;
                if (link.to==link.from) {  // a link to the node itself
                    if (at_anchor==0 &&
                            (node==null||node!=link.from)) {  // move back to the node
                        moveBackFrom(link, main_graph.arrowstyle);
                    }
                } else if (at_anchor==0) {
                    if (node==null || node==link.to) {  // move back to the node
                        moveBackFrom(link, main_graph.arrowstyle);
                    } else if (node!=link.from&&node.graph.equals(link.from.graph)) {
                        // change 'from'
                        link.setFrom(node);
                    } // else do nothing
                } else if (at_anchor==link.getNumberOfControlPoints()-1) {
                    if (node==null || node==link.from) {  // move back to the node
                        moveBackTo(link, main_graph.arrowstyle);
                    } else if (node!=link.to&&node.graph.equals(link.to.graph)) {
                        // change 'to'
                        link.setTo(node);
                    } // else do nothing
                } else {     // a middle link
                    if (link.isElbowType()) {
                        if (at_anchor==1) {
                            moveBackFrom(link, main_graph.arrowstyle);
                        }
                        if (at_anchor==link.getNumberOfControlPoints()-2) {
                            moveBackTo(link, main_graph.arrowstyle);
                        }
                    }
                }
            } else if (selected_obj instanceof SubGraph) {
                mouseReleased_subgraph();
        	} else if (selected_obj instanceof Node && at_anchor<0) {	// move node
				checkIfMoveIntoSubprocess((Node)selected_obj, x, y, page, recordchange);
            }
            repaint();
            dragging = false;
//        } else if (useLabelEditor && editing_obj!=null) {
//            labelEditor.setZoom(main_graph.zoom);
//            labelEditor.setVisible(true);
//            repaint();  // TODO do we need this?
        } else if (useLabelEditor && obj!=null && at_anchor==-2) {
            if (obj instanceof Node) {
            	if (!main_graph.isReadonly()) {
            		openLabelEditor((Node)obj, main_graph.zoom);
            	}
	        } else if (obj instanceof Link) {
	        	if (!main_graph.isReadonly()) {
	        		openLabelEditor((Link)obj, main_graph.zoom);
	        	}
	        } // else not possible (TextNote never has at_anchor==-2)
        } else if (useLabelEditor && obj instanceof TextNote
        		|| !useLabelEditor && textEditor!=null) {
        	if (!main_graph.isReadonly()) {
        		openLabelEditor((TextNote)obj, main_graph.zoom);
        	}
    	} else if (marquee!=null) {
			GraphFragment frag = new GraphFragment(main_graph, marquee);
			if (!frag.isEmpty()) {
				if (frag.isNode()) selected_obj = frag.getOnlyNode();
				else if (frag.isSubGraph()) selected_obj = frag.getOnlySubGraph();
				else selected_obj = frag;
			}
			marquee = null;
			repaint();
        }
        drag_to_create_link = false;
    }

    protected void mouseReleased_subgraph() {
        // to be overridden by subclasses when needed
    }

    private JFrame getFrame() {
    	Container ancester = this.getParent();
    	while (ancester!=null && !(ancester instanceof JFrame)) {
    		ancester = ancester.getParent();
    	}
    	return (JFrame)ancester;
    }

    private void openLabelEditor(Link link, int zoom) {
    	if (useLabelEditor) {
			editing_obj = link;
			String label = link.getLabelAndEventType();
			if (label==null) label = "";
			labelEditor.setText(label, link.lx, link.ly, zoom);
            labelEditor.setVisible(true);
            labelEditor.grabFocus();
		} else {
			textEditor = new CanvasTextEditor(getFrame(), link, link.lx, link.ly,
					120, 20, link.getLabelAndEventType(), this);
			textEditor.setVisible(true);
		}
	}

	private void openLabelEditor(Node node, int zoom) {
		if (useLabelEditor) {
			editing_obj = node;
			String label = node.getName();
			if (label==null) label = "";
			labelEditor.setText(label, node.lx, node.ly, zoom);
            labelEditor.setVisible(true);
            labelEditor.grabFocus();
		} else {
			textEditor = new CanvasTextEditor(getFrame(), node, node.lx, node.ly,
					120, 20, node.getName(), this);
			textEditor.setVisible(true);
		}
	}

	private void openLabelEditor(TextNote textNote, int zoom) {
		if (useLabelEditor) {
			editing_obj = textNote;
			String label = textNote.vo.getContent();
			if (label==null) label = "";
			labelEditor.setText(label, textNote.x+1, textNote.y+1, zoom);
    		labelEditor.setVisible(true);
            labelEditor.grabFocus();
		} else {
			textEditor = new CanvasTextEditor(getFrame(), textNote, textNote.x+1, textNote.y+1,
					textNote.w-2, textNote.h-2, textNote.vo.getContent(), this);
	    	textEditor.setVisible(true);
		}
	}

    private void closeLabelEditor() {
    	if (useLabelEditor) {
    		if (editing_obj!=null && !updateLabelWhileTyping) {
    			editing_obj.setText(labelEditor.getText());
    			if (editing_obj instanceof TextNote) {
    				int w = labelEditor.getWidth();
    				int h = labelEditor.getHeight();
    				if (w<80) w = 80;
    				if (h<20) h = 20;
    				TextNote textNote = (TextNote)editing_obj;
    				textNote.w = w+2;
    				textNote.h = h+2;
    				textNote.textarea.setSize(w, h);
    			}
    		}
    		editing_obj = null;
    		if (labelEditor!=null) labelEditor.setVisible(false);
    	} else {
	    	if (textEditor!=null) {
	    		textEditor.setVisible(false);
	    		textEditor = null;
	    	}
    	}
    }

    protected void mousePressed(MouseEvent arg0, Graph main_graph) {
    	closeLabelEditor();
        int x = arg0.getX();
        int y = arg0.getY();
        if (main_graph.zoom!=100) {
            x = x * 100 / main_graph.zoom;
            y = y * 100 / main_graph.zoom;
        }
        Object obj = objectAt(main_graph, x, y, getGraphics());
        if (selected_obj!=null && selected_obj instanceof GraphFragment && obj!=null
    			&& ((GraphFragment)selected_obj).contains(obj)) {
    			// dragging to move
        	drag_x = x;
        	drag_y = y;
        } else if (selected_obj!=null && obj!=null && obj instanceof Node && arg0.isControlDown()) {
        	GraphFragment frag;
        	if (selected_obj instanceof Node) {
        		frag = new GraphFragment(main_graph.getId());
        		frag.nodes.add((Node)selected_obj);
        		selected_obj = frag;
        	} else if (selected_obj instanceof SubGraph) {
        		frag = new GraphFragment(main_graph.getId());
        		frag.subgraphs.add((SubGraph)selected_obj);
        		selected_obj = frag;
        	} else if (selected_obj instanceof Link) {
        		frag = new GraphFragment(main_graph.getId());
        		frag.links.add((Link)selected_obj);
        		selected_obj = frag;
        	} else if (selected_obj instanceof GraphFragment) {
        		frag = (GraphFragment)selected_obj;
        	} else frag = null;
        	if (frag!=null) frag.nodes.add((Node)obj);
        	repaint();
        } else if (obj!=null) {
            selected_obj = obj;
            if (obj instanceof Node) {
                if (arg0.isShiftDown() && !main_graph.isReadonly()) {
                    drag_to_create_link = true;
                }
            }
//            closeLabelEditor(true);
            repaint();
        } else if (selected_obj!=null) {
            selected_obj = null;
//            closeLabelEditor(true);
            repaint();
			marquee = new Rectangle(arg0.getX(),arg0.getY(),0,0);
		} else {
			marquee = new Rectangle(arg0.getX(),arg0.getY(),0,0);
        }
        this.requestFocus();
    }

	private void drawRubberBand(Rectangle marquee, int neww, int newh) {
		Graphics g = this.getGraphics();
		g.setXORMode(getBackground());
        g.setColor(Color.cyan);
        g.drawRect(marquee.x, marquee.y, marquee.width, marquee.height);
    	marquee.width = neww;
    	marquee.height = newh;
        g.drawRect(marquee.x, marquee.y, marquee.width, marquee.height);
	}

    protected void mouseDragged(MouseEvent arg0, Graph main_graph) {
	    int x = arg0.getX();
	    int y = arg0.getY();
	    if (selected_obj!=null) {
            if (main_graph.zoom!=100) {
                x = x * 100 / main_graph.zoom;
                y = y * 100 / main_graph.zoom;
            }
            dragging = true;
            if (at_anchor>=0) {
                if (selected_obj instanceof Node) {
                    resizeNode(x, y, main_graph);
                } else if (selected_obj instanceof Link) {
                    ((Link)selected_obj).moveControlPoint(at_anchor, x-delta_x, y-delta_y, main_graph.arrowstyle);
                    if (!main_graph.isReadonly()) main_graph.setDirtyLevel(Graph.GEOCHANGE);
                } else if (selected_obj instanceof SubGraph) {
                    resizeSubGraph(x, y);
                } else if (selected_obj instanceof TextNote) {
                	resizeTextNote(x, y, main_graph);
                }
            } else {
                if (drag_to_create_link) {
                    drag_x = x;
                    drag_y = y;
                } else if (selected_obj instanceof Node) {
                    moveNode(x, y);
                } else if (selected_obj instanceof Link && at_anchor==-2) {
                    // move link label
                    Link link = (Link)selected_obj;
                    link.lx = x-delta_x;
                    link.ly = y-delta_y;
                } else if (selected_obj instanceof SubGraph) {
                    if (((SubGraph)selected_obj).isSwimLane()) moveSubgraph(0, y);
                    else moveSubgraph(x, y);
                } else if (selected_obj instanceof Graph) {
                    moveGraphLabel(x, y);
                } else if (selected_obj instanceof GraphFragment) {
    				((GraphFragment)selected_obj).shift(main_graph, x-drag_x, y-drag_y, main_graph.arrowstyle);
    				drag_x = x;
    				drag_y = y;
    				if (!main_graph.isReadonly()) main_graph.setDirtyLevel(Graph.GEOCHANGE);
                } else if (selected_obj instanceof TextNote) {
                    moveTextNote(x, y);
                }
            }
            repaint();
		} else if (marquee!=null) {
			if (x<0) x = 0;
			else if (x>this.getWidth()) x = this.getWidth();
			if (y<0) y = 0;
			else if (y>this.getHeight()) y = this.getHeight();
			drawRubberBand(marquee, x-marquee.x, y-marquee.y);
        }
    }

    protected void resizeSubGraph(int x, int y) {
        // to be overridden by subclasses when needed
    }

    private void resizeNode(int x, int y, Graph main_graph) {
        Node selected_node = (Node)selected_obj;
        Rectangle rect = new Rectangle(selected_node.x,
                selected_node.y, selected_node.w, selected_node.h);
        resizeRectangle(rect, x, y, 4);
        selected_node.x = rect.x;
        selected_node.y = rect.y;
        selected_node.w = rect.width;
        selected_node.h = rect.height;
        selected_node.graph.recalcLinkPosition(selected_node, main_graph.arrowstyle);
//      selected_node.save();
        if (!main_graph.isReadonly()) main_graph.setDirtyLevel(Graph.GEOCHANGE);
    }

    private void resizeTextNote(int x, int y, Graph main_graph) {
    	TextNote selected_note = (TextNote)selected_obj;
        Rectangle rect = new Rectangle(selected_note.x,
                selected_note.y, selected_note.w, selected_note.h);
        resizeRectangle(rect, x, y, 4);
        selected_note.x = rect.x;
        selected_note.y = rect.y;
        selected_note.w = rect.width;
        selected_note.h = rect.height;
        selected_note.textarea.setBounds(rect.x+1, rect.y+1, rect.width-2, rect.height-2);
        if (!main_graph.isReadonly()) main_graph.setDirtyLevel(Graph.GEOCHANGE);
    }

    protected void resizeRectangle(Rectangle rect, int x0, int y0, int min) {
        int t1, t2;
        if (at_anchor==0) {
            t1 = rect.x + rect.width;
            t2 = rect.y + rect.height;
            rect.x = x0-delta_x;
            rect.y = y0-delta_y;
            if (t1-rect.x<min) rect.x = t1 - min;
            if (t2-rect.y<min) rect.y = t2 - min;
            rect.width = t1 - rect.x;
            rect.height = t2 - rect.y;
        } else if (at_anchor==1) {
            t2 = rect.y + rect.height;
            rect.y = y0-delta_y;
            if (t2-rect.y<min) rect.y = t2 - min;
            rect.width = x0-(rect.x+delta_x);
            if (rect.width<min) rect.width = min;
            rect.height = t2 - rect.y;
        } else if (at_anchor==2) {
            rect.width = x0-(rect.x+delta_x);
            rect.height= y0-(rect.y+delta_y);
            if (rect.width<min) rect.width = min;
            if (rect.height<min) rect.height = min;
        } else if (at_anchor==3) {
            t1 = rect.x + rect.width;
            rect.x = x0-delta_x;
            if (t1-rect.x<min) rect.x = t1 - min;
            rect.width = t1 - rect.x;
            rect.height = y0-(rect.y+delta_y);
            if (rect.height<min) rect.height = min;
        }
    }

    protected void mouseMoved(MouseEvent arg0, Graph main_graph) {
        if (!dragging) {
            int x = arg0.getX();
            int y = arg0.getY();
            if (main_graph.zoom!=100) {
                x = x * 100 / main_graph.zoom;
                y = y * 100 / main_graph.zoom;
            }
            Object obj = objectAt(main_graph, x, y, getGraphics());
            if (obj!=null) {
                if (at_anchor>=0) {
                    if (obj instanceof Node || obj instanceof SubGraph || obj instanceof TextNote) {
                        if (at_anchor==0 || at_anchor==2)
                            this.setCursor(nw_resize_cursor);
                        else this.setCursor(ne_resize_cursor);
                    } else if (obj instanceof Link) {
                        this.setCursor(crosshair_cursor);
                    }
                } else this.setCursor(hand_cursor);
            } else this.setCursor(default_cursor);
        }
    }

    protected void mouseClicked(MouseEvent arg0, Graph main_graph, DesignerPage page) {
        if (arg0.getClickCount()<2) return;
        closeLabelEditor();
        int x = arg0.getX();
        int y = arg0.getY();
        if (main_graph.zoom!=100) {
            x = x * 100 / main_graph.zoom;
            y = y * 100 / main_graph.zoom;
        }
        Object obj = main_graph.objectAt(x, y, getGraphics());
        if (obj==null) return;
        if (obj instanceof TextNote) {
        	// never happens
        } else if (obj instanceof Selectable) {    // Node, SubGraph or Graph
        	if (at_anchor==-2 && obj instanceof Node && !useLabelEditor) {
        		if (!main_graph.isReadonly())
        			this.openLabelEditor((Node)obj, main_graph.zoom);
        	} else {
	            //Get the Attribute data for this activity
        	}
        } else if (obj instanceof Link) {
        	if (at_anchor==-2 && !useLabelEditor) {
        		if (!main_graph.isReadonly())
        			this.openLabelEditor((Link)obj, main_graph.zoom);
        	}
        }
    }

    protected void initialize_editable(KeyListener keyListener) {
        selected_obj = null;
        textEditor = null;
        if (useLabelEditor) {
	        labelEditor = new LabelEditor(this);
	        labelEditor.setVisible(false);
	        labelEditor.setBackground(Color.pink);
	        labelEditor.setSelectionColor(Color.cyan);
	        labelEditor.setCaretColor(Color.red);
	        if (keyListener!=null) labelEditor.addKeyListener(keyListener);
	        add(labelEditor);
        } else labelEditor = null;
        dragging = false;
        default_cursor =
            Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    //  move_cursor =
    //          Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        nw_resize_cursor =
                Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
        ne_resize_cursor =
                Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
        crosshair_cursor =
            Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        hand_cursor =
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    protected void paintDraggingAndSelection(Graphics g) {
        if (dragging && drag_to_create_link) {
            if (selected_obj instanceof Node) {
                g.setColor(Color.GREEN);
                g.drawLine(((Node)selected_obj).x + delta_x,
                        ((Node)selected_obj).y + delta_y,
                        drag_x, drag_y);
            }
        } else if (selected_obj!=null) {
            if (selected_obj instanceof Node) {
                drawSelectionBox(g, (Node)selected_obj);
            } else if (selected_obj instanceof Link) {
                drawSelectionBox(g, (Link)selected_obj);
            } else if (selected_obj instanceof SubGraph) {
                drawSelectionBox(g, (SubGraph)selected_obj);
            } else if (selected_obj instanceof Graph) {
                drawSelectionBox(g, (Graph)selected_obj);
            } else if (selected_obj instanceof GraphFragment) {
            	drawSelectionBox(g, (GraphFragment)selected_obj);
            } else if (selected_obj instanceof TextNote) {
            	drawSelectionBox(g, (TextNote)selected_obj);
            }
        }
        if (useLabelEditor && selected_obj!=null /*&& at_anchor==-2 */) {
            if (labelEditor.isVisible()){
                Rectangle rect = labelEditor.getBounds();
                g.drawRect(rect.x-1, rect.y-1, rect.width+2, rect.height+2);
            } else {
                Label label;
                int lx, ly;
                if (selected_obj instanceof Node) {
//                    label = ((Node)selected_obj).label;
//                    lx = ((Node)selected_obj).lx;
//                    ly = ((Node)selected_obj).ly;
//                    g.drawRect(lx-1, ly-1, label.width+2, label.height+2);
                } else if (selected_obj instanceof Link){
                    label = ((Link)selected_obj).label;
                    if (label!=null) {
                    	lx = ((Link)selected_obj).lx;
                    	ly = ((Link)selected_obj).ly;
                    	g.drawRect(lx-1, ly-1, label.width+2, label.height+2);
                    }
                } else if (selected_obj instanceof TextNote) {
                    JTextArea textarea = ((TextNote)selected_obj).textarea;
                    lx = ((TextNote)selected_obj).x;
                    ly = ((TextNote)selected_obj).y;
                    g.drawRect(lx-1, ly-1, textarea.getWidth()+2, textarea.getHeight()+2);
                } // else GraphSegment
            }
        }
    }

    protected void keyPressed(KeyEvent arg0, Graph main_graph,
    		DesignerPage page, boolean recordchange) {
        if (useLabelEditor && arg0.getSource()==labelEditor) {
            return;
        }
        if (main_graph.isReadonly()) return;
        int keycode = arg0.getKeyCode();
        char ch = arg0.getKeyChar();
        if (selected_obj!=null) {
            if ((useLabelEditor && editing_obj!=null) ||
            		(!useLabelEditor && textEditor!=null)) {
                // do not need to do any thing - handled by editor
            } else if (selected_obj instanceof Node) {
                if (keycode==KeyEvent.VK_DELETE) {
                    if (page.getConfirmation("Are you sure you want to delete the node?")) {
                        main_graph.removeNode((Node)selected_obj, recordchange);
                        selected_obj = null;
                        repaint();
                    }
                } else if (ch!=KeyEvent.CHAR_UNDEFINED && !arg0.isControlDown()) {
                	String label = ((Node)selected_obj).getName();
                	if (label==null) label = "";
                	if (keycode!=KeyEvent.VK_BACK_SPACE) {
                		label = label+ch;
                		updateLabel(selected_obj, label, main_graph);
                	}
                	openLabelEditor((Node)selected_obj, main_graph.zoom);
                    repaint();
                } else {
                    if (keycode==KeyEvent.VK_DOWN || keycode==KeyEvent.VK_UP
                            || keycode==KeyEvent.VK_LEFT || keycode==KeyEvent.VK_RIGHT) {
                        handleArrowKey(keycode, arg0, (Node)selected_obj, main_graph);
                    }
                }
            } else if (selected_obj instanceof Link) {
                if (keycode==KeyEvent.VK_DELETE) {
                    if (page.getConfirmation("Are you sure you want to delete the link?")) {
                        main_graph.removeLink((Link)selected_obj, recordchange);
                        selected_obj = null;
                        repaint();
                    }
                } else if (ch!=KeyEvent.CHAR_UNDEFINED) {
                	String label = ((Link)selected_obj).getLabelAndEventType();
                	if (label==null) label = "";
                	if (label.length()==0) ((Link)selected_obj).calcLinkLabelPosition();
                	if (keycode!=KeyEvent.VK_BACK_SPACE) {
                		label = label+ch;
                		updateLabel(selected_obj, label, main_graph);
                	}
                	openLabelEditor((Link)selected_obj, main_graph.zoom);
                    repaint();
                } else {
                    if (keycode==KeyEvent.VK_DOWN || keycode==KeyEvent.VK_UP
                            || keycode==KeyEvent.VK_LEFT || keycode==KeyEvent.VK_RIGHT) {
                        if (at_anchor>=0) {
                            Link link = (Link)selected_obj;
                            int x = link.getControlPointX(at_anchor);
                            int y = link.getControlPointY(at_anchor);
                            switch (keycode) {
                            case KeyEvent.VK_UP: y--; break;
                            case KeyEvent.VK_DOWN: y++; break;
                            case KeyEvent.VK_LEFT: x--; break;
                            case KeyEvent.VK_RIGHT: x++; break;
                            }
                            link.moveControlPoint(at_anchor, x, y, main_graph.arrowstyle);
                            arg0.consume();
                            repaint();
                        }
                    }
                }
            } else if (selected_obj instanceof SubGraph) {
                if (keycode==KeyEvent.VK_DELETE) {
                    if (page.getConfirmation("Are you sure you want to delete the embedded process?")) {
                        main_graph.removeSubGraph((SubGraph)selected_obj);
                        selected_obj = null;
                        repaint();
                    }
                }
            } else if (selected_obj instanceof GraphFragment) {
                if (keycode==KeyEvent.VK_DELETE) {
                    if (page.getConfirmation("Are you sure you want to delete the selected objects?")) {
                    	GraphFragment frag = (GraphFragment)selected_obj;
                    	for (Link l : frag.links) {
                    		main_graph.removeLink(l, recordchange);
                    	}
                    	for (Node n : frag.nodes) {
                    		main_graph.removeNode(n, recordchange);
                    	}
                    	for (SubGraph sg : frag.subgraphs) {
                    		main_graph.removeSubGraph(sg);
                    	}
                        selected_obj = null;
                        repaint();
                    }
                }
            } else if (selected_obj instanceof TextNote) {
                if (keycode==KeyEvent.VK_DELETE) {
                    if (page.getConfirmation("Are you sure you want to delete the selected note?")) {
                        main_graph.removeTextNote((TextNote)selected_obj);
                        selected_obj = null;
                        repaint();
                    }
                }
            }
        }
    }

    protected void keyReleased(KeyEvent arg0, Graph main_graph, boolean recordchange) {
        if (useLabelEditor && arg0.getSource()==labelEditor) {
            if (main_graph.isReadonly()) return;
            if (updateLabelWhileTyping) editing_obj.setText(labelEditor.getText());
            if (labelEditor.adjustSize()) repaint();
        } else {
            int keycode = arg0.getKeyCode();
            if (keycode!=KeyEvent.VK_CONTROL) {
            	if (arg0.isControlDown()) {
                	if (keycode==KeyEvent.VK_C) {
                		if (selected_obj!=null) {
                			GraphClipboard cb = GraphClipboard.getInstance();
                			cb.put(selected_obj);
                		}
                	} else if (keycode==KeyEvent.VK_V || keycode==KeyEvent.VK_X){
                        if (main_graph.isReadonly()) return;
                		GraphClipboard cb = GraphClipboard.getInstance();
                		GraphFragment frag = cb.get();
                		if (frag!=null) {
                			performPaste(frag, main_graph, recordchange);
                		}
                    } else {
                    	if (selected_obj instanceof Node) {
                    		handleActivityMenuShortCut((Node)selected_obj, keycode);
                    	}
                	}
                }
        	}
        }
    }

    private void updateLabel(Object obj, String label, Graph main_graph) {
        if (obj instanceof Node) {
            ((Node)obj).setName(label);
            main_graph.setDirtyLevel(Graph.DIRTY);
            repaint();
        } else if (obj instanceof Link) {
            ((Link)obj).setLabelAndEventType(label);
            main_graph.setDirtyLevel(Graph.DIRTY);
            repaint();
        }
    }

    private void handleArrowKey(int keycode, KeyEvent event, Node node, Graph main_graph) {
        boolean canNotMove = false;
        event.consume();
        if (keycode==KeyEvent.VK_DOWN) {
            Dimension size = this.getSize();
            int y = node.y + 1;
            if (y+node.h<= size.height) {
                node.y = y;
            } else canNotMove = true;
        } else if (keycode==KeyEvent.VK_UP) {
            int y = node.y - 1;
            if (y>=0) {
                node.y = y;
            } else canNotMove = true;
        } else if (keycode==KeyEvent.VK_RIGHT) {
            Dimension size = this.getSize();
            int x = node.x + 1;
            if (x+node.w<=size.width) {
                node.x = x;
            } else canNotMove = true;
        } else if (keycode==KeyEvent.VK_LEFT) {
            int x = node.x - 1;
            if (x>=0) {
                node.x = x;
            } else canNotMove = true;
        }
        if (!canNotMove) {
            node.graph.recalcLinkPosition(node, main_graph.arrowstyle);
//            node.save();
            if (!main_graph.isReadonly()) main_graph.setDirtyLevel(Graph.GEOCHANGE);
            repaint();
        }
    }

    private void performPaste(GraphFragment sourcefrag, Graph process, boolean recordchange) {
    	if (sourcefrag.isEmpty()) return;
    	Rectangle visibleRect = this.getVisibleRect();
    	Rectangle boundingRect = sourcefrag.getBoundary();
    	process.save_temp_vars();
    	GraphFragment targetfrag = new GraphFragment(process.getId());
    	Map<Long,Long> nodeIdMap = new HashMap<Long,Long>();
    	int xoff = 0, yoff = 0;
    	if (visibleRect.intersects(boundingRect)) {
    		if (sourcefrag.getSourceProcessId().equals(process.getId())) {
    			xoff = 40;
    			yoff = 40;
    		} else {
    			for (SubGraph subgraph : process.subgraphs) {
    				if (sourcefrag.getSourceProcessId().equals(subgraph.getId())) {
    	    			xoff = 40;
    	    			yoff = 40;
    	    			break;
    				}
    			}
    		}
    	} else {
    		xoff = visibleRect.x - boundingRect.x + 100;
    		yoff = visibleRect.y - boundingRect.y + 100;
    	}
    	for (Node sourcenode : sourcefrag.nodes) {
    		Node node = process.addNode(process, sourcenode.nodet, xoff, yoff, recordchange, true);
    		targetfrag.nodes.add(node);
    		nodeIdMap.put(sourcenode.getActivityId(), node.getActivityId());
    		if (recordchange) node.getChanges().setChangeType(Changes.NEW);
    	}
    	for (Link sourcelink : sourcefrag.links) {
    		Link link = process.addLink(process, sourcelink.conn,
    				nodeIdMap.get(sourcelink.conn.getFromWorkId()),
    				nodeIdMap.get(sourcelink.conn.getToWorkId()),
    				xoff, yoff, recordchange, true);
    		targetfrag.links.add(link);
    		if (recordchange) link.getChanges().setChangeType(Changes.NEW);
    	}
    	for (SubGraph sourceSubgraph : sourcefrag.subgraphs) {
//    		if (xoff>0) yoff = sourceSubgraph.h + 15;
    		SubGraph subgraph = process.addSubGraph(sourceSubgraph.getProcessVO(), xoff, yoff, recordchange);
    		if (subgraph!=null) {
    			targetfrag.subgraphs.add(subgraph);
    			// TODO below only mark the entire subgraph as new
    			if (recordchange) subgraph.getChanges().setChangeType(Changes.NEW);
    		}
    	}
    	if (targetfrag.isNode()) selected_obj = targetfrag.getOnlyNode();
		else if (targetfrag.isSubGraph()) selected_obj = targetfrag.getOnlySubGraph();
		else selected_obj = targetfrag;
    	repaint();
    }

    public String getToolTipText(MouseEvent arg0, Graph main_graph) {
        int x = arg0.getX();
        int y = arg0.getY();
        if (main_graph.zoom!=100) {
            x = x * 100 / main_graph.zoom;
            y = y * 100 / main_graph.zoom;
        }
        Object obj = objectAt(main_graph, x, y, getGraphics());
        if (obj!=null && obj instanceof Node) {
            Node node = (Node)obj;
            String tip = node.getDescription();
            if (tip!=null) {
                if (tip.indexOf('\n')>=0) {
                    StringBuffer sb = new StringBuffer();
                    int i, n=tip.length();
                    char ch;
                    sb.append("<html>");
                    for (i=0; i<n; i++) {
                        ch = tip.charAt(i);
                        if (ch=='\n') sb.append("<br>");
                        else sb.append(ch);
                    }
                    sb.append("</html>");
                    return sb.toString();
                } else return tip;
            } else return null;
        } else return null;
    }

    /**
     * Just to make it visible to PrintProcessingPage
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    protected void handleActivityMenuShortCut(Node node, int keycode) {
    	// do nothing by default
    }

}
