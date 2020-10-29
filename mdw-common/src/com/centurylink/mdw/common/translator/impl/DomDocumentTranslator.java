package com.centurylink.mdw.common.translator.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.DomHelper;

public class DomDocumentTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            return DomHelper.toDomDocument(str);
        } catch (Exception e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    @Override
    public String toString(Object obj, String variableType) throws TranslationException {

        try {
            Document document = (Document) obj;
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
