/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.model.data.common.Changes;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.LaneVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;

public class SubGraph extends GraphCommon implements Selectable {
    private static final int DEFAULT_BOX_HEIGHT = 80;
    private static final int DEFAULT_NODE_INTERVAL = 120;
    public static final int SWIMLANE_LABEL_BAR_WIDTH = 20;
    private Graph graph;
    private boolean toConvert;
    private String status;
    private List<ProcessInstanceVO> instances;
    public int lx, ly;      // working variables

    public SubGraph(ProcessVO processVO, Graph graph, NodeMetaInfo metainfo, IconFactory iconFactory) {
        super(iconFactory);
        this.graph = graph;
        this.processVO = processVO;
        this.geo_attribute = graph.geo_attribute;
        toConvert = false;
        nodes = new ArrayList<Node>();
        links = new ArrayList<Link>();
        status = null;
        instances = null;
        Node node, node2;
        List<ActivityVO> activities = processVO.getActivities();
        String guiinfo = getProcessVO().getAttribute(this.geo_attribute);
        Rectangle rect = parseGeoInfo(guiinfo);
        if (rect!=null) {
            this.x = rect.x;
            this.y = rect.y;
            this.w = rect.width;
            this.h = rect.height;
        } else {
            this.x = 400;
            this.y = 40;
            this.w = activities!=null?(activities.size()*DEFAULT_NODE_INTERVAL):
                2*DEFAULT_NODE_INTERVAL;
            this.h = DEFAULT_BOX_HEIGHT;
        }
        if (activities!=null) {
            for (ActivityVO nodet : activities) {
                node = new Node(nodet, this, metainfo);
                nodes.add(node);
                if (!toConvert && outsideBox(node)) toConvert = true;
            }
        }
        List<WorkTransitionVO> conns = processVO.getTransitions();
        if (conns!=null) {
            for (WorkTransitionVO conn : conns) {
                node = findNode(conn.getFromWorkId());
                node2 = findNode(conn.getToWorkId());
                Link link = new Link(node, node2, conn, graph.arrowstyle);
                links.add(link);
            }
        }
        if (toConvert) {
            auto_layout();
        }
    	changes = new Changes(processVO.getAttributes());
    }

    void setStatus(StringBuffer errmsg) {
        for (Link link : this.links) link.setColor(Color.LIGHT_GRAY);
        if (instances==null) return;
        for (ProcessInstanceVO instance : instances) {
            String newStatus = Integer.toString(instance.getStatusCode());
            if (status==null) status = newStatus;
            else status = status+newStatus;
            for (ActivityInstanceVO ai : instance.getActivities()) {
                Node node = findNode(ai.getDefinitionId());
                if (node!=null) {
                    node.addInstance(ai, true);
                } else {
                    if (!OwnerType.PROCESS.equals(ai.getStatusMessage())) {
                        // not a subgraph
                        errmsg.append("  activity ").append(ai.getDefinitionId()).append('\n');
                    }
                }
            }
            String v = processVO.getAttribute(WorkAttributeConstant.START_TRANSITION_ID);
            Long startTransitionId = (v==null)?new Long(0):new Long(v);
            for (WorkTransitionInstanceVO ti : instance.getTransitions()) {
                Link link = this.findLink(ti.getTransitionID());
                if (link!=null) {
                    link.addInstance(ti);
                } else if (!ti.getTransitionID().equals(startTransitionId)) {
                    errmsg.append("  transition ").append(ti.getTransitionID()).append('\n');
                }
            }
        }
    }

    /**
     * For immediate-display when running process in designer
     */
    public void setInstanceStatus(Long procInstId, Integer statusCode) {
        if (instances==null) return;
        int n = instances.size();
        for (int i=0; i<n; i++) {
        	ProcessInstanceVO pi1 = instances.get(i);
        	if (pi1.getId().equals(procInstId)) {
        		pi1.setStatusCode(statusCode);
                String newStatus = Integer.toString(statusCode);
                if (status==null) status = newStatus;
                else if (i >= status.length()) status = status + newStatus;
                else if (i==0) status = newStatus + status.substring(1);
                else if (i==n-1) status = status.substring(0,i) + newStatus;
                else status = status.substring(0,i) + newStatus + status.substring(i+1);
                break;
        	}
        }
    }

    private boolean outsideBox(Node node) {
        return (node.x<x || node.x+node.w > x+w ||
                node.y<y || node.y+node.h>y+h);
    }

    private List<Node> sort_nodes() {
        List<Node> newNodes = new ArrayList<Node>();
        Node first = null;
        boolean hasInLinks;
        for (Node node : nodes) {
            hasInLinks = false;
            for (Link l : links) {
                if (l.to==node) hasInLinks = true;
            }
            if (!hasInLinks) {
                first = node;
                break;
            }
        }
        if (first==null) first = nodes.get(0);
        nodes.remove(first);
        newNodes.add(first);
        Node last = first, next;
        while (nodes.size()>0) {
            next = null;
            for (Link l : links) {
                if (l.from==last && nodes.contains(l.to)) {
                    next=l.to;
                    break;
                }
            }
            if (next==null) next = nodes.get(0);
            nodes.remove(next);
            newNodes.add(next);
            last = next;
        }
        return newNodes;
    }

    void auto_layout() {
        if (this.isSwimLane()) {
            int max_h = 40;
            for (Node node : nodes) {
                String av = node.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO);
                Rectangle r = this.parseGeoInfo(av);
                if (r==null) {
                    node.x = 10;
                    node.y = 10;
                    node.w = 40;
                    node.y = 40;
                } else {
                    node.x = r.x;
                    node.w = r.width;
                    node.h = r.height;
                    node.y = r.y;
                }
                if (node.y + node.h > max_h) max_h = node.y + node.h;
            }
            double ratio = (double)h/(double)max_h;
            for (Node node : nodes) {
                node.y = y + (int)(node.y*ratio);
            }
        } else {
            // sort nodes
            nodes = sort_nodes();
            // layout them
            int x1 = this.x+40;
            int y1 = this.y+20;
            for (Node n : nodes) {
                n.x = x1;
                n.y = y1;
                x1 += DEFAULT_NODE_INTERVAL;
            }
        }
        for (Link l : links) {
            if (!l.isHidden()) l.calcLinkPosition(l.getNumberOfControlPoints(), graph.arrowstyle);
        }
    }

    public String getName() {
        return processVO.getProcessName();
    }

    public boolean isSwimLane() {
        return WorkAttributeConstant.SWIMLANE_GEO_INFO.equals(geo_attribute);
    }

    public boolean labelOnPoint(Graphics g, int x, int y) {
        if (isSwimLane()) {
            return (x>=this.x && x<=this.x+SWIMLANE_LABEL_BAR_WIDTH
                    && y>=this.y&&y<=this.y+this.h);
        } else {
            String label = getName();
            if (label==null || label.length()==0) return false;
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(label);
            int a = fm.getAscent();
            int d = fm.getDescent();
            return (x>=lx && x<=lx+w && y>=ly-a&&y<=ly+d);
        }
    }

    public void move(int newx, int newy, String arrowstyle) {
        int dx = newx-x;
        int dy = newy-y;
        x = newx;
        y = newy;
        for (Node n : nodes) {
            n.x += dx;
            n.y += dy;
        }
        for (Link link : links) {
            link.move(dx, dy, arrowstyle);
        }
    }

    public void resize(Rectangle newrect) {
        x = newrect.x;
        y = newrect.y;
        w = newrect.width;
        h = newrect.height;
        for (Node node : nodes) {
            if (!outsideBox(node)) continue;
            if (node.x<x) {
                node.x = x;
            } else if (node.x+node.w>x+w) {
                node.x = x+w-node.w;
            }
            if (node.y<y) {
                node.y = y;
            } else if (node.y+node.h>y+h) {
                node.y = y+h-node.h;
            }
            this.recalcLinkPosition(node, graph.arrowstyle);
        }
    }

    public boolean containsPoint(int x, int y) {
        return (x>=this.x && x<this.x+this.w && y>=this.y && y<this.y+this.h);
    }

    public ProcessVO getProcessVO() {
        return processVO;
    }

    public void save_temp_vars() {
        getProcessVO().setAttribute(this.geo_attribute, formatGeoInfo(x,y,w,h));
        if (this.getLogicalId().length()==0) {
        	String id = graph.generateLogicalId("P");
    		setAttribute(WorkAttributeConstant.LOGICAL_ID, id);
        }
        for (Link link : links) link.save_temp_vars(this.geo_attribute);
        for (Node node : nodes) node.save_temp_vars();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ProcessInstanceVO> getInstances() {
        return instances;
    }

    public void setInstances(List<ProcessInstanceVO> instances) {
        this.instances = instances;
    }

    public String getAttribute(String name) {
        return processVO.getAttribute(name);
    }

    public List<AttributeVO> getAttributes() {
        return processVO.getAttributes();
    }

    public Long getId() {
        return processVO.getProcessId();
    }

    public String getLogicalId() {
    	String id = getAttribute(WorkAttributeConstant.LOGICAL_ID);
    	return (id==null)?"":id;
    }

    public String getReferenceId() {
        String id = getAttribute(WorkAttributeConstant.REFERENCE_ID);
        return (id==null)?"":id;
    }

    public void setAttribute(String name, String value) {
        processVO.setAttribute(name, value);
    }

    public String getDescription() {
        return processVO.getProcessDescription();
    }

    public int getSLA() {
        // not longer used, but needs this for MDW 4 backward compatibility
    	return 0;
    }

    public void setDescription(String value) {
        processVO.setProcessDescription(value);
    }

    public void setName(String value) {
        processVO.setProcessName(value);
    }

    public void setSLA(int value) {
        // not longer used, but needs this for MDW 4 backward compatibility
    }

    public boolean isParticipant() {
        return processVO.getAttribute(LaneVO.PARTICIPANT_TYPE)!=null;
    }

    public Graph getGraph() {
        return graph;
    }

    public boolean isNew() {
        return changes.getChangeType()==Changes.NEW;
    }

    public boolean isDeleted()  {
        return changes.getChangeType()==Changes.DELETE;
    }

    public String getDisplayId(String nodeIdType) {
        if (Node.ID_LOGICAL.equals(nodeIdType))
            return getLogicalId();
        else if (Node.ID_REFERENCE.equals(nodeIdType))
            return getReferenceId();
        else if (Node.ID_SEQUENCE.equals(nodeIdType))
            return String.valueOf(getSequenceId());
        else if (Node.ID_NONE.equals(nodeIdType))
            return "";
        else
            return String.valueOf(getId());
    }

    public int getSequenceId() {
        if (processVO.getSequenceId() == 0)
            graph.assignSequenceIds();
        return processVO.getSequenceId();
    }

    public void setSequenceId(int seqId) {
        processVO.setSequenceId(seqId);
    }
}
