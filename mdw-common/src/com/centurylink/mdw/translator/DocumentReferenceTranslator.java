/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.translator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.model.variable.DocumentReference;

public abstract class DocumentReferenceTranslator extends VariableTranslator {

    public final Object toObject(String str) throws TranslationException {
        return new DocumentReference(new Long(str.substring(9)));
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
