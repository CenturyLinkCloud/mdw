/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import java.lang.reflect.Constructor;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.java.CompiledJavaCache;

public class JsonableTranslator extends DocumentReferenceTranslator {

    public static final String TYPE = "_type";

    public Object realToObject(String string) throws TranslationException {
        return realToObject(string, ApplicationContext.isOsgi());
    }

    @Override
    protected Object realToObject(String str, boolean tryProviders) throws TranslationException {
        try {
            if (tryProviders)
                return providerDeserialize(str);

            JSONObject json = new JSONObject(str);
            String type = json.getString(TYPE);
            Class<? extends Jsonable> clazz;
            try {
                clazz = Class.forName(type).asSubclass(Jsonable.class);

            }
            catch (ClassNotFoundException cnfe) {
                if (ApplicationContext.isCloud()) {
                    clazz = CompiledJavaCache
                            .getResourceClass(type, getClass().getClassLoader(), getPackage())
                            .asSubclass(Jsonable.class);
                }
                else {
                    throw cnfe;
                }
            }
            Constructor<? extends Jsonable> ctor = clazz.getConstructor(JSONObject.class);
            Iterator<?> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                if (!TYPE.equals(key)) {
                    JSONObject objectJson = json.getJSONObject(key);
                    Jsonable jsonable = ctor.newInstance(objectJson);
                    return jsonable;
                }
            }
            throw new JSONException("Object not found for " + TYPE + ": " + clazz);
        }
        catch (Exception ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

    public String realToString(Object object) throws TranslationException {
        Jsonable jsonable = (Jsonable) object;
        JSONObject json = new JSONObject();
        try {
            json.put(TYPE, jsonable.getClass().getName());
            json.put(jsonable.getJsonName(), jsonable.getJson());
            return json.toString(2);
        }
        catch (JSONException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
}
