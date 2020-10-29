package com.centurylink.mdw.model;

import org.json.JSONException;
import org.json.JSONObject;

import io.limberest.json.Jsonator;
import io.swagger.annotations.ApiModelProperty;

/**
 * Extends Limberest Jsonable for MDW compatibility.
 */
public interface Jsonable extends io.limberest.json.Jsonable {

    default JSONObject getJson() throws JSONException {
        return new Jsonator(this) {
            @Override
            protected Object getJsonObject(Object o) {
                if (o != null && o.getClass().getName().equals("groovy.lang.MetaClassImpl"))
                    return null;
                return super.getJsonObject(o);
            }
        }.getJson(create());
    };

    /**
     * @return a JSONObject implementation with predictable property ordering.
     */
    default JSONObject create() {
        return new JsonObject();
    }

    /**
     * May be overridden to name the JSON object returned from {@link #getJson()}.
     */
    @ApiModelProperty(hidden=true)
    default String getJsonName() {
        return jsonName();
    }

}
