/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.jaxb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.DomHelper;

public class JaxbElementTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    private Marshaller marshaller;
    public Marshaller getMarshaller() { return marshaller; }
    public void setMarshaller(Marshaller marshaller) { this.marshaller = marshaller; }

    private Unmarshaller unmarshaller;
    public Unmarshaller getUnmarshaller() { return unmarshaller; }
    public void setUnmarshaller(Unmarshaller unmarshaller) { this.unmarshaller = unmarshaller; }

    public Object getJaxbObject(String xml) throws TranslationException {
        return realToObject(xml);
    }

    public String getXml(Object jaxbObject) throws TranslationException {
        return realToString(jaxbObject);
    }

    public Object realToObject(String string) throws TranslationException {
        return realToObject(string, ApplicationContext.isOsgi());
    }

    @Override
    protected Object realToObject(String str, boolean tryProviders) throws TranslationException {
        try {
            if (tryProviders)
                return providerDeserialize(str);

            ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes());
            return unmarshaller.unmarshal(in);
        } catch (JAXBException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public String realToString(Object obj) throws TranslationException {
        return realToString(obj, ApplicationContext.isOsgi());
    }

    @Override
    public String realToString(Object obj, boolean tryProviders) throws TranslationException {
        try {
            if (tryProviders)
                return providerSerialize(obj);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(obj, out);
            return out.toString();
        }
        catch (JAXBException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public Document toDomDocument(Object obj) throws TranslationException {
        return toDomDocument(obj, ApplicationContext.isOsgi());
    }

    public Document toDomDocument(Object obj, boolean tryProviders) throws TranslationException {
        try {
            if (tryProviders)
                return providerToDomDoc(obj);

            // TODO: use JAXB Binder to avoid reparse
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            marshaller.marshal(obj, out);
            return DomHelper.toDomDocument(out.toString());
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public Object fromDomNode(Node domNode) throws TranslationException {
        return fromDomNode(domNode, ApplicationContext.isOsgi());
    }

    public Object fromDomNode(Node domNode, boolean tryProviders) throws TranslationException {
        try {
            if (tryProviders)
                return providerFromDomNode(domNode);

            return unmarshaller.unmarshal(domNode);
        }
        catch (JAXBException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

}