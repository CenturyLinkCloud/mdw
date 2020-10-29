package com.centurylink.mdw.common.translator.impl;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;

@SuppressWarnings("unused")
public class JsonObjectTranslator extends DocumentReferenceTranslator implements JsonTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            return new JsonObject(str);
        } catch (JSONException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public String toString(Object obj, String variableType) throws TranslationException {
        try {
            JSONObject jsonObject = (JSONObject) obj;
            return jsonObject.toString(2);
        }
        catch (JSONException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public JSONObject toJson(Object obj) throws TranslationException {
        return (JSONObject) obj;
    }

    public Object fromJson(JSONObject json, String type) throws TranslationException {
        return json;
    }
}
