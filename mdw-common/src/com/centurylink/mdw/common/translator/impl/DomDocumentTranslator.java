/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.DomHelper;

public class DomDocumentTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    public Object realToObject(String xml) throws TranslationException {
        try {
            return DomHelper.toDomDocument(xml);
        } catch (Exception e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public String realToString(Object object) throws TranslationException {

        try {
            Document document = (Document)object;
            return DomHelper.toXml(document);
        } catch (Exception e) {
            throw new TranslationException(e.getMessage(), e);
        }

    }

    public Document toDomDocument(Object obj) throws TranslationException {
        return (Document) obj;
    }

    public Object fromDomNode(Node domNode) throws TranslationException {
        try {
            return DomHelper.toDomDocument(domNode);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
}
