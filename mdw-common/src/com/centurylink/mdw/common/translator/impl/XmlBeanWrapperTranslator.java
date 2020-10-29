package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.XmlBeanWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.lang.reflect.Constructor;

@Deprecated
public class XmlBeanWrapperTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            int xmlIdx = str.indexOf('<');
            String declaredType = str.substring(0, xmlIdx).trim();
            String xml = str.substring(xmlIdx);
            Class<?> wrapperClass = Class.forName(declaredType);
            Constructor<?> constructor = wrapperClass.getConstructor(new Class[]{String.class});
            return constructor.newInstance(xml);
        } catch (Exception e) {
            throw new TranslationException(e.getMessage() + "\nstring content: \"" + str + "\"", e);
        }
    }

    @Override
    public String toString(Object obj, String variableType) throws TranslationException {
        XmlBeanWrapper xmlBeanWrapper = (XmlBeanWrapper) obj;
        return xmlBeanWrapper.getClass().getName() + "\n" + xmlBeanWrapper.getXml();
    }

    public Document toDomDocument(Object obj) throws TranslationException {
        return ((XmlBeanWrapper)obj).toDomDocument();
    }

    public Object fromDomNode(Node domNode) throws TranslationException {
        throw new UnsupportedOperationException("Cannot create XmlBeanWrapper from DOM Node");
    }
}
