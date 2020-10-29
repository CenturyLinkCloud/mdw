package com.centurylink.mdw.common.translator.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;

@SuppressWarnings("unused")
public class StringListTranslator extends DocumentReferenceTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            List<String> stringList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(str);
            for (int i = 0; i < jsonArray.length(); i++)
              stringList.add(jsonArray.optString(i, null));
            return stringList;
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public String toString(Object obj, String variableType) throws TranslationException {
        List<String> stringList = (List<String>) obj;
        JSONArray jsonArray = new JSONArray();
        for (String string : stringList)
            jsonArray.put(string);
        return jsonArray.toString();
    }
}