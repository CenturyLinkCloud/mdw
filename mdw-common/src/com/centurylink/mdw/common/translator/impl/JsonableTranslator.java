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

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonableTranslator extends DocumentReferenceTranslator implements JsonTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            JSONObject json = new JsonObject(str);
            return createJsonable(json, type);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public String toString(Object obj, String variableType) throws TranslationException {
        try {
            return toJson(obj).toString(2);
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public JSONObject toJson(Object obj) throws TranslationException {
        if (obj instanceof JSONObject)
            return (JSONObject)obj;
        else
            return ((Jsonable)obj).getJson();
    }

    public Object fromJson(JSONObject json, String type) throws TranslationException {
        try {
            return createJsonable(json, type);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
}
