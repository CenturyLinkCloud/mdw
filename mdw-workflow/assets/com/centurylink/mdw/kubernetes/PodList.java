package com.centurylink.mdw.kubernetes;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Not serializable.
 */
public class PodList implements Jsonable {

    private List<Pod> pods;
    public List<Pod> getPods() { return pods; }

    public PodList(JSONObject json) {
        if (json.has("items")) {
            pods = new ArrayList<>();
            JSONArray items = json.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                pods.add(new Pod(items.getJSONObject(i)));
            }
        }
    }

    @Override
    public JSONObject getJson() {
        throw new JSONException("Not serializable");
    }

}
