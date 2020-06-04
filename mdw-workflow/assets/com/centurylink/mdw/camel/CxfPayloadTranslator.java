/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.DomHelper;
import org.apache.camel.component.cxf.CxfPayload;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates a Camel CXF payload into an MDW document variable.
 * Note: does not handle headers; just the body.
 */
public class CxfPayloadTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    @SuppressWarnings("rawtypes")
    @Override
    public String toString(Object obj, String variableType) throws TranslationException {
        try {
            CxfPayload cxfPayload = (CxfPayload)obj;
            return DomHelper.toXml((Node)cxfPayload.getBody().get(0));
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            Document doc = DomHelper.toDomDocument(str);
            List<Element> body = new ArrayList<Element>();
            body.add((Element)doc);
            return new CxfPayload(null, body);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("rawtypes")
    public Document toDomDocument(Object obj) throws TranslationException {
        return (Document)((CxfPayload)obj).getBody().get(0);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    public Object fromDomNode(Node domNode) throws TranslationException {
        List nodeList = new ArrayList();
        nodeList.add(domNode);
        return new CxfPayload(null, nodeList);
    }
}
