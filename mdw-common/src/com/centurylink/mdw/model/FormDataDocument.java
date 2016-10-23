/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.constant.FormConstants;
import com.centurylink.mdw.model.task.TaskStatus;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.FormatDom;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;
import com.qwest.mbeng.MbengRuleSet;
import com.qwest.mbeng.MbengRuntime;
import com.qwest.mbeng.NodeFinder;
import com.qwest.mbeng.StreamLogger;

public class FormDataDocument extends DomDocument {

    // datadoc root attributes
    public static final String ATTR_ID = "ID";
//    public static final String ATTR_JSON_COMPLIANT = "JSON_COMPLIANT";
    	// For in-flow tasks, the ID is set to "TaskInstance:<task_inst_id>"
    	// For pre-flow tasks handled by event handlers,
    	// this is set to the log tag of the window session
    public static final String ATTR_APPNAME = "APPNAME";
    public static final String ATTR_ACTION = "ACTION";
    public static final String ATTR_FORM = "FORM";
    public static final String ATTR_ENGINE_CALL_STATUS = "ENGINE_CALL_STATUS";	// for async engine call
    // datadoc node attributes
    public static final String ATTR_NAME = "NAME";
    public static final String ATTR_VALUE = "VALUE";
    public static final String ATTR_SELECTED = "SELECTED";

    // datadoc node kinds (tags)
    public static final String KIND_LIST = "LIST";
    public static final String KIND_TABLE = "TABLE";
    public static final String KIND_FIELD = "FIELD";
    public static final String KIND_FORMDATA = "FORMDATA";
    public static final String KIND_SUBFORM = "SUBFORM";
    public static final String KIND_ENTRY = "ENTRY";
    public static final String KIND_ROW = "ROW";
    // the followings are private to force compiler error so that they are not used explicitly
    private static final String KIND_ERROR = "ERROR";
    private static final String KIND_META = "META";

    // meta node names
    public static final String META_PROMPT = "PROMPT";
    public static final String META_TITLE = "TITLE";
    public static final String META_INITIALIZATION = "INITIALIZATION";
    public static final String META_PROCESS_INSTANCE_ID = "PROCESS_INSTANCE_ID";
    public static final String META_MASTER_REQUEST_ID = "MASTER_REQUEST_ID";
    public static final String META_PROCESS_NAME = "PROCESS_NAME";
    public static final String META_FORM_VERSION = "FORM_VERSION";
    public static final String META_TASK_CUSTOM_ACTIONS = "TASK_CUSTOM_ACTIONS";
    public static final String META_TASK_PRIORITY = "TASK_PRIORITY";
    public static final String META_DUE_IN_SECONDS = "DUE_IN_SECONDS";
    public static final String META_ACTIVITY_INSTANCE_ID = "ACTIVITY_INSTANCE_ID";
    public static final String META_TASK_LOGICAL_ID = "TASK_LOGICAL_ID";
    public static final String META_TASK_TRANS_INST_ID = "TASK_TRANS_INST_ID";	//used by old style task creation in MDW 5 only
    public static final String META_TASK_ERRMSG = "TASK_ERRMSG";	//used by old style task creation in MDW 5 only
    public static final String META_PRIVILEGES = "USER_PRIVILEGES";
    public static final String META_FORM_DATA_VARIABLE_NAME = "FORM_DATA_VARIABLE_NAME"; // proc inst form data var name
    public static final String META_USER = "USER";		// cuid of authenticated user
    public static final String META_AUTOBIND = "AUTOBIND";	// true or false (default), for autobinding of processes started by tasks
    public static final String META_TASK_GROUPS = "TASK_GROUPS";		// groups to which the task instance belongs to
    // the followings are for json-compliant root attributes
    public static final String META_ACTION = ATTR_ACTION;
    public static final String META_FORM = ATTR_FORM;
    public static final String META_ID = ATTR_ID;
    public static final String META_APPNAME = ATTR_NAME;
    // followings are for standard in-flow task attributes
    public static final String META_TASK_INSTANCE_ID = "TaskInstanceId";
    public static final String META_TASK_STATUS = "TaskStatus";
    public static final String META_TASK_ASSIGNEE = "TaskAssignee";
    public static final String META_TASK_NAME = "TaskName";
    public static final String META_TASK_START_DATE = "TaskStartDate";
    public static final String META_TASK_END_DATE = "TaskEndDate";
    public static final String META_TASK_DUE_DATE = "TaskDueDate";
    public static final String META_TASK_ASSIGN_STATUS = "TaskAssignStatus";
    public static final String META_TASK_COMMENT = "TaskComment";
    public static final String META_TASK_OWNER_APPL = "TaskOwnerApplication";
    public static final String META_PACKAGE_NAME = "PackageName";
    // temporary variable
    public static final String META_DIALOG = "DialogName";
    // for response status - good status is "0", generic error status is "1"
    public static final String META_STATUS = "Status";

    // assignment status - if not one of the following three, assuming it is a CUID
	public static final String ASSIGN_STATUS_SELF = "self";
	public static final String ASSIGN_STATUS_OPEN = "open";
	public static final String ASSIGN_STATUS_CLOSE = "close";

	private static boolean defaultIsJson = true;

    private boolean isJson;

    /**
     * Create a blank document
     */
    public FormDataDocument() {
        setNameAttribute(ATTR_NAME);
        getRootNode().setKind(KIND_FORMDATA);
        getRootNode().setName(KIND_FORMDATA);	// for Magic Rule logging purpose only
        isJson = defaultIsJson;
    }

    public void load(String content) throws MbengException {
    	if (content.startsWith("{")) {
    		try {
				loadJson(content);
		        isJson = true;
			} catch (JSONException e) {
				throw new MbengException(e.getMessage(), e);
			}
    	} else {
    		loadXml(content);
	        isJson = false;
    	}
    }

    /**
     * Load the XML into the document. The document is cleared first.
     * @param xmlstr
     * @throws MbengException
     */
    public void loadXml(String xmlstr) throws MbengException {
        FormatDom fmter = new FormatDom();
//        getRootNode().setAttribute(ATTR_JSON_COMPLIANT,null);
        fmter.load(this, xmlstr);
//        String av = getRootNode().getAttribute(ATTR_JSON_COMPLIANT);
//        isJsonCompliant = "true".equalsIgnoreCase(av);
    }

    /**
     * Load the xml contained in the specified file into the document. The document is cleared first.
     * @param file
     * @throws MbengException
     * @throws IOException
     */
    public void load(File file) throws MbengException,IOException {
        FileInputStream in = new FileInputStream(file);
        byte[] bytes = new byte[(int)file.length()];
        in.read(bytes);
        in.close();
        String xmlstr = new String(bytes);
        FormatDom fmter = new FormatDom();
//        getRootNode().setAttribute(ATTR_JSON_COMPLIANT,null);
        fmter.load(this, xmlstr);
//        String av = getRootNode().getAttribute(ATTR_JSON_COMPLIANT);
//        isJsonCompliant = "true".equalsIgnoreCase(av);
    }

    /**
     * Output the document as an XML string
     * @return generated XML string
     */
    public String format() {
    	if (this.isJson) {
    		try {
				return formatJson();
			} catch (JSONException e) {
				throw new RuntimeException("Failed to format JSON from FormDataDocument");
			}
    	} else return this.xmlText();
    }

    /**
     * Get the node as specified by the path.
     * @param path MagicBox path notation (dot-delimited, excluding name of root element).
     * @return
     */
    public MbengNode getNode(String path) {
        NodeFinder finder = new NodeFinder(false);
        return finder.find(this, path);
    }

    /**
     * Get the node as specified by the path, relative to the supplied reference node
     * @param refnode the reference node
     * @param path MagicBox path notation (dot-delimited, relative to the reference node)
     * @return
     */
    public MbengNode getNode(MbengNode refnode, String path) {
        NodeFinder finder = new NodeFinder(false);
        return finder.find(refnode, path);
    }

    /**
     * Get the node value as specified by the path
     */
    public String getValue(String path) {
        MbengNode node = getNode(path);
        return node==null?null:node.getValue();
    }

    /**
     * Get the node value as specified by the path, relative to the supplied root node
     */
    public String getValue(MbengNode refnode, String path) {
        MbengNode node = getNode(refnode, path);
        return node==null?null:node.getValue();
    }

    /**
     * Set the value on the given path. If the node does not exist, it will be created.
     * It also creates intermediate nodes if the path contains multiple segments
     * and some intermediate nodes are not all present.
     * @param path MagicBox path notation (dot-delimited, excluding name of root element).
     * @param value
     * @return the node containing the value, which may be newly created
     * @throws MbengException
     */
    public MbengNode setValue(String path, String value)
    	throws MbengException {
    	NodeFinder finder = new NodeFinder(false);
    	MbengNode node = finder.find(this, path);
    	if (node==null) {
    		node = finder.create(this, path, KIND_FIELD, value);
    	} else node.setValue(value);
    	return node;
    }

    public MbengNode setValue(String path, String value, String kind)
            throws MbengException {
        NodeFinder finder = new NodeFinder(false);
        MbengNode node = finder.find(this, path);
        if (node==null) {
            node = finder.create(this, path, kind==null?defaultChildKind():kind, value);
        } else node.setValue(value);
        return node;
    }

    /**
     * Set the value on the given path, relative to the reference node. If the node does not exist, it will be created.
     * It also creates intermediate nodes if the path contains multiple segments
     * and some intermediate nodes are not all present.
     * @param refnode the reference node
     * @param path MagicBox path notation (dot-delimited, relative to the reference node)
     * @param value
     * @return
     * @throws MbengException
     */
    public MbengNode setValue(MbengNode refnode, String path, String value)
		    throws MbengException {
		NodeFinder finder = new NodeFinder(false);
		MbengNode node = finder.find(refnode, path);
		if (node==null) {
		    node = finder.create(this, refnode, path, KIND_FIELD, value);
		} else node.setValue(value);
		return node;
    }

    /**
     * This is not JSON-compliant, so should no longer be used.
     */
    @Deprecated
    public MbengNode addValue(String path, String value, String kind)
	    throws MbengException {
	    NodeFinder finder = new NodeFinder(false);
	    MbengNode node = finder.create(this, path, kind==null?defaultChildKind():kind, value);
	    return node;
	}

    /**
     * Remove the specified node from its parent
     */
    public void removeNode(MbengNode node) {
        node.getParent().removeChild(node);
    }

    @Override
    public String defaultChildKind() {
        return KIND_FIELD;
    }

    /**
     * For JSON-compliant document, this is equivalent to getMetaValue
     */
    public String getAttribute(String attrname) {
    	return getMetaValue(attrname);
//       	if (isJsonCompliant) return getMetaValue(attrname);
//    	else return getRootNode().getAttribute(attrname);
    }

    /**
     * For JSON-compliant document, this is equivalent to setMetaValue
     */
    public void setAttribute(String attrname, String value) {
    	setMetaValue(attrname, value);
//    	if (isJsonCompliant) setMetaValue(attrname, value);
//    	else getRootNode().setAttribute(attrname, value);
    }

    /**
     * Set meta field value.
     * @param name
     * @param value
     */
    public void setMetaValue(String name, String value) {
//    	if (isJsonCompliant) {
    		MbengNode meta = getRootNode().findChild(KIND_META);
    		if (meta==null) {
    			meta = newNode(KIND_META, null, KIND_SUBFORM, ' ');
    	    	getRootNode().appendChild(meta);
    		}
    		MbengNode node = meta.findChild(name);
    		if (node==null) {
    			if (value!=null)
    				meta.appendChild(newNode(name, value, KIND_FIELD, ' '));
    		} else {
    			if (value==null) meta.removeChild(node);
    			else node.setValue(value);
    		}
//    	} else {
//	    	MbengNode node;
//	    	for (node=getRootNode().getFirstChild();
//	    		node!=null; node=node.getNextSibling()) {
//	    		if (node.getKind().equals(KIND_META)) {
//	    			if (node.getName().equals(name)) {
//	    				if (value==null) getRootNode().removeChild(node);
//	    				else node.setValue(value);
//	    				return;
//	    			}
//	    		}
//	    	}
//	    	if (value!=null) {
//	    		MbengNode meta = newNode(name, value, KIND_META, ' ');
//	    		getRootNode().appendChild(meta);
//	    	}
//    	}
    }

    /**
     * Get meta field value
     * @param name
     * @return
     */
    public String getMetaValue(String name) {
//    	if (isJsonCompliant) {
    		MbengNode meta = getRootNode().findChild(KIND_META);
    		if (meta==null) return null;
    		MbengNode node = meta.findChild(name);
    		if (node==null) return null;
    		return node.getValue();
//    	} else {
//	    	MbengNode node;
//	    	for (node=getRootNode().getFirstChild();
//	    		node!=null; node=node.getNextSibling()) {
//	    		if (node.getKind().equals(KIND_META)) {
//	    			if (node.getName().equals(name)) return node.getValue();
//	    		}
//	    	}
//	    	return null;
//    	}
    }

    public Map<String,String> collectMetaData() {
    	HashMap<String,String> ret = new HashMap<String,String>();
//    	if (isJsonCompliant) {
    		MbengNode meta = getRootNode().findChild(KIND_META);
    		if (meta!=null) {
    			for (MbengNode one=meta.getFirstChild(); one!=null; one=one.getNextSibling()) {
    				ret.put(one.getName(), one.getValue());
    			}
    		}
//    	} else {
//    		MbengNode node;
//    		for (node=getRootNode().getFirstChild();
//    			node!=null; node=node.getNextSibling()) {
//    			if (node.getKind().equals(FormDataDocument.KIND_META)) {
//    				ret.put(node.getName(), node.getValue());
//    			}
//    		}
//    	}
    	return ret;
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

    public String[] validate(String rulesetname, String rules) {
        if (rules==null || rules.length()==0) return null;
        MbengRuntime runtime;
        try {
            MbengRuleSet ruleset = new MyRuleSet(rulesetname, MbengRuleSet.RULESET_RULE);
            ruleset.parse(rules);
            runtime = new MbengRuntime(ruleset, new StreamLogger(System.out));
            runtime.bind("$", this);
            runtime.run();
            return runtime.getErrorMsgs();
        } catch (MbengException e) {
            e.printStackTrace();
            String[] msgs = new String[1];
            msgs[0] = "Error running validation rules: " + e.getMessage();
            return msgs;
        }
    }

    /**
     * Add an error message. All error messages are put in a table
     * name "ERROR".
     * @param errmsg
     */
    public void addError(String errmsg) {
//    	if (isJsonCompliant) {
    		MbengNode errors = getRootNode().findChild(KIND_ERROR);
    		if (errors==null) {
    			errors = newNode(KIND_ERROR, null, FormDataDocument.KIND_TABLE, ' ');
    			getRootNode().appendChild(errors);
    		}
    		MbengNode errnode = newNode(null, errmsg, FormDataDocument.KIND_ENTRY, ' ');
    		errors.appendChild(errnode);
//    	} else {
//    		MbengNode errnode = newNode("ERROR", errmsg, FormDataDocument.KIND_ERROR, ' ');
//    		getRootNode().appendChild(errnode);
//    	}
    }

//    private String locateClass(String className) {
//        String resource = new String(className);
//        // format the file name into a valid resource name
//        if (!resource.startsWith("/")) {
//            resource = "/" + resource;
//        }
//        resource = resource.replace('.', '/');
//        resource = resource + ".class";
//        URL classUrl = this.getClass().getResource(resource);
//        if (classUrl == null) {
//            return "Class not found: [" + className + "]";
//        } else {
//            return classUrl.getFile();
//        }
//    }
//
    /**
     * Same as setTable(null, name, false)
     */
    public MbengNode setTable(String name) throws MbengException {
    	return setTable(null, name, false);
    }

    /**
     * Create a table node if it does not already exist.
     * @param refnode the reference node; root is assumed when it is null
     * @param path MagicBox path notation (dot-delimited, excluding name of root element).
     * @param clean When it is true and the node already exists, the content will be cleaned
     * @return the table node, which may be newly created.
     * @throws MbengException
     */
    public MbengNode setTable(MbengNode refnode, String path, boolean clean) throws MbengException {
    	NodeFinder finder = new NodeFinder(false);
    	if (refnode==null) refnode = this.getRootNode();
		MbengNode node = finder.find(refnode, path);
		if (node==null) {
		    node = finder.create(this,
		    		refnode, path, FormDataDocument.KIND_TABLE, null);
		} else {
			if (!node.getKind().equals(FormDataDocument.KIND_TABLE))
				node.setKind(FormDataDocument.KIND_TABLE);
			if (clean) removeChildren(node);
		}
		return node;
    }

    /**
     * Create a sub form node if it does not already exist.
     * @param refnode the reference node; root is assumed when it is null
     * @param path MagicBox path notation (dot-delimited, excluding name of root element).
     * @return the sub form node, which may be newly created.
     * @throws MbengException
     */
    public MbengNode setSubform(MbengNode refnode, String path) throws MbengException {
    	NodeFinder finder = new NodeFinder(false);
    	if (refnode==null) refnode = this.getRootNode();
		MbengNode node = finder.find(refnode, path);
		if (node==null) {
		    node = finder.create(this, refnode, path, KIND_SUBFORM, null);
		} else {
			if (!node.getKind().equals(KIND_SUBFORM)) node.setKind(KIND_SUBFORM);
		}
		return node;
    }

    /**
     * Add a row to a table.
     * @param table
     * @return
     * @throws MbengException
     */
    public MbengNode addRow(MbengNode table) throws MbengException {
		MbengNode row = newNode(FormDataDocument.KIND_ROW, null, FormDataDocument.KIND_ROW, ' ');
		table.appendChild(row);
		return row;
    }

    /**
     * Add an entry to a table (an entry can be viewed as a row that contains a simple value)
     * @param table
     * @param value
     * @return
     * @throws MbengException
     */
    public MbengNode addEntry(MbengNode table, String value) throws MbengException {
		MbengNode row = newNode(FormDataDocument.KIND_ROW, value, FormDataDocument.KIND_ENTRY, ' ');
		table.appendChild(row);
		return row;
    }

    /**
     * Search for a row in a table that has the given key with the given value.
     * @param table
     * @param key
     * @param value
     * @return
     * @throws MbengException
     */
    public MbengNode findRow(MbengNode table, String key, String value) throws MbengException {
    	MbengNode rowNode;
		for (rowNode=table.getFirstChild(); rowNode!=null; rowNode=rowNode.getNextSibling()) {
			if (getValue(rowNode, key).equals(value)) break;
		}
		return rowNode;
    }

    /**
     * get row by index
     * @param table
     * @param index
     * @return
     * @throws MbengException
     */
    public MbengNode getRow(MbengNode table, int index) throws MbengException {
    	MbengNode rowNode;
    	int k = 0;
		for (rowNode=table.getFirstChild(); rowNode!=null; rowNode=rowNode.getNextSibling()) {
			if (k==index) break;
			k++;
		}
		return rowNode;
    }

    /**
     * Same as setValue(row, name, value)
     * @param row
     * @param name
     * @param value
     * @return
     * @throws MbengException
     */
    public MbengNode setCell(MbengNode row, String name, String value) throws MbengException {
    	return setValue(row, name, value);
    }

    /**
     * Remove all children of the node.
     * @param node
     */
    public void removeChildren(MbengNode node) {
    	MbengNode child = node.getFirstChild();
    	while (child!=null) {
    		node.removeChild(child);
    		child = node.getFirstChild();
    	}
    }

    public Long getActivityInstanceId() {
    	String actInstId = this.getMetaValue(META_ACTIVITY_INSTANCE_ID);
    	if (actInstId==null) return null;
    	else return new Long(actInstId);
    }

    public Long getTaskInstanceId() {
    	String id = this.getMetaValue(META_TASK_INSTANCE_ID);
    	if (id!=null) return new Long(id);
    	id = this.getAttribute(ATTR_ID);
    	if (id==null || !id.startsWith(FormConstants.TASK_CORRELATION_ID_PREFIX)) return null;
    	else return new Long(id.substring(FormConstants.TASK_CORRELATION_ID_PREFIX.length()));
    }

	public void translateFromXml(AdHocXmlDocument source, MbengNode whereInDataDoc,
			String[] multiOccurances) throws MbengException {
		List<String> molist;
		if (multiOccurances==null) molist = new ArrayList<String>();
		else molist = Arrays.asList(multiOccurances);
    	if (!FormDataDocument.KIND_FORMDATA.equals(whereInDataDoc.getKind()))
    		whereInDataDoc.setName(source.getRootNode().getName());
    	translateFromXml(source.getRootNode(), whereInDataDoc, molist, null);
	}

	private void translateFromXml(MbengNode sourceNode, MbengNode targetNode,
			List<String> multiOccurances, String curpath) throws MbengException {
		for (MbengNode sourceChild = sourceNode.getFirstChild();
				sourceChild!=null; sourceChild=sourceChild.getNextSibling()) {
			String childPath = curpath==null?sourceChild.getName():(curpath+"."+sourceChild.getName());
			MbengNode targetChild;
			if (multiOccurances.contains(childPath)) {
				MbengNode table = setTable(targetNode, sourceChild.getName(), false);
				targetChild = newNode(FormDataDocument.KIND_ROW, sourceChild.getValue(),
						FormDataDocument.KIND_ROW, ' ');
				table.appendChild(targetChild);
			} else {
				targetChild = newNode(sourceChild.getName(), sourceChild.getValue(),
						FormDataDocument.KIND_FIELD, ' ');
				targetNode.appendChild(targetChild);
			}
			translateFromXml(sourceChild, targetChild, multiOccurances, childPath);
		}
	}

	public AdHocXmlDocument translateToXml(MbengNode whereInDataDoc,
			String[] multiOccurances) throws MbengException {
		List<String> molist;
		if (multiOccurances==null) molist = new ArrayList<String>();
		else molist = Arrays.asList(multiOccurances);
		AdHocXmlDocument xmldoc = new AdHocXmlDocument();
		MbengNode root = xmldoc.getRootNode();
    	if (!FormDataDocument.KIND_FORMDATA.equals(whereInDataDoc.getKind()))
    		root.setName(whereInDataDoc.getName());
		translateToXml(xmldoc, whereInDataDoc, root, molist, null);
		return xmldoc;
	}

	private void translateToXml(AdHocXmlDocument xmldoc, MbengNode sourceNode, MbengNode targetNode,
			List<String> multiOccurances, String curpath) throws MbengException {
		for (MbengNode sourceChild = sourceNode.getFirstChild();
				sourceChild!=null; sourceChild=sourceChild.getNextSibling()) {
			if (KIND_META.equals(sourceChild.getName())) continue;
			String childPath = curpath==null?sourceChild.getName():(curpath+"."+sourceChild.getName());
			MbengNode targetChild;
			if (multiOccurances.contains(childPath)) {
				for (MbengNode grand=sourceChild.getFirstChild(); grand!=null; grand=grand.getNextSibling()) {
					targetChild = xmldoc.addNode(targetNode, sourceChild.getName(), grand.getValue());
					translateToXml(xmldoc, grand, targetChild, multiOccurances, childPath);
				}
			} else {
				targetChild = xmldoc.addNode(targetNode, sourceChild.getName(), sourceChild.getValue());
				translateToXml(xmldoc, sourceChild, targetChild, multiOccurances, childPath);
			}
		}
	}

	/*
	 * This method only makes META, ERROR and root attribute json-compliance.
	 * It does not handle duplicate fields, which formatJson still needs to handle
	 */
	public void complyJson() {
//		if (isJsonCompliant) return;
//		MbengNode meta = getRootNode().findChild(KIND_META);
//		if (meta==null) {
//			meta = newNode(KIND_META, null, KIND_SUBFORM, ' ');
//	    	getRootNode().appendChild(meta);
//		}
//		List<String> errmsgs = new ArrayList<String>();
//		MbengNode root = getRootNode();
//		// root attribute
//		Iterator<String> attriter = root.attributeIterator();
//		while (attriter.hasNext()) {
//			String attrname = attriter.next();
//			if (attrname.equals(ATTR_JSON_COMPLIANT)) continue;
//			String attrvalue = root.getAttribute(attrname);
//			meta.appendChild(newNode(attrname, attrvalue, KIND_FIELD, ' '));
//		}
//		// convert meta fields and collect errors
//		MbengNode child, next;
//		for (child=root.getFirstChild(); child!=null; child=next) {
//			next = child.getNextSibling();
//			if (child.getKind().equals(KIND_META)) {
//				meta.appendChild(newNode(child.getName(),child.getValue(),KIND_FIELD,' '));
//				root.removeChild(child);
//			} else if (child.getKind().equals(KIND_ERROR)) {
//				errmsgs.add(child.getValue());
//				root.removeChild(child);
//			}
//		}
//		// set error table
//		MbengNode errors = getRootNode().findChild(KIND_ERROR);
//		if (errors==null) {
//			errors = newNode(KIND_ERROR, null, KIND_TABLE, ' ');
//	    	getRootNode().appendChild(errors);
//		}
//		for (String errmsg : errmsgs) {
//			errors.appendChild(newNode(null,errmsg,KIND_ENTRY,' '));
//		}
//		// set compliance indicator
//		getRootNode().setAttribute(ATTR_JSON_COMPLIANT, "true");
//		isJsonCompliant = true;
	}

	/**
	 * Generate the JSON representation of the document.
	 * @return
	 * @throws JSONException
	 */
    public String formatJson() throws JSONException {
    	complyJson();
		MbengNode root = this.getRootNode();
    	JSONObject jsonobj = (JSONObject)formatJson(root);
		String ret = jsonobj.toString();
//		System.out.println("JSONARRAY: " + ret);
		return ret;
	}

	private Object formatJson(MbengNode xmlobj) throws JSONException {
		if (xmlobj.getKind().equals(KIND_TABLE)) {
			JSONArray jsonarray = new JSONArray();
			for (MbengNode child=xmlobj.getFirstChild(); child!=null; child=child.getNextSibling()) {
				if (child.getFirstChild()!=null) {
					jsonarray.put(formatJson(child));
				} else {
					jsonarray.put(child.getValue());
				}
			}
			return jsonarray;
		} else {
			JSONObject jsonobj = new JSONObject();
			for (MbengNode child=xmlobj.getFirstChild(); child!=null; child=child.getNextSibling()) {
				String childName = child.getName();
				if (childName==null || childName.length()==0) continue;	// TODO avoid nameless elements
				if (child.getFirstChild()!=null) {
					Object childjson = formatJson(child);
					if (childjson instanceof JSONArray) {
						jsonobj.put(child.getName(), childjson);
						// can't use accumulate which puts an extra level of array
					} else jsonobj.accumulate(child.getName(), childjson);
				} else if (child.getKind().equals(KIND_SUBFORM)) {
					Object childjson = new JSONObject();
					jsonobj.put(child.getName(), childjson);
				} else if (child.getKind().equals(KIND_TABLE)) {
					JSONArray jsonarray = new JSONArray();
					jsonobj.put(child.getName(), jsonarray);
				} else {
					jsonobj.accumulate(child.getName(), child.getValue());
				}
			}
			return jsonobj;
		}
	}

	/**
	 * Load the document with the JSON data. The document is cleaned first.
	 * @param jsondata
	 * @throws JSONException
	 * @throws MbengException
	 */
	public void loadJson(String jsondata) throws JSONException, MbengException {
//		this.isJsonCompliant = true;
		// clear existing content
		MbengNode root = getRootNode();
		MbengNode node = root.getFirstChild();
		while (node!=null) {
			root.removeChild(node);
			node = root.getFirstChild();
		}
		JSONObject jsonobj = new JSONObject(jsondata);
		loadJson(jsonobj, root);
// 		getRootNode().setAttribute(ATTR_JSON_COMPLIANT, "true");
 		//		System.out.println("FORMDATADOC: " + this.format());
	}

	private void loadJson(JSONObject jsonobj, MbengNode xmlobj)
			throws JSONException {
		Iterator<?> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = (String)keys.next();
			Object value = jsonobj.get(key);
			MbengNode child;
			if (value instanceof JSONArray) {
				JSONArray array = (JSONArray)value;
				child = newNode(key, null, KIND_TABLE, ' ');
				xmlobj.appendChild(child);
				MbengNode row;
				for (int i=0; i<array.length(); i++) {
					value = array.get(i);
					if (value instanceof JSONObject) {
						row = newNode(KIND_ROW, null, KIND_ROW, ' ');
						this.loadJson((JSONObject)value, row);
					} else {	// assume String - can it be anything else?
						row = newNode(KIND_ROW, value.toString(), KIND_ENTRY, ' ');
					}
					child.appendChild(row);
				}
			} else if (value instanceof JSONObject) {
				child = newNode(key, null, KIND_SUBFORM, ' ');
				xmlobj.appendChild(child);
				this.loadJson((JSONObject)value, child);
			} else {
				child = newNode(key, value.toString(), KIND_FIELD, ' ');
				xmlobj.appendChild(child);
			}
		}
	}

	/**
	 * Return a list of error messages, added by <code>addError</code>
	 * @return
	 */
	public List<String> getErrors()
	{
		List<String> errors = new ArrayList<String>();
	    for (MbengNode node1 = getRootNode().getFirstChild();
	    	node1 != null; node1 = node1.getNextSibling())
	    {
	    	if (FormDataDocument.KIND_ERROR.equals(node1.getKind()))
	    		errors.add(node1.getValue());
	    	if (FormDataDocument.KIND_TABLE.equals(node1.getKind())
					&& FormDataDocument.KIND_ERROR.equals(node1.getName())) {
	    		for (MbengNode node2=node1.getFirstChild();node2!=null;
	    			node2=node2.getNextSibling()) {
	    			errors.add(node2.getValue());
	    		}
	    	}
	    }
	    return errors;
	}

	/**
	 * simply returns true when there is any error message.
	 * @return
	 */
    public boolean hasErrors() {
    	for (MbengNode node1=getRootNode().getFirstChild();
    			node1!=null; node1=node1.getNextSibling()) {
			if (FormDataDocument.KIND_ERROR.equals(node1.getKind()))
				return true;
			if (FormDataDocument.KIND_TABLE.equals(node1.getKind())
					&& FormDataDocument.KIND_ERROR.equals(node1.getName())) return true;
		}
    	return false;
    }

    /**
     * Remove all error messages.
     */
    public void clearErrors()
    {
    	MbengNode next = null;
    	for (MbengNode node1=getRootNode().getFirstChild(); node1!=null; node1=next) {
    		next = node1.getNextSibling();
    		if (FormDataDocument.KIND_ERROR.equals(node1.getKind()))
    			removeNode(node1);
    		else if (FormDataDocument.KIND_TABLE.equals(node1.getKind())
					&& FormDataDocument.KIND_ERROR.equals(node1.getName()))
    			removeNode(node1);
    	}
    }

    public String getAssignStatus() {
    	String taskStatus = this.getMetaValue(META_TASK_STATUS);
    	if (taskStatus==null) return ASSIGN_STATUS_SELF;
    	if (taskStatus.equalsIgnoreCase(TaskStatus.STATUSNAME_OPEN)) return ASSIGN_STATUS_OPEN;
    	if (taskStatus.equalsIgnoreCase(TaskStatus.STATUSNAME_CANCELLED)) return ASSIGN_STATUS_CLOSE;
    	if (taskStatus.equalsIgnoreCase(TaskStatus.STATUSNAME_COMPLETED)) return ASSIGN_STATUS_CLOSE;
    	String assignee = this.getMetaValue(META_TASK_ASSIGNEE);
    	String user = this.getMetaValue(META_USER);
    	if (user.equalsIgnoreCase(assignee)) return ASSIGN_STATUS_SELF;
    	return assignee;
    }

    public static FormDataDocument createSimpleResponse(int status, String message,
    		Long taskInstId) {
    	FormDataDocument datadoc = new FormDataDocument();
		datadoc.setMetaValue(FormDataDocument.META_STATUS, Integer.toString(status));
		if (message!=null) {
			if (status==0) datadoc.setMetaValue(FormDataDocument.META_PROMPT, message);
			else datadoc.addError(message);
		}
		if (taskInstId!=null) datadoc.setMetaValue(FormDataDocument.META_TASK_INSTANCE_ID,
				taskInstId.toString());
		return datadoc;
    }

    public static String parseSimpleResponse(String content)
    throws MbengException {
    	FormDataDocument datadoc = new FormDataDocument();
    	datadoc.load(content);
    	String v = datadoc.getMetaValue(FormDataDocument.META_STATUS);
    	if (v==null || v.equals("0")) return null;
    	List<String> errorList = datadoc.getErrors();
    	if (errorList.size()>0) return errorList.get(0);
    	else return "Unknown error";
    }

	public static void main(String[] args) throws MbengException, JSONException {
		String input = "{\"Title\" : \"A Very Good Book\"," +
				"\"Chapter\" : [{\"Title\": \"Chapter 1\", \"Content\": \"Something here\"}," +
				"{\"Title\": \"Chapter 2\", \"Content\": \"Something else here\"}]}";
		FormDataDocument datadoc = new FormDataDocument();
		datadoc.loadJson(input);
		String json = datadoc.formatJson();
		System.out.println("JSON: " + json);
	}

}
