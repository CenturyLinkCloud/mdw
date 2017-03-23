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
package com.centurylink.mdw.designer.pages;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.centurylink.mdw.common.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.MainFrame;
import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.display.GraphFragment;
import com.centurylink.mdw.designer.display.Link;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.display.Selectable;
import com.centurylink.mdw.designer.display.SubGraph;
import com.centurylink.mdw.designer.display.TextNote;
import com.centurylink.mdw.designer.utils.Constants;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.model.data.common.Changes;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;

public class FlowchartPage extends DesignerPage
	implements ActionListener,DragSourceListener,DropTargetListener, DragGestureListener {

    private static final String ACTION_NODETYPE = "NODETYPE";
    private static final String ACTION_ARROWTYPE = "ARROWTYPE";
    private static final String ACTION_SHOW_ID = "SHOW_ID";
    private static final String ACTION_ZOOM = "ZOOM";
    private static final String ACTION_ALL_LINK_STYLE_CHANGE = "ACTION_ALL_LINK_STYLE_CHANGE";
    private static final String ACTION_FORCE_UPDATE = "ACTION_FORCE_UPDATE";
    private static final String ACTION_SAVE_TO_FILE = "SAVE_TO_FILE";
    private static final String ACTION_LOAD_FROM_FILE = "LOAD_FROM_FILE";
    private static final String ACTION_RECORD_CHANGES = "ACTION_RECORD_CHANGES";
    private static final String ACTION_SAVE_AS = "ACTION_SAVE_AS";
    private static final String ACTION_COMMIT_CHANGES = "ACTION_COMMIT_CHANGES";
    private static final String ACTION_COMPARE = "ACTION_COMPARE";
    private static final String ACTION_LINEUP_HORIZONTAL = "ACTION_LINEUP_HORIZONTAL";
    private static final String ACTION_LINEUP_VERTICAL = "ACTION_LINEUP_VERTICAL";
    private static final String ACTION_ADD_NOTE = "ACTION_ADD_NOTE";
    static final String ACTION_MARK_AS_NEW = "ACTION_MARK_AS_NEW";
    static final String ACTION_MARK_AS_DELETED = "ACTION_MARK_AS_DELETED";
    static final String ACTION_MARK_AS_NOCHANGE = "ACTION_MARK_AS_NOCHANGE";
    static final String ACTION_ACTIVITY_POPUP = "ACTION_ACTIVITY_POPUP";
    static final String ACTION_RUN_ACTIVITY = "ACTION_RUN_ACTIVITY";


	public DesignerCanvas canvas;
	public NodeChoicePane nodepane;
	private Graph process;
	DragSource dragSource;
	DropTarget dropTarget;
	DragGestureRecognizer recognizer;
    public String name;
    public String eventType;
    public String processId;
    public String processName;
    Graph reloadedProcess = null;
    public static boolean showtip = true;
    boolean recordchange = false;
    private JComboBox arrowTypeWidget, nodeTypeWidget, zoomWidget, nodeIdType;
//    JButton save_xy_button;
	// final static String MODE_ACT = "Activity Mode";

	String linktype;

	public FlowchartPage(MainFrame frame) {
		super(frame);
        canvas = new DesignerCanvas(this);
        nodepane = new NodeChoicePane(this);

        dragSource = new DragSource();
        dropTarget = new DropTarget(canvas,
                DnDConstants.ACTION_COPY_OR_MOVE, this);
        recognizer = dragSource.createDefaultDragGestureRecognizer(
                nodepane, DnDConstants.ACTION_COPY_OR_MOVE, this);
        linktype = Link.ELBOW;
	}

	/**
	 * New method for non-singleton access from Eclipse Designer.
	 * @param frame the mainFrame
	 * @return new instance
	 */
	public static FlowchartPage newPage(MainFrame frame) {
	  return new FlowchartPage(frame);
	}

	public void createMenuBar() {
		menubar = new JMenuBar();
		JMenu menu1 = new JMenu("File");
		menubar.add(menu1);
        createMenuItem(menu1, "Process List", Constants.ACTION_PROCLIST, this);
        createMenuItem(menu1, "Save to File", ACTION_SAVE_TO_FILE, this);
		if (model.schemaVersionAllowEdit())
        createMenuItem(menu1, "Load from File", ACTION_LOAD_FROM_FILE, this);
        createMenuItem(menu1, "Save as ...", ACTION_SAVE_AS, this);
		if (model.schemaVersionAllowEdit())
        createMenuItem(menu1, "Commit Changes", ACTION_COMMIT_CHANGES, this);
        createMenuItem(menu1, "Compare", ACTION_COMPARE, this);
		if (model.schemaVersionAllowEdit())
        createMenuItem(menu1, "Force to update same version", ACTION_FORCE_UPDATE, this);
        if (!frame.dao.noDatabase())
            createMenuItem(menu1, "Export Process", Constants.ACTION_EXPORT, this);
        createMenuItem(menu1, "Print ...", Constants.ACTION_PRINT, this);
		createMenuItem(menu1, "Close", Constants.ACTION_LOGOUT, this);

		JMenu menu2 = new JMenu("Edit");
		menubar.add(menu2);
		createMenuItem(menu2, "Change style for all links", ACTION_ALL_LINK_STYLE_CHANGE, this);
		createMenuItem(menu2, "Line up horizontally", ACTION_LINEUP_HORIZONTAL, this);
		createMenuItem(menu2, "Line up vertically", ACTION_LINEUP_VERTICAL, this);
		createMenuItem(menu2, "Add Note", ACTION_ADD_NOTE, this);
	}

	public void createToolBar() {
		ToolPane toolpane = new ToolPane(this);
		toolbar = toolpane;
		add(toolpane, BorderLayout.NORTH);
		ToolPane southPane = new ToolPane(this);
		add(southPane, BorderLayout.SOUTH);
		if (model.schemaVersionAllowEdit()) {
		toolpane.createButton(null, "object.gif", "edit/unedit process", Constants.ACTION_EDIT);
		    toolpane.createButton("Save", "save.gif", "save process", Constants.ACTION_SAVE);
            toolpane.createButton("Delete", "delete.gif", "delete node", Constants.ACTION_DELETE);
		}
		toolpane.createButton("Variables", "variable.jpg", "variables", Constants.ACTION_DOCVIEW);
		//createToolButton("Servers", "servers.gif", "Servers", Constants.ACTION_SVRVIEW, this);
        if (!frame.dao.noDatabase())
            toolpane.createButton("ProcessInstance","table24.gif","All Process Instance for this process",Constants.VIEW_ALL_PROCESS_INSTANCE);
		String[] actions = {
        		Constants.ACTION_PROCLIST,
        		Constants.ACTION_NEW_ACTIVITY_IMPL,
        		Constants.EXTERNAL_EVENT_BUTTON,
        		ACTION_SCRIPT,
        		Constants.ACTION_PACKAGE
        };
        addCommonToolButtons(actions);
        toolpane.createButton("Start", "start.gif", "Start Process", Constants.ACTION_START);

        //AK..added 02/25/2011
        toolpane.createButton("Logout", "logout.gif", "Log out", Constants.ACTION_LOGOUT);

        southPane.createCheckBox("Show Tip", true, Constants.ACTION_TIPMODE);
//		southPane.createCheckBox("Show Node ID", Node.showid, ACTION_SHOW_ID);
		nodeIdType = southPane.createDropdown(Node.IdChoices, ACTION_SHOW_ID, 0, 100);

//        ButtonGroup bg = new ButtonGroup();
//        for (int k=0; k<linkTypeNames.length; k++) {
//        	createToolRadio(bg, linkTypeNames[k], linkTypeImages[k],
//        			k==0, Constants.ACTION_LINKTYPE+":"+linkTypeNames[k], this);
//        }
		southPane.createDropdown(Link.styles, Link.styleIcons, "Link style", Constants.ACTION_LINKTYPE, 0, 50);
		arrowTypeWidget = southPane.createDropdown(Link.ArrowStyles, Link.ArrowStyleIcons, "Arrow location", ACTION_ARROWTYPE, 0, 50);
		nodeTypeWidget = southPane.createDropdown(Node.Styles, Node.StyleIcons, "Node style", ACTION_NODETYPE, 0, 50);
		southPane.createCheckBox("Record changes", false, ACTION_RECORD_CHANGES);
		zoomWidget = southPane.createDropdown(Graph.zoomLevelNames, ACTION_ZOOM, 3, 60);
	}

	public void actionPerformed(ActionEvent event) {
		String cmd = event.getActionCommand();

		if (cmd.equals(Constants.ACTION_DELETE)) {
            if (process.isReadonly()) {
                return;
            }
            if (canvas.getSelectedObject()!=null) {
				if (canvas.getSelectedObject() instanceof Node) {
					if (getConfirmation("Are you sure you want to delete the node?")) {
                        process.removeNode((Node)canvas.getSelectedObject(), this.recordchange);
                    }
				} else if (canvas.getSelectedObject() instanceof Link) {
					if (getConfirmation("Are you sure you want to delete the link?")) {
                        process.removeLink((Link)canvas.getSelectedObject(), this.recordchange);
                    }
				} else if (canvas.getSelectedObject() instanceof SubGraph) {
                    if(getConfirmation("Are you sure you want to delete the handler?")) {
                        process.removeSubGraph((SubGraph)canvas.getSelectedObject());
                    }
				} else if (canvas.getSelectedObject() instanceof GraphFragment) {
                    if (getConfirmation("Are you sure you want to delete the selected objects?")) {
                    	GraphFragment frag = (GraphFragment)canvas.getSelectedObject();
                    	for (Link l : frag.links) {
                    		process.removeLink(l, recordchange);
                    	}
                    	for (Node n : frag.nodes) {
                    		process.removeNode(n, recordchange);
                    	}
                    	for (SubGraph sg : frag.subgraphs) {
                    		process.removeSubGraph(sg);		// TODO record changes
                    	}
                    }
				} else if (canvas.getSelectedObject() instanceof TextNote) {
					if (getConfirmation("Are you sure you want to delete the note?")) {
                        process.removeTextNote((TextNote)canvas.getSelectedObject());
                    }
				}
				canvas.setSelectedObject(null);
				canvas.repaint();
			}
		} else if (cmd.equals(Constants.ACTION_TIPMODE)) {
			JCheckBox tipMode = (JCheckBox)event.getSource();
			showtip = tipMode.isSelected();
		} else if (cmd.equals(ACTION_RECORD_CHANGES)) {
			JCheckBox checkbox = (JCheckBox)event.getSource();
			recordchange = checkbox.isSelected();
		} else if (cmd.equals(ACTION_SHOW_ID)) {
			if (process!=null) {
				JComboBox showId = (JComboBox)event.getSource();
				process.setNodeIdType((String)showId.getSelectedItem());
				canvas.repaint();
			} // else during init
        } else if (cmd.equals(Constants.ACTION_START)) {
        	showStartProcessDialog(null);
        } else if (cmd.equals(ACTION_RUN_ACTIVITY)) {
        	if (model.canExecuteProcess(process.getProcessVO())) {
        		Node node = (Node)canvas.getSelectedObject();
        		showStartProcessDialog(node);
        	} else this.showError("You are not authorized to run processes");
        } else if (cmd.equals(ACTION_ARROWTYPE)) {
            JComboBox arrowType = (JComboBox)event.getSource();
            ImageIcon arrowTypeIcon = (ImageIcon)arrowType.getSelectedItem();
            setArrowStyle(arrowTypeIcon.getDescription());
        } else if (cmd.equals(ACTION_NODETYPE)) {
            JComboBox nodeType = (JComboBox)event.getSource();
            ImageIcon nodeTypeIcon = (ImageIcon)nodeType.getSelectedItem();
            setNodeStyle(nodeTypeIcon.getDescription());
        } else if (cmd.equals(ACTION_ZOOM)) {
            int zoomLevel = Graph.zoomLevels[zoomWidget.getSelectedIndex()];
            canvas.zoom(process, zoomLevel);
        } else if (cmd.equals(ACTION_ALL_LINK_STYLE_CHANGE)) {
            process.changeAllLinkStyle(this.linktype);
            canvas.repaint();
        } else if (cmd.equals(ACTION_COMMIT_CHANGES)) {
        	if (process.isReadonly()) {
                return;
        	}
    		if (getConfirmation("This will commit recorded changes. Proceed?")) {
    			process.commitChanges();
    			canvas.repaint();
    		}
        } else if (cmd.equals(ACTION_MARK_AS_NEW)) {
        	markChangeType(Changes.NEW);
        } else if (cmd.equals(ACTION_MARK_AS_DELETED)) {
        	markChangeType(Changes.DELETE);
        } else if (cmd.equals(ACTION_MARK_AS_NOCHANGE)) {
        	markChangeType(Changes.NONE);
        } else if (cmd.equals(ACTION_LINEUP_HORIZONTAL)) {
        	lineUpActivities(false);
        } else if (cmd.equals(ACTION_LINEUP_VERTICAL)) {
        	lineUpActivities(true);
        } else if (cmd.equals(ACTION_ADD_NOTE)) {
            if (process.isReadonly()) {
                return;
            }
        	addNote();
        } else {
			super.actionPerformed(event);
		}
	}

	private void showStartProcessDialog(Node startActivity) {
        if (process.dirtyLevel==Graph.NEW) {
            showError("The newly created process has not been saved yet.");
        }
	}

	private void markChangeType(char changeType) {
		Object obj = canvas.getSelectedObject();
    	if (obj instanceof Node) {
    		Node node = (Node)obj;
    		node.getChanges().setChangeType(changeType);
    		canvas.repaint();
    	} else if (obj instanceof Link) {
    		Link link = (Link)obj;
    		link.getChanges().setChangeType(changeType);
    		canvas.repaint();
    	}
	}

	public Graph getProcess() {
		return process;
	}

	/**
	 * @param process The process to set.
	 */
	public void setProcess(Graph process) {
        if (process.isFileVersion()) process.setReadonly(false);
        else if (!model.canDesignProcess(process.getProcessVO())) process.setReadonly(true);
        else if (!model.schemaVersionAllowEdit()) process.setReadonly(true);
        else if (process.getProcessVO().getNextVersion()!=null && !DesignerDataAccess.isArchiveEditAllowed()) process.setReadonly(true);
        else if (process.getProcessVO().isRemote()) process.setReadonly(true);
        else if (process.getProcessVO().getModifyingUser()==null ||
        		!process.getProcessVO().getModifyingUser().equalsIgnoreCase(frame.getCuid()))
        	process.setReadonly(true);
        else process.setReadonly(false);
        nodeIdType.setSelectedItem(process.getNodeIdType());
        canvas.editable = !process.isReadonly();
        if (this.process!=null && this.process==process) return;
		this.process = process;
        canvas.setSelectedObject(null);

        for (int i=0; i<Link.ArrowStyles.length; i++) {
            if (Link.ArrowStyles[i].equals(process.arrowstyle)) arrowTypeWidget.setSelectedIndex(i);
        }
        for (int i=0; i<Node.Styles.length; i++) {
            if (Node.Styles[i].equals(process.nodestyle)) nodeTypeWidget.setSelectedIndex(i);
        }
        java.awt.Rectangle aRect = new java.awt.Rectangle(0,0,64,64);
        canvas.scrollRectToVisible(aRect);
        Dimension size = process.getGraphSize();
        size.width = (size.width+40)*process.zoom/100;
        size.height = (size.height+40)*process.zoom/100;
        canvas.setPreferredSize(size);
        int k = 6;
        for (int i=0; i<Graph.zoomLevels.length; i++) {
            if (process.zoom == Graph.zoomLevels[i]) {
                k = i;
                break;
            }
        }
        zoomWidget.setSelectedIndex(k);
	}

	protected String promptEmbeddedProcessType(Graph process) {
//	    List<String> optList = new ArrayList<String>(4);
//	    ProcessVO procVO = process.getProcessVO();
//	    String[] allSubTypes = {
//	            ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS,
//	            ProcessVisibilityConstant.EMBEDDED_ABORT_PROCESS,
//	            ProcessVisibilityConstant.EMBEDDED_CORRECT_PROCESS,
//	            ProcessVisibilityConstant.EMBEDDED_DELAY_PROCESS
//	    };
//	    for (int i=0; i<allSubTypes.length; i++) {
//	        if (procVO.findEmbeddedProcess(allSubTypes[i])==null)
//	            optList.add(allSubTypes[i]);
//	    }
//	    if (optList.size()>0) {
//	        String[] options = optList.toArray(new String[optList.size()]);
//	        int resp = frame.getOptionPane().choose(this, "Choose an embedded process type", options);
//	        return resp>=0?optList.get(resp):null;
//	    } else {
//	        showError("All embedded processes are present");
//	        return null;
//	    }
	    String[] allSubTypes = {
	            ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS,
	            ProcessVisibilityConstant.EMBEDDED_ABORT_PROCESS,
	            ProcessVisibilityConstant.EMBEDDED_CORRECT_PROCESS,
	            ProcessVisibilityConstant.EMBEDDED_DELAY_PROCESS
	    };
	    int resp = frame.getOptionPane().choose(this, "Choose an embedded process type", allSubTypes);
	    return resp>=0?allSubTypes[resp]:null;
	}

	public void dragDropEnd(DragSourceDropEvent e) {
	    if (Constants.isMacOsX()) {
            ActivityImplementorVO nmi = model.getNodeMetaInfo().get(nodepane.getSelectedNode());
            Selectable object;
            Point p = canvas.getMousePosition();
            int x = p.x;
            int y = p.y;
            if (process.zoom!=100) {
                x = x * 100 / process.zoom;
                y = y * 100 / process.zoom;
            }
            if (nmi.getImplementorClassName().equals(NodeMetaInfo.PSEUDO_PROCESS_ACTIVITY)) {
                String type = promptEmbeddedProcessType(process);
                if (type!=null) object = process.addSubGraph(x, y, type, recordchange);
                else object = null;
            } else {
                object = process.addNode(nmi, x, y, recordchange);
            }
            if (object!=null) {
                canvas.setSelectedObject(object);
                canvas.requestFocus();
                canvas.repaint();
            }
	    }
	}

	public void dragEnter(DragSourceDragEvent arg0) {
	}

	public void dragExit(DragSourceEvent arg0) {
	}

	public void dragOver(DragSourceDragEvent arg0) {
	    if (Constants.isMacOsX())
	        canvas.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	public void dropActionChanged(DragSourceDragEvent arg0) {
	}

	public void dragEnter(DropTargetDragEvent e) {
		e.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
	}

	public void dragExit(DropTargetEvent arg0) {
	}

	public void dragOver(DropTargetDragEvent arg0) {
	}

	public void drop(DropTargetDropEvent e) {
	    if (process.isReadonly()) {
	        return;
	    }
		try {
			if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                Transferable tr = e.getTransferable();
                e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                String s = (String)tr.getTransferData(DataFlavor.stringFlavor);
                int i = Integer.parseInt(s.substring(3));
                ActivityImplementorVO nmi = model.getNodeMetaInfo().get(i);
                Selectable object;
                int x = e.getLocation().x;
                int y = e.getLocation().y;
                if (process.zoom!=100) {
                    x = x * 100 / process.zoom;
                    y = y * 100 / process.zoom;
                }
                if (nmi.getImplementorClassName().equals(NodeMetaInfo.PSEUDO_PROCESS_ACTIVITY)) {
                    String type = promptEmbeddedProcessType(process);
                    if (type!=null) object = process.addSubGraph(x, y, type, recordchange);
                    else object = null;
                } else {
                    object = process.addNode(nmi, x, y, recordchange);
                }
                e.dropComplete(true);
                if (object!=null) {
                    canvas.setSelectedObject(object);
                    canvas.requestFocus();
                    canvas.repaint();
                }
			} else {
				e.rejectDrop();
			}
		} catch (IOException io) {
//			io.printStackTrace();
			e.rejectDrop();
		} catch (UnsupportedFlavorException ufe) {
//			ufe.printStackTrace();
			e.rejectDrop();
		}

	}

	public void dropActionChanged(DropTargetDragEvent arg0) {
	}

	public void dragGestureRecognized(DragGestureEvent e) {
		if (nodepane.getSelectedNode()>=0) {
			e.startDrag(DragSource.DefaultCopyDrop,
					new StringSelection("NMI" + nodepane.getSelectedNode()),
					this);
		}
	}

	public String getTitle() {
        if(process.getId() != null) {
            return  frame.getDesignerTitle() + " - Process " + process.getId();
        } else return  frame.getDesignerTitle() + " - Process";
	}

    public void setLinkStyle(String linkStyle) {
        this.linktype = linkStyle;
    }

    public void setArrowStyle(String arrowstyle) {
        String v = process.getAttribute(Link.ATTRIBUTE_ARROW_STYLE);
        if (v!=null&&!arrowstyle.equals(v) || v==null&&!arrowstyle.equals(Link.ARROW_STYLE_END)) {
            if (!frame.isInEclipse() && process.isReadonly()) {
                return;
            }
            process.setArrowStyle(arrowstyle);
            process.setDirtyLevel(Graph.DIRTY);
            canvas.repaint();
        }
    }

    public void setNodeStyle(String nodeStyle) {
        process.nodestyle = nodeStyle;
        String v = process.getAttribute(Node.ATTRIBUTE_NODE_STYLE);
        if (v!=null&&!process.nodestyle.equals(v) || v==null&&!process.nodestyle.equals(Node.NODE_STYLE_ICON)) {
            if (!frame.isInEclipse() && process.isReadonly()) {
                return;
            }
            process.setAttribute(Node.ATTRIBUTE_NODE_STYLE, process.nodestyle);
            process.setDirtyLevel(Graph.DIRTY);
            process.resetNodeImageSize(process.nodestyle);
            canvas.repaint();
        }
    }

    public void setZoomLevel(int zoomLevel) {
      canvas.zoom(process, zoomLevel);
    }

    public boolean isRecordChange() {
        return recordchange;
    }

    public void setRecordChange(boolean record) {
        this.recordchange = record;
    }

    public void commitChanges() {
        process.commitChanges();
        canvas.repaint();
    }

    private void lineUpActivities(boolean vertical) {
    	Object selectedObj = canvas.getSelectedObject();
    	if (selectedObj==null || !(selectedObj instanceof GraphFragment)) {
    		this.showError("You need to select at least 2 activities to line up");
    		return;
    	}
    	GraphFragment marquee = (GraphFragment)selectedObj;
    	if (marquee.nodes.size()<2) {
    		this.showError("You need to select at least 2 activities to line up");
    		return;
    	}
    	if (vertical) {
    		Node topmost = null;
    		for (Node n : marquee.nodes) {
    			if (topmost==null || n.y<topmost.y) topmost = n;
    		}
    		for (Node n : marquee.nodes) {
    			if (n==topmost) continue;
    			n.x = topmost.x + topmost.w/2 - n.w/2;
    			process.recalcLinkPosition(n, process.arrowstyle);
    		}
    		canvas.repaint();
    	} else {
    		Node leftmost = null;
    		for (Node n : marquee.nodes) {
    			if (leftmost==null || n.x<leftmost.x) leftmost = n;
    		}
    		for (Node n : marquee.nodes) {
    			if (n==leftmost) continue;
    			n.y = leftmost.y + leftmost.h/2 - n.h/2;
    			process.recalcLinkPosition(n, process.arrowstyle);
    		}
    		canvas.repaint();
    	}
    }

    private void addNote() {
    	if (!process.getProcessVO().isInRuleSet()) {
    		showError("Notes can only be added to processes stored in RULE_SET table");
    		return;
    	}
    	Rectangle rect = canvas.getVisibleRect();
    	TextNote note = process.addTextNote(rect.x+100, rect.y+100, "Enter note content here");
        canvas.setSelectedObject(note);
        canvas.requestFocus();
        canvas.repaint();
    }

}
