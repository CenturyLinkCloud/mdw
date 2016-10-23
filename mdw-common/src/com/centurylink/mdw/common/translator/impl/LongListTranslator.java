/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;

public class LongListTranslator extends DocumentReferenceTranslator {

    @Override
    public Object realToObject(String json) throws TranslationException {
        try {
            List<Long> longList = new ArrayList<Long>();
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++)
                longList.add(jsonArray.getLong(i));
            return longList;
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public String realToString(Object object) throws TranslationException {
        List<Long> longList = (List<Long>)object;
        JSONArray jsonArray = new JSONArray();
        for (Long longElement : longList)
            jsonArray.put(longElement);
        return jsonArray.toString();
    }

}