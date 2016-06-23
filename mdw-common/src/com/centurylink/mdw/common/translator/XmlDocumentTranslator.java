/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.common.exception.TranslationException;

public interface XmlDocumentTranslator {

    public Document toDomDocument(Object obj) throws TranslationException;
    
    public Object fromDomNode(Node domNode) throws TranslationException;
}
