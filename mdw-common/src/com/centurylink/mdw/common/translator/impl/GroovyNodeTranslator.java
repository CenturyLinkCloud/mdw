/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import groovy.util.Node;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.w3c.dom.Document;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.DomHelper;

public class GroovyNodeTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    public Object realToObject(String string) throws TranslationException {
        return realToObject(string, ApplicationContext.isOsgi());
    }

    @Override
    protected Object realToObject(String str, boolean tryProviders) throws TranslationException {
        try {
            if (tryProviders)
                return providerDeserialize(str);
            
            return new XmlParser().parseText(str);
        } catch (Exception e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public String realToString(Object object) throws TranslationException {
        StringWriter writer = null;
        try {
            Node node = (Node)object;
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
            return DomHelper.toDomDocument(realToString(obj));
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
    
    public Object fromDomNode(org.w3c.dom.Node domNode) throws TranslationException {
        try {
            return realToObject(DomHelper.toXml(domNode));
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
    
}
