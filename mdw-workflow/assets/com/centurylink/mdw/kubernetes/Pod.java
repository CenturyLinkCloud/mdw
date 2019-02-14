package com.centurylink.mdw.kubernetes;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Not serializable.
 */
public class Pod implements Jsonable, Comparable<Pod> {

    private String name;
    public String getName() { return name; }

    private String namespace;
    public String getNamespace() { return namespace; }

    public Pod(JSONObject json) {

        if (json.has("metadata")) {
            JSONObject metadata = json.getJSONObject("metadata");
            if (metadata.has("name"))
                this.name = metadata.getString("name");
            if (metadata.has("namespace"))
                this.namespace = metadata.getString("namespace");
        }
    }

    @Override
    public JSONObject getJson() {
        throw new JSONException("Not serializable");
    }

    @Override
    public int compareTo(Pod other) {
        if (this.namespace.equals(other.namespace)) {
            return this.name.compareToIgnoreCase(other.name);
        }
        else {
            return this.namespace.compareToIgnoreCase(other.namespace);
        }
    }
}
