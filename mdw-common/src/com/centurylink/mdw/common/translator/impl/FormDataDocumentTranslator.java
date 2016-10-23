/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.DomHelper;
import com.qwest.mbeng.MbengException;

public class FormDataDocumentTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    public Object realToObject(String str) throws TranslationException {
        try {
            FormDataDocument datadoc = new FormDataDocument();
            datadoc.load(str);
            return datadoc;
        } catch (MbengException e) {
            throw new TranslationException(e.getMessage());
        }
    }

    public String realToString(Object object) throws TranslationException {
        if (object instanceof FormDataDocument) {
            return ((FormDataDocument)object).format();
//        } else if (object instanceof XmlObject) {
//            return ((XmlObject)object).toString();
        } else throw new TranslationException("Not a FormDataDocument");
    }

    public Document toDomDocument(Object obj) throws TranslationException {
        return ((FormDataDocument)obj).getXmlDocument();
    }

    public Object fromDomNode(Node domNode) throws TranslationException {
        try {
            Document domDoc = DomHelper.toDomDocument(domNode);
            FormDataDocument formDataDoc = new FormDataDocument();
            formDataDoc.setXmlDocument(domDoc);
            return formDataDoc;
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

}
