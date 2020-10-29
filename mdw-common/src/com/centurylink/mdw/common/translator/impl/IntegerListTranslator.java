package com.centurylink.mdw.common.translator.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;

@SuppressWarnings("unused")
public class IntegerListTranslator extends DocumentReferenceTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            List<Integer> intList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(str);
            for (int i = 0; i < jsonArray.length(); i++)
              intList.add((Integer)jsonArray.opt(i));
            return intList;
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public String toString(Object obj, String variableType) throws TranslationException {
        List<Integer> intList = (List<Integer>) obj;
        JSONArray jsonArray = new JSONArray();
        for (Integer integer : intList)
            jsonArray.put(integer);
        return jsonArray.toString();
    }
}