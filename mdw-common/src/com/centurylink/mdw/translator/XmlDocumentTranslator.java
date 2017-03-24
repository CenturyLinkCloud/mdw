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
