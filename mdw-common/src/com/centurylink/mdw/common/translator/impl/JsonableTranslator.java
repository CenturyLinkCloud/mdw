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
