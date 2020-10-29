package com.centurylink.mdw.common.translator.impl;

import org.json.JSONObject;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;

public class StringDocumentTranslator extends DocumentReferenceTranslator implements JsonTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        return str;
    }

    @Override
    public String toString(Object obj, String variableType) throws TranslationException {
        return obj.toString();
    }

    @Override
    public JSONObject toJson(Object obj) throws TranslationException {
        return new JSONObject(obj.toString());
    }

    @Override
    public Object fromJson(JSONObject json, String type) throws TranslationException {
        return json.toString(2);
    }
}
