/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import java.lang.reflect.Constructor;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.XmlBeanWrapper;

public class XmlBeanWrapperTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    public Object realToObject(String str) throws TranslationException {
        try {
            int xmlIdx = str.indexOf('<');
            String type = str.substring(0, xmlIdx).trim();
            if (Compatibility.hasCodeSubstitutions())
                type = Compatibility.getInstance().performCodeSubstitutions(type).getOutput();
            String xml = str.substring(xmlIdx);
            Class<?> wrapperClass = Class.forName(type);
            Constructor<?> constructor = wrapperClass.getConstructor(new Class[]{String.class});
            return constructor.newInstance(xml);
        } catch (Exception e) {
            throw new TranslationException(e.getMessage() + "\nstring content: \"" + str + "\"", e);
        }
    }

    public String realToString(Object object) throws TranslationException {
        XmlBeanWrapper xmlBeanWrapper = (XmlBeanWrapper)object;
        return xmlBeanWrapper.getClass().getName() + "\n" + xmlBeanWrapper.getXml();
    }

    public Document toDomDocument(Object obj) throws TranslationException {
        return ((XmlBeanWrapper)obj).toDomDocument();
    }

    public Object fromDomNode(Node domNode) throws TranslationException {
        throw new UnsupportedOperationException("Cannot create XmlBeanWrapper from DOM Node");
    }

}
