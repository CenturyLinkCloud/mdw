/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
package com.centurylink.mdw.translator;

import java.lang.reflect.Constructor;
import java.util.Iterator;

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.JsonableImpl;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.workflow.Package;

/**
 * Provides a default implementation for extracting a Jsonable.
 */
public interface JsonTranslator {

    String JSONABLE_TYPE = "_type";

    JSONObject toJson(Object obj) throws TranslationException;

    Object fromJson(JSONObject json, String type) throws TranslationException;

    Package getPackage();

    /**
     * For compatibility
     * @param json
     * @param type
     * @return
     * @throws Exception
     */
    default Object createJsonable(JSONObject json, String type) throws Exception {
        JSONObject jsonObject = json;
        if (json.has(JSONABLE_TYPE)) {
            // serialized the old way, with embedded _type property
            type = json.getString(JSONABLE_TYPE);
            Iterator<?> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                if (!JSONABLE_TYPE.equals(key)) {
                    jsonObject = json.optJSONObject(key);
                    if (jsonObject != null)
                        break;
                }
            }
            if (jsonObject == null)
                throw new JSONException("Object not found for " + JSONABLE_TYPE + ": " + type);
        }
        Class<? extends Jsonable> clazz;
        Package pkg = getPackage();
        if (pkg == null) {
            pkg = PackageCache.getMdwBasePackage();
        }

        if (JSONObject.class.getName().equals(type) || JsonObject.class.getName().equals(type)) {
            return json;
        }
        else if (Jsonable.class.getName().equals(type)) {
            return new JsonableImpl(json);
        }
        else {
            // dynamically typed Jsonable implementation
            clazz = pkg.getClassLoader().loadClass(type).asSubclass(Jsonable.class);
            Constructor<? extends Jsonable> ctor = clazz.getConstructor(JSONObject.class);
            Jsonable jsonable = ctor.newInstance(jsonObject);
            return jsonable;
        }
    }
}
