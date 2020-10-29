package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.Attributes;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Yamlable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class TextNote implements Jsonable, Yamlable {

    private String content;
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    private String reference;
    public String getReference() {
        return reference;
    }
    public void setReference(String reference) {
        this.reference = reference;
    }

    private Attributes attributes;
    public Attributes getAttributes() { return attributes; }
    public String getAttribute(String name) {
        return attributes == null ? null : attributes.get(name);
    }
    public void setAttribute(String name, String value) {
        if (attributes == null)
            attributes = new Attributes();
        attributes.put(name, value);
    }

    public String getLogicalId() {
        return getAttribute(WorkAttributeConstant.LOGICAL_ID);
    }
    public void setLogicalId(String id) {
        setAttribute(WorkAttributeConstant.LOGICAL_ID, id);
    }

    public TextNote() {

    }

    public TextNote(JSONObject json) throws JSONException {
        this.content = json.getString("content");
        if (json.has("attributes"))
            this.attributes = new Attributes(json.getJSONObject("attributes"));
        setLogicalId(json.getString("id"));
    }

    @SuppressWarnings("unchecked")
    public TextNote(Map<String,Object> yaml) {
        this.content = (String)yaml.get("content");
        if (yaml.containsKey("attributes"))
            this.attributes = new Attributes((Map<String,Object>)yaml.get("attributes"));
        setLogicalId((String)yaml.get("id"));
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("id", getLogicalId());
        json.put("content", content);
        if (attributes != null && !attributes.isEmpty())
            json.put("attributes", attributes.clean().getJson());
        return json;
    }

    @Override
    public Map<String,Object> getYaml() {
        Map<String,Object> yaml = Yamlable.create();
        yaml.put("id", getLogicalId());
        yaml.put("content", content);
        if (attributes != null && !attributes.isEmpty())
            yaml.put("attributes", attributes.clean());
        return yaml;
    }
}
