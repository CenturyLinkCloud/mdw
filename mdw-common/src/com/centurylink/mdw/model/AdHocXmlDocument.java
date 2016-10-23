/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;


import java.util.List;

import org.w3c.dom.Document;

import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.FormatDom;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;
import com.qwest.mbeng.NodeFinder;
import com.qwest.mbeng.XmlPath;

public class AdHocXmlDocument {
    
    private DomDocument domdoc;
    
    public AdHocXmlDocument() {
        domdoc = new DomDocument();
    }
    
    public AdHocXmlDocument(Document doc) {
        domdoc = new DomDocument(doc);
    }
    
    public AdHocXmlDocument(String xmltext) throws MbengException {
        FormatDom fmter = new FormatDom();
        domdoc = new DomDocument();
        fmter.load(domdoc, xmltext);
    }
    
    public Document getDocument() {
        return domdoc.getXmlDocument();
    }
    
    public MbengNode getNode(MbengNode root, String path) {
        NodeFinder finder = new NodeFinder(false);
        return finder.find(root, path);
    }
    
    public MbengNode getNode(String path) {
        NodeFinder finder = new NodeFinder(false);
        return finder.find(domdoc, path);
    }
    
    public String getValue(String path) {
        return getValue(domdoc.getRootNode(), path);
    }
    
    public String getValue(MbengNode root, String path) {
        MbengNode node = getNode(root, path);
        String v = node==null?null:node.getValue();
        return v==null?null:v.trim();
    }
    
    public MbengNode setValue(String path, String value) throws MbengException {
        return setValue(domdoc.getRootNode(), path, value);
    }
    
    public MbengNode setValue(MbengNode root, String path, String value) throws MbengException {
        NodeFinder finder = new NodeFinder(false);
        MbengNode node = finder.find(root, path);
        if (node!=null) node.setValue(value);
        else node = finder.create(domdoc, root, path, "X", value);
        return node;
    }
    
    public MbengNode addNode(String path, String value) throws MbengException {
        return addNode(domdoc.getRootNode(), path, value);
    }
    
    public MbengNode addNode(MbengNode root, String path, String value) throws MbengException {
        NodeFinder finder = new NodeFinder(false);
        return finder.create(domdoc, root, path, "X", value);
    }
    
    public String getXmlText() {
        FormatDom fmter = new FormatDom();
        return fmter.format(domdoc);
    }
    
    public MbengNode getRootNode() {
        return domdoc.getRootNode();
    }
    
    public MbengNode xpathGetNode(String xpath) throws MbengException {
        XmlPath xpathobj = new XmlPath(xpath);
        return xpathobj.findNode(domdoc);
    }
    
    public MbengNode xpathGetNode(String xpath, MbengNode from) throws MbengException {
        XmlPath xpathobj = new XmlPath(xpath);
        return xpathobj.findNode(from);
    }
    
    public List<MbengNode> xpathGetNodes(String xpath) throws MbengException {
        XmlPath xpathobj = new XmlPath(xpath);
        return xpathobj.findNodes(domdoc);
    }
    
    public List<MbengNode> xpathGetNodes(String xpath, MbengNode from) throws MbengException {
        XmlPath xpathobj = new XmlPath(xpath);
        return xpathobj.findNodes(from);
    }

}
