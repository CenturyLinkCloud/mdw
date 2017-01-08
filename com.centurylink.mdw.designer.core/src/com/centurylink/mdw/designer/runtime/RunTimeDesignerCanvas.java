/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.runtime;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.display.Link;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.display.Selectable;
import com.centurylink.mdw.designer.display.SubGraph;
import com.centurylink.mdw.designer.pages.CanvasCommon;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;

public class RunTimeDesignerCanvas extends CanvasCommon
	implements ImageObserver, MouseListener, MouseMotionListener,
		KeyListener
{
    private static final long serialVersionUID = 1L;

    ProcessInstancePage page;
    Cursor default_cursor, hand_cursor;

    public RunTimeDesignerCanvas(ProcessInstancePage page){
        super(page.frame.getIconFactory());
        Dimension size = new Dimension(640, 480);
        setMinimumSize(size);
//		setPreferredSize(size);
		setBackground(Color.white);
        this.page = page;
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		this.setToolTipText("please tell me!");
		default_cursor =
		    Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		hand_cursor =
		    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		String[] activity_menu_labels = {"one", "two"};
		String[] activity_menu_actions = {"eins", "twei"};
		popup_activity = createPopup(activity_menu_labels, activity_menu_actions, page);
    }

    /* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graph process = page.getProcess();
        if (process != null) {
          if (g instanceof Graphics2D) {
              Graphics2D g2d = (Graphics2D) g;
              g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
              g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
              if (process!=null && process.zoom!=100) {
                  double scale = process.zoom / 100.0;
                  g2d.scale(scale, scale);
              }
          }
          draw_graph(g, process, true);
          if (page.frame!=null && !page.frame.isVisible() && selected_obj!=null) {
            if (selected_obj instanceof Node) {
                drawSelectionBox(g, (Node)selected_obj);
            } else if (selected_obj instanceof Link) {
                drawSelectionBox(g, (Link)selected_obj);
            } else if (selected_obj instanceof SubGraph) {
                drawSelectionBox(g, (SubGraph)selected_obj);
            } else if (selected_obj instanceof Graph) {
                drawSelectionBox(g, (Graph)selected_obj);
            }
          }
       }
	}

    public void keyPressed(KeyEvent arg0)
    {

    }

    public void keyReleased(KeyEvent arg0)
    {
    }

    public void keyTyped(KeyEvent arg0)
    {
    }

    public void mouseClicked(MouseEvent arg0)
    {
    	if (arg0.getButton()!=1) return;
        if (arg0.getClickCount() != 2) return;
        Graph process = page.getProcess();
        int x = arg0.getX();
        int y = arg0.getY();
        if (process.zoom!=100) {
            x = x * 100 / process.zoom;
            y = y * 100 / process.zoom;
        }
        Object obj = process.objectAt(x, y, getGraphics());
        if (obj == null) return;
        if (obj instanceof Link) {
            Link link = (Link) obj;
            try {
                List<WorkTransitionInstanceVO> workTransitionList;
                Long linkId = new Long(link.conn.getWorkTransitionId());
                if (link.from.graph instanceof SubGraph) {
                    workTransitionList = new ArrayList<WorkTransitionInstanceVO>();
                    SubGraph subgraph = (SubGraph) link.from.graph;
                    if (subgraph.getInstances() != null) {
                        for (ProcessInstanceVO inst : subgraph.getInstances()) {
                            List<WorkTransitionInstanceVO> coll1 =
                                inst.getTransitionInstances(linkId);
                            workTransitionList.addAll(coll1);
                        }
                    }
                } else {
                    workTransitionList = page.getProcessInstance().getTransitionInstances(linkId);
                }
                WorkTransitionsDialog workTransDialog = new WorkTransitionsDialog(page.frame);
                workTransDialog.setWorkTransitionList(workTransitionList);
                workTransDialog.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                page.frame.setNewServer();
            }
        }
    }

    public void mouseEntered(MouseEvent arg0)
    {
    }

    public void mouseExited(MouseEvent arg0)
    {
    }

    public void mousePressed(MouseEvent arg0)
    {
    	if (arg0.getButton()!=1) return;
        Graph process = page.getProcess();
        int x = arg0.getX();
        int y = arg0.getY();
        if (process.zoom!=100) {
            x = x * 100 / process.zoom;
            y = y * 100 / process.zoom;
        }

        Object obj = objectAt(page.getProcess(), x, y, getGraphics());
        if (obj!=null) {
            selected_obj = obj;
            repaint();
        } else {
        	selected_obj = null;
        }
    }

   public void mouseReleased(MouseEvent arg0) {
	   	if (arg0.getButton()==1) return;
    	if (!arg0.isPopupTrigger()) return;
        Graph process = page.getProcess();
        int x = arg0.getX();
        int y = arg0.getY();
        if (process.zoom!=100) {
            x = x * 100 / process.zoom;
            y = y * 100 / process.zoom;
        }
        Object obj = process.objectAt(x, y, getGraphics());
        if (obj == null) return;
        if (obj instanceof Selectable) {
//        	JPopupMenu popup = this.popup_activity;	// TODO
//			if (popup!=null) popup.show(this, arg0.getX(), arg0.getY());
        } else if (obj instanceof Link) {

    	}
	}

    public void mouseDragged(MouseEvent arg0)
    {
        Graph process = page.getProcess();
//      if (process.isReadonly()) return;
      if (selected_obj!=null) {
          int x = arg0.getX();
          int y = arg0.getY();
          if (process.zoom!=100) {
              x = x * 100 / process.zoom;
              y = y * 100 / process.zoom;
          }
          if (selected_obj instanceof Node) {
              moveNode(x, y);
          } else if (selected_obj instanceof SubGraph) {
              moveSubgraph(x, y);
          } else if (selected_obj instanceof Graph) {
              moveGraphLabel(x, y);
          }
          repaint();
      }
    }

    public void mouseMoved(MouseEvent arg0) {
        Graph process = page.getProcess();
        int x = arg0.getX();
        int y = arg0.getY();
        if (process.zoom!=100) {
            x = x * 100 / process.zoom;
            y = y * 100 / process.zoom;
        }
        Object obj = objectAt(process, x, y, getGraphics());
        if (obj!=null) {
            this.setCursor(hand_cursor);
        } else this.setCursor(default_cursor);
	}

    public String getToolTipText(MouseEvent arg0) {
        int x = arg0.getX();
        int y = arg0.getY();
        Graph process = page.getProcess();
        if (process.zoom!=100) {
            x = x * 100 / process.zoom;
            y = y * 100 / process.zoom;
        }
        Object obj = objectAt(process, x, y, getGraphics());
        if (obj!=null && obj instanceof Node) {
            Node node = (Node)obj;
            List<ActivityInstanceVO> insts = node.getInstances();
            if (insts!=null) {
                StringBuffer sb = new StringBuffer();
                sb.append("<html>");
                int k = 0;
                for (ActivityInstanceVO one : node.getInstances()) {
                    sb.append(one.getId()).append(": ").
                        append(one.getStartDate().toString()).append("<p>");
                    k++;
                    if (k>=5) break;
                }
                sb.append("</html>");
                return sb.toString();
            } else return null;
        } else if (obj!=null && obj instanceof Link) {
            Link link = (Link)obj;
            List<WorkTransitionInstanceVO> insts = link.getInstances();
            if (insts!=null) {
                StringBuffer sb = new StringBuffer();
                int k = 0;
                sb.append("<html>");
                for (WorkTransitionInstanceVO one : link.getInstances()) {
                    sb.append(one.getTransitionInstanceID()).append(": ").
                        append(one.getStartDate().toString()).append("<p>");
                    k++;
                    if (k>=5) break;
                }
                sb.append("</html>");
                return sb.toString();
            } else return null;
        } else return null;
    }

}
