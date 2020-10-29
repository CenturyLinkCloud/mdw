package com.centurylink.mdw.jaxb;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.DomHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class JaxbElementTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes());
            return unmarshaller.unmarshal(in);
        } catch (JAXBException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public String toString(Object obj, String variableType) throws TranslationException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(obj, out);
            return out.toString();
        }
        catch (JAXBException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    private Marshaller marshaller;
    public Marshaller getMarshaller() { return marshaller; }
    public void setMarshaller(Marshaller marshaller) { this.marshaller = marshaller; }

    private Unmarshaller unmarshaller;
    public Unmarshaller getUnmarshaller() { return unmarshaller; }
    public void setUnmarshaller(Unmarshaller unmarshaller) { this.unmarshaller = unmarshaller; }

    public Object getJaxbObject(String xml) throws TranslationException {
        return toObject(xml, null);
    }

    public String getXml(Object jaxbObject) throws TranslationException {
        return toString(jaxbObject, null);
    }

    public Document toDomDocument(Object obj) throws TranslationException {
        try {
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
        try {
            return unmarshaller.unmarshal(domNode);
        }
        catch (JAXBException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

}