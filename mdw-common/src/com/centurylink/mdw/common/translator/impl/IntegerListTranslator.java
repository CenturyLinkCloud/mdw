/*
 * Copyright (C) 2019 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.common.translator.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;

public class IntegerListTranslator extends DocumentReferenceTranslator {

    @Override
    public Object realToObject(String json) throws TranslationException {
        try {
            List<Integer> intList = new ArrayList<Integer>();
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++)
              intList.add((Integer)jsonArray.opt(i));
            return intList;
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public String realToString(Object object) throws TranslationException {
        List<Integer> intList = (List<Integer>)object;
        JSONArray jsonArray = new JSONArray();
        for (Integer integer : intList)
            jsonArray.put(integer);
        return jsonArray.toString();
    }

}