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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;

import com.centurylink.mdw.designer.icons.Icon;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.model.data.common.Changes;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

public abstract class GraphCommon {
    public List<Node> nodes;
    public List<Link> links;
    public int x, y, w, h;
    protected String geo_attribute;
    protected Changes changes;
    protected ProcessVO processVO;

    private IconFactory iconFactory;
    public IconFactory getIconFactory() { return iconFactory; }

    public GraphCommon(IconFactory iconFactory) {
        this.iconFactory = iconFactory;
    }

    public abstract List<AttributeVO> getAttributes();
    public abstract String getAttribute(String name);

    public int numberOfLinks() {
        return links.size();
    }

    public int numberOfNodes() {
        return nodes.size();
    }

    public Node getNode(int i) {
        return nodes.get(i);
    }

    public Node getNode(String logicalId) {
        for (Node node : nodes) {
            if (node.getLogicalId().equals(logicalId)) {
                return node;
            }
        }
        return null;
    }

    public Link getLink(int i) {
        return links.get(i);
    }

    protected Node nodeAt(Graphics g, int x, int y) {
        int i;
        Node node;
        for (i=nodes.size()-1; i>=0; i--) {
            node = (Node)nodes.get(i);
            if (node.onPoint(x, y)) return node;
        }
        for (i=nodes.size()-1; i>=0; i--) {
            node = (Node)nodes.get(i);
            if (node.labelOnPoint(g, x, y)) return node;
        }
        return null;
    }

    protected Link linkAt(Graphics g, int x, int y) {
        int i;
        Link link;
        // look for link itself first, then link label
        for (i=links.size()-1; i>=0; i--) {
            link = links.get(i);
            if (link.isHidden()) continue;
            if (link.onPoint(x, y)) return link;
        }
        for (i=links.size()-1; i>=0; i--) {
            link = links.get(i);
            if (link.isHidden()) continue;
            if (link.labelOnPoint(g, x, y)) return link;
        }
        return null;
    }

    protected TextNote textNoteAt(Graphics g, int x, int y) {
        int i;
        if (! (this instanceof Graph)) return null;
        List<TextNote> notes = ((Graph)this).notes;
        if (notes==null) return null;
        TextNote textNote;
        for (i=notes.size()-1; i>=0; i--) {
        	textNote = notes.get(i);
            if (textNote.onPoint(x, y)) return textNote;
        }
        return null;
    }

    public Node findNode(Long id) {
        for (Node node : nodes) {
            if (node.getId().equals(id))
                return node;
        }
        return null;
    }

    public Link findLink(Long id) {
        for (Link link : this.links) {
            if (link.conn.getWorkTransitionId().equals(id))
                return link;
        }
        return null;
    }

    /**
     * Return the slope (in polar degree)
     * @param x1 x-coord of starting point
     * @param y1 y-coord of starting point
     * @param x2 x-coord of ending point
     * @param y2 y-coord of ending point
     * @return the slope in the range [-pi, pi]
     */
    public static double calcSlope(int x1, int y1, int x2, int y2) {
        double slope;
        if (x1==x2) slope = (y1<y2)? 1.5708 : -1.5708;
        else slope = Math.atan((double)(y2-y1)/(double)(x2-x1));
        if (x1>x2) {
            if (slope>0.0) slope -= 3.1416;
            else slope += 3.1416;
        }
        return slope;
    }

    public void recalcLinkPosition(Node node, String arrowstyle) {
        int n = numberOfLinks();
        Link conn;
        for (int i=0; i<n; i++) {
            conn = getLink(i);
            if (conn.from==node || conn.to==node) {
                if (!conn.isHidden()) conn.recalcLinkPosition(node, arrowstyle);
            }
        }
    }

    abstract public ProcessVO getProcessVO();

    public void resetNodeImageSize(String nodestyle, String arrowstyle) {
        for (Node node : nodes) {
            String iconName = node.getIconName();
            if (iconName.startsWith("shape:")) {
                if (nodestyle.equals(Node.NODE_STYLE_ICON)) {
                    Object icon = iconFactory.getIcon(iconName);
                    if (icon!=null && icon instanceof Icon) {
                        node.w = ((Icon)icon).getIconWidth();
                        node.h = ((Icon)icon).getIconHeight();
                    }
                } else {
                    if (node.w<60) node.w = 60;
                    if (node.h<40) node.h = 40;
                }
            } else {
                int k = iconName.indexOf('@');
                if (k>0) {
                    iconName = iconName.substring(0,k);
                    node.setIconName(iconName);
                }
                if (nodestyle.equals(Node.NODE_STYLE_ICON)) {
                    Object icon = iconFactory.getIcon(iconName);
                    if (icon!=null && icon instanceof ImageIcon) {
                        node.w = ((ImageIcon)icon).getIconWidth();
                        node.h = ((ImageIcon)icon).getIconHeight();
                    }
                } else {
                    if (node.w<100) node.w = 100;
                    if (node.h<60) node.h = 60;
                }
            }
        }
        for (Node node : nodes) {
            recalcLinkPosition(node, arrowstyle);
        }
    }

    String getGeoAttributeName() {
        return this.geo_attribute;
    }

    Rectangle parseGeoInfo(String attrvalue) {
        if (attrvalue==null || attrvalue.length()==0) return null;
        Rectangle rect = new Rectangle();
        String [] tmps = attrvalue.split(",");
        int k;
        String an, av;
        for (int i=0; i<tmps.length; i++) {
            k = tmps[i].indexOf('=');
            if (k<=0) continue;
            an = tmps[i].substring(0,k);
            av = tmps[i].substring(k+1);
            if (an.equals("x")) rect.x = Integer.parseInt(av);
            else if (an.equals("y")) rect.y = Integer.parseInt(av);
            else if (an.equals("w")) rect.width = Integer.parseInt(av);
            else if (an.equals("h")) rect.height = Integer.parseInt(av);
        }
        return rect;
    }

    String formatGeoInfo(int x, int y, int w, int h) {
        StringBuffer sb = new StringBuffer();
        sb.append("x=").append(x);
        sb.append(',').append("y=").append(y);
        sb.append(',').append("w=").append(w);
        sb.append(',').append("h=").append(h);
        return sb.toString();
    }

	public Changes getChanges() {
		return changes;
	}

    public List<Node> getNodes(String sortIdType) {
        Comparator<Node> comparator = null;
        if (Node.ID_REFERENCE.equals(sortIdType)) {
            comparator = new Comparator<Node>() {
                public int compare(Node n1, Node n2) {
                    if (n2.getReferenceId().isEmpty())
                        return -1;
                    else if (n1.getReferenceId().isEmpty())
                        return 1;
                    else {
                        return n1.getReferenceId().compareTo(n2.getReferenceId());
                    }
                }
            };
        }
        else if (Node.ID_DATABASE.equals(sortIdType)) {
            comparator = new Comparator<Node>() {
                public int compare(Node n1, Node n2) {
                    return n1.getId().compareTo(n2.getId());
                }
            };
        }
        else if (Node.ID_SEQUENCE.equals(sortIdType)) {
            comparator = new Comparator<Node>() {
                public int compare(Node n1, Node n2) {
                    return new Integer(n1.getSequenceId()).compareTo(new Integer(n2.getSequenceId()));
                }
            };
        }

        if (comparator == null) {
            return nodes;
        }
        else {
            List<Node> sorted = new ArrayList<Node>(nodes);
            Collections.sort(sorted, comparator);
            return sorted;
        }
    }
}
