/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;

public class IntegerListTranslator extends DocumentReferenceTranslator {

    @Override
    public Object realToObject(String json) throws TranslationException {
        try {
            List<Integer> intList = new ArrayList<Integer>();
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++)
              intList.add(jsonArray.getInt(i));
            return intList;
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public String realToString(Object object) throws TranslationException {
        List<Integer> stringList = (List<Integer>)object;
        JSONArray jsonArray = new JSONArray();
        for (Integer integer : stringList)
            jsonArray.put(integer);
        return jsonArray.toString();
    }

}