package com.centurylink.mdw.translator;

import java.lang.reflect.Constructor;
import java.util.Iterator;

import com.centurylink.mdw.model.JsonObject;
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
        else {
            // dynamically typed Jsonable implementation
            clazz = pkg.getClassLoader().loadClass(type).asSubclass(Jsonable.class);
            Constructor<? extends Jsonable> ctor = clazz.getConstructor(JSONObject.class);
            Jsonable jsonable = ctor.newInstance(jsonObject);
            return jsonable;
        }
    }
}
