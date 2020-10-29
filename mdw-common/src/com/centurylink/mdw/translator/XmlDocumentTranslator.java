package com.centurylink.mdw.translator;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.xml.DomHelper;

public interface XmlDocumentTranslator extends JsonTranslator {

    Document toDomDocument(Object obj) throws TranslationException;

    Object fromDomNode(Node domNode) throws TranslationException;

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
    }

    @Override
    default Object fromJson(JSONObject json, String type) throws TranslationException {
        try {
            return createJsonable(json, type);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
}
