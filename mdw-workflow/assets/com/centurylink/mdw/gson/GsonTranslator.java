package com.centurylink.mdw.gson;

import com.centurylink.mdw.annotations.Variable;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;

@Variable(type="com.google.gson.Gson")
public class GsonTranslator extends DocumentReferenceTranslator implements JsonTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            Class<?> cls = getPackage().getClassLoader().loadClass(type);
            return new Gson().fromJson(str, cls);
        } catch (ClassNotFoundException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public String toString(Object obj, String variableType) throws TranslationException {
        return new GsonBuilder().setPrettyPrinting().create().toJson(obj);
    }

    public JSONObject toJson(Object obj) throws TranslationException {
        return new JSONObject(toString(obj, null));
    }

    public Object fromJson(JSONObject json, String type) throws TranslationException {
        return toObject(json.toString(), type);
    }
}
