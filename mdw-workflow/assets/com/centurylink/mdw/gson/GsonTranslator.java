package com.centurylink.mdw.gson;

import com.centurylink.mdw.annotations.Variable;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;
import org.json.JSONException;
import org.json.JSONObject;

@Variable(type="com.google.gson.JsonElement")
public class GsonTranslator extends DocumentReferenceTranslator implements JsonTranslator {

    public Object realToObject(String str) throws TranslationException {
        try {
            JSONObject json = new JsonObject(str);
            return createJsonable(json);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public String realToString(Object obj) throws TranslationException {
        Jsonable jsonable = (Jsonable) obj;
        JSONObject json = new JsonObject();
        try {
            json.put(JSONABLE_TYPE, jsonable.getClass().getName());
            String name = jsonable.getJsonName();
            if (name == null)
                name = jsonable.getClass().getSimpleName().substring(0, 1).toLowerCase() + jsonable.getClass().getSimpleName().substring(1);

            json.put(name, jsonable.getJson());
            return json.toString(2);
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public JSONObject toJson(Object obj) throws TranslationException {
        try {
            return ((Jsonable)obj).getJson();
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public Object fromJson(JSONObject json) throws TranslationException {
        try {
            return createJsonable(json);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
}
