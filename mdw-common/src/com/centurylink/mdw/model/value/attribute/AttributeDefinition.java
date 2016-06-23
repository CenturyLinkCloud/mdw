/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.attribute;

import com.centurylink.mdw.common.constant.FormConstants;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.MbengNode;

public class AttributeDefinition {
	
	// the following constants may need to go to FormConstants
	private static final String FORMATTR_NAME = "NAME";
	private static final String FORMATTR_URL = "URL";
	public static final String FORMATTR_DEFAULT = "DEFAULT";
	public static final String FORMATTR_SOURCE = "SOURCE";
	private static final String WIDGET_MAPPING = "MAPPING";
	private static final String WIDGET_SELECT = "SELECT";
	private static final String WIDGET_BOOLEAN = "BOOLEAN";
	
	private static final String OPTION = "OPTION";
	
	public static final String SOURCE_VARIABLES = "Variables";
	
	private DomDocument dom;
	
	public AttributeDefinition() {
		dom = new DomDocument();
        dom.getRootNode().setName(FormConstants.WIDGET_PAGELET);
	}
	
	public String xmlText() {
		return dom.xmlText();
	}
	
	private MbengNode defineAttribute(String widgetType, String attrname, String label) {
		MbengNode node = dom.newNode(widgetType, null, "X", ' ');
		dom.getRootNode().appendChild(node);
		node.setAttribute(FORMATTR_NAME, attrname);
		if (label!=null) node.setAttribute(FormConstants.FORMATTR_LABEL, label);
		return node;
	}
	
	/**
	 * 
	 * @param attrname
	 * @param label when it is null, attribute name is used as label
	 * @param width when it is 0, use default (currently 400)
	 * @return
	 */
	public MbengNode defineTEXT(String attrname, String label, int width) {
		MbengNode node = defineAttribute(FormConstants.WIDGET_TEXT, attrname, label);
		if (width>0) node.setAttribute(FormConstants.FORMATTR_VW, Integer.toString(width));
		return node;
	}
	
	/**
	 * 
	 * @param attrname
	 * @param label when it is null, attribute name is used as label
	 * @return
	 */
	public MbengNode defineMAPPING(String attrname, String label) {
		return defineAttribute(WIDGET_MAPPING, attrname, label);
	}
	
	/**
	 * 
	 * @param attrname
	 * @param label when it is null, attribute name is used as label
	 * @param choices list of OPTION entries
	 * @param defaultValue when it is not null, set as DEFAULT attribute
	 * @return
	 */
	public MbengNode defineDROPDOWN(String attrname, String label, String[] choices, String defaultValue) {
		MbengNode node = defineAttribute(FormConstants.WIDGET_DROPDOWN, attrname, label);
		if (defaultValue!=null) node.setAttribute(FORMATTR_DEFAULT, defaultValue);
		if (choices!=null) {
			for (String one : choices) {
				MbengNode choiceNode = dom.newNode(OPTION, one, "X", ' ');
				node.appendChild(choiceNode);
			}
		}
		return node;
	}
	
	/**
	 * This is an alternative where we use source to populate the choice list
	 * @param attrname
	 * @param label
	 * @param source
	 * @param width
	 * @return
	 */
	public MbengNode defineDROPDOWN(String attrname, String label, String source, int width) {
		MbengNode node = defineAttribute(FormConstants.WIDGET_DROPDOWN, attrname, label);
		node.setAttribute(FORMATTR_SOURCE, source);
		if (width>0) node.setAttribute(FormConstants.FORMATTR_VW, Integer.toString(width));
		return node;
	}
	
	/**
	 * 
	 * @param attrname
	 * @param label when it is null, attribute name is used as label
	 * @param choices list of OPTION entries
	 * @param defaultValue when it is not null, set as DEFAULT attribute
	 * @return
	 */
	public MbengNode defineSELECT(String attrname, String label, String[] choices, String defaultValue) {
		MbengNode node = defineAttribute(WIDGET_SELECT, attrname, label);
		if (defaultValue!=null) node.setAttribute(FORMATTR_DEFAULT, defaultValue);
		if (choices!=null) {
			for (String one : choices) {
				MbengNode choiceNode = dom.newNode(OPTION, one, "X", ' ');
				node.appendChild(choiceNode);
			}
		}
		return node;
	}
	
	/**
	 * Define a boolean attribute using SELECT
	 * @param attrname
	 * @param label when it is null, attribute name is used as label
	 * @return
	 */
	public MbengNode defineBoolean(String attrname, String label, boolean defaultTrue) {
		return defineSELECT(attrname, label, new String[]{"false","true"}, defaultTrue?"true":"false");
	}
	
	public MbengNode defineHYPERLINK(String attrname, String label) {
		return defineAttribute(FormConstants.WIDGET_HYPERLINK, attrname, label);
	}
	
	public MbengNode defineHYPERLINKstatic(String url, String text) {
		MbengNode node = dom.newNode(FormConstants.WIDGET_HYPERLINK, text, "X", ' ');
		dom.getRootNode().appendChild(node);
		node.setAttribute(FORMATTR_URL, url);
		return node;
	}
	
	/**
	 * 
	 * @param attrname
	 * @param label when it is null, attribute name is used as label
	 * @return
	 */
	public MbengNode defineTABLE(String attrname, String label) {
		return defineAttribute(FormConstants.WIDGET_TABLE, attrname, label);
	}
	
	public MbengNode defineTEXTColumn(MbengNode table, String label) {
		MbengNode node = dom.newNode(FormConstants.WIDGET_TEXT, null, "X", ' ');
		node.setAttribute(FormConstants.FORMATTR_LABEL, label);
		table.appendChild(node);
		return node;
	}
	
	public MbengNode defineBOOLEANColumn(MbengNode table, String label, boolean defaultTrue) {
		MbengNode node = dom.newNode(WIDGET_BOOLEAN, null, "X", ' ');
		node.setAttribute(FormConstants.FORMATTR_LABEL, label);
		node.setAttribute(FORMATTR_DEFAULT, defaultTrue?"true":"false");
		table.appendChild(node);
		return node;
	}
}
