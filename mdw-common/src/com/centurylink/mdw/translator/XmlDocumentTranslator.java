/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.translator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public interface XmlDocumentTranslator {

    public Document toDomDocument(Object obj) throws TranslationException;
    
    public Object fromDomNode(Node domNode) throws TranslationException;
}
