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
package com.centurylink.mdw.common.translator.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.xml.DomHelper;

public class DomDocumentTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    public Object realToObject(String xml) throws TranslationException {
        try {
            return DomHelper.toDomDocument(xml);
        } catch (Exception e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public String realToString(Object object) throws TranslationException {

        try {
            Document document = (Document)object;
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
