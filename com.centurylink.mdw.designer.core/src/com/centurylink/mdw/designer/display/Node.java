/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.model.data.common.Changes;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;

public class Node implements Serializable,Selectable,EditableCanvasText{

    public final static String ATTRIBUTE_NODE_STYLE = "NodeStyle";

    public final static String NODE_STYLE_ICON = "Icon";
    public final static String NODE_STYLE_BOX = "Box";
    public final static String NODE_STYLE_BOX_ICON = "BoxIcon";

    public final static String ID_NONE = "Show no ID";
    public final static String ID_DATABASE = "Database ID";
    public final static String ID_LOGICAL = "Logical ID";
    public final static String ID_SEQUENCE = "Sequence ID";
    public final static String ID_REFERENCE = "Reference ID";

    public static String[] StyleIcons = {"node-icon.gif", "node-box.gif", "node-box-icon.gif"};
    public static String[] Styles = {NODE_STYLE_ICON, NODE_STYLE_BOX, NODE_STYLE_BOX_ICON};

    public static String[] IdChoices = { ID_DATABASE, ID_LOGICAL, ID_NONE };

	public int x, y, w, h;
	public int lx, ly;		// only used as temp variable for now
	public Label label;    // only used as temp variable for now
	private String status;     // runtime status
	private List<ActivityInstanceVO> instances;
    public GraphCommon graph;
    public ActivityImplementorVO nmi;
    protected String iconName;
    public Color color;     // used by activity instance only to show status
    public ActivityVO nodet;
    private Changes changes;

    public Node(ActivityVO nodet, GraphCommon graph, NodeMetaInfo metainfo) {
        this.graph = graph;
        load(nodet, metainfo);
        instances = null;
    	changes = new Changes(nodet.getAttributes());
    }

    public void save_temp_vars() {
        setAttribute(graph.geo_attribute, graph.formatGeoInfo(x,y,w,h));
        changes.toAttributes(nodet.getAttributes());
		if (this.getLogicalId().length()==0) {
			String id;
    		if (graph instanceof SubGraph) {
    			id = ((SubGraph)graph).getGraph().generateLogicalId("A");
    		} else {
    			id = ((Graph)graph).generateLogicalId("A");
    		}
    		nodet.setAttribute(WorkAttributeConstant.LOGICAL_ID, id);
    	}
    }

	public boolean onPoint(int x1, int y1) {
		return (x1>=x && x1<x+w && y1>=y && y1<y+h);
	}

	public boolean labelOnPoint(Graphics g, int x, int y) {
	    String name = getName();
	    if (name==null || name.length()==0) return false;
	    if (label==null) {
	        // not yet initialized - prior to repaint
	        return false;
	    }
	    if (x>=lx && x<=lx+label.width
	            && y>=ly&&y<=ly+label.height) return true;
        return false;
	}

	public String getIconName() {
	    return iconName;
	}

	public void setIconName(String name) {
	    iconName = name;
	}

    public void load(ActivityVO nodet, NodeMetaInfo metainfo) {
        this.nodet = nodet;
        this.nmi = metainfo.find(nodet.getImplementorClassName());
        if (nmi==null) {
            iconName = "shape:activity";
        } else {
            iconName = nmi.getIconName();
        }
        String guiinfo = nodet.getAttribute(graph.geo_attribute);
        Rectangle rect = graph.parseGeoInfo(guiinfo);
        if (rect!=null) {
            this.x = rect.x;
            this.y = rect.y;
            this.w = rect.width;
            this.h = rect.height;
        } else {
            this.x = this.y = 10;
            this.w = this.h = 24;
        }
    }

    public String getAttribute(String attrname) {
        return nodet.getAttribute(attrname);
    }

    public void setAttribute(String attrname, String v) {
        nodet.setAttribute(attrname, v);
    }

    public Long getId() {
        return nodet.getActivityId();
    }

    public String getLogicalId() {
    	String id = nodet.getLogicalId();
    	return id == null ? "" : id;
    }

    public String getReferenceId() {
        String refId = nodet.getReferenceId();
        return refId == null ? "" : refId;
    }

    public int getSequenceId() {
        if (nodet.getSequenceId() == 0 && graph instanceof Graph)
            ((Graph)graph).assignNodeSequenceIds();
        return nodet.getSequenceId();
    }

    public void setSequenceId(int seqId) {
        nodet.setSequenceId(seqId);
    }

    public String getName() {
        return nodet.getActivityName();
    }

    public void setName(String name) {
        nodet.setActivityName(name);
    }

    public String getDescription() {
        return nodet.getActivityDescription();
    }

    public List<AttributeVO> getAttributes() {
        return nodet.getAttributes();
    }

    public Changes getChanges() {
		return changes;
	}

    public boolean isNew() {
        return changes.getChangeType()==Changes.NEW;
    }

    public boolean isDeleted()  {
        return changes.getChangeType()==Changes.DELETE;
    }

    public String getStatus() {
        return status;
    }

    /**
     * when reverse is true, the instance added is older than existing ones.
     * Note the sequence of instances are stored in reverse order.
     */
    public void addInstance(ActivityInstanceVO ai, boolean reverse) {
        if (instances==null) instances = new ArrayList<ActivityInstanceVO>();
        String newStatus = Integer.toString(ai.getStatusCode());
        if (reverse) {
	        instances.add(ai);
	        if (status==null) status = newStatus;
	        else status = status+newStatus;
        } else {
        	instances.add(0, ai);
        	if (status==null) status = newStatus;
	        else status = newStatus+status;
        }
    }

    /**
     * For displaying MDW 3 subprocesses that are converted to activities during
     * display.
     *
     * @param ai
     */
    public void addStatus(ActivityInstanceVO ai) {
        String newStatus = Integer.toString(ai.getStatusCode());
        if (status==null) status = newStatus;
        else status = status+newStatus;
    }

    /**
     * For immediate-display when running process in designer
     */
    public void setInstanceStatus(ActivityInstanceVO ai) {
        if (instances==null) return;
        int n = instances.size();
        for (int i=0; i<n; i++) {
        	ActivityInstanceVO ai1 = instances.get(i);
        	if (ai1.getId().equals(ai.getId())) {
                String newStatus = Integer.toString(ai.getStatusCode());
                if (i==0) status = newStatus + status.substring(1);
                else if (i==n-1) status = status.substring(0,i) + newStatus;
                else status = status.substring(0,i) + newStatus + status.substring(i+1);
                break;
        	}
        }
    }

    public List<ActivityInstanceVO> getInstances() {
        return instances;
    }

    public boolean isSubProcessActivity() {
        return nmi != null && nmi.isSubProcessInvoke();
    }

    public boolean isTaskActivity() {
        return nmi != null && nmi.isManualTask();
    }

    public boolean isStartActivity() {
        return nmi != null && nmi.isStart();
    }

    public boolean isSynchronizationActivity() {
        return nmi != null && nmi.isSync();
    }

    public boolean isAdapterActivity() {
        return nmi != null && nmi.isAdapter();
    }

    public boolean isEventWaitActivity() {
        return nmi != null && nmi.isEventWait();
    }

    public boolean isGeneralTaskActivity() {
    	return isTaskActivity() && nodet.getAttribute(TaskActivity.ATTRIBUTE_FORM_NAME)!=null;
    }

	// do not delete - need this for $+SLA in implementor spec
    public int getSLA() {
        return nodet.getSla();
    }

    public void setDescription(String value) {
       nodet.setActivityDescription(value);

    }

	// do not delete - need this for $+SLA in implementor spec
    public void setSLA(int value) {
        nodet.setSla(value);
    }

    public Long getActivityId() {
         return nodet.getActivityId();
    }

    public String getBaseClassName() {
        if (nmi!=null) return nmi.getBaseClassName();
        else return GeneralActivity.class.getName();
    }

    public Graph getMainGraph() {
    	if (graph instanceof Graph) return (Graph)graph;
    	else return ((SubGraph)graph).getGraph();
    }

	@Override
	public void setText(String text) {
		this.setName(text);
	}

	public String getDisplayId(String nodeIdType) {
	    if (ID_LOGICAL.equals(nodeIdType))
	        return getLogicalId();
	    else if (ID_REFERENCE.equals(nodeIdType))
	        return getReferenceId();
        else if (ID_SEQUENCE.equals(nodeIdType))
            return String.valueOf(getSequenceId());
	    else if (ID_NONE.equals(nodeIdType))
	        return "";
	    else
	        return String.valueOf(getId());
	}

}
