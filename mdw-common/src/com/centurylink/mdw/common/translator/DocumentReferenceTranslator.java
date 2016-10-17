/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.model.value.variable.DocumentReference;

public abstract class DocumentReferenceTranslator extends VariableTranslator {

    public final Object toObject(String str) throws TranslationException {
        int k = str.indexOf('@');
        if (k<0) return new DocumentReference(new Long(str.substring(9)), null);
        else return new DocumentReference(new Long(str.substring(9,k)), str.substring(k+1));
    }

    public final String toString(Object object) throws TranslationException {
        return ((DocumentReference)object).toString();
    }

    /**
     * toString converts DocumentReference to string,
     * whereas this method converts the real object to string
     * @param pObject
     * @return
     * @throws TranslationException
     */
    public abstract String realToString(Object pObject)
    throws TranslationException;

    /**
     * toObject converts String to DocumentReference
     * whereas this methods converts the string to real object
     * @param pStr
     * @return
     * @throws TranslationException
     */
    public abstract Object realToObject(String pStr)
    throws TranslationException;

    /**
     * Default implementation ignores providers.  Override to try registry lookup.
     */
    protected Object realToObject(String str, boolean tryProviders) throws TranslationException {
        return realToObject(str);
    }

    /**
     * Default implementation ignores providers.  Override to try registry lookup.
     */
    protected String realToString(Object obj, boolean tryProviders) throws TranslationException {
        return realToString(obj);
    }

    /**
     * Default implementation ignores providers.  Override to try registry lookup.
     */
    public Document toDomDocument(Object obj, boolean tryProviders) throws TranslationException {
        if (this instanceof XmlDocumentTranslator)
            return ((XmlDocumentTranslator)this).toDomDocument(obj);
        else
            throw new UnsupportedOperationException("Translator: " + this.getClass().getName() + " does not implement" + XmlDocumentTranslator.class.getName());
    }

    /**
     * Default implementation ignores providers.  Override to try registry lookup.
     */
    public Object fromDomNode(Node domNode, boolean tryProviders) throws TranslationException {
        if (this instanceof XmlDocumentTranslator)
            return ((XmlDocumentTranslator)this).fromDomNode(domNode);
        else
            throw new UnsupportedOperationException("Translator: " + this.getClass().getName() + " does not implement" + XmlDocumentTranslator.class.getName());
    }


    interface Translation {
        public Object translate(Object in, DocumentReferenceTranslator providedTranslator) throws Exception;
    }
}
