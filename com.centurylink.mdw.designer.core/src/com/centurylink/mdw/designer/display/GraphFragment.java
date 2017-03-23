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
package com.centurylink.mdw.designer.display;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.bpm.MDWActivity;
import com.centurylink.mdw.bpm.MDWAttribute;
import com.centurylink.mdw.bpm.MDWProcess;
import com.centurylink.mdw.bpm.MDWProcessDefinition;
import com.centurylink.mdw.bpm.MDWTransition;
import com.centurylink.mdw.bpm.MDWTransitionEvent;
import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;

public class GraphFragment {
	public Long sourceProcessId;
    public List<Node> nodes;
    public List<Link> links;
    public List<SubGraph> subgraphs;

    public GraphFragment(Long sourceProcessId) {
    	this.sourceProcessId = sourceProcessId;
    	nodes = new ArrayList<Node>();
        links = new ArrayList<Link>();
        subgraphs = new ArrayList<SubGraph>();
    }

    public GraphFragment(GraphCommon sourceGraph, Rectangle marquee) {
    	this(sourceGraph.getProcessVO().getProcessId());
    	Graph graph = (sourceGraph instanceof SubGraph)?((SubGraph)sourceGraph).getGraph():(Graph)sourceGraph;
    	if (graph.zoom!=100) {
            marquee.x = marquee.x * 100 / graph.zoom;
            marquee.y = marquee.y * 100 / graph.zoom;
            marquee.width = marquee.width * 100 / graph.zoom;
            marquee.height = marquee.height * 100 / graph.zoom;
        }
        subgraphs = new ArrayList<SubGraph>();
        HashMap<Long,Node> map = new HashMap<Long,Node>();
        for (Node node : sourceGraph.nodes) {
        	if (insideMarquee(node, marquee)) {
        		nodes.add(node);
        		map.put(node.getId(), node);
        	}
        }
        for (Link link : sourceGraph.links) {
        	if (map.get(link.from.getId())!=null
        		&& map.get(link.to.getId())!=null) {
        		links.add(link);
        	}
        }
        if (sourceGraph instanceof Graph) {
        	for (SubGraph subgraph : ((Graph)sourceGraph).subgraphs) {
        		if (insideMarquee(subgraph, marquee)) {
        			subgraphs.add(subgraph);
        		}
        	}
        }
    }

    private boolean insideMarquee(Node node, Rectangle marquee) {
        return (node.x>=marquee.x && node.x+node.w<marquee.x+marquee.width
        		&& node.y>=marquee.y && node.y+node.h<marquee.y+marquee.height);
    }

    private boolean insideMarquee(SubGraph subgraph, Rectangle marquee) {
        return (subgraph.x>=marquee.x && subgraph.x+subgraph.w<marquee.x+marquee.width
        		&& subgraph.y>=marquee.y && subgraph.y+subgraph.h<marquee.y+marquee.height);
    }

    public boolean isEmpty() {
    	return nodes.isEmpty() && subgraphs.isEmpty();
    }

    public boolean contains(Object obj) {
    	return nodes.contains(obj) || links.contains(obj) || subgraphs.contains(obj);
    }

    public boolean isNode() {
    	return nodes.size()==1 && links.size()==0 && subgraphs.size()==0;
    }

    public Node getOnlyNode() {
    	return nodes.get(0);
    }

    public boolean isSubGraph() {
    	return nodes.size()==0 && links.size()==0 && subgraphs.size()==1;
    }

    public SubGraph getOnlySubGraph() {
    	return subgraphs.get(0);
    }

    public Rectangle getBoundary() {
		int x, y, w, h;
		x = y = 3000;
		w = h = 0;
		for (Node node : nodes) {
			if (node.x < x) x = node.x;
			if (node.y < y) y = node.y;
			if (node.x+node.w>x+w) w = node.x+node.w-x;
			if (node.y+node.h>y+h) h = node.y+node.h-y;
		}
		for (SubGraph subgraph : subgraphs) {
			if (subgraph.x < x) x = subgraph.x;
			if (subgraph.y < y) y = subgraph.y;
            if (subgraph.x+subgraph.w>x+w) w = subgraph.x+subgraph.w-x;
            if (subgraph.y+subgraph.h>y+h) h = subgraph.y+subgraph.h-y;
		}
		return new Rectangle(x, y, w, h);
	}

    public void shift(GraphCommon sourceGraph, int dx, int dy, String arrowstyle) {
    	Rectangle bound = getBoundary();
    	if (bound.x + dx < sourceGraph.x) dx = sourceGraph.x - bound.x;
    	else if (bound.x+bound.width+dx>sourceGraph.x+sourceGraph.w)
    		dx = sourceGraph.x+sourceGraph.w-bound.x-bound.width;
    	if (bound.y + dy < sourceGraph.y) dy = sourceGraph.y - bound.y;
    	else if (bound.y+bound.height+dy>sourceGraph.y+sourceGraph.h)
    		dy = sourceGraph.y+sourceGraph.h-bound.y-bound.height;
    	HashMap<Node,Node> nodemap = new HashMap<Node,Node>();
    	HashMap<Link,Link> linkmap = new HashMap<Link,Link>();
    	for (Node node : nodes) {
    		node.x += dx;
        	node.y += dy;
        	nodemap.put(node, node);
    	}
    	for (Link link : links) {
    		link.shift(dx, dy, arrowstyle);
    		linkmap.put(link, link);
    	}
    	for (SubGraph subgraph : subgraphs) {
    		shiftSubgraph(subgraph, dx, dy);
    	}
    	for (Link link : sourceGraph.links) {
    		if (linkmap.containsKey(link)) continue;
    		if (nodemap.containsKey(link.from)) {
    			link.recalcLinkPosition(link.from, arrowstyle);
    		}
    		if (nodemap.containsKey(link.to)) {
    			link.recalcLinkPosition(link.to, arrowstyle);
    		}
    	}
    }

    private void shiftSubgraph(SubGraph subgraph, int dx, int dy) {
        Graph graph = subgraph.getGraph();
        int x = subgraph.x;
        int y = subgraph.y;
        int w = subgraph.w;
        int h = subgraph.h;
        if (x<graph.x) x = graph.x;
        else if (x+w>graph.x+graph.w) x = graph.x+graph.w-w;
        if (y<graph.y) y = graph.y;
        else if (y+h>graph.y+graph.h) y = graph.y+graph.h-h;
        subgraph.move(x, y, graph.arrowstyle);
    }

    public Long getSourceProcessId() {
    	return sourceProcessId;
    }

    private void exportAttribute(AttributeVO attr, MDWAttribute xmlattr) {
    	xmlattr.setName(attr.getAttributeName());
    	xmlattr.setValue(attr.getAttributeValue());
    }

    private void exportActivity(ActivityVO act, MDWActivity xmlact) {
    	xmlact.setId(act.getActivityId().toString());
		xmlact.setName(act.getActivityName());
        xmlact.setImplementation(act.getImplementorClassName());
        for (AttributeVO attr : act.getAttributes()) {
        	exportAttribute(attr, xmlact.addNewAttribute());
        }
    }

    private void exportTransition(WorkTransitionVO trans, MDWTransition xmltrans) {
    	xmltrans.setFrom(trans.getFromWorkId().toString());
		xmltrans.setTo(trans.getToWorkId().toString());
		xmltrans.setCompletionCode(trans.getCompletionCode());
		String eventName = EventType.getEventTypeName(trans.getEventType());
		xmltrans.setEvent(MDWTransitionEvent.Enum.forString(eventName));
		for (AttributeVO attr : trans.getAttributes()) {
        	exportAttribute(attr, xmltrans.addNewAttribute());
        }
    }

    @Override
    public String toString() {
    	ProcessDefinitionDocument defnDoc = ProcessDefinitionDocument.Factory.newInstance();
    	MDWProcessDefinition procDefn = defnDoc.addNewProcessDefinition();
    	MDWProcess process = procDefn.addNewProcess();
    	process.setId(sourceProcessId.toString());
    	for (Node node : nodes) {
    		exportActivity(node.nodet, process.addNewActivity());
    	}
    	for (Link link : links) {
    		exportTransition(link.conn, process.addNewTransition());
    	}
    	for (SubGraph subgraph : subgraphs) {
    		MDWProcess subproc = process.addNewSubProcess();
    		subproc.setId(subgraph.getId().toString());
    		subproc.setName(subgraph.getName());
    		for (Node node : subgraph.nodes) {
        		exportActivity(node.nodet, subproc.addNewActivity());
        	}
        	for (Link link : subgraph.links) {
        		exportTransition(link.conn, subproc.addNewTransition());
        	}
    	}
    	return defnDoc.xmlText(new XmlOptions().setSavePrettyPrint());
    }
}
