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

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class LongListTranslator extends DocumentReferenceTranslator {

    @Override
    public Object toObject(String str, String type) throws TranslationException {
        try {
            List<Long> longList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(str);
            for (int i = 0; i < jsonArray.length(); i++)
                longList.add(jsonArray.opt(i) == null ? null : jsonArray.getLong(i));
            return longList;
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public String toString(Object obj, String variableType) throws TranslationException {
        List<Long> longList = (List<Long>) obj;
        JSONArray jsonArray = new JSONArray();
        for (Long longElement : longList)
            jsonArray.put(longElement);
        return jsonArray.toString();
    }
}