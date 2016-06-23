/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;

public class JsonObjectTranslator extends DocumentReferenceTranslator {

    public Object realToObject(String string) throws TranslationException {
        return realToObject(string, ApplicationContext.isOsgi());
    }

    @Override
    protected Object realToObject(String str, boolean tryProviders) throws TranslationException {
        try {
            if (tryProviders)
                return providerDeserialize(str);
            
            return new JSONObject(str);
        } catch (JSONException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public String realToString(Object object) throws TranslationException {
        try {
            JSONObject jsonObject = (JSONObject)object;
            return jsonObject.toString(2);
        }
        catch (JSONException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }
}
