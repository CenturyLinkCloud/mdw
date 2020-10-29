package com.centurylink.mdw.common.translator.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.w3c.dom.Document;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.DomHelper;

import groovy.util.Node;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;

@Deprecated
public class GroovyNodeTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    public Object toObject(String str, String type) throws TranslationException {
        try {
            return new XmlParser().parseText(str);
        } catch (Exception e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public String toString(Object obj, String variableType) throws TranslationException {
        StringWriter writer = null;
        try {
            Node node = (Node) obj;
            writer = new StringWriter();
            new XmlNodePrinter(new PrintWriter(writer)).print(node);
            return writer.toString();
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (IOException ex) {
                }
            }
        }
    }

    public Document toDomDocument(Object obj) throws TranslationException {
        // TODO: avoid reparsing
        try {
            return DomHelper.toDomDocument(toString(obj, null));
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public Object fromDomNode(org.w3c.dom.Node domNode) throws TranslationException {
        try {
            return toObject(DomHelper.toXml(domNode), null);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
}
