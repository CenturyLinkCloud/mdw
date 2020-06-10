package com.centurylink.mdw.gson;

import com.centurylink.mdw.annotations.Variable;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;
import org.json.JSONObject;

@Variable(type="com.google.gson.JsonElement")
public class GsonTranslator extends DocumentReferenceTranslator implements JsonTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        throw new TranslationException("Not implemented");
    }

    @Override
    public String toString(Object obj, String variableType) throws TranslationException {
        throw new TranslationException("Not implemented");
    }

    public JSONObject toJson(Object obj) throws TranslationException {
        throw new TranslationException("Not implemented");
    }

    public Object fromJson(JSONObject json, String type) throws TranslationException {
        throw new TranslationException("Not implemented");
    }
}
