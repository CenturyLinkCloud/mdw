/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.impl.DomDocumentTranslator;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.xml.DomHelper;

/**
 * Facilitates xpath-based reads and updates of a document variable via
 * expressions. For java.lang.Object type documents,
 * the de-serialized content is simply stored in memory;
 */
public class XPathHelper {

    private PackageVO pkg;

    public XPathHelper(PackageVO pkg) {
        this.pkg = pkg;
    }

    private DocumentVO documentVO;

    public String getType() {
        return documentVO.getDocumentType();
    }

    public Object getObject() {
        return documentVO.getObject(getType(), pkg);
    }

    public XPathHelper(DocumentVO documentVO) {
        this.documentVO = documentVO;
    }

    public XPathHelper(DocumentVO documentVO, XPathHelper parent) {
        this(documentVO);
        this.parent = parent;
    }

    public XPathHelper() {
        // TODO Auto-generated constructor stub
    }

    private XPathHelper parent;

    public XPathHelper getParent() {
        return parent;
    }

    public boolean isJavaObject() {
        return documentVO.getDocumentType().equals(Object.class.getName());
    }

    public boolean isJaxbElement() {
        return documentVO.getDocumentType().equals("javax.xml.bind.JAXBElement");
    }

    public boolean isXml() {
        return DocumentReferenceTranslator.isXmlDocumentTranslator(pkg, getType());
    }

    public boolean isDomDocument() {
        return documentVO.getDocumentType().equals(Document.class.getName());
    }

    public boolean isXmlBean() {
        return documentVO.getDocumentType().equals(XmlObject.class.getName());
    }

    /**
     * Retrieves the value of a document element or attribute as a String. If
     * multiple matches are found or the match has children, will return a List
     * <XPathHelper>.
     *
     * @param xpath
     *            the expression indicating the document location
     * @return a String, List<XPathHelper>, or null depending on how many
     *         matches are found
     */

    public Object peekDom(Document doc, String xpathExpr) throws Exception {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        xpath.setNamespaceContext(new UniversalNamespaceCache(doc, false));
        XPathExpression expr = xpath.compile(xpathExpr);
        NodeList nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        if (nodeList == null || nodeList.getLength() == 0) {
            return null;
        }
        else if (nodeList.getLength() == 1) {
            Node node = nodeList.item(0);
            if (node instanceof Attr)
                return ((Attr) node).getValue();
            else
                return node.getChildNodes().item(0).getNodeValue();
        }
        else {
            List<XPathHelper> childDocs = new ArrayList<XPathHelper>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                DocumentVO docVO = new DocumentVO();
                docVO.setObject(DomHelper.toDomDocument(node));
                docVO.setDocumentType(Document.class.getName());
                childDocs.add(new XPathHelper(docVO, this));
            }
            return childDocs;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof XPathHelper) {
            XPathHelper otherDoc = (XPathHelper) other;
            if (otherDoc.getType().equals(this.getType())) {
                if (otherDoc.isJavaObject())
                    return otherDoc.getObject().equals(getObject());
                else
                    return otherDoc.toString().equals(this.toString());
            }
        }

        return false;
    }

    public String toString() {
        if (isXmlBean())
            return ((XmlObject) getObject()).xmlText();
        else if (isDomDocument())
            return new DomDocumentTranslator().realToString(getObject());
        else
            return getObject().toString();
    }

    /**
     * Backward compatibility.
     */
    public String getXmlText() {
        return toString();
    }
}
