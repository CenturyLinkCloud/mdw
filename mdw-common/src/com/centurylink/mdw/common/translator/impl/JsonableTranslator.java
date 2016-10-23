/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import java.lang.reflect.Constructor;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.TranslationException;

public class JsonableTranslator extends DocumentReferenceTranslator implements JsonTranslator {

    public static final String TYPE = "_type";

    public Object realToObject(String str) throws TranslationException {
        try {
            JSONObject json = new JSONObject(str);
            return createJsonable(json);
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

    protected Object createJsonable(JSONObject json) throws Exception {
        String type = json.getString(TYPE);
        Class<? extends Jsonable> clazz;
        try {
            clazz = Class.forName(type).asSubclass(Jsonable.class);

        }
        catch (ClassNotFoundException cnfe) {
            clazz = CompiledJavaCache
                    .getResourceClass(type, getClass().getClassLoader(), getPackage())
                    .asSubclass(Jsonable.class);
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
}
