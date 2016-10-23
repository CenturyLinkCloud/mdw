/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
