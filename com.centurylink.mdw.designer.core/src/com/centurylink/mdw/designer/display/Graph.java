/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.constant.WorkTransitionAttributeConstant;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.model.data.common.Changes;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.activity.TextNoteVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.LaneVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.PoolVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.FormatDom;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

/**
 *
 */
public class Graph extends GraphCommon implements Selectable {

    public static final int CLEAN = 0;
    public static final int GEOCHANGE = 1;
    public static final int DIRTY =2;
    public static final int NEW = 3;
    private static final int DEFAULT_W = 3200;
    private static final int DEFAULT_H = 3200;

    public static final int file_version = 99099;

    public static final String[] zoomLevelNames =
        {"25%", "50%", "75%", "100%", "150%", "200%", "Fit"};
    public static final int[] zoomLevels =
        {25, 50, 75, 100, 150, 200, 0};

	private PackageVO packageVO = null;
	public ArrayList<SubGraph> subgraphs;
	public ArrayList<TextNote> notes;
	public int dirtyLevel;        // 0 - clean, 1 - geometry changed,
	                              // 2 - more change, 3 - brand new
	public String arrowstyle;
    public String nodestyle;
    public int zoom;
    private boolean isReadonly;
    private JMenuItem menuitem;
    private ProcessInstanceVO instance;                 // used only for runtime display
    private String originalName;
    public int lx, ly, lw, lh;      // x, y, w, h are for area
    private long lastGenId = -1;
    private static int newProcessId = -999999;
    private NodeMetaInfo metainfo;
    private int largestActivityLogicalId;
    private int largestTransitionLogicalId;
    private static String nodeIdType = Node.ID_LOGICAL;

    /**
     * This constructor is used when creating a brand new process
     * in the designer.
     *
     * @param procdef
     * @param isnew
     * @param isPublic
     */
    public Graph(String procName, String processType, NodeMetaInfo metainfo, IconFactory iconFactory) {
        super(iconFactory);
		this.w = DEFAULT_W;
		this.h = DEFAULT_H;
		this.x = this.y = 0;
		this.nodestyle = Node.NODE_STYLE_BOX_ICON;
		this.arrowstyle = Link.ARROW_STYLE_END;
		this.zoom = 100;
        largestActivityLogicalId = -1;
        largestTransitionLogicalId = -1;
		this.geo_attribute = WorkAttributeConstant.WORK_DISPLAY_INFO;
		this.metainfo = metainfo;
		Long pProcessId = null;
        String pDesc = null;
        List<ExternalEventVO> pExtEvents = null;
        processVO = new ProcessVO(pProcessId, procName, pDesc, pExtEvents);
        List<AttributeVO> attributes = new ArrayList<AttributeVO>();
        AttributeVO a = new AttributeVO(null, WorkAttributeConstant.PROCESS_VISIBILITY,processType);
        attributes.add(a);
        processVO.setAttributes(attributes);
        processVO.setAttribute(Node.ATTRIBUTE_NODE_STYLE, this.nodestyle);
        processVO.setAttribute(Link.ATTRIBUTE_ARROW_STYLE, this.arrowstyle);
        processVO.setActivities(new ArrayList<ActivityVO>());
        processVO.setTransitions(new ArrayList<WorkTransitionVO>());
        processVO.setVariables(new ArrayList<VariableVO>());
        processVO.setVersion(1);
        originalName = procName;
		nodes = new Vector<Node>();
		links = new Vector<Link>();
		subgraphs = new ArrayList<SubGraph>();
		dirtyLevel = NEW;
		menuitem = null;
        lx = 50;
        ly = 50;
        getProcessVO().setProcessId(new Long(newProcessId++));

	    Node node1 = addNode(this, metainfo.getStartActivity(), 60, 260, false);
	    node1.nodet.setActivityName("Start");
		Node node2 = addNode(this, metainfo.getStopActivity(), 480, 260, false);
	    node2.nodet.setActivityName("Stop");
        addLink(node1, node2, EventType.FINISH, Link.ELBOW, 2, false);
        // addSubGraph(320, 50);

       	changes = new Changes(processVO.getAttributes());
	}

    /**
     * This constructor is used when loading an existing process.
     *
     * @param procdef ProcessVO must be loaded already
     */
	public Graph(ProcessVO procdef, NodeMetaInfo metainfo, IconFactory iconFactory) {
	    super(iconFactory);
        this.w = DEFAULT_W;
        this.h = DEFAULT_H;
        this.x = this.y = 0;
		this.instance = null;
		this.metainfo = metainfo;
		menuitem = null;
		reinit(procdef);
    	changes = new Changes(procdef.getAttributes());
	}

	/**
	 * This constructor is for a process instance
	 * @param procdef
	 * @param statuses
	 */
	public Graph(ProcessVO procdef, ProcessInstanceVO instance, NodeMetaInfo metainfo, IconFactory iconFactory) {
	    super(iconFactory);
	    this.instance = instance;
        this.w = DEFAULT_W;
        this.h = DEFAULT_H;
        this.x = this.y = 0;
        this.metainfo = metainfo;
        menuitem = null;
	    reinit(procdef);
	}

	public Graph(PackageVO packageVO, NodeMetaInfo metainfo, IconFactory iconFactory) {
	    super(iconFactory);
	    this.w = DEFAULT_W;
	    this.h = DEFAULT_H;
	    this.x = this.y = 0;
	    this.nodestyle = Node.NODE_STYLE_BOX;
	    this.arrowstyle = Link.ARROW_STYLE_END;
	    this.zoom = 100;
        largestActivityLogicalId = -1;
        largestTransitionLogicalId = -1;
	    this.geo_attribute = WorkAttributeConstant.SWIMLANE_GEO_INFO;
	    this.processVO = null;
	    this.packageVO = packageVO;
	    this.metainfo = metainfo;
        nodes = new Vector<Node>(0);
        links = new Vector<Link>(0);
        subgraphs = new ArrayList<SubGraph>();
        int lane_y = 0;
        int lane_h = 300;
        for (ProcessVO proc : packageVO.getProcesses()) {
            String geo = proc.getAttribute(geo_attribute);
            SubGraph subgraph;
            if (geo==null) {
                proc.setAttribute(geo_attribute, formatGeoInfo(0,lane_y,1600,lane_h));
                subgraph = new SubGraph(proc, this, metainfo, iconFactory);
                subgraph.auto_layout();
                lane_y += lane_h;
            } else {
                subgraph = new SubGraph(proc, this, metainfo, iconFactory);
            }
            subgraphs.add(subgraph);
        }
        List<PoolVO> pools = packageVO.getPools();
        lane_h = 120;
        if (pools!=null && !pools.isEmpty()) {
            List<LaneVO> lanes = pools.get(0).getLanes();
            for (LaneVO lane : lanes) {
                String geo = lane.getAttribute(geo_attribute);
                SubGraph subgraph;
                ProcessVO proc = new ProcessVO();
                proc.setProcessName(lane.getLaneName());
                proc.setProcessId(lane.getLaneId());
                proc.setAttributes(lane.getAttributes());
                proc.setAttribute(LaneVO.PARTICIPANT_TYPE, lane.isSystem()?
                        LaneVO.PARTICIPANT_SYSTEM:LaneVO.PARTICIPANT_PEOPLE);    // indicate this is a participant
                if (geo==null) {
                    lane.setAttribute(geo_attribute, formatGeoInfo(0,lane_y,1600,lane_h));
                    subgraph = new SubGraph(proc, this, metainfo, iconFactory);
//                    subgraph.auto_layout();
                    lane_y += lane_h;
                } else {
                    subgraph = new SubGraph(proc, this, metainfo, iconFactory);
                }
                subgraphs.add(subgraph);
            }
        }
	}


	/**
	 * Invoked loading an existing process definition or process instance
	 * Invoked after saving the process
	 * Also invoked by constructor above.
	 */
	public void reinit(ProcessVO processVO) {
	    this.processVO = processVO;
	    this.geo_attribute = WorkAttributeConstant.WORK_DISPLAY_INFO;
        nodes = new Vector<Node>();
        links = new Vector<Link>();
        subgraphs = new ArrayList<SubGraph>();
        notes = null;
        dirtyLevel = CLEAN;
        originalName = processVO.getProcessName();
        zoom = 100;
        largestActivityLogicalId = -1;
        largestTransitionLogicalId = -1;
        Node node, node2;
        Link link;
        ActivityVO nodet;
        WorkTransitionVO conn;
        parseCoord(geo_attribute);
        arrowstyle = getAttribute(Link.ATTRIBUTE_ARROW_STYLE);
        if (arrowstyle==null) arrowstyle = Link.ARROW_STYLE_END;
        nodestyle = getAttribute(Node.ATTRIBUTE_NODE_STYLE);
        if (nodestyle==null) nodestyle = Node.NODE_STYLE_ICON;
        int i, n;

        List<ActivityVO> nodes0 = processVO.getActivities();
        n = nodes0==null?0:nodes0.size();
        for (i=0; i<n; i++) {
            nodet = processVO.getActivities().get(i);
            node = new Node(nodet, this, metainfo);
            nodes.add(node);
        }
        List<WorkTransitionVO> connectors = processVO.getTransitions();
        n = connectors==null?0:connectors.size();
        for (i=0; i<n; i++) {
            conn = processVO.getTransitions().get(i);
            node = findNode(conn.getFromWorkId());
            node2 = findNode(conn.getToWorkId());
            if (node != null && node2 != null) {
                link = new Link(node, node2, conn, arrowstyle);
                links.add(link);
            }
        }
        List<ProcessVO> subprocs = processVO.getSubProcesses();
        if (subprocs!=null) {
            for (ProcessVO subproc : subprocs) {
                SubGraph subgraph = new SubGraph(subproc, this, metainfo, getIconFactory());
                subgraphs.add(subgraph);
            }
        }
        if (processVO.getTextNotes()!=null) {
        	for (TextNoteVO note : processVO.getTextNotes()) {
        		if (notes==null) notes = new ArrayList<TextNote>();
        		notes.add(new TextNote(note, this));
        	}
        }
	}

    private void parseCoord(String attrname) {
        String guiinfo = processVO.getAttribute(attrname);
        Rectangle rect = parseGeoInfo(guiinfo);
        if (rect!=null) {
            this.lx = rect.x;
            this.ly = rect.y;
            this.lw = rect.width;
            this.lh = rect.height;
        }
    }

    private Node findConvertedSubProcessNode(Long id) {
        int i;
        Node node;
        for (i=0; i<nodes.size(); i++) {
            node = nodes.get(i);
            if (node.getId()<0 && node.isSubProcessActivity()) {
                String subprocid = node.getAttribute("process_id");
                if (subprocid!=null && subprocid.equals(id.toString())) return node;
                subprocid = node.getAttribute(WorkAttributeConstant.ALIAS_PROCESS_ID);
                if (subprocid!=null && subprocid.equals(id.toString())) return node;
            }
        }
        return null;
    }

	public void setStatus(StringBuffer errmsg) {
	    for (ActivityInstanceVO ai : instance.getActivities()) {
	        Node node = findNode(ai.getDefinitionId());
	        if (node!=null) {
	            node.addInstance(ai, true);
	        } else {
	            SubGraph subgraph = this.findSubGraph(ai.getDefinitionId());
	            if (subgraph==null) {
	                if (OwnerType.PROCESS.equals(ai.getStatusMessage())) {
	                    node = findConvertedSubProcessNode(ai.getDefinitionId());
	                    if (node!=null) node.addStatus(ai);
	                } else {
	                    errmsg.append("  activity ").append(ai.getDefinitionId()).append('\n');
	                }
	            } else {
	                // the following is done in subgraph.setStatus()
//	                String status = subgraph.getStatus();
//	                String newStatus = Integer.toString(ai.getStatusCode());
//	                if (status==null) subgraph.setStatus(newStatus);
//	                else subgraph.setStatus(status+newStatus);
	            }
	        }
	    }
        for (Link link : this.links) link.setColor(Color.LIGHT_GRAY);
        for (WorkTransitionInstanceVO ti : instance.getTransitions()) {
	        Link link = this.findLink(ti.getTransitionID());
	        if (link!=null) {
	            link.addInstance(ti);
	        } else if (!isHiddenTransition(ti.getTransitionID())) {
	            errmsg.append("  transition ").append(ti.getTransitionID()).append('\n');
	        }
	    }
        for (SubGraph subgraph : subgraphs) {
            subgraph.setStatus(errmsg);
        }
	}

	private boolean isHiddenTransition(Long transId) {
		String v = processVO.getAttribute(WorkAttributeConstant.START_TRANSITION_ID);
		if (transId.toString().equals(v)) return true;
		if (processVO.getSubProcesses()!=null) {
			for (ProcessVO subproc : processVO.getSubProcesses()) {
				v = subproc.getAttribute(WorkAttributeConstant.ENTRY_TRANSITION_ID);
				if (transId.toString().equals(v)) return true;
			}
		}
		return false;
	}

    public void setMenuItem(JMenuItem menuitem) {
        this.menuitem = menuitem;
    }

    public JMenuItem getMenuItem() {
        return menuitem;
    }

    public String getMenuItemLabel() {
        String statusFlag;
        if (dirtyLevel==Graph.NEW) statusFlag = "** ";
        else if (dirtyLevel==Graph.DIRTY) statusFlag = "** ";
        else if (dirtyLevel==Graph.GEOCHANGE) statusFlag = "*  ";
        else statusFlag = "   ";
        return statusFlag + getName() + " - V" + getProcessVO().getVersionString();
    }

    public void setDirtyLevel(int level) {
        if (level==DIRTY) {
            if (dirtyLevel==CLEAN || dirtyLevel==GEOCHANGE) {
                dirtyLevel = DIRTY;
                if (menuitem!=null) menuitem.setText(getMenuItemLabel());
            }
        } else if (level==CLEAN) {
            dirtyLevel = CLEAN;
            if (menuitem!=null) menuitem.setText(getMenuItemLabel());
        } else if (level==GEOCHANGE) {
            if (dirtyLevel==CLEAN) {
                dirtyLevel = GEOCHANGE;
                if (menuitem!=null) menuitem.setText(getMenuItemLabel());
            }
        } else if (level==NEW) {
            dirtyLevel = NEW;
        }
    }

    public Node addNode(ActivityImplementorVO nmi, int x, int y, boolean recordchange) {
        GraphCommon owner = this;
        for (SubGraph subgraph : subgraphs) {
            if (subgraph.containsPoint(x, y)) {
                owner = subgraph;
                break;
            }
        }
        return addNode(owner, nmi, x, y, recordchange);
    }

	private Node addNode(GraphCommon owner, ActivityImplementorVO nmi, int x, int y, boolean recordchange) {
        String name = "New "+nmi.getLabel();
        if (nmi.isManualTask()) {
            if (owner instanceof SubGraph)
              name = processVO.getProcessName() + " Fallout";
            else
              name = "New Task for " + processVO.getProcessName();
        }
        Long pActId = genNodeId();
        String pDesc = null;
        String pActImplClass = nmi.getImplementorClassName();
        ArrayList<AttributeVO> pAttribs = new ArrayList<AttributeVO>();
        if (nmi.isManualTask()) {
        	pAttribs.add(new AttributeVO(TaskActivity.ATTRIBUTE_TASK_NAME, name));
        	pAttribs.add(new AttributeVO(TaskActivity.ATTRIBUTE_TASK_CATEGORY, "COM"));
        	pAttribs.add(new AttributeVO(TaskActivity.ATTRIBUTE_TASK_VARIABLES,
        			TaskVO.getVariablesAsString(processVO.getVariables(), null)));
        }
        ActivityVO nodet = new ActivityVO(pActId, name, pDesc, pActImplClass, pAttribs);
        owner.getProcessVO().getActivities().add(nodet);
        Node node = new Node(nodet, owner, metainfo);
        String iconname = node.getIconName();
        javax.swing.Icon icon = getIconFactory().getIcon(iconname);
        if (nodestyle.equals(Node.NODE_STYLE_ICON)) {
            node.w = icon.getIconWidth();
            node.h = icon.getIconHeight();
        } else {
            if (icon instanceof ImageIcon) {
                node.w = 100;
                node.h = 60;
            } else {
                node.w = 60;
                node.h = 40;
            }
        }
		node.x = x;
		node.y = y;
		owner.nodes.add(node);
		String xmldesc = nmi.getAttributeDescription();
		if (xmldesc!=null && xmldesc.length()>0) {
	        FormatDom fmter = new FormatDom();
			DomDocument doc = new DomDocument();
			try {
				fmter.load(doc, xmldesc);
				MbengNode widget;
				String default_value, attr_name;
				for (widget=doc.getRootNode().getFirstChild();
					widget!=null; widget=widget.getNextSibling()) {
			        default_value = widget.getAttribute("DEFAULT");
			        if (default_value==null) continue;
			        if (widget.getAttribute("UNITS_ATTR")!=null) continue;
			        attr_name = widget.getAttribute("NAME");
			        if (attr_name==null) continue;
			        if (default_value.equals("$DefaultNotices")) {
			        	node.setAttribute(attr_name, TaskVO.getDefaultNotices());
		    		} else node.setAttribute(attr_name, default_value);
				}
			} catch (MbengException e) {
			}
		}
		if (recordchange) node.getChanges().setChangeType(Changes.NEW);
		setDirtyLevel(DIRTY);
		return node;
	}

	/**
	 * This is for pasting
	 * @param actvo
	 * @return
	 */
	public Node addNode(GraphCommon owner, ActivityVO sourceact, int xoff, int yoff,
			boolean recordchange, boolean newLogicalId) {
        Long pActId = genNodeId();
        ArrayList<AttributeVO> attributes = new ArrayList<AttributeVO>();
        for (AttributeVO attr : sourceact.getAttributes()) {
        	AttributeVO nattr = new AttributeVO(attr.getAttributeName(), attr.getAttributeValue());
        	attributes.add(nattr);
        }
        ActivityVO nodet = new ActivityVO(pActId, sourceact.getActivityName(), sourceact.getActivityDescription(),
        		sourceact.getImplementorClassName(), attributes);
        owner.getProcessVO().getActivities().add(nodet);
        if (newLogicalId) {
	        String lid;
	        if (owner instanceof SubGraph) {
				lid = ((SubGraph)owner).getGraph().generateLogicalId("A");
			} else {
				lid = ((Graph)owner).generateLogicalId("A");
			}
			nodet.setAttribute(WorkAttributeConstant.LOGICAL_ID, lid);
        }
		Node node = new Node(nodet, owner, metainfo);
		if (newLogicalId && node.isTaskActivity()) {
			node.setAttribute(TaskActivity.ATTRIBUTE_TASK_LOGICAL_ID, null);
		}
        node.x += xoff;
        node.y += yoff;
		owner.nodes.add(node);
		if (recordchange) node.getChanges().setChangeType(Changes.NEW);
		setDirtyLevel(DIRTY);
		return node;
	}

	/**
	 * @param from
	 * @param to
	 * @param type
	 * @param style
	 * @param control_points
	 * @return
	 */
	public Link addLink(Node from, Node to, Integer type,
	        String style, int control_points, boolean recordchange) {
	    GraphCommon owner = from.graph;
        WorkTransitionVO conn = new WorkTransitionVO();
        conn.setFromWorkId(from.nodet.getActivityId());
        conn.setToWorkId(to.nodet.getActivityId());
        conn.setEventType(EventType.FINISH);
        conn.setWorkTransitionId(new Long(0));
        conn.setAttributes(new ArrayList<AttributeVO>());
        String av = processVO.getAttribute(WorkTransitionAttributeConstant.TRANSITION_RETRY_COUNT);
        if (av!=null) conn.setAttribute(WorkTransitionAttributeConstant.TRANSITION_RETRY_COUNT, av);
        owner.getProcessVO().getTransitions().add(conn);
        if (null != type) conn.setEventType(type);
        Link link = new Link(from, to, conn, arrowstyle);
        link.setType(style);
        owner.links.add(link);
        setDirtyLevel(DIRTY);
        link.calcLinkPosition(control_points, arrowstyle);
        if (recordchange) link.getChanges().setChangeType(Changes.NEW);
		return link;
	}

	/**
	 * this is for pasting
	 * @param trans
	 * @param fromId
	 * @param toId
	 * @return
	 */
	public Link addLink(GraphCommon owner, WorkTransitionVO trans, Long fromId, Long toId,
			int xoff, int yoff, boolean recordchange, boolean newLogicalId) {
		WorkTransitionVO conn = new WorkTransitionVO();
        conn.setFromWorkId(fromId);
        conn.setToWorkId(toId);
        conn.setEventType(trans.getEventType());
        conn.setWorkTransitionId(new Long(0));
        conn.setCompletionCode(trans.getCompletionCode());
        ArrayList<AttributeVO> attributes = new ArrayList<AttributeVO>();
        for (AttributeVO attr : trans.getAttributes()) {
        	AttributeVO nattr = new AttributeVO(attr.getAttributeName(), attr.getAttributeValue());
        	attributes.add(nattr);
        }
        conn.setAttributes(attributes);
        owner.getProcessVO().getTransitions().add(conn);
        if (newLogicalId) {
	        String lid;
	        if (owner instanceof SubGraph) {
				lid = ((SubGraph)owner).getGraph().generateLogicalId("T");
			} else {
				lid = ((Graph)owner).generateLogicalId("T");
			}
			conn.setAttribute(WorkAttributeConstant.LOGICAL_ID, lid);
        }
        Link link = new Link(owner.findNode(fromId), owner.findNode(toId), conn, arrowstyle);
        if (xoff!=0 || yoff!=0) link.shift(xoff, yoff, arrowstyle);
        owner.links.add(link);
        setDirtyLevel(DIRTY);
        if (recordchange) link.getChanges().setChangeType(Changes.NEW);
		return link;
	}

	public SubGraph addSubGraph(int x, int y, String subtype, boolean recordchange) {
        Long pProcessId = genNodeId();
        String pDesc = null;
        List<ExternalEventVO> pExtEvents = null;
        ProcessVO subproc = new ProcessVO(pProcessId, subtype, pDesc, pExtEvents);
        List<AttributeVO> attributes = new ArrayList<AttributeVO>();
        AttributeVO a = new AttributeVO(null, WorkAttributeConstant.PROCESS_VISIBILITY,
                   ProcessVisibilityConstant.EMBEDDED);
        attributes.add(a);
        a = new AttributeVO(null, WorkAttributeConstant.EMBEDDED_PROCESS_TYPE, subtype);
        attributes.add(a);
        subproc.setAttributes(attributes);
        subproc.setActivities(new ArrayList<ActivityVO>());
        subproc.setTransitions(new ArrayList<WorkTransitionVO>());
        subproc.setVariables(new ArrayList<VariableVO>());
        List<ProcessVO> subprocs = processVO.getSubProcesses();
        if (subprocs==null) {
            subprocs = new ArrayList<ProcessVO>();
            processVO.setSubProcesses(subprocs);
        }
        subprocs.add(subproc);

        SubGraph subgraph = new SubGraph(subproc, this, metainfo, getIconFactory());
        subgraph.x = x;
        subgraph.y = y;
        subgraph.w = 440;
        subgraph.h = 120;
        subgraphs.add(subgraph);        // need to add it before generating first node to avoid dup id
        Node node1 = addNode(subgraph, metainfo.getStartActivity(), x+40, y+40, recordchange);
        node1.nodet.setActivityName("Start");
        if (subtype.equals(ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS)) {
            ActivityImplementorVO nmi = metainfo.getTaskActivity();
            Node node2 = addNode(subgraph, nmi, x+170, y+30, recordchange);
            Node node3 = addNode(subgraph, metainfo.getStopActivity(), x+340, y+40, recordchange);
            node3.nodet.setActivityName("Stop");
            addLink(node1, node2, EventType.FINISH, Link.ELBOW, 2, recordchange);
            addLink(node2, node3, EventType.FINISH, Link.ELBOW, 2, recordchange);
        } else {
            Node node3 = addNode(subgraph, metainfo.getStopActivity(), x+340, y+40, recordchange);
            node3.nodet.setActivityName("Stop");
            addLink(node1, node3, EventType.FINISH, Link.ELBOW, 2, recordchange);
        }
        if (recordchange) subgraph.getChanges().setChangeType(Changes.NEW);
        setDirtyLevel(DIRTY);
	    return subgraph;
	}

	/**
	 * This is for paste
	 * @param source
	 * @return
	 */
	public SubGraph addSubGraph(ProcessVO source, int xoff, int yoff, boolean recordchange) {
        Long pProcessId = genNodeId();
        String subproctype = source.getAttribute(WorkAttributeConstant.EMBEDDED_PROCESS_TYPE);
        if (subproctype==null) subproctype = ProcessVisibilityConstant.EMBEDDED_ERROR_PROCESS;
        ProcessVO existing = processVO.findEmbeddedProcess(subproctype);
        if (existing!=null) return null;
        String subprocName = subproctype;
        ProcessVO subproc = new ProcessVO(pProcessId, subprocName, source.getProcessDescription(), null);
        List<AttributeVO> attributes = new ArrayList<AttributeVO>();
        for (AttributeVO a : source.getAttributes()) {
        	if (a.getAttributeName().equals(WorkAttributeConstant.ENTRY_TRANSITION_ID)) continue;
        	if (a.getAttributeName().equals(WorkAttributeConstant.START_TRANSITION_ID)) continue;
        	AttributeVO attr = new AttributeVO(a.getAttributeName(),a.getAttributeValue());
        	attributes.add(attr);
        }
        subproc.setAttributes(attributes);
        subproc.setActivities(new ArrayList<ActivityVO>());
        subproc.setTransitions(new ArrayList<WorkTransitionVO>());
        subproc.setVariables(new ArrayList<VariableVO>());
        String lid = generateLogicalId("P");
		subproc.setAttribute(WorkAttributeConstant.LOGICAL_ID, lid);
        List<ProcessVO> subprocs = processVO.getSubProcesses();
        if (subprocs==null) {
            subprocs = new ArrayList<ProcessVO>();
            processVO.setSubProcesses(subprocs);
        }
        subprocs.add(subproc);
        SubGraph subgraph = new SubGraph(subproc, this, metainfo, getIconFactory());
        subgraphs.add(subgraph);
        HashMap<Long,Long> map = new HashMap<Long,Long>();
        for (ActivityVO a : source.getActivities()) {
        	Node node = this.addNode(subgraph, a, 0, 0, recordchange, true);
        	map.put(a.getActivityId(), node.getActivityId());
        }
        for (WorkTransitionVO t : source.getTransitions()) {
        	this.addLink(subgraph, t, map.get(t.getFromWorkId()), map.get(t.getToWorkId()),
        			0, 0, recordchange, true);
        }
        if (xoff!=0 || yoff!=0) {
            subgraph.move(subgraph.x+xoff, subgraph.y+yoff, arrowstyle);
        }
        if (recordchange) subgraph.getChanges().setChangeType(Changes.NEW);
        setDirtyLevel(DIRTY);
	    return subgraph;
	}

    public TextNote addTextNote(int x, int y, String content) {
        TextNoteVO vo = new TextNoteVO();
        List<TextNoteVO> textNotes = getProcessVO().getTextNotes();
        if (textNotes==null) {
        	textNotes = new ArrayList<TextNoteVO>();
        	getProcessVO().setTextNotes(textNotes);
        }
        textNotes.add(vo);
        String logicalId = generateLogicalId("N");
        vo.setContent(content);
        vo.setLogicalId(logicalId);
        TextNote textNote = new TextNote(vo, this, x, y, 200, 60);
        if (notes==null) notes = new ArrayList<TextNote>();
        notes.add(textNote);
		setDirtyLevel(DIRTY);
		return textNote;
    }

	public boolean hasErrorHandler() {
        for (ProcessVO subproc : processVO.getSubProcesses()) {
            if (ProcessVisibilityConstant.EMBEDDED.equals(
                    subproc.getAttribute(WorkAttributeConstant.PROCESS_VISIBILITY))) return true;
        }
        return false;
	}

	public void removeLink(Link link, boolean recordchange) {
	    GraphCommon owner = link.from.graph;
	    if (!recordchange || link.isNew()) {
		    owner.getProcessVO().getTransitions().remove(link.conn);
			for (int i=0; i<owner.links.size(); i++) {
				if (link==owner.links.get(i)) {
				    if (link.conn.getWorkTransitionId()!=null
				    		&& link.conn.getWorkTransitionId().longValue()>0L)
				        owner.getProcessVO().
				            addDeletedTransitions(link.conn.getWorkTransitionId());
					owner.links.remove(i);
					break;
				}
			}
	    } else {
	    	link.getChanges().setChangeType(Changes.DELETE);
	    }
        setDirtyLevel(DIRTY);
	}

	public void removeNode(Node node, boolean recordchange) {
	    GraphCommon owner = node.graph;
		List<WorkTransitionVO> connectors = owner.getProcessVO().getTransitions();
        Long id = node.nodet.getActivityId();
        if (!recordchange || node.isNew()) {
	        for (int i=connectors.size()-1; i>=0; i--) {
	            WorkTransitionVO w = connectors.get(i);
	            if (id.equals(w.getFromWorkId()) || id.equals(w.getToWorkId())) {
	                connectors.remove(i);
	            }
	        }
	        owner.getProcessVO().getActivities().remove(node.nodet);
			int i;
			Link link;
			for (i=owner.links.size()-1; i>=0; i--) {
				link = owner.links.get(i);
				if (link.from==node || link.to==node) {
					removeLink(link, recordchange);
				}
			}
			for (i=0; i<owner.nodes.size(); i++) {
				if (node==owner.nodes.get(i)) {
				    owner.nodes.remove(i);
					break;
				}
			}
        } else {
			int i;
			Link link;
			for (i=owner.links.size()-1; i>=0; i--) {
				link = owner.links.get(i);
				if (link.from==node || link.to==node) {
			    	link.getChanges().setChangeType(Changes.DELETE);
				}
			}
			node.getChanges().setChangeType(Changes.DELETE);
        }
        setDirtyLevel(DIRTY);
	}

	public void removeSubGraph(SubGraph subgraph) {
	    ProcessVO subProcDef = subgraph.getProcessVO();
	    String entryTransId = subProcDef.getAttribute(WorkAttributeConstant.ENTRY_TRANSITION_ID);
	    List<ProcessVO> subprocs = processVO.getSubProcesses();
	    subprocs.remove(subProcDef);
	    subgraphs.remove(subgraph);
	    if (entryTransId!=null) {
	        Long deleteTransId = new Long(entryTransId);
	        if (deleteTransId.longValue()>0) processVO.addDeletedTransitions(deleteTransId);
	    }
	    setDirtyLevel(DIRTY);
	}

	public void removeTextNote(TextNote note) {
        note.graph.getProcessVO().getTextNotes().remove(note.vo);
        int i;
        for (i=0; i<notes.size(); i++) {
        	if (note==notes.get(i)) {
				notes.remove(i);
				break;
        	}
        }
        setDirtyLevel(DIRTY);
	}

	public void save_temp_vars() {
        processVO.setAttribute(this.geo_attribute, formatGeoInfo(lx,ly,lw,lh));
		for (Link link : links) link.save_temp_vars(this.geo_attribute);
		for (Node node : nodes) node.save_temp_vars();
		for (SubGraph subgraph : subgraphs) subgraph.save_temp_vars();
		if (notes!=null) {
			for (TextNote note : notes) note.save_temp_vars();
		}
	}

	public void blankOutTaskLogicalId() {
		for (Node node : nodes) {
			if (node.isTaskActivity()) {
				node.setAttribute(TaskActivity.ATTRIBUTE_TASK_LOGICAL_ID, null);
			}
		}
		for (SubGraph subgraph : subgraphs) {
			for (Node node : subgraph.nodes) {
				if (node.isTaskActivity()) {
					node.setAttribute(TaskActivity.ATTRIBUTE_TASK_LOGICAL_ID, null);
				}
			}
		}
	}

	public boolean isSwimLanes() {
	    return packageVO!=null;
	}

	public Dimension getGraphSize() {
		int w, h;
		w = h = 0;
		for (Node node : nodes) {
			if (node.x+node.w>w) w = node.x+node.w;
			if (node.y+node.h>h) h = node.y+node.h;
		}
		if (isSwimLanes()) {
            for (SubGraph subgraph : subgraphs) {
                for (Node node : subgraph.nodes) {
                    if (node.x+node.w>w) w = node.x+node.w;
                }
                if (subgraph.y+subgraph.h>h) h = subgraph.y+subgraph.h;
            }
		} else {
    		for (SubGraph subgraph : subgraphs) {
                if (subgraph.x+subgraph.w>w) w = subgraph.x+subgraph.w;
                if (subgraph.y+subgraph.h>h) h = subgraph.y+subgraph.h;
    		}
		}
		return new Dimension(w, h);
	}

    /**
     * @param version The version to set.
     */
    public void setVersion(String version) {
        int k = version.indexOf('.');
        if (k>=0) processVO.setVersion(Integer.parseInt(version.substring(0,k))
            * 1000 + Integer.parseInt(version.substring(k+1)));
        else processVO.setVersion(Integer.parseInt(version));
    }

    public ProcessInstanceVO getInstance() {
        return instance;
    }

    public boolean isReadonly() {
        return isReadonly;
    }

    public void setReadonly(boolean isReadonly) {
        this.isReadonly = isReadonly;
    }

    public ProcessVO getProcessVO() {
        return processVO;
    }

    public void setProcessVO(ProcessVO processVO) {
        this.processVO = processVO;
    }

    private boolean labelOnPoint(Graphics g, int x, int y) {
        return (x>=lx && x<=lx+lw && y>=ly&&y<=ly+lh);
    }

    public Object objectAt(int x, int y, Graphics g) {
        Node node;
        Link link;
        TextNote textNote;
        if (labelOnPoint(g, x, y)) {
            return this;
        }
        node = this.nodeAt(g, x, y);
        if (node!=null) return node;
        link = this.linkAt(g, x, y);
        if (link!=null) return link;
        textNote = this.textNoteAt(g, x, y);
        if (textNote!=null) return textNote;
        for (SubGraph subgraph : subgraphs) {
            // label is a little outside the boundary, so check first
            if (subgraph.labelOnPoint(g, x, y)) {
                return subgraph;
            }
            if (!subgraph.containsPoint(x, y)) continue;
            node = subgraph.nodeAt(g, x, y);
            if (node!=null) return node;
            link = subgraph.linkAt(g, x, y);
            if (link!=null) return link;
            break;
        }
        return null;
    }

    public String getAttribute(String name) {
        return processVO.getAttribute(name);
    }

    public List<AttributeVO> getAttributes() {
        return processVO.getAttributes();
    }

    public String getDescription() {
        return processVO.getProcessDescription();
    }

    public Long getId() {
        return processVO.getProcessId();
    }

    public String getName() {
        if (processVO==null && packageVO!=null) return packageVO.getPackageName();
        else if (processVO.isRemote()) return processVO.getRemoteName();
        else return processVO.getProcessName();
    }

    public int getVersion() {
        return processVO.getVersion();
    }

    public int getSLA() {
        // not longer used, but needs this for MDW 4 backward compatibility
    	return 0;
    }

    public void setAttribute(String name, String value) {
        processVO.setAttribute(name, value);
    }

    public void setDescription(String value) {
        processVO.setProcessDescription(value);
    }

    public void setName(String value) {
        processVO.setProcessName(value);
    }

    public void setSLA(int value) {
        // no longer used, but needs this for MDW 4 backward compatibility
    }

    public boolean nameChanged() {
        return !originalName.equals(processVO.getProcessName());
    }

    public VariableVO addVariable(String name, VariableTypeVO vt) {
        VariableVO doc = new VariableVO();
        doc.setVariableName(name);
        doc.setVariableId(null);
        doc.setVariableType(vt.getVariableType());
        processVO.getVariables().add(doc);
        updateTaskVariableMapping(null, name);
        return doc;
    }

    public void removeVariable(VariableVO doc) {
        processVO.getVariables().remove(doc);
        updateTaskVariableMapping(doc.getVariableName(), null);
        // TODO: remove references to this document in activities
    }

    public void renameVariable(VariableVO var, String newName) {
        String oldName = var.getVariableName();
        var.setVariableName(newName);
        updateTaskVariableMapping(oldName, newName);
    }

    public VariableVO getVariable(int i) {
        return processVO.getVariables().get(i);
    }

    public int numberOfVariables() {
        List<VariableVO> variables = processVO.getVariables();
        return variables==null?0:variables.size();
    }

    public int getNewVersion(boolean major) {
        int version = processVO.getVersion();
        if (major) return (version/1000+1)*1000;
        else return version+1;
    }

    public String getNewVersionString(boolean major) {
        int version = getNewVersion(major);
        return version/1000 + "." + version%1000;
    }

    private boolean existNodeId(long id) {
        for (Node node : nodes) {
            if (node.getActivityId().longValue()==id) return true;
        }
        if (subgraphs==null) return false;
        for (SubGraph subgraph : subgraphs) {
            if (subgraph.getId().longValue()==id) return true;
            for (Node node : subgraph.nodes) {
                if (node.getActivityId().longValue()==id) return true;
            }
        }
        return false;
    }

    private Long genNodeId() {
        if (lastGenId>=0) lastGenId = -1;
        while (existNodeId(lastGenId)) {
            lastGenId--;
        }
        return new Long(lastGenId);
    }

    private SubGraph findSubGraph(Long id) {
        for (SubGraph subgraph : this.subgraphs) {
            if (subgraph.getId().equals(id)) return subgraph;
        }
        return null;
    }

    private void updateTaskVariableMapping(String oldName, String newName) {
        for (Node node : nodes) {
            if (node.isTaskActivity()) {
                String attrvalue = node.getAttribute(TaskActivity.ATTRIBUTE_TASK_VARIABLES);
                attrvalue = TaskVO.updateVariableInString(attrvalue, getProcessVO().getVariables());
                node.setAttribute(TaskActivity.ATTRIBUTE_TASK_VARIABLES, attrvalue);
            }
        }
        for (SubGraph subgraph : subgraphs) {
            for (Node node : subgraph.nodes) {
                if (node.isTaskActivity()) {
                    String attrvalue = node.getAttribute(TaskActivity.ATTRIBUTE_TASK_VARIABLES);
                    attrvalue = TaskVO.updateVariableInString(attrvalue, getProcessVO().getVariables());
                    node.setAttribute(TaskActivity.ATTRIBUTE_TASK_VARIABLES, attrvalue);
                }
            }
        }
    }

    public void resetNodeImageSize(String nodestyle) {
        super.resetNodeImageSize(nodestyle, arrowstyle);
        for (SubGraph subgraph : subgraphs) {
            subgraph.resetNodeImageSize(nodestyle, arrowstyle);
        }
    }

    public void setArrowStyle(String arrowstyle) {
        this.arrowstyle = arrowstyle;
        setAttribute(Link.ATTRIBUTE_ARROW_STYLE, arrowstyle);
        for (Link link : links) {
            link.determineArrow(arrowstyle);
        }
        for (SubGraph subgraph : subgraphs) {
            for (Link link : subgraph.links) {
                link.determineArrow(arrowstyle);
            }
        }
    }

    public void changeAllLinkStyle(String style) {
        for (Link link : links) {
            if (link.from!=link.to) link.setType(style);
        }
        for (SubGraph subgraph : subgraphs) {
            for (Link link : subgraph.links) {
                if (link.from!=link.to) link.setType(style);
            }
        }
    }

    public String generateLogicalId(String prefix) {
    	if (prefix.equals("A") || prefix.equals("P") || prefix.equals("N")) {
    		if (this.largestActivityLogicalId<0) {
    			String v;
    			for (Node node : nodes) {
    				v = node.getAttribute(WorkAttributeConstant.LOGICAL_ID);
    				if (v!=null && v.startsWith("A")) {
    					try {
							int k = Integer.parseInt(v.substring(1));
							if (k>largestActivityLogicalId) largestActivityLogicalId = k;
						} catch (NumberFormatException e) {
						}
    				}
    			}
    			if (this.subgraphs!=null) {
    				for (SubGraph subgraph : subgraphs) {
    					v = subgraph.getAttribute(WorkAttributeConstant.LOGICAL_ID);
    					if (v!=null && v.startsWith("P")) {
    						try {
    							int k = Integer.parseInt(v.substring(1));
    							if (k>largestActivityLogicalId) largestActivityLogicalId = k;
    						} catch (NumberFormatException e) {
    						}
    	    			}
    					for (Node node : subgraph.nodes) {
    	    				v = node.getAttribute(WorkAttributeConstant.LOGICAL_ID);
    	    				if (v!=null && v.startsWith("A")) {
    	    					try {
    								int k = Integer.parseInt(v.substring(1));
    								if (k>largestActivityLogicalId) largestActivityLogicalId = k;
    							} catch (NumberFormatException e) {
    							}
    	    				}
    	    			}
    				}
    			}
    			if (largestActivityLogicalId<0) largestActivityLogicalId = 0;
    		}
    		largestActivityLogicalId++;
    		return prefix + largestActivityLogicalId;
    	} else if (prefix.equals("T")) {
    		if (this.largestTransitionLogicalId<0) {
    			String v;
    			for (Link link : links) {
    				v = link.conn.getAttribute(WorkAttributeConstant.LOGICAL_ID);
    				if (v!=null && v.startsWith("T")) {
    					try {
							int k = Integer.parseInt(v.substring(1));
							if (k>largestTransitionLogicalId) largestTransitionLogicalId = k;
						} catch (NumberFormatException e) {
						}
    				}
    			}
    			if (this.subgraphs!=null) {
    				for (SubGraph subgraph : subgraphs) {
    					for (Link link : subgraph.links) {
    	    				v = link.conn.getAttribute(WorkAttributeConstant.LOGICAL_ID);
    	    				if (v!=null && v.startsWith("T")) {
    	    					try {
    								int k = Integer.parseInt(v.substring(1));
    								if (k>largestTransitionLogicalId) largestTransitionLogicalId = k;
    							} catch (NumberFormatException e) {
    							}
    	    				}
    	    			}
    				}
    			}
    			if (largestTransitionLogicalId<0) largestTransitionLogicalId = 0;
    		}
    		largestTransitionLogicalId++;
    		return "T" + largestTransitionLogicalId;
    	} else return "???";

    }

    public boolean isFileVersion() {
    	return processVO.getRawFile() != null;
    }

    public void commitChanges() {
    	this.changes.setChangeType(Changes.NONE);
    	List<Object> toDelete = new ArrayList<Object>();
    	for (Link link : links) {
			if (link.isDeleted()) {
				toDelete.add(link);
			} else {
				link.getChanges().setChangeType(Changes.NONE);
			}
		}
    	for (Node node : nodes) {
			if (node.isDeleted()) {
				toDelete.add(node);
			} else {
				node.getChanges().setChangeType(Changes.NONE);
			}
		}
		for (SubGraph subgraph : subgraphs) {
			if (subgraph.isDeleted()) {
				toDelete.add(subgraph);
			} else {
				subgraph.getChanges().setChangeType(Changes.NONE);
				for (Link link : subgraph.links) {
					if (link.isDeleted()) {
						toDelete.add(link);
					} else {
						link.getChanges().setChangeType(Changes.NONE);
					}
				}
				for (Node node : subgraph.nodes) {
					if (node.isDeleted()) {
						toDelete.add(node);
					} else {
						node.getChanges().setChangeType(Changes.NONE);
					}
				}

			}
		}
		for (Object obj : toDelete) {
			if (obj instanceof Node) {
				removeNode((Node)obj, false);
			} else if (obj instanceof Link) {
				removeLink((Link)obj, false);
			} else if (obj instanceof SubGraph) {
				removeSubGraph((SubGraph)obj);
			}
		}
    	this.save_temp_vars();
    }

    private String getOrGenerateLogicalId(ActivityVO act) {
    	String lid = act.getLogicalId();
    	if (lid==null || lid.length()==0) {
    		lid = this.generateLogicalId("A");
    		act.setAttribute(WorkAttributeConstant.LOGICAL_ID, lid);
    	}
    	return lid;
    }

    private String getOrGenerateLogicalId(WorkTransitionVO trans) {
    	String lid = trans.getAttribute(WorkAttributeConstant.LOGICAL_ID);
    	if (lid==null || lid.length()==0) {
    		lid = this.generateLogicalId("T");
    		trans.setAttribute(WorkAttributeConstant.LOGICAL_ID, lid);
    	}
    	return lid;
    }

    private String getOrGenerateLogicalId(ProcessVO procdef) {
    	String lid = procdef.getAttribute(WorkAttributeConstant.LOGICAL_ID);
    	if (lid==null || lid.length()==0) {
    		lid = this.generateLogicalId("P");
    		procdef.setAttribute(WorkAttributeConstant.LOGICAL_ID, lid);
    	}
    	return lid;
    }

    private void checkActivityInSource(ActivityVO sourceact, GraphCommon graphcommon,
    		HashMap<String,Node> nodemap, HashMap<Long,Long> delidmap) {
    	String lid = sourceact.getLogicalId();
		Node node;
    	if (lid!=null && lid.length()>0) node = nodemap.get(lid);
    	else node = null;
    	if (node==null) {
    		node = this.addNode(graphcommon, sourceact, 0, 0, false, false);
    		if (lid==null || lid.length()==0) {
    			lid = this.generateLogicalId("A");
    			node.setAttribute(WorkAttributeConstant.LOGICAL_ID, lid);
    		}
    		node.getChanges().setChangeType(Changes.DELETE);
    		nodemap.put(lid, node);
    	} else {
    		node.getChanges().setChangeType(Changes.NONE);
    	}
		delidmap.put(sourceact.getActivityId(), node.getActivityId());
    }

    private void checkTransitionInSource(WorkTransitionVO sourcetrans, GraphCommon graphcommon,
    		HashMap<String,Link> linkmap, HashMap<Long,Long> delidmap) {
    	String lid = sourcetrans.getAttribute(WorkAttributeConstant.LOGICAL_ID);
		Link link;
    	if (lid!=null && lid.length()>0) link = linkmap.get(lid);
    	else link = null;
    	if (link==null) {
    		Long fromId = delidmap.get(sourcetrans.getFromWorkId());
    		Long toId = delidmap.get(sourcetrans.getToWorkId());
    		link = this.addLink(this, sourcetrans, fromId, toId, 0, 0, false, false);
    		link.getChanges().setChangeType(Changes.DELETE);
    	} else {
    		link.getChanges().setChangeType(Changes.NONE);
    	}
    }

    public void compareProcess(ProcessVO otherproc) {
    	this.save_temp_vars();
    	HashMap<String,Node> nodemap = new HashMap<String,Node>();
    	HashMap<String,Link> linkmap = new HashMap<String,Link>();
    	HashMap<String,SubGraph> subgraphmap = new HashMap<String,SubGraph>();
    	HashMap<Long,Long> delidmap = new HashMap<Long,Long>();
    	String lid;
    	for (Node node : nodes) {
    		node.getChanges().setChangeType(Changes.NEW);
    		lid = getOrGenerateLogicalId(node.nodet);
    		nodemap.put(lid, node);
    	}
    	for (Link link : links) {
    		link.getChanges().setChangeType(Changes.NEW);
    		lid = getOrGenerateLogicalId(link.conn);
    		linkmap.put(lid, link);
    	}
    	for (SubGraph subgraph : subgraphs) {
    		subgraph.getChanges().setChangeType(Changes.NEW);
    		lid = getOrGenerateLogicalId(subgraph.getProcessVO());
    		subgraphmap.put(lid, subgraph);
    		for (Node node : subgraph.nodes) {
        		node.getChanges().setChangeType(Changes.NEW);
        		lid = getOrGenerateLogicalId(node.nodet);
        		nodemap.put(lid, node);
        	}
        	for (Link link : subgraph.links) {
        		link.getChanges().setChangeType(Changes.NEW);
        		lid = getOrGenerateLogicalId(link.conn);
        		linkmap.put(lid, link);
        	}
    	}
    	for (ActivityVO a : otherproc.getActivities()) {
    		checkActivityInSource(a, this, nodemap, delidmap);
    	}
    	for (WorkTransitionVO t : otherproc.getTransitions()) {
    		checkTransitionInSource(t, this, linkmap, delidmap);
    	}
    	if (otherproc.getSubProcesses()!=null) {
    		for (ProcessVO subproc : otherproc.getSubProcesses()) {
    			lid = subproc.getAttribute(WorkAttributeConstant.LOGICAL_ID);
    			SubGraph subgraph;
            	if (lid!=null && lid.length()>0) subgraph = subgraphmap.get(lid);
            	else subgraph = null;
            	if (subgraph==null) {
            		subgraph = this.addSubGraph(subproc, 0, 0, false);
            		if (subgraph!=null) {
	            		if (lid==null || lid.length()==0) {
	            			lid = this.generateLogicalId("P");
	            			subgraph.setAttribute(WorkAttributeConstant.LOGICAL_ID, lid);
	            		}
	            		subgraph.getChanges().setChangeType(Changes.DELETE);
	            		delidmap.put(subproc.getProcessId(), subgraph.getId());
	            		subgraphmap.put(lid, subgraph);
            		} else {
            			// TODO when both have same type of subprocesses, cannot
            			// add the other one. How do we show the difference?
            		}
            	} else {
            		subgraph.getChanges().setChangeType(Changes.NONE);
            		for (ActivityVO a : subproc.getActivities()) {
                		checkActivityInSource(a, subgraph, nodemap, delidmap);
                	}
                	for (WorkTransitionVO t : subproc.getTransitions()) {
                		checkTransitionInSource(t, subgraph, linkmap, delidmap);
                	}
            	}
    		}
    	}
    }

    public Node findNodeByLogicalId(String lid) {
	    for (Node a : nodes) {
            if (a.getLogicalId().equals(lid)) return a;
        }
	    for (SubGraph subgraph : subgraphs) {
	    	for (Node a : subgraph.nodes) {
	    		if (a.getLogicalId().equals(lid)) return a;
	    	}
	    }
	    return null;
    }

	public String getNodeIdType() {
		return nodeIdType;
	}

	public void setNodeIdType(String newNodeIdType) {
		nodeIdType = newNodeIdType;
	}

    public void assignNodeSequenceIds() {
        int curSeq = assignNodeSequenceIds(1);
        if (subgraphs != null) {
            for (SubGraph sub : subgraphs)
                curSeq = sub.assignNodeSequenceIds(++curSeq);
        }
    }

    public void assignSubgraphSequenceIds() {
        List<SubGraph> subs = new ArrayList<SubGraph>(subgraphs);
        Collections.sort(subs, new Comparator<SubGraph>() {
            public int compare(SubGraph sg1, SubGraph sg2) {
                if (Math.abs(sg1.y - sg2.y) > 100)
                    return sg1.y - sg2.y;
                // otherwise closest to top-left of canvas
                return (int)(Math.sqrt(Math.pow(sg1.x,2) + Math.pow(sg1.y,2)) - Math.sqrt(Math.pow(sg2.x,2) + Math.pow(sg2.y,2)));
            }
        });
        for (int i = 0; i < subs.size(); i++)
            subs.get(i).setSequenceId(i+1);
    }

    public List<SubGraph> getSubgraphs(String sortIdType) {
        Comparator<SubGraph> comparator = null;
        if (Node.ID_REFERENCE.equals(sortIdType)) {
            comparator = new Comparator<SubGraph>() {
                public int compare(SubGraph sg1, SubGraph sg2) {
                    if (sg2.getReferenceId().isEmpty())
                        return -1;
                    else if (sg1.getReferenceId().isEmpty())
                        return 1;
                    else {
                        return sg1.getReferenceId().compareTo(sg2.getReferenceId());
                    }
                }
            };
        }
        else if (Node.ID_DATABASE.equals(sortIdType)) {
            comparator = new Comparator<SubGraph>() {
                public int compare(SubGraph sg1, SubGraph sg2) {
                    return sg1.getId().compareTo(sg2.getId());
                }
            };
        }
        else if (Node.ID_SEQUENCE.equals(sortIdType)) {
            comparator = new Comparator<SubGraph>() {
                public int compare(SubGraph sg1, SubGraph sg2) {
                    return new Integer(sg1.getSequenceId()).compareTo(new Integer(sg2.getSequenceId()));
                }
            };
        }

        if (comparator == null) {
            return subgraphs;
        }
        else {
            List<SubGraph> sorted = new ArrayList<SubGraph>(subgraphs);
            Collections.sort(sorted, comparator);
            return sorted;
        }
    }

}
