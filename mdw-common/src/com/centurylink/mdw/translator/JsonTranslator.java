/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.translator;

import java.lang.reflect.Constructor;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.workflow.Package;

/**
 * Provides a default implementation for extracting a Jsonable.
 */
public interface JsonTranslator {

    public JSONObject toJson(Object obj) throws TranslationException;

    public Object fromJson(JSONObject json) throws TranslationException;

    public Package getPackage();

    public static final String JSONABLE_TYPE = "_type";
    default Object createJsonable(JSONObject json) throws Exception {
        String type = json.getString(JSONABLE_TYPE);
        Class<? extends Jsonable> clazz;
        try {
            clazz = Class.forName(type).asSubclass(Jsonable.class);

        }
        catch (ClassNotFoundException cnfe) {
            clazz = CompiledJavaCache
                    .getResourceClass(type, JsonTranslator.class.getClassLoader(), getPackage())
                    .asSubclass(Jsonable.class);
        }
        Constructor<? extends Jsonable> ctor = clazz.getConstructor(JSONObject.class);
        Iterator<?> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next().toString();
            if (!JSONABLE_TYPE.equals(key)) {
                JSONObject objectJson = json.getJSONObject(key);
                Jsonable jsonable = ctor.newInstance(objectJson);
                return jsonable;
            }
        }
        throw new JSONException("Object not found for " + JSONABLE_TYPE + ": " + clazz);
    }
}
