package com.centurylink.mdw.model;

import com.centurylink.mdw.common.service.Query;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Exportable JSON services.
 */
public interface JsonExportable {

    /**
     * The exported row-wise JSON content.
     */
    default Jsonable toExportJson(Query query, JSONObject json) {
        return toJsonable(query, json);
    }

    default String getExportName() { return null; }

    /**
     * Return null if no filter output desired.
     */
    default JSONObject getExportFilters(Query query) {
        JSONObject json = null;;
        if (!query.getFilters().isEmpty()) {
            for (String key : query.getFilters().keySet()) {
                if (!key.equals("NoPersistence")) {
                    if (json == null)
                        json = new JsonObject();
                    json.put(key, query.getFilter(key));
                }
            }
        }
        return json;
    }

    /**
     * @deprecated implement {@link #toExportJson(Query, JSONObject)}
     */
    @Deprecated
    default Jsonable toJsonable(Query query, JSONObject json) throws JSONException {
        return null;
    }
}
