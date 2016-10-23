/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.translator;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.xml.DomHelper;

public interface XmlDocumentTranslator extends JsonTranslator {

    public Document toDomDocument(Object obj) throws TranslationException;

    public Object fromDomNode(Node domNode) throws TranslationException;

    @Override
    default JSONObject toJson(Object obj) throws TranslationException {
        try {
            if (obj instanceof Jsonable) {
                // prefer jsonable serialization
                return ((Jsonable)obj).getJson();
            }
            else {
                Document doc = toDomDocument(obj);
                return org.json.XML.toJSONObject(DomHelper.toXml(doc));
            }
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    };

    @Override
    default Object fromJson(JSONObject json) throws TranslationException {
        try {
            if (json.has(JSONABLE_TYPE)) {
                return createJsonable(json);
            }
            else {
                String xml = org.json.XML.toString(json);
                return fromDomNode(DomHelper.toDomNode(xml));
            }
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    };

}
