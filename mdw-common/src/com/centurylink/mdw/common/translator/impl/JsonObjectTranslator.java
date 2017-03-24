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

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;

public class JsonObjectTranslator extends DocumentReferenceTranslator implements JsonTranslator {

    public Object realToObject(String str) throws TranslationException {
        try {
            return new JSONObject(str);
        } catch (JSONException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public String realToString(Object object) throws TranslationException {
        try {
            JSONObject jsonObject = (JSONObject)object;
            return jsonObject.toString(2);
        }
        catch (JSONException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public JSONObject toJson(Object obj) throws TranslationException {
        return (JSONObject) obj;
    }

    public Object fromJson(JSONObject json) throws TranslationException {
        return json;
    }
}
