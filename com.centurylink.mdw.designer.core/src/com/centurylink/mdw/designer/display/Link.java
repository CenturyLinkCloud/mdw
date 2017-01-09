/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.constant.WorkTransitionAttributeConstant;
import com.centurylink.mdw.model.data.common.Changes;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.data.work.WorkTransitionStatus;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;

/**
 *
 */
public class Link implements EditableCanvasText {

	public final static String STRAIGHT = "Straight";
	public final static String ELBOW = "Elbow";
	public final static String CURVE = "Curve";
	public final static String ELBOWH = "ElbowH";
	public final static String ELBOWV = "ElbowV";
	
    public static String[] styleIcons = {"elbow1.gif", "curved1.gif", "straight.gif", "elbow3.gif", "elbow2.gif"};
    public static String[] styles = {ELBOW, CURVE, STRAIGHT, ELBOWH, ELBOWV};
    
    public final static String ATTRIBUTE_ARROW_STYLE = "ArrowStyle";
    
    public final static String ARROW_STYLE_END = "ArrowEnd";
    public final static String ARROW_STYLE_MIDDLE = "ArrowMiddle";

    public static String[] ArrowStyleIcons = {"arrow-end.gif", "arrow-middle.gif"};
    public static String[] ArrowStyles = {ARROW_STYLE_END, ARROW_STYLE_MIDDLE};
    
	public final static int AUTOLINK_H = 1;
    public final static int AUTOLINK_V = 2;
    public final static int AUTOLINK_HV = 3;
    public final static int AUTOLINK_VH = 4;
    public final static int AUTOLINK_HVH = 5;
    public final static int AUTOLINK_VHV = 6;
    
//    public static boolean USE_SHAPE = true;
    
    public final static int gap = 4;
    private final static int cr = 8;
    private final static double elbow_threshold = 0.8; 
    private final static int elbowVH_threshold = 60; 
    
//	public final static Color COLOR_NORMAL = Color.BLUE;
    public final static Color COLOR_NORMAL = Color.GRAY;
    public final static Color COLOR_OTHER = Color.ORANGE;

	public WorkTransitionVO conn;
	public Node from, to;
	private int xs[];
	private int ys[];
	private String type;
	public int lx, ly;
	public Label label;
    public Color color;
    private Shape shape, arrowshape;
    private Changes changes;
    private List<WorkTransitionInstanceVO> instances;
    
    private static Map<Integer,String> eventInteger2Name = null;
    private static Map<String,Integer> eventName2Integer = null;

	Link(Node from, Node to, WorkTransitionVO conn, String arrowstyle) {
	    this.from = from;
	    this.to = to;
	    this.conn = conn;
	    type = STRAIGHT;
	    loadEventTypesMap();
	    parseAttribute(getGeoAttributeName(from.graph.geo_attribute));
	    if (xs==null) calcLinkPosition(0, arrowstyle);
	    determineShape();
	    determineArrow(arrowstyle);
	    instances = null;
    	changes = new Changes(conn.getAttributes());
	}
	
	public void shift(int dx, int dy, String arrowstyle) {
	    if (xs!=null) {
	    	for (int i=0; i<xs.length; i++) {
	    		xs[i] += dx;
	    		ys[i] += dy;
	    	}
	        determineShape();
	        determineArrow(arrowstyle);
	    }
	    lx += dx;
	    ly += dy;
	}
	
	private String getGeoAttributeName(String node_geo_attr_name) {
	    if (WorkAttributeConstant.SWIMLANE_GEO_INFO.equals(node_geo_attr_name))
	        return node_geo_attr_name;
	    else return WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO;
	}
	
	public boolean isElbowType() {
	    return type.startsWith("Elbow");
	}
	
	private void loadEventTypesMap() {
	    if (eventInteger2Name==null) {
	        Integer[] eventTypes = EventType.allEventTypes;
	        String[] eventTypeNames = EventType.allEventTypeNames;
	        eventInteger2Name = new HashMap<Integer,String>();
	        eventName2Integer = new HashMap<String,Integer>();
	        for (int i=0; i<eventTypes.length; i++) {
	            eventInteger2Name.put(eventTypes[i],eventTypeNames[i]);
	            eventName2Integer.put(eventTypeNames[i], eventTypes[i]);
	        }
	    }
	}
	
	private void setStatus(int statusCode) {
	    if (statusCode == WorkTransitionStatus.STATUS_INITIATED)
            setColor(Color.BLUE);
        else setColor(Color.DARK_GRAY);     // STATUS_COMPLETED, or multiple transitions
	}
	
	public void addInstance(WorkTransitionInstanceVO ti) {
	    if (instances==null) instances = new ArrayList<WorkTransitionInstanceVO>();
	    instances.add(ti);
	    ti = instances.get(0);
	    setStatus(ti.getStatusCode());
	}
	
	public List<WorkTransitionInstanceVO> getInstances() {
	    return instances;
	}
	
	private void parseAttribute(String geo_attribute) {
	    String dispinfo = conn.getAttribute(geo_attribute);
	    color = getColorForType();
	    if (dispinfo!=null && dispinfo.length()>0) {
	        String attrs[] = dispinfo.split(",");
            int i, k;
            String an, av;
            for (i=0; i<attrs.length; i++) {
                k = attrs[i].indexOf('=');
                if (k<=0) continue;
                an = attrs[i].substring(0,k);
                av = attrs[i].substring(k+1);
                if (an.equals("lx")) {
                    lx = Integer.parseInt(av);
                } else if (an.equals("ly")) {
                    ly = Integer.parseInt(av);
                } else if (an.equals("type")) {
                    if (av.equalsIgnoreCase(ELBOW)) type = ELBOW;
                    else if (av.equalsIgnoreCase(CURVE)) type = CURVE;
                    else if (av.equalsIgnoreCase(ELBOWH)) type = ELBOWH;
                    else if (av.equalsIgnoreCase(ELBOWV)) type = ELBOWV;
                    else type = STRAIGHT;
                } else if (an.equals("xs")) {
                    if (av!=null && av.length()>0) {
                        String sts[] = av.split("&");
                        xs = new int[sts.length];
                        for (int j=0; j<xs.length; j++) {
                            xs[j] = Integer.parseInt(sts[j]);
                        }
                    }
                } else if (an.equals("ys")) {
                    if (av!=null && av.length()>0) {
                        String sts[] = av.split("&");
                        ys = new int[sts.length];
                        for (int j=0; j<ys.length; j++) {
                            ys[j] = Integer.parseInt(sts[j]);
                        }
                    }
                }
            }
        }
	}

	public void save_temp_vars(String geo_attribute) {
        StringBuffer sb = new StringBuffer();
        sb.append("lx=").append(lx);
        sb.append(',').append("ly=").append(ly);
        sb.append(',').append("type=").append(type);
        if (xs!=null) {
            sb.append(',').append("xs=");
            for (int j=0; j<xs.length; j++) {
                if (j>0) sb.append('&');
                sb.append(xs[j]);
            }
            sb.append(',').append("ys=");
            for (int j=0; j<ys.length; j++) {
                if (j>0) sb.append('&');
                sb.append(ys[j]);
            }
        }
        conn.setAttribute(getGeoAttributeName(geo_attribute), sb.toString());
		conn.setFromWorkId(from.getId());
		conn.setToWorkId(to.getId());
		String comp = conn.getCompletionCode();
		if (comp!=null) conn.setCompletionCode(comp.trim());
		changes.toAttributes(conn.getAttributes());
		if (this.getLogicalId().length()==0) {
			String id;
			if (from.graph instanceof SubGraph) {
    			id = ((SubGraph)from.graph).getGraph().generateLogicalId("T");
    		} else {
    			id = ((Graph)from.graph).generateLogicalId("T");
    		}
    		conn.setAttribute(WorkAttributeConstant.LOGICAL_ID, id);
        }
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCondition() {
		return conn.getValidatorClassName();
	}

	public String getLabel() {
		return conn.getCompletionCode();
	}

	private Color getColorForType() {
		Integer eventType = conn.getEventType();
        if (EventType.FINISH.equals(eventType))
        	return COLOR_NORMAL;
        else return COLOR_OTHER;
	}

	public void setEventType(String eventType) {
		conn.setEventType(eventName2Integer.get(eventType.trim()));
		color = getColorForType();
	}

    public String getLabelAndEventType(/*boolean simplified*/){
        String label = conn.getCompletionCode();
        if (EventType.FINISH.equals(conn.getEventType()))  {
        	 return label;
        } else {
            String eventType = eventInteger2Name.get(conn.getEventType());
            if(label==null || label.length()==0)
        		 return eventType;
             else if (label.startsWith(":"))
            	 return eventType + ":" + label.substring(1);
             else return eventType + ":" + label;
        }
     }

	public void setFrom(Node node) {
	    this.from = node;
	}

	public void setTo(Node node) {
	    this.to = node;
	}

	public void setLabel(String value) {
		conn.setCompletionCode(value);
	}

	public void setLabelAndEventType(String value) {
		Integer eventTypeCode;
		int k = value.indexOf(':');
		String label;
		if (k>0) {
		    eventTypeCode = eventName2Integer.get(value.substring(0,k));
			if (eventTypeCode!=null) {
				if (value.length()==k+1) label = ":";
				else label = value.substring(k+1);
			} else label = value;
		} else {
            eventTypeCode = eventName2Integer.get(value);
			if (eventTypeCode!=null) label = "";
			else label = value;
		}
		if (eventTypeCode!=null) conn.setEventType(eventTypeCode);
		else conn.setEventType(EventType.FINISH);
		color = getColorForType();
		setLabel(label);
	}

	public void setCondition(String value) {
		conn.setValidatorClassName(value);
	}

	public boolean onPoint(int x, int y) {
		int i;
		if (type.equals(CURVE)) {
		    Shape s = getShape();
		    // this is not accurate -- it checks intersection
		    // of all interior area, not just the curve.
		    return s.intersects(x-2,y-2,4,4);
		} else if (isElbowType()&&xs.length==2) {
		    Shape shape = getShape();
		    return shape.intersects(x-1,y-1,3,3);
//		    return shape.contains(x, y);
		} else {
//		    return getShape().intersects(x-1,y-1,3,3);
		    // using shape.intersects is not accurate - it counts all interior area,
		    // not just on the line
		    for (i=0; i<xs.length-1; i++) {
				if (onLine(xs[i], ys[i], xs[i+1], ys[i+1], x, y)) return true;
			}
	        return false;
		}
	}
	
	private int getAutoElbowLinkType() {
	    if (type.equals(ELBOWH)) {
	        if (xs[0]==xs[1]) {
                return AUTOLINK_V; 
            } else if (ys[0]==ys[1]) {
                return AUTOLINK_H;
            } else if (Math.abs(to.x-from.x)>elbowVH_threshold) {
                return AUTOLINK_HVH;
            } else {
                return AUTOLINK_HV;
            }
	    } else if (type.equals(ELBOWV)) {
	        if (xs[0]==xs[1]) {
                return AUTOLINK_V; 
            } else if (ys[0]==ys[1]) {
                return AUTOLINK_H;
            } else if (Math.abs(to.y-from.y)>elbowVH_threshold) {
                return AUTOLINK_VHV;
            } else {
                return AUTOLINK_VH;
            }
	    } else {
        	if (xs[0]==xs[1]) {
                return AUTOLINK_V; 
            } else if (ys[0]==ys[1]) {
                return AUTOLINK_H;
            } else if (Math.abs(to.x-from.x) < Math.abs(to.y-from.y)*elbow_threshold) {
                return AUTOLINK_VHV;
            } else if (Math.abs(to.y-from.y) < Math.abs(to.x-from.x)*elbow_threshold) {
                return AUTOLINK_HVH;
            } else {
                return AUTOLINK_HV;
            }
	    }
	}

	public boolean labelOnPoint(Graphics g, int x, int y) {
		String value = getLabelAndEventType();
	    if (value==null || value.length()==0) return false;
	    if (label==null) return false;
        if (x>=lx && x<=lx+label.width 
                && y>=ly&&y<=ly+label.height) return true;
        return false;
	}

	private boolean onLine(int x1, int y1, int x2, int y2, int x0, int y0) {
		int d = 3;
		if (Math.abs(x1-x2)<d) {
			if (Math.abs(x0-x1)>d) return false;
			if (y1<y2) {
				if (y0<y1 || y0>y2) return false;
			} else if (y1>y2) {
				if (y0<y2|| y0>y1) return false;
			} else {  // y1==y2
				if (Math.abs(x0-y1)>d) return false;
			}
		} else if (Math.abs(y1-y2)<d) {
			if (Math.abs(y0-y1)>d) return false;
			if (x1<x2) {
				if (x0<x1 || x0>x2) return false;
			} else if (x1>x2) {
				if (x0<x2|| x0>x1) return false;
			}
		} else {
			if (x1<x2) {
				if (x0<x1 || x0>x2) return false;
			} else {
				if (x0<x2 || x0>x1) return false;
			}
			int y3 = (y2-y1)*(x0-x1)/(x2-x1) + y1;
			if (Math.abs(y0-y3)>d) return false;
		}
		return true;
	}

	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color =color;
	}

    public boolean isNew(){
        return changes.getChangeType()==Changes.NEW;
    }
    
    public boolean isDeleted(){
        return changes.getChangeType()==Changes.DELETE;
    }
	
	public Changes getChanges() {
		return changes;
	}
    
    public boolean isHidden() {
        // this is now always false, as server side removes the hidden links
        // keep it here for a while until we are sure we do not have to
        // handle this on the client side
        if (conn.getEventType().equals(EventType.START)) return true;
        if (conn.getEventType().equals(EventType.ERROR)&&this.to==null) return true;
        return false;
    }
    
    private boolean anchorHorizontalThenVertical(int anchor) {
        int p = anchor-1;
        int n = anchor+1;
        boolean horizontalThenVertical;
        if (p>=0 && xs[p]!=xs[anchor] && ys[p]==ys[anchor]) {
            horizontalThenVertical = true;
        } else if (n<xs.length && xs[n]==xs[anchor] && ys[n]!=ys[anchor]) {
            horizontalThenVertical = true;
        } else {
            horizontalThenVertical = false;
        }
        return horizontalThenVertical;
    }
    
    public Shape getShape() {
//        return determineShape();
        return shape;
    }

    private void determineShape() {
        if (isElbowType()) {
            if (xs.length==2) determineShapeAutoElbow();
            else {
                GeneralPath path = new GeneralPath();
                boolean horizontal = ys[0]==ys[1] && (xs[0]!=xs[1] || xs[1]==xs[2]);
                path.moveTo(xs[0], ys[0]);
                for (int i=1; i<xs.length; i++) {
                    if (horizontal) {
                        path.lineTo(xs[i]>xs[i-1]?xs[i]-cr:xs[i]+cr, ys[i]);
                        if (i<xs.length-1) path.quadTo(xs[i], ys[i], xs[i], ys[i+1]>ys[i]?ys[i]+cr:ys[i]-cr);
                    } else {
                        path.lineTo(xs[i], ys[i]>ys[i-1]?ys[i]-cr:ys[i]+cr);
                        if (i<xs.length-1) path.quadTo(xs[i], ys[i], xs[i+1]>xs[i]?xs[i]+cr:xs[i]-cr, ys[i]);
                    }
                    horizontal = !horizontal;
                }
                shape = path;
            }
        } else if (type.equals(CURVE)) {
            if (xs.length==4) {
                int x1 = (int)Math.round(-5.0/6.0*xs[0]+3.0*xs[1]-1.5*xs[2]+1.0/3.0*xs[3]);
                int y1 = (int)Math.round(-5.0/6.0*ys[0]+3.0*ys[1]-1.5*ys[2]+1.0/3.0*ys[3]);
                int x2 = (int)Math.round(-5.0/6.0*xs[3]+3.0*xs[2]-1.5*xs[1]+1.0/3.0*xs[0]);
                int y2 = (int)Math.round(-5.0/6.0*ys[3]+3.0*ys[2]-1.5*ys[1]+1.0/3.0*ys[0]);
                /* obtained by reversing Bezier cubic formula below
                 * xs[1] = 8/27*xs[0] + 12/27*x1 +  6/27*x2 + 1/27*xs[3]
                 * xs[2] = 1/27*xs[0] +  6/27*x1 + 12/27*x2 + 8/27*xs[3]
                 */
                shape = new CubicCurve2D.Float(xs[0],ys[0],x1,y1,x2,y2,xs[3],ys[3]);
            } else if (xs.length==3) {
                int x1 = (int)Math.round(-0.5*xs[0]+2*xs[1]-0.5*xs[2]);
                int y1 = (int)Math.round(-0.5*ys[0]+2*ys[1]-0.5*ys[2]);
                shape = new QuadCurve2D.Float(xs[0],ys[0],x1,y1,xs[2],ys[2]);
            } else if (from==to) {  // length==2
                double d =  Math.sqrt((ys[1]-ys[0])*(ys[1]-ys[0])
                        + (xs[1]-xs[0])*(xs[1]-xs[0]));
                double alpha = Math.acos((xs[1]-xs[0])/d);
                if (ys[1]>ys[0]) alpha = -alpha;
                double rectX = xs[0]- d/2 * (1.0-Math.cos(alpha));
                double rectY = ys[0]- d/2 * (1.0+Math.sin(alpha));
                double startAngle = Math.toDegrees(alpha) - 135;
                shape = new Arc2D.Double(rectX, rectY, d, d, startAngle, 270.0, Arc2D.OPEN);
            } else {
                Point m = getCurveMidPoint();
                int x1 = (int)Math.round(-0.5*xs[0]+2*m.x-0.5*xs[1]);
                int y1 = (int)Math.round(-0.5*ys[0]+2*m.y-0.5*ys[1]);
                shape = new QuadCurve2D.Float(xs[0],ys[0],x1,y1,xs[1],ys[1]);
            }
        } else {
            GeneralPath path = new GeneralPath();
            path.moveTo(xs[0], ys[0]);
            for (int i=1; i<xs.length; i++) {
                path.lineTo(xs[i], ys[i]);
            }
            shape = path;
        }
    }
    
    private void determineShapeAutoElbow() {
        GeneralPath path;
        int t;
        switch (getAutoElbowLinkType()) {
        case AUTOLINK_V:
        case AUTOLINK_H:
//            shape = new Line2D.Double(xs[0], ys[0], xs[1], ys[1]);
            path = new GeneralPath();
            path.moveTo(xs[0], ys[0]);
            path.lineTo(xs[1], ys[1]);
            break;
        case AUTOLINK_VHV:
            path = new GeneralPath();
            t = (ys[0]+ys[1])/2;
            path.moveTo(xs[0], ys[0]);
            path.lineTo(xs[0], t>ys[0]?t-cr:t+cr);
            path.quadTo(xs[0], t, xs[1]>xs[0]?xs[0]+cr:xs[0]-cr, t);
            path.lineTo(xs[1]>xs[0]?xs[1]-cr:xs[1]+cr, t);
            path.quadTo(xs[1], t, xs[1], ys[1]>t?t+cr:t-cr);
            path.lineTo(xs[1], ys[1]);
            break;
        case AUTOLINK_HVH:
            path = new GeneralPath();
            t = (xs[0]+xs[1])/2;
            path.moveTo(xs[0], ys[0]);
            path.lineTo(t>xs[0]?t-cr:t+cr, ys[0]);
            path.quadTo(t, ys[0], t, ys[1]>ys[0]?ys[0]+cr:ys[0]-cr);
            path.lineTo(t, ys[1]>ys[0]?ys[1]-cr:ys[1]+cr);
            path.quadTo(t, ys[1], xs[1]>t?t+cr:t-cr, ys[1]);
            path.lineTo(xs[1], ys[1]);
            break;
        case AUTOLINK_HV:
            path = new GeneralPath();
            path.moveTo(xs[0], ys[0]);
            path.lineTo(xs[1]>xs[0]?xs[1]-cr:xs[1]+cr, ys[0]);
            path.quadTo(xs[1], ys[0], xs[1], ys[1]>ys[0]?ys[0]+cr:ys[0]-cr);
            path.lineTo(xs[1], ys[1]);
            break;
        case AUTOLINK_VH:
            path = new GeneralPath();
            path.moveTo(xs[0], ys[0]);
            path.lineTo(xs[0], ys[1]>ys[0]?ys[1]-cr:ys[1]+cr);
            path.quadTo(xs[0], ys[1], xs[1]>xs[0]?xs[0]+cr:xs[0]-cr, ys[1]);
            path.lineTo(xs[1], ys[1]);
            break;
        default: path = null;
        }
        shape = path;
    }
    
    private Point getCurveMidPoint() {
        int k = xs.length-1;
        double d =  Math.sqrt((ys[k]-ys[0])*(ys[k]-ys[0]) + (xs[k]-xs[0])*(xs[k]-xs[0]));
        double alpha = Graph.calcSlope(xs[0],ys[0],xs[k],ys[k]) - 0.2618;   // pi/12
        return new Point((int)(xs[0]+d*0.545*Math.cos(alpha)),
                (int)(ys[0]+d*0.545*Math.sin(alpha)));
    }
    
    public int getNumberOfControlPoints() {
        return xs==null?0:xs.length;
    }
    
    private void calcAutoElbowLinkEndPoints() {

        if (to.x+to.w>=from.x&&to.x<=from.x+from.w) {
            // V
            xs[0] = xs[1] = (Math.max(from.x,to.x)
                +Math.min(from.x+from.w,to.x+to.w))/2;
            if (to.y>from.y) {
                ys[0] = from.y+from.h+gap;
                ys[1] = to.y-gap;
            } else {
                ys[0] = from.y-gap;
                ys[1] = to.y+to.h+gap;
            }
        } else if (to.y+to.h>=from.y&&to.y<=from.y+from.h) {
            // H
            ys[0] = ys[1] = (Math.max(from.y,to.y)
                    +Math.min(from.y+from.h,to.y+to.h))/2;
            if (to.x>from.x) {
                xs[0] = from.x+from.w+gap;
                xs[1] = to.x-gap;
            } else {
                xs[0] = from.x-gap;
                xs[1] = to.x+to.w+gap;
            }
        } else if (type.equals(ELBOW) &&  
                Math.abs(to.x-from.x)<Math.abs(to.y-from.y)*elbow_threshold
                || type.equals(ELBOWV) && Math.abs(to.y-from.y)>elbowVH_threshold) {
            // VHV
            xs[0] = from.x + from.w/2;
            xs[1] = to.x + to.w/2;
            if (to.y>from.y) {
                ys[0] = from.y+from.h+gap;
                ys[1] = to.y-gap;
            } else {
                ys[0] = from.y-gap;
                ys[1] = to.y+to.h+gap;
            }
        } else if (type.equals(ELBOW) && 
                Math.abs(to.y-from.y)<Math.abs(to.x-from.x)*elbow_threshold
                || type.equals(ELBOWH) && Math.abs(to.x-from.x)>elbowVH_threshold) {
            // HVH
            ys[0] = from.y + from.h/2;
            ys[1] = to.y + to.h/2;
            if (to.x>from.x) {
                xs[0] = from.x+from.w+gap;
                xs[1] = to.x-gap;
            } else {
                xs[0] = from.x-gap;
                xs[1] = to.x+to.w+gap;
            }
        } else if (type.equals(ELBOWV)) {
            // VH
            if (to.y>from.y) ys[0] = from.y+from.h+gap;
            else ys[0] = from.y-gap;
            xs[0] = from.x + from.w/2;
            ys[1] = to.y + to.h/2;
            if (to.x>from.x) xs[1] = to.x-gap;
            else xs[1] = to.x + to.w + gap;
        } else {
            // HV
            if (to.x>from.x) xs[0] = from.x+from.w+gap;
            else xs[0] = from.x-gap;
            ys[0] = from.y + from.h/2;
            xs[1] = to.x + to.w/2;
            if (to.y>from.y) ys[1] = to.y-gap;
            else ys[1] = to.y + to.h + gap;
        }
    }
    
    public void calcLinkPosition(int n, String arrowstyle) {
        int x1 = from.x;
        int y1 = from.y;
        int w1 = from.w;
        int h1 = from.h;
        int x2 = to.x;
        int y2 = to.y;
        int w2 = to.w;
        int h2 = to.h;
        if (n<2) {
            if (type.equals(CURVE) && from!=to) n = 4;
            else n = 2;
        }
        if (type.equals(CURVE)) {
            if (n==4) {
                xs = new int[4];
                ys = new int[4];
                boolean horizontal = false;
                if (Math.abs(x1-x2)>=Math.abs(y1-y2)) {
                    horizontal = true;
                    xs[0] = (x1<=x2)?(x1+w1):x1;
                    ys[0] = y1+h1/2;
                    xs[3] = (x1<=x2)?x2:(x2+w2);
                    ys[3] = y2+h2/2;
                } else {
                    xs[0] = x1+w1/2;
                    ys[0] = (y1<=y2)?(y1+h1):y1;
                    xs[3] = x2+w2/2;
                    ys[3] = (y1<=y2)?y2:(y2+h2);
                }
                if (horizontal) {
                    xs[1] = (2*xs[0]+xs[3])/3;
                    ys[1] = ys[0];
                    xs[2] = (xs[0]+2*xs[3])/3;
                    ys[2] = ys[3];
                } else {    // link vertically
                    xs[1] = xs[0];
                    ys[1] = (2*ys[0]+ys[3])/3;
                    xs[2] = xs[3];
                    ys[2] = (ys[0]+2*ys[3])/3;
                }
            } else if (n==3) {
                xs = new int[3];
                ys = new int[3];
                if (Math.abs(x1-x2)>=Math.abs(y1-y2)) {
                    // horizontal-ish
                    xs[0] = (x1<=x2)?(x1+w1):x1;
                    ys[0] = y1 + h1/2;
                    xs[2] = (x2<=x1)?(x2+w2):x2;
                    ys[2] = y2 + h2/2;
                } else {
                    // vertical-ish
                    xs[0] = x1+w1/2;
                    ys[0] = (y1<=y2)?(y1+h1):y1;
                    xs[2] = x2 + w2/2;
                    ys[2] = (y2<=y1)?(y2+h2):y2;
                }
            } else if (from==to) {    // n==2 and a link to the same node
                xs = new int[2];
                ys = new int[2];
                xs[0] = x1 + w1/2;
                ys[0] = y1;
                xs[1] = x1 + w1/2;
                ys[1] = y1 - 20;
            } else {    // n==2
                xs = new int[2];
                ys = new int[2];
                if (Math.abs(x1-x2)>=Math.abs(y1-y2)) {
                    // horizontal-ish
                    xs[0] = (x1<=x2)?(x1+w1):x1;
                    ys[0] = y1 + h1/2;
                    xs[1] = (x2<=x1)?(x2+w2):x2;
                    ys[1] = y2 + h2/2;
                } else {
                    // vertical-ish
                    xs[0] = x1+w1/2;
                    ys[0] = (y1<=y2)?(y1+h1):y1;
                    xs[1] = x2 + w2/2;
                    ys[1] = (y2<=y1)?(y2+h2):y2;
                }
            }
        } else if (type.equals(STRAIGHT)) {
            xs = new int[n];
            ys = new int[n];
            if (Math.abs(x1-x2)>=Math.abs(y1-y2)) {
                // more of a horizontal link
                xs[0] = (x1<=x2)?(x1+w1):x1;
                ys[0] = y1+h1/2;
                xs[n-1] = (x1<=x2)?x2:(x2+w2);
                ys[n-1] = y2+h2/2;
                for (int i=1; i<n-1; i++) {
                    if (i%2!=0) {
                        ys[i] = ys[i-1];
                        xs[i] = (xs[n-1]-xs[0])*((i+1)/2)/(n/2) + xs[0];
                    } else {
                        xs[i] = xs[i-1];
                        ys[i] = (ys[n-1]-ys[0])*((i+1)/2)/((n-1)/2) + ys[0];
                    }
                }
            } else {    // more of a vertical link
                xs[0] = x1+w1/2;
                ys[0] = (y1<=y2)?(y1+h1):y1;
                xs[n-1] = x2+w2/2;
                ys[n-1] = (y1<=y2)?y2:(y2+h2);
                for (int i=1; i<n-1; i++) {
                    if (i%2!=0) {
                        xs[i] = xs[i-1];
                        ys[i] = (ys[n-1]-ys[0])*((i+1)/2)/(n/2) + ys[0];
                    } else {
                        ys[i] = ys[i-1];
                        xs[i] = (xs[n-1]-xs[0])*(i/2)/((n-1)/2) + xs[0];
                    }
                }
            }
        } else if (n==2) {  // auto ELBOW, ELBOWH, ELBOWV 
        	xs = new int[2];
            ys = new int[2];
            this.calcAutoElbowLinkEndPoints();
        } else {    // ELBOW, ELBOWH, ELBOWV with middle control points
            boolean horizontalFirst = type.equals(ELBOWH)
                    || type.equals(ELBOW) && Math.abs(x1-x2)>=Math.abs(y1-y2);
            boolean evenN = n%2==0;
            boolean horizontalLast = horizontalFirst && evenN || !horizontalFirst && !evenN;
            xs = new int[n];
            ys = new int[n];
            if (horizontalFirst) {
                xs[0] = (x1<=x2)?(x1+w1):x1;
                ys[0] = y1 + h1/2;
            } else {
                xs[0] = x1+w1/2;
                ys[0] = (y1<=y2)?(y1+h1):y1;
            }
            if (horizontalLast) {
                xs[n-1] = (x2<=x1)?(x2+w2):x2;
                ys[n-1] = y2+h2/2;
            } else {
                xs[n-1] = x2 + w2/2;
                ys[n-1] = (y2<=y1)?(y2+h2):y2;
            }
            if (horizontalFirst) {
                for (int i=1; i<n-1; i++) {
                    if (i%2!=0) {
                        ys[i] = ys[i-1];
                        xs[i] = (xs[n-1]-xs[0])*((i+1)/2)/(n/2) + xs[0];
                    } else {
                        xs[i] = xs[i-1];
                        ys[i] = (ys[n-1]-ys[0])*((i+1)/2)/((n-1)/2) + ys[0];
                    }
                }
            } else {
                for (int i=1; i<n-1; i++) {
                    if (i%2!=0) {
                        xs[i] = xs[i-1];
                        ys[i] = (ys[n-1]-ys[0])*((i+1)/2)/(n/2) + ys[0];
                    } else {
                        ys[i] = ys[i-1];
                        xs[i] = (xs[n-1]-xs[0])*(i/2)/((n-1)/2) + xs[0];
                    }
                }
            }
        }
        calcLinkLabelPosition();
        determineShape();
        determineArrow(arrowstyle);
    }
    
    public void calcLinkLabelPosition() {
        int x1 = from.x;
        int y1 = from.y;
        int x2 = to.x;
        int n = xs.length;
        if (type.equals(CURVE)) {
            if (n==4) {
                lx = (xs[0]+xs[3])/2;
                ly = (ys[0]+ys[3])/2;
            } else if (n==3) {
                Point m = getCurveMidPoint();
                lx = xs[1] = m.x;
                ly = ys[1] = m.y;
            } else if (from==to) {    // n==2 and a link to the same node
                lx = x1;
                ly = y1 - 24;
            } else {    // n==2
                lx = (xs[0]+xs[1])/2 + (xs[1]-xs[0])/4;
                ly = (ys[0]+ys[1])/2 - (ys[1]-ys[0])/4;
            }
        } else if (type.equals(STRAIGHT)) {
            lx = (xs[0]+xs[n-1])/2;
            ly = (ys[0]+ys[n-1])/2;
        } else if (n==2) {  // auto ELBOW, ELBOWH, ELBOWV 
            lx = (xs[0]+xs[n-1])/2;
            ly = (ys[0]+ys[n-1])/2;
        } else {    // ELBOW, ELBOWH, ELBOWV with middle control points
            boolean horizontalFirst = ys[0]==ys[1];
            if (n<=3) {
                if (horizontalFirst) {
                    lx = (x1+x2)/2-40;
                    ly = ys[0]-4;
                } else {
                    lx = xs[0]+2;
                    ly = (ys[0]+ys[1])/2;
                }
            } else /* if (!evenN) */ {
                if (horizontalFirst) {
                    lx = (x1<=x2)?(xs[(n-1)/2]+2):(xs[(n-1)/2+1]+2);
                    ly = ys[n/2]-4;
                } else {
                    lx = (x1<=x2)?xs[n/2-1]:xs[n/2];
                    ly = ys[n/2-1]-4;
                }
            }
        }
    }
    
    /**
     * recalculate link position after moving a node
     * @param node the node that is moved
     */
    public void recalcLinkPosition(Node node, String arrowstyle) {
        int n = xs.length;
        // remember relative label position
        double label_slope = 
            Graph.calcSlope(xs[0], ys[0], lx, ly) -
            Graph.calcSlope(xs[0], ys[0], xs[n-1], ys[n-1]);
        double label_dist = calcDistance(xs[0], ys[0], xs[n-1], ys[n-1]);
        if (label_dist<5.0) label_dist = 0.5;
        else label_dist = calcDistance(xs[0], ys[0], lx, ly) / label_dist;
        if (type.equals(CURVE) && from==to) {
            int oldDX = xs[1]-xs[0];
            int oldDY = ys[1]-ys[0];
            xs[0] = node.x+node.w/2;
            ys[0] = node.y+node.h/2;
            xs[1] = xs[0] + oldDX;
            ys[1] = ys[0] + oldDY;
        } else if (type.equals(CURVE)) {
            boolean moveAllAnchors = false;
            if (moveAllAnchors) {
                int[] oxs = xs;
                int[] oys = ys;
                // calculate link w/o considering old
                calcLinkPosition(getNumberOfControlPoints(), arrowstyle);
                // calculate old distance and angle
                int k = n-1;
                double od = calcDistance(oxs[0],oys[0],oxs[k],oys[k]);
                double oa = Graph.calcSlope(oxs[0],oys[0],oxs[k],oys[k]);
                // calculate new distance and angle
                double nd = calcDistance(xs[0],ys[0],xs[k],ys[k]);
                double na = Graph.calcSlope(xs[0],ys[0],xs[k],ys[k]);
                double odk, oak, ndk, nak;  // old/new distance and angle for k'th node
                for (k=1; k<n-1; k++) {
                    odk = calcDistance(oxs[0],oys[0],oxs[k],oys[k]);
                    oak = Graph.calcSlope(oxs[0],oys[0],oxs[k],oys[k]);
                    ndk = odk*nd/od;
                    nak = oak+na-oa;
                    xs[k] = xs[0] + (int)(ndk * Math.cos(nak));
                    ys[k] = ys[0] + (int)(ndk * Math.sin(nak));
                }   
            } else moveEndAnchor(node, n);
        } else if (type.equals(STRAIGHT)) {
            if (n==2) calcLinkPosition(2, arrowstyle);
            else moveEndAnchor(node, n);
        } else if (n==2) {    // automatic ELBOW, ELBOWH, ELBOWV
            calcAutoElbowLinkEndPoints();
        } else {     // controlled ELBOW, ELBOWH, ELBOWV
            boolean wasHorizontal = !anchorHorizontalThenVertical(0);
            boolean horizontalFirst = (Math.abs(from.x-to.x)
                    >= Math.abs(from.y-to.y));
            if (type.equals(ELBOW) && wasHorizontal!=horizontalFirst) {
                calcLinkPosition(getNumberOfControlPoints(), arrowstyle);
            } else if (from==node) {
                if (xs[1]>node.x+node.w) xs[0] = node.x+node.w+gap;
                else if (xs[1]<node.x) xs[0] = node.x - gap;
                else xs[0] = xs[1];
                if (ys[1]>node.y+node.h) ys[0] = node.y+node.h+gap;
                else if (ys[1]<node.y) ys[0] = node.y-gap;
                else ys[0] = ys[1];
                if (wasHorizontal) ys[1] = ys[0];
                else xs[1] = xs[0];
            } else {
                int k = n-1;
                if (xs[k-1]>node.x+node.w) xs[k] = node.x+node.w+gap;
                else if (xs[k-1]<node.x) xs[k] = node.x-gap;
                else xs[k] = xs[k-1];
                if (ys[k-1]>node.y+node.h) ys[k] = node.y+node.h+gap;
                else if (ys[k-1]<node.y) ys[k] = node.y-gap;
                else ys[k] = ys[k-1];
                if (wasHorizontal&&n%2==0 || !wasHorizontal&&n%2!=0) ys[k-1] = ys[k];
                else xs[k-1] = xs[k];
            }
        }
        // move label relatively
        label_slope = Graph.calcSlope(xs[0], ys[0], xs[n-1], ys[n-1]) + label_slope;
        label_dist = calcDistance(xs[0], ys[0], xs[n-1], ys[n-1]) * label_dist;
        lx = (int)Math.round(xs[0] + Math.cos(label_slope)*label_dist);
        ly = (int)Math.round(ys[0] + Math.sin(label_slope)*label_dist);
        // determine shape and arrow object
        determineShape();
        determineArrow(arrowstyle);
    }
    
    /**
     * Return the distance of two points
     * @param x1 x-coord of starting point
     * @param y1 y-coord of starting point
     * @param x2 x-coord of ending point
     * @param y2 y-coord of ending point
     * @return the distance
     */
    private double calcDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt((y2-y1)*(y2-y1)+(x2-x1)*(x2-x1));
    }
    
    private void moveEndAnchor(Node node, int n) {
        if (from==node) {
            if (xs[1]>node.x+node.w+gap) xs[0] = node.x+node.w+gap;
            else if (xs[1]<node.x-gap) xs[0] = node.x-gap;
            if (ys[1]>node.y+node.h+gap) ys[0] = node.y+node.h+gap;
            else if (ys[1]<node.y-gap) ys[0] = node.y-gap;
        } else {
            int k = n-1;
            if (xs[k-1]>node.x+node.w+gap) xs[k] = node.x+node.w+gap;
            else if (xs[k-1]<node.x-gap) xs[k] = node.x-gap;
            if (ys[k-1]>node.y+node.h+gap) ys[k] = node.y+node.h+gap;
            else if (ys[k-1]<node.y-gap) ys[k] = node.y-gap;
        }
    }
    
    public void move(int dx, int dy, String arrowstyle) {
        lx += dx;
        ly += dy;
        if (xs!=null) {
            for (int j=0; j<xs.length; j++) {
                xs[j] += dx;
                ys[j] += dy;
            }
        }
        determineShape();
        determineArrow(arrowstyle);
    }
    
    public void moveControlPoint(int anchor, int newX, int newY, String arrowstyle) {
        if (isElbowType()&&xs.length!=2) {
            boolean horizontalThenVertical = anchorHorizontalThenVertical(anchor);
            if (horizontalThenVertical) {
                xs[anchor] = newX;
                ys[anchor] = newY;
                if (anchor>0) ys[anchor-1] = newY;
                if (anchor<xs.length-1) xs[anchor+1] = newX;
            } else {
                xs[anchor] = newX;
                ys[anchor] = newY;
                if (anchor>0) xs[anchor-1] = newX;
                if (anchor<xs.length-1) ys[anchor+1] = newY;
            }
        } else {
            xs[anchor] = newX;
            ys[anchor] = newY;
        }
        determineShape();
        determineArrow(arrowstyle);
    }
    
    public int getControlPointX(int i) {
        return xs[i];
    }
    
    public int getControlPointY(int i) {
        return ys[i];
    }
    
    public Shape getArrow(String arrowstyle) {
//        return determineArrow(arrowstyle);
        return arrowshape;
    }
    
    void determineArrow(String arrowstyle) {
        int[] xas = new int[3];
        int[] yas = new int[3];
        int p = 12;
        double dl, dr;
        double slope;
        int x, y;
        // calculate tip point and slope
        if (type.equals(CURVE)) {
            if (xs.length==4) {
                int x1 = (int)Math.round(-5.0/6.0*xs[0]+3.0*xs[1]-1.5*xs[2]+1.0/3.0*xs[3]);
                int y1 = (int)Math.round(-5.0/6.0*ys[0]+3.0*ys[1]-1.5*ys[2]+1.0/3.0*ys[3]);
                int x2 = (int)Math.round(-5.0/6.0*xs[3]+3.0*xs[2]-1.5*xs[1]+1.0/3.0*xs[0]);
                int y2 = (int)Math.round(-5.0/6.0*ys[3]+3.0*ys[2]-1.5*ys[1]+1.0/3.0*ys[0]);
                // copied from determineShape()
                if (arrowstyle.equals(ARROW_STYLE_END)) {
                    x = xs[3];
                    y = ys[3];
                    slope = Graph.calcSlope(x2,y2,xs[3],ys[3]);
                } else {
                    x = (int)(0.125*xs[0]+0.375*x1+0.375*x2+0.125*xs[3]);
                    y = (int)(0.125*ys[0]+0.375*y1+0.375*y2+0.125*ys[3]);
                    int x12 = (int)(0.25*xs[0] + 0.5*x1 + 0.25*x2);
                    int y12 = (int)(0.25*ys[0] + 0.5*y1 + 0.25*y2);
                    int x23 = (int)(0.25*x1 + 0.5*x2 + 0.25*xs[3]);
                    int y23 = (int)(0.25*y1 + 0.5*y2 + 0.25*ys[3]);
                    slope = Graph.calcSlope(x12,y12,x23,y23);
                }
            } else if (xs.length==3) {
                int x1 = (int)Math.round(-0.5*xs[0]+2*xs[1]-0.5*xs[2]);
                int y1 = (int)Math.round(-0.5*ys[0]+2*ys[1]-0.5*ys[2]);
                if (arrowstyle.equals(ARROW_STYLE_END)) {
                    x = xs[2];
                    y = ys[2];
                    slope = Graph.calcSlope(x1,y1,xs[2],ys[2]);
                } else {
                    x = xs[1];
                    y = ys[1];
                    slope = Graph.calcSlope(xs[0],ys[0],xs[2],ys[2]);
                }
            } else if (from!=to) {
                // n==2 but from!=to
                Point m = getCurveMidPoint();
                if (arrowstyle.equals(ARROW_STYLE_END)) {
                    int x1 = (int)Math.round(-0.5*xs[0]+2*m.x-0.5*xs[1]);
                    int y1 = (int)Math.round(-0.5*ys[0]+2*m.y-0.5*ys[1]);
                    x = xs[1];
                    y = ys[1];
                    slope = Graph.calcSlope(x1,y1,xs[1],ys[1]);
                } else {
                    x = m.x;
                    y = m.y;
                    slope = Graph.calcSlope(xs[0],ys[0],xs[1],ys[1]);
                }
            } else {    // from==to; not used but need to set the values to rid of compilation error
                x = xs[1];
                y = ys[1];
                slope = Graph.calcSlope(xs[0],ys[0],xs[1],ys[1]) + 1.5708;
            }
        } else if (type.equals(STRAIGHT)){
            if (arrowstyle.equals(ARROW_STYLE_END)) {
                int p2 = xs.length-1;
                int p1 = p2-1;
                x = xs[p2];
                y = ys[p2];
                slope = Graph.calcSlope(xs[p1],ys[p1],xs[p2],ys[p2]);
            } else {
                int p2 = (xs.length+1)/2;
                int p1 = p2 - 1;
                x = (xs[p1]+xs[p2])/2;
                y = (ys[p1]+ys[p2])/2;
                slope = Graph.calcSlope(xs[p1],ys[p1],xs[p2],ys[p2]);
            }
        } else if (xs.length==2) {      // auto ELBOW/ELBOWH/ELBOWV type
            if (arrowstyle.equals(ARROW_STYLE_END)) {
                switch (getAutoElbowLinkType()) {
                case AUTOLINK_V:
                case AUTOLINK_VHV:                   
                case AUTOLINK_HV:
                    x = xs[1];
                    y = ys[1]>ys[0]?ys[1]+gap:ys[1]-gap;
                    slope = ys[1]>ys[0]?1.5708:4.7124;
                    break;
                case AUTOLINK_H:
                case AUTOLINK_HVH:
                case AUTOLINK_VH:
                default:    // just to rid of compiler error
                    x = xs[1]>xs[0]?xs[1]+gap:xs[1]-gap;
                    y = ys[1];
                    slope = xs[1]>xs[0]?0:3.1416;
                    break;
                }                
            } else {
                switch (getAutoElbowLinkType()) {
                case AUTOLINK_V:
                    x = xs[0];
                    y = (ys[0]+ys[1])/2;
                    slope = ys[1]>ys[0]?1.5708:4.7124;
                    break;
                case AUTOLINK_H:
                    x = (xs[0]+xs[1])/2;
                    y = ys[0];
                    slope = xs[1]>xs[0]?0:3.1416;
                    break;
                case AUTOLINK_VHV:
                    x = (xs[0]+xs[1])/2;
                    y = (ys[0]+ys[1])/2;
                    slope = xs[1]>xs[0]?0:3.1416;
                    break;
                case AUTOLINK_HVH:
                    x = (xs[0]+xs[1])/2;
                    y = (ys[0]+ys[1])/2;
                    slope = ys[1]>ys[0]?1.5708:4.7124;
                    break;
                case AUTOLINK_HV:
                    x = xs[1];
                    y = (ys[0]+ys[1])/2;
                    slope = ys[1]>ys[0]?1.5708:4.7124;
                    break;
                default:    // just to rid of compiler error
                case AUTOLINK_VH:
                    x = (xs[1]+xs[0])/2;
                    y = ys[1];
                    slope = xs[1]>xs[0]?0:3.1416; 
                    break;
                }
            }
        } else {        // ELBOW/ELBOWH/ELBOWV, control points > 2
            if (arrowstyle.equals(ARROW_STYLE_END)) {
                int k = xs.length-1;
                if (xs[k]==xs[k-1] && (ys[k]!=ys[k-1] || ys[k-1]==ys[k-2])) {
                    // verticle arrow
                    x = xs[k];
                    y = ys[k]>ys[k-1]?ys[k]+gap:ys[k]-gap;
                    slope = ys[k]>ys[k-1]?1.5708:4.7124;              
                } else {
                    x = xs[k]>xs[k-1]?xs[k]+gap:xs[k]-gap;
                    y = ys[k];
                    slope = xs[k]>xs[k-1]?0:3.1416;
                }
            } else {
                int p2 = (xs.length+1)/2;
                int p1 = p2 - 1;
                x = (xs[p1]+xs[p2])/2;
                y = (ys[p1]+ys[p2])/2;
                slope = Graph.calcSlope(xs[p1],ys[p1],xs[p2],ys[p2]);
            }
        }
        // convert point and slope to polygon
        dl = slope-2.7052;   // 25 degree
        dr = slope+2.7052;   // 25 degree
        xas[0] = x;
        yas[0] = y;
        xas[1] = (int)Math.round(Math.cos(dl)*p + xas[0]);
        yas[1] = (int)Math.round(Math.sin(dl)*p + yas[0]);
        xas[2] = (int)Math.round(Math.cos(dr)*p + xas[0]);
        yas[2] = (int)Math.round(Math.sin(dr)*p + yas[0]);
        arrowshape = new Polygon(xas, yas, 3);
    }
    
    public String getLogicalId() {
    	String id = conn.getAttribute(WorkAttributeConstant.LOGICAL_ID);
    	return id==null?"":id;
    }

	@Override
	public void setText(String text) {
		this.setLabelAndEventType(text);
	}
    
}
