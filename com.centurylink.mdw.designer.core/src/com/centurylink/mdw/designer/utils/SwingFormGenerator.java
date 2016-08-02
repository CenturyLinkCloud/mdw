/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.JTableHeader;

import org.w3c.dom.Node;

import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.pages.FormDesignCanvas;
import com.centurylink.mdw.designer.pages.FormPanel;
import com.centurylink.mdw.designer.utils.calendar.JDateTextField;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.qwest.mbeng.DomNode;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;
import com.qwest.mbeng.MbengRuleSet;
import com.qwest.mbeng.MbengRuntime;
import com.qwest.mbeng.NodeFinder;
import com.qwest.mbeng.StreamLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Description of type.
 *
 * @version 1.0
 */
public class SwingFormGenerator {

    public static final String ACTION_BUTTON = "ACTION_BUTTON";
    public static final String ACTION_MENUITEM = "ACTION_MENUITEM";
    public static final String ACTION_DROPDOWN = "ACTION_DROPDOWN";
    public static final String ACTION_RADIOBUTTON = "ACTION_RADIOBUTTON";
    public static final String ACTION_CHECKBOX = "ACTION_CHECKBOX";
    public static final String ACTION_TABBING = "ACTION_TABBING";

    private boolean atRuntime;
    private FormDataDocument dataxml;
    private FormDesignCanvas canvas;
    private Map<JLabel,MbengNode> labelmap;
    private Map<Component,MbengNode> widgetmap;
    private Map<Node,JLabel> nodelabel;
    private Map<Node,Component> nodewidget;
    private Map<String,MbengNode> idnodemap;
    private DesignerDataAccess dao;
    private FormPanel formpanel;
	private String assignStatus;
    private final int lx_default = 12, vx_default = 120;

//    private DragGestureRecognizer recognizer;

	public SwingFormGenerator(FormDesignCanvas canvas, FormPanel formpanel, DesignerDataAccess dao) {
	    this.canvas = canvas;
	    this.dao = dao;
	    this.formpanel = formpanel;
	    atRuntime = (formpanel!=null);
	    labelmap = new HashMap<JLabel,MbengNode>();
	    widgetmap = new HashMap<Component,MbengNode>();
	    nodelabel = new HashMap<Node,JLabel>();
	    nodewidget = new HashMap<Node,Component>();
	    idnodemap = new HashMap<String,MbengNode>();
//	    this.dataxml = dataxml;
	}

    private Node getDomNode(MbengNode node) {
        return ((DomNode)node).getXmlNode();
    }

    private void saving_vr(MbengNode node, Rectangle vr) {
        node.setAttribute(FormConstants.FORMATTR_VX, Integer.toString(vr.x));
        node.setAttribute(FormConstants.FORMATTR_VY, Integer.toString(vr.y));
        node.setAttribute(FormConstants.FORMATTR_VW, Integer.toString(vr.width));
        node.setAttribute(FormConstants.FORMATTR_VH, Integer.toString(vr.height));
    }

    private void saving_lr(MbengNode node, Rectangle lr) {
        node.setAttribute(FormConstants.FORMATTR_LX, Integer.toString(lr.x));
        node.setAttribute(FormConstants.FORMATTR_LY, Integer.toString(lr.y));
        node.setAttribute(FormConstants.FORMATTR_LW, Integer.toString(lr.width));
        node.setAttribute(FormConstants.FORMATTR_LH, Integer.toString(lr.height));
    }

    public String formatText(String text) {
    	if (text==null) return text;
    	if (text.startsWith("<HTML>")) return text;
		if (text.contains("\\n"))
			return "<HTML>" + text.replaceAll("\\\\n", "<BR>") + "</HTML>";
		return text;
    }

    private void create_label(MbengNode node, int lx, int ly, String text, Container container) {
        String v = node.getAttribute(FormConstants.FORMATTR_LX);
        if (v!=null) lx = Integer.parseInt(v);
        v = node.getAttribute(FormConstants.FORMATTR_LY);
        if (v!=null) ly = Integer.parseInt(v);
        v = node.getAttribute(FormConstants.FORMATTR_LW);
        int lw = (v!=null)?Integer.parseInt(v):60;
        v = node.getAttribute(FormConstants.FORMATTR_LH);
        int lh = (v!=null)?Integer.parseInt(v):20;
        JLabel label = new JLabel(formatText(text));
        label.setBounds(lx, ly, lw, lh);
        if (!atRuntime) {
        	label.addMouseListener(canvas);
        	label.addMouseMotionListener(canvas);
        }
        labelmap.put(label, node);
        nodelabel.put(getDomNode(node), label);
        container.add(label);
        saving_lr(node, label.getBounds());
    }

    private Rectangle determine_vr(MbengNode node, int vx, int vy, int vw, int vh) {
        Rectangle vr = new Rectangle();
        determine_vr(node, vx, vy, vw, vh, vr);
        return vr;
    }

    private void determine_vr(MbengNode node, int vx, int vy, int vw, int vh, Rectangle vr) {
        String v = node.getAttribute(FormConstants.FORMATTR_VX);
        vr.x = (v!=null)?Integer.parseInt(v):vx;
        v = node.getAttribute(FormConstants.FORMATTR_VY);
        vr.y = (v!=null)?Integer.parseInt(v):vy;
        v = node.getAttribute(FormConstants.FORMATTR_VW);
        vr.width = (v!=null)?Integer.parseInt(v):vw;
        v = node.getAttribute(FormConstants.FORMATTR_VH);
        vr.height = (v!=null)?Integer.parseInt(v):vh;
    }

    private void create_children(MbengNode node, Container container)
            throws Exception {
        int vx_default = 120;
        int max_pw = 0, max_ph = 5;
        for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
            Rectangle r = create_component(child, lx_default, vx_default, max_ph, container);
            if (r!=null) {
                if (r.x+r.width+5>max_pw) max_pw = r.x+r.width+5;
                if (r.y+r.height+5>max_ph) max_ph = r.y+r.height+5;
            }
        }
    }

    public Rectangle create_component(MbengNode node,
    		int lx, int vx, int vy, Container container) throws Exception {
        String label_text = node.getAttribute(FormConstants.FORMATTR_LABEL);
        Rectangle vr;
        Component widget;

        if (atRuntime) {
            if (!visible(node)) return null;
        }

        if (node.getName().equals(FormConstants.WIDGET_TEXT)) {
        	 create_label(node, lx, vy, label_text, container);
             vr = determine_vr(node, vx, vy, 240, 20);
             widget = create_text(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_TEXTAREA)) {
            create_label(node, lx, vy, label_text, container);
            vr = determine_vr(node, vx, vy, 240, 70);
        	widget = create_textarea(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_LIST)) {
            create_label(node, lx, vy, label_text, container);
            vr = determine_vr(node, vx, vy, 160, 100);
        	widget = create_list(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_DROPDOWN)) {
            create_label(node, lx, vy, label_text, container);
            vr = determine_vr(node, vx, vy, 160, 20);
        	widget = create_dropdown(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_PANEL)) {
            vr = determine_vr(node, vx, vy, 600, 300);
        	widget = create_panel(node, vr, label_text);
        } else if (node.getName().equals(FormConstants.WIDGET_BUTTON)) {
            vr = determine_vr(node, vx, vy, 120, 24);
        	widget = create_button(node, vr, label_text);
        } else if (node.getName().equals(FormConstants.WIDGET_MENUBAR)) {
            vr = determine_vr(node, 0, 0, container.getWidth(), 20);
        	widget = create_menubar(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_MENU)) {
        	if (node.getParent().getName().equals(FormConstants.WIDGET_PANEL) ||
        			node.getParent().getName().equals(FormConstants.WIDGET_TAB))
        		vr = determine_vr(node, vx, vy, 120, 24);
        	else vr = determine_vr(node, vx, 0, 120, 20);
        	widget = create_menu(node, vr, label_text);
        } else if (node.getName().equals(FormConstants.WIDGET_MENUITEM)) {
            vr = null;
            widget = create_menuitem(node, vr, label_text);
        } else if (node.getName().equals(FormConstants.WIDGET_TABBEDPANE)) {
            vr = determine_vr(node, vx, vy, 600, 300);
        	widget = create_tabbedpane(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_TAB)) {
            vr = null;
        	widget = create_tab(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_TABLE)) {
            vr = determine_vr(node, vx, vy, 600, 300);
            widget = create_table(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_COLUMN)) {
            vr = null;
            widget = create_column(node, vr, label_text, container);
        } else if (node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)) {
            create_label(node, lx, vy, label_text, container);
            vr = determine_vr(node, vx, vy, 160, 20);
            widget = create_radiobuttons(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_CHECKBOX)) {
        	create_label(node, lx, vy, label_text, container);
            vr = determine_vr(node, vx, vy, 24, 24);
        	widget = create_checkbox(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_DATE)) {
        	create_label(node, lx, vy, label_text, container);
            vr = determine_vr(node, vx, vy, 150, 20);
        	widget = create_date(node, vr);
        } else if (node.getName().equals(FormConstants.WIDGET_LISTPICKER)) {
        	create_label(node, lx, vy, label_text, container);
            vr = determine_vr(node, vx, vy, 300, 120);
        	widget = create_listpicker(node, vr);
        // TODO implements the following
//        } else if (node.getName().equals(FormConstants.WIDGET_INCLUDE)) {
        } else {
            throw new Exception("Widget not implemented: " + node.getName());
//            widget = null;
//            vr = null;
        }
        if (widget!=null) {
        	if (!atRuntime) {
        		widget.addMouseListener(canvas);
        		widget.addMouseMotionListener(canvas);
        	}
            if (container instanceof JTabbedPane)
                ((JTabbedPane)container).addTab(label_text, widget);
            else container.add(widget);
            if (atRuntime) widget.setName(node.getAttribute(FormConstants.FORMATTR_ID));
            widgetmap.put(widget, node);
            nodewidget.put(getDomNode(node), widget);
            String id = node.getAttribute(FormConstants.FORMATTR_ID);
            if (id!=null) idnodemap.put(id, node);
            if (vr!=null) saving_vr(node, widget.getBounds());
        }
        return vr;
    }

    public Rectangle generate(MbengDocument desc_doc, FormDataDocument dataxml) throws Exception {
        this.dataxml = dataxml;
    	this.assignStatus = dataxml==null?FormDataDocument.ASSIGN_STATUS_SELF:dataxml.getAssignStatus();
        labelmap = new HashMap<JLabel,MbengNode>();
        widgetmap = new HashMap<Component,MbengNode>();
        nodelabel = new HashMap<Node,JLabel>();
        nodewidget = new HashMap<Node,Component>();
	    idnodemap = new HashMap<String,MbengNode>();
        MbengNode node;
        int max_pw = 0, max_ph = 25;
        //setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        canvas.setLayout(null);
        MbengNode rootnode = desc_doc.getRootNode();
        for (node=rootnode.getFirstChild(); node!=null; node=node.getNextSibling()) {
            Rectangle vr = create_component(node, lx_default, vx_default, max_ph, canvas);
            if (vr!=null) {
                if (vr.x+vr.width+5>max_pw) max_pw = vr.x+vr.width+5;
                if (vr.y+vr.height+5>max_ph) max_ph = vr.y+vr.height+5;
            }
        }
        widgetmap.put(canvas, rootnode);
        nodewidget.put(getDomNode(rootnode), canvas);
        //panel.setBounds(0, 0, max_pw, max_ph);
//        panel.setPreferredSize(new Dimension(max_pw,max_ph));
        return determine_vr(desc_doc.getRootNode(), 0, 0, 800, 600);
    }

    private class MyRuleSet extends MbengRuleSet {
        MyRuleSet(String name, char type) throws MbengException {
            super(name, type, true, false);
        }

        @Override
        protected boolean isSectionName(String name) {
            return true;
        }

    }

    private boolean evaluateCondition(String name, String cond) {
        try {
			if (cond==null || cond.length()==0) return true;
			MbengRuleSet ruleset = new MyRuleSet(name, MbengRuleSet.RULESET_COND);
			ruleset.parse(cond);
			MbengRuntime runtime = new MbengRuntime(ruleset, new StreamLogger(System.out));
			runtime.bind("$", dataxml);
			return runtime.verify();
		} catch (MbengException e) {
			e.printStackTrace();
			return false;
		}
    }

    private boolean editable(MbengNode node) throws Exception {
        String av = node.getAttribute(FormConstants.FORMATTR_EDITABLE);
        if (av!=null && av.length()>0) {
            String rulename = node.getAttribute(FormConstants.FORMATTR_ID) + "_EDITABLE";
            try {
	            if (av.equalsIgnoreCase("true")) return true;
	        	else if (av.equalsIgnoreCase("false")) return false;
	        	else return evaluateCondition(rulename, av);
            } catch (Exception e) {
				e.printStackTrace();
				throw new Exception("Failed to evaluate editability rule for "
						+ node.getAttribute(FormConstants.FORMATTR_LABEL) + ": " + av);
			}
        } else return true;
    }

    private boolean visible(MbengNode node) throws Exception {
        String av = node.getAttribute(FormConstants.FORMATTR_VISIBLE);
        if (av!=null && av.length()>0) {
            String rulename = node.getAttribute(FormConstants.FORMATTR_ID) + "_VISIBLE";
            try {
            	if (av.equalsIgnoreCase("true")) return true;
            	else if (av.equalsIgnoreCase("false")) return false;
            	else return evaluateCondition(rulename, av);
			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception("Failed to evaluate visibility rule for "
						+ node.getAttribute(FormConstants.FORMATTR_LABEL) + ": " + av);
			}
        } else return true;
    }

    private String getValue(MbengNode node) {
        String value = null;
        String dataassoc = node.getAttribute(FormConstants.FORMATTR_DATA);
        MbengNode vnode = dataassoc==null?null:dataxml.getNode(dataassoc);
        if (vnode!=null) value = vnode.getValue();
        else value = null;
        if (value==null || value.length()==0) {
            String av = node.getAttribute(FormConstants.FORMATTR_AUTOVALUE);
            if (av!=null && av.length()>0) {
//                String rulename = node.getAttribute(FormConstants.FORMATTR_ID) + "_DEFAULT";
//                try {
//					value = this.evaluateExpression(rulename, av, dataxml);
//				} catch (MbengException e) {
//					// return null if data element is not there
//					// rule syntax error should be detected at design time?
//					value = null;
//				}
            	value = av;		// this makes it the same as JSF version
            	try {
					dataxml.setValue(dataassoc, value, FormDataDocument.KIND_FIELD);
				} catch (MbengException e) {
				}
            }
        }
        return value;
    }

    private void setListSelection(MbengNode node, SelectOption[] options) throws Exception {
        String dataassoc = node.getAttribute(FormConstants.FORMATTR_DATA);
        MbengNode valuesNode = dataassoc==null?null:dataxml.getNode(dataassoc);
        if (valuesNode==null) return;
        MbengNode one = valuesNode.getFirstChild();
        while (one!=null) {
        	String value = one.getName();
        	for (int i=0; i<options.length; i++) {
        		if (options[i].value.equals(value)) {
        			options[i].selected = true;
        			break;
        		}
        	}
        	one = one.getNextSibling();
        }
    }

    private SelectOption[] string2options(String str) {
        String[] strings = str.split(",");
        SelectOption[] options = new SelectOption[strings.length];
        int k;
        for (int i=0; i<strings.length; i++) {
            options[i] = new SelectOption();
            k = strings[i].indexOf(':');
            if (k>=0) {
                options[i].value = strings[i].substring(0,k);
                options[i].label = strings[i].substring(k+1);
            } else {
                options[i].value = strings[i];
                options[i].label = strings[i];
            }
            options[i].selected = false;
        }
        return options;
    }

    private SelectOption[] getChoices(MbengNode node) throws Exception {
        SelectOption[] options;
        String av = node.getAttribute(FormConstants.FORMATTR_CHOICES);
        if (av!=null && av.length()>0) {
            if (av.startsWith("$$.")) {
                if (atRuntime) {
                    NodeFinder nodeFinder = new NodeFinder();
                    String path = av.substring(3).replaceAll("\\.", "/");
                    MbengNode choicesNode = nodeFinder.find(dataxml.getRootNode(), path);
                    if (choicesNode==null) {
                        options = new SelectOption[0];
                    } else {
                        ArrayList<SelectOption> list = new ArrayList<SelectOption>();
                        MbengNode choiceNode = choicesNode.getFirstChild();
                        while (choiceNode!=null) {
                            SelectOption o = new SelectOption();
                            String v = choiceNode.getValue();
                            int k = v.indexOf(':');
                            if (k>=0) {
                            	o.value = v.substring(0,k);
                            	o.label = v.substring(k+1);
                            } else o.value = o.label = v;
                            o.selected = false;
                            list.add(o);
                            choiceNode = choiceNode.getNextSibling();
                        }
                        options = list.toArray(new SelectOption[list.size()]);
                    }
                } else options = string2options("dynamically,generated");
            } else options = string2options(av);
        } else {
            options = string2options("red,green,blue");
        }
        return options;
    }

    public static class SelectOption {
        String value;
        String label;
        boolean selected;
        @Override
        public String toString() {
            return label;
        }
        public String getValue() {
        	return value;
        }
    }

    public void deleteNode(MbengNode parent, MbengNode node) {
        Container container = (Container)nodewidget.get(getDomNode(parent));
        JLabel label = nodelabel.remove(getDomNode(node));
        if (label!=null) {
            labelmap.remove(label);
            container.remove(label);
        }
        Component widget = nodewidget.remove(getDomNode(node));
        if (widget!=null) {
            widgetmap.remove(widget);
            container.remove(widget);
        }
        String id = node.getAttribute(FormConstants.FORMATTR_ID);
        if (id!=null) idnodemap.remove(id);
    }

    public JLabel getLabel(MbengNode node) {
        return nodelabel.get(getDomNode(node));
    }

    public Component getWidget(MbengNode node) {
        return nodewidget.get(getDomNode(node));
    }

    public MbengNode getNode(Component component) {
    	if (component instanceof JLabel) return labelmap.get(component);
        else if (component instanceof JRadioButton) return widgetmap.get(component.getParent());
        else return widgetmap.get(component);
    }

    public void saveGeoInfo(Component component) {
        MbengNode node;
        if (component instanceof JLabel) {
            node = labelmap.get(component);
            this.saving_lr(node, component.getBounds());
            if (node.getName().equals(FormConstants.WIDGET_TEXTAREA)) {
            	String isStatic = node.getAttribute(FormConstants.FORMATTR_IS_STATIC);
            	if (isStatic!=null&&isStatic.equalsIgnoreCase("true")) {
            		this.saving_vr(node, component.getBounds());
            		Component textarea_pane = nodewidget.get(getDomNode(node));
            		textarea_pane.setBounds(component.getBounds());
            	}
            }
        } else {
            node = widgetmap.get(component);
            this.saving_vr(node, component.getBounds());
        }
    }

    public Icon loadIcon(String imagename) {
    	try {
			RuleSetVO ruleset = dao.getRuleSet(imagename, null, 0);
			String content = ruleset.getRuleSet();
			int k = content.indexOf('\n');
//    	String type = content.substring(0,k);
			byte[] bytes = RuleSetVO.decode(content.substring(k+1));
			return new ImageIcon(bytes);
		} catch (Exception e) {
			System.err.println("Unable to load icon " + imagename);
			return null;
		}
    }

    public Component getWidgetFromEventSource(Component obj) {
    	if (obj instanceof JTextArea) {
    		obj = obj.getParent().getParent();
    	} else if (obj instanceof JTable) {
    		while (obj!=null && !(obj instanceof JTablePlus)) {
    			obj = obj.getParent();
    		}
    	} else if (obj instanceof JTableHeader) {
    		JTableHeader header = (JTableHeader)obj;
    		obj = header.getTable();
    		while (obj!=null && !(obj instanceof JTablePlus)) {
    			obj = obj.getParent();
    		}
    	} else if (obj instanceof JScrollPane) {
    		// can be text area, list, or table (the last one is not the main widget)
    		Component rootobj = obj;
    		while (rootobj!=null) {
        		MbengNode node = widgetmap.get(rootobj);
        		if (node!=null) return rootobj;
    			rootobj = rootobj.getParent();
    		}
    		return obj;	// should never come here
    	} else if (obj instanceof JList) {
    		obj = obj.getParent().getParent();
    		// TODO handle JPickList
    	}
    	return obj;
    }

    public MbengNode getNodeById(String id) {
    	return idnodemap.get(id);
    }

    public class MenuButton extends JButton {
		private static final long serialVersionUID = 1L;
		private JPopupMenu menu;
    	MenuButton(String label) {
    		super(label);
    	}
    	void setMenu(JPopupMenu menu) {
    		this.menu = menu;
    	}
    	public JPopupMenu getMenu() {
    		return menu;
    	}
    }

    private Component create_radiobuttons(MbengNode node, Rectangle vr) throws Exception {
	    JPanel panel = new JPanel();
	    panel.setBounds(vr);
	    ButtonGroup group = new ButtonGroup();
	    JRadioButton button;
	    String value = (atRuntime?getValue(node):null);
	    SelectOption[] choices = this.getChoices(node);
	    button = new JRadioButton("", false);
	    button.setName("(invisible)");
	    button.setVisible(false);
	    panel.add(button);
	    group.add(button);

	    for (int i=0; i<choices.length; i++) {
	        boolean selected = false;
	        if (atRuntime) {
	            if (value!=null && value.equals(choices[i].value))
	                selected = true;
	        }
	        button = new JRadioButton(choices[i].label, selected);
	        button.setName(choices[i].value);
	        button.setEnabled(!atRuntime || editable(node));
	        if (atRuntime) {
	        	button.addActionListener(formpanel);
	        	button.setActionCommand(ACTION_RADIOBUTTON);
	        }
	        panel.add(button);
	        group.add(button);
	    }
	    panel.doLayout();
	    return panel;
    }

    private Component create_checkbox(MbengNode node, Rectangle vr) throws Exception {
    	JCheckBox checkbox = new JCheckBox();
    	checkbox.setBounds(vr);
	    String value = (atRuntime?getValue(node):null);
        String av = node.getAttribute(FormConstants.FORMATTR_CHOICES);
        String[] choices = {"false", "true"};
        if (av!=null && av.indexOf(",")>0) choices = av.split(",");
        else av = "false,true";
        checkbox.setName(av);
        checkbox.setEnabled(!atRuntime || editable(node));
        if (atRuntime) {
        	checkbox.addActionListener(formpanel);
        	checkbox.setActionCommand(ACTION_CHECKBOX);
        	if (value!=null) {
        		checkbox.setSelected(value.equalsIgnoreCase(choices[1]));
        	} else checkbox.setSelected(false);
        }
	    return checkbox;
    }

    private Component create_table(MbengNode node, Rectangle vr) throws Exception {
    	String av = node.getAttribute(FormConstants.FORMATTR_TABLE_STYLE);
    	boolean isPaginated = FormConstants.FORMATTRVALUE_TABLESTYLE_PAGINATED.equals(av);
        JTablePlus table = new JTablePlus(isPaginated, atRuntime);
        if (atRuntime) {
            String paginator = node.getAttribute(FormConstants.FORMATTR_ACTION);
        	if (paginator!=null) table.setPaginator(formpanel, paginator);
        	String id = node.getAttribute(FormConstants.FORMATTR_ID);
        	table.setName(id);
        }
        table.setBounds(vr);
        create_children(node, table);
        if (atRuntime) {
        	av = node.getAttribute(FormConstants.FORMATTR_DATA);
            table.setData(dataxml, av);
        }
        return table;
    }

	// TODO hide password; validator; required; mask
    private Component create_text(MbengNode node, Rectangle vr) throws Exception {
        boolean secrete = false;
        if (atRuntime) {
       	 	String data = node.getAttribute(FormConstants.FORMATTR_DATA);
            if (data!=null && data.equals("w_pass")) secrete = true;
        }
        JTextField textfield = secrete?(new JPasswordField()):(new JTextField());
        textfield.setEditable(atRuntime && editable(node));
        textfield.setBounds(vr);
        if (atRuntime) {
            textfield.setText(getValue(node));
            textfield.addFocusListener(formpanel);
        }
    	return textfield;
    }

	// TODO required
    private Component create_textarea(MbengNode node, Rectangle vr) throws Exception {
    	String defval = node.getAttribute(FormConstants.FORMATTR_AUTOVALUE);
        String is_static = node.getAttribute(FormConstants.FORMATTR_IS_STATIC);
        JScrollPane pane = new JScrollPane();
        JTextArea textarea = new JTextArea();
//            textarea.setBackground(page.getBackground());
        textarea.setEditable(atRuntime);
        textarea.setColumns(64);
        textarea.setLineWrap(true);
        textarea.setWrapStyleWord(true);
        if (!atRuntime) {
        	textarea.addMouseListener(canvas);
        	textarea.addMouseMotionListener(canvas);
        }
//            MouseListener[] ls = textarea.getMouseListeners();
//            for (int i=0; i<ls.length; i++) textarea.removeMouseListener(ls[i]);
//            MouseMotionListener[] mmls = textarea.getMouseMotionListeners();
//            for (int i=0; i<mmls.length; i++) textarea.removeMouseMotionListener(mmls[i]);
        pane.setViewportView(textarea);
        pane.setBounds(vr);
//      	      pane.setBorder(null);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        if (is_static!=null && is_static.equalsIgnoreCase("true")) {
        	pane.setVisible(false);
        	JLabel label = nodelabel.get(getDomNode(node));
        	label.setBounds(vr);
            if (atRuntime) {
                label.setText(getValue(node));
            } else {
            	String dataassoc = node.getAttribute(FormConstants.FORMATTR_DATA);
            	if (dataassoc!=null && dataassoc.length()>0) label.setText("$$." + dataassoc);
            	else label.setText(defval);
            }
        } else {
            if (atRuntime) {
                textarea.setText(getValue(node));
                textarea.addFocusListener(formpanel);
            } else {
            	String dataassoc = node.getAttribute(FormConstants.FORMATTR_DATA);
            	if (dataassoc!=null && dataassoc.length()>0) textarea.setText("$$." + dataassoc);
            	else textarea.setText(defval);
            }
        }
        return pane;
    }

    private Component create_list(MbengNode node, Rectangle vr) throws Exception {
        SelectOption[] choices = this.getChoices(node);
        JList jlist = new JList(choices);
        if (atRuntime) {
        	this.setListSelection(node, choices);
        	int n=0;
        	for (int i=0; i<choices.length; i++) {
        		if (choices[i].selected) n++;
        	}
        	if (n>0) {
        		int[] indices = new int[n];
        		n = 0;
        		for (int i=0; i<choices.length; i++) {
            		if (choices[i].selected) indices[n++] = i;
            	}
        		jlist.setSelectedIndices(indices);
        	}
        } else {
            jlist.addMouseListener(canvas);
            jlist.addMouseMotionListener(canvas);
        }
        JScrollPane scrollpane = new JScrollPane(jlist);
        scrollpane.setBounds(vr);
        return scrollpane;
    }

    private Component create_dropdown(MbengNode node, Rectangle vr) throws Exception {
        JComboBox dropdown = new JComboBox();
        SelectOption[] choices = this.getChoices(node);
        dropdown.addItem("");
        String value = (atRuntime?getValue(node):null);
        int selected_index = -1;
        for (int i=0; i<choices.length; i++) {
            dropdown.addItem(choices[i]);
            if (atRuntime) {
                if (value!=null && value.equals(choices[i].value))
                    selected_index = i;
            }
        }
        if (selected_index>=0) dropdown.setSelectedIndex(selected_index+1);
        dropdown.setBounds(vr);
        dropdown.setEnabled(!atRuntime || editable(node));
        if (atRuntime) {
        	dropdown.addActionListener(formpanel);
        	dropdown.setActionCommand(ACTION_DROPDOWN);
        }
        return dropdown;
    }

    private Component create_panel(MbengNode node, Rectangle vr, String label_text) throws Exception {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(null);
        Border redline = BorderFactory.createLineBorder(Color.DARK_GRAY);
        TitledBorder border = BorderFactory.createTitledBorder(redline, label_text);
        panel.setBorder(border);
        panel.setBounds(vr);
        create_children(node, panel);
        return panel;
    }

    private Component create_button(MbengNode node, Rectangle vr, String label_text) throws Exception {
        String imagename = node.getAttribute(FormConstants.FORMATTR_IMAGE);
        JButton button;
        if (imagename!=null&&imagename.length()>0) {
        	button = new JButton(loadIcon(imagename));
        } else {
        	button = new JButton(label_text);
        }
        button.setBounds(vr);
        if (atRuntime) {
            button.setActionCommand(ACTION_BUTTON);
            button.addActionListener(formpanel);
            String id = node.getAttribute(FormConstants.FORMATTR_ID);
            if (this.isReadonly(node, id)) button.setEnabled(false);
        }
        return button;
    }

    private Component create_menubar(MbengNode node, Rectangle vr) throws Exception {
//      if (vr.width==0) vr.width = 1600;
    	JMenuBar menubar = new JMenuBar();
    	menubar.setBounds(vr);
    	menubar.setLayout(null);
    	create_children(node, menubar);
    	return menubar;
    }

    private Component create_menuitem(MbengNode node, Rectangle vr, String label_text) throws Exception {

        JMenuItem menuitem = new JMenuItem(label_text);
        menuitem.setActionCommand(ACTION_MENUITEM);
        menuitem.addActionListener(atRuntime?formpanel:canvas);
        return menuitem;
    }

    private Component create_menu(MbengNode node, Rectangle vr, String label_text) throws Exception {
    	Component widget;
    	if (node.getParent().getName().equals(FormConstants.WIDGET_MENUBAR) ||
    			node.getParent().getName().equals(FormConstants.WIDGET_MENU)) {
            JMenu menu = new JMenu(label_text);
            menu.setBounds(vr);
            widget = menu;
            create_children(node, menu);
    	} else if (node.getParent().getName().equals(FormConstants.WIDGET_PANEL) ||
    			node.getParent().getName().equals(FormConstants.WIDGET_TAB) ||
    			node.getParent().getName().equals(FormConstants.WIDGET_PAGELET)) {
    		MenuButton button = new MenuButton(label_text);
            button.setBounds(vr);
            button.addActionListener(atRuntime?formpanel:canvas);
            button.setActionCommand(FormConstants.ACTION_MENU);
            JPopupMenu menu = new JPopupMenu();
            menu.setInvoker(canvas);
            create_children(node, menu);
            button.setMenu(menu);
            widget = button;
        } else {	// parent is a menu - this is a subment
            JPopupMenu menu = new JPopupMenu();
            menu.setInvoker(canvas);
            widget = menu;
            create_children(node, menu);
        }
    	return widget;
    }

    private Component create_tabbedpane(final MbengNode node, Rectangle vr) throws Exception {
        String id = node.getAttribute(FormConstants.FORMATTR_ID);
    	int activeIndex = 0;
        String assoc_data = node.getAttribute(FormConstants.FORMATTR_DATA);
        if (assoc_data==null || assoc_data.length()==0)
        	assoc_data = "__mdwtabindex__" + id;
        if (atRuntime) {
        	String v = dataxml.getValue(assoc_data);
        	if (v!=null && v.length()>0) activeIndex = Integer.parseInt(v);
        }
        String tabbing_style = node.getAttribute(FormConstants.FORMATTR_TABBING_STYLE);
        boolean readonly = atRuntime?isReadonlyRegardlessAsssignment(node,id):false;
        if (readonly) {
        	JPanel panel = new JPanel();
        	panel.setLayout(null);
        	panel.setBackground(null);
        	panel.setBounds(vr);
        	int k = 0;
	    	for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
	    		if (activeIndex==k) {
	    			String tablabel = child.getAttribute(FormConstants.FORMATTR_LABEL);
	    			if (tablabel!=null && tablabel.length()>0) {
	    				Border redline = BorderFactory.createLineBorder(Color.DARK_GRAY);
	    	        	TitledBorder border = BorderFactory.createTitledBorder(redline, tablabel);
	    	        	panel.setBorder(border);
	    			}
	            	create_children(child, panel);
	    		}
	    		k++;
	        }
            return panel;
        } else if (atRuntime && (
        		FormConstants.FORMATTRVALUE_TABBINGSTYLE_SERVER.equalsIgnoreCase(tabbing_style)
        		|| FormConstants.FORMATTRVALUE_TABBINGSTYLE_AJAX.equalsIgnoreCase(tabbing_style))) {
        	final JTabbedPane panel = new JTabbedPane();
            panel.setBackground(null);
            panel.setBounds(vr);
            create_children(node, panel);
            ChangeListener tabChangeListener = new ChangeListener() {
                public void stateChanged(ChangeEvent changeEvent) {
//                	JTabbedPane panel = (JTabbedPane) changeEvent.getSource();
                	int index = panel.getSelectedIndex();
                	ActionEvent e = new ActionEvent(panel, index, ACTION_TABBING);
                	formpanel.actionPerformed(e);
                }
            };
            panel.setSelectedIndex(activeIndex);
            panel.addChangeListener(tabChangeListener);
            return panel;
        } else {	// client/jQUery style, or design time
        	JTabbedPane panel = new JTabbedPane();
            panel.setBackground(null);
            panel.setBounds(vr);
            create_children(node, panel);
            panel.setSelectedIndex(activeIndex);
            return panel;
        }
    }

    private Component create_tab(MbengNode node, Rectangle vr) throws Exception {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(null);
//        panel.setBounds(vr);
        create_children(node, panel);
        return panel;
    }

    private Component create_column(MbengNode node, Rectangle vr,
    		String label_text, Container container) throws Exception {
        JTablePlus table = (JTablePlus)container;
        table.addColumn(node, label_text);
        if (!atRuntime) canvas.repaint();
        return null;
    }

    private Component create_date(MbengNode node, Rectangle vr) throws Exception {
    	JDateTextField cDateTextField = new JDateTextField();
//    	cDateTextField.setEditable(forTesting && editable(node));
    	cDateTextField.setBounds(vr);
        if (atRuntime) {
        	cDateTextField.setText(getValue(node));
        	cDateTextField.addFocusListener(formpanel);
        }
        return cDateTextField;
    }

    private Component create_listpicker(MbengNode node, Rectangle vr) throws Exception {
    	JListPicker chooser = new JListPicker();
//    	cDateTextField.setEditable(forTesting && editable(node));
    	chooser.setBounds(vr);
        return chooser;
    }

    private boolean isReadonly(MbengNode node, String id) {
    	if (!assignStatus.equals(FormDataDocument.ASSIGN_STATUS_SELF)) return true;
    	String cond = node.getAttribute(FormConstants.FORMATTR_EDITABLE);
    	if (cond==null || cond.length()==0) return false;
    	if (cond.equalsIgnoreCase("false")) return true;
    	if (cond.equalsIgnoreCase("true")) return false;
    	return !evaluateCondition("Editable_"+id, cond);
    }

    private boolean isReadonlyRegardlessAsssignment(MbengNode node, String id) {
    	String cond = node.getAttribute(FormConstants.FORMATTR_EDITABLE);
    	if (cond==null || cond.length()==0) return false;
    	if (cond.equalsIgnoreCase("false")) return true;
    	if (cond.equalsIgnoreCase("true")) return false;
    	return !evaluateCondition("Editable_"+id, cond);
    }

}
