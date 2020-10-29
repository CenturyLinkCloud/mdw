package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.constant.ActivityResultCodeConstant;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.Attributes;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Yamlable;
import com.centurylink.mdw.model.event.EventType;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class Transition implements Jsonable, Yamlable {

    private Long id;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    private Long fromId;
    public Long getFromId() {
        return fromId;
    }
    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    private Long toId;
    public Long getToId() {
        return toId;
    }
    public void setToId(Long toId) {
        this.toId = toId;
    }

    private Integer eventType;
    public Integer getEventType() {
        return eventType;
    }
    public void setEventType(Integer eventType) {
        this.eventType = eventType;
    }

    private String completionCode;
    public String getCompletionCode() {
        return completionCode;
    }
    public void setCompletionCode(String completionCode) {
        this.completionCode = completionCode;
    }

    private Long processId;
    public Long getProcessId(){
        return this.processId;
    }
    public void setProcessId(Long processId){
        this.processId = processId;
    }

    private Attributes attributes;
    public String getAttribute(String name) {
        return attributes == null ? null : attributes.get(name);
    }
    public void setAttribute(String name, String value) {
        if (attributes == null)
            attributes = new Attributes();
        attributes.put(name, value);
    }

    public Transition() {
    }

    public Transition(Long id, Long fromId, Long toId,
        Integer eventType, String completionCode, String pValClassName, Attributes attributes) {
        this.id = id;
        this.fromId = fromId;
        this.toId = toId;
        this.eventType = eventType;
        this.attributes = attributes;
        this.completionCode = completionCode;
    }

    public String toString() {
        return getJson().toString(2);
    }

    public boolean match(Integer eventType, String compCode) {
        if (!eventType.equals(this.eventType))
            return false;
        if (this.completionCode == null)
            return compCode==null;
        return this.completionCode.equals(compCode);
    }

    public boolean isDefaultTransition(boolean noLabelIsDefault) {
        if (noLabelIsDefault)
            return this.completionCode==null && this.eventType.equals(EventType.FINISH);
        else return this.completionCode!=null &&
            this.completionCode.equals(ActivityResultCodeConstant.RESULT_DEFAULT)
            && this.eventType.equals(EventType.FINISH);

    }

    public String getLabel() {
        if (eventType.equals(EventType.FINISH)) return completionCode;
        if (completionCode==null) return EventType.getEventTypeName(eventType);
        return EventType.getEventTypeName(eventType) + ":" + completionCode;
    }

    public String getLogicalId() {
        return getAttribute(WorkAttributeConstant.LOGICAL_ID);
    }

    public boolean isHidden() {
        String logicalId = getLogicalId();
        return logicalId != null && logicalId.startsWith("H");
    }

    /**
     * Does not set fromWorkId since JSON transitions are children of activities.
     */
    public Transition(JSONObject json) throws JSONException {
        String logicalId = json.getString("id");
        if (logicalId.startsWith("Transition"))
            logicalId = "T" + logicalId.substring(10);
        id = Long.valueOf(logicalId.substring(1));
        this.toId = Long.parseLong(json.getString("to").substring(1));
        if (json.has("resultCode") && json.getString("resultCode").length() > 0)
            this.completionCode = json.getString("resultCode");
        if (json.has("event"))
            this.eventType = EventType.getEventTypeFromName(json.getString("event"));
        else
            this.eventType = EventType.FINISH;
        if (json.has("attributes"))
            this.attributes = new Attributes(json.getJSONObject("attributes"));
        setAttribute(WorkAttributeConstant.LOGICAL_ID, logicalId);
    }

    @SuppressWarnings("unchecked")
    public Transition(Map<String,Object> yaml) {
        String logicalId = (String)yaml.get("id");
        if (logicalId.startsWith("Transition"))
            logicalId = "T" + logicalId.substring(10);
        id = Long.valueOf(logicalId.substring(1));
        this.toId = Long.parseLong(((String)yaml.get("to")).substring(1));
        if (yaml.containsKey("resultCode") && yaml.get("resultCode").toString().length() > 0)
            this.completionCode = (String)yaml.get("resultCode");
        if (yaml.containsKey("event"))
            this.eventType = EventType.getEventTypeFromName((String)yaml.get("event"));
        else
            this.eventType = EventType.FINISH;
        if (yaml.containsKey("attributes"))
            this.attributes = new Attributes((Map<String,Object>)yaml.get("attributes"));
        setAttribute(WorkAttributeConstant.LOGICAL_ID, logicalId);
    }

    /**
     * Does not populate from field since JSON transitions are children of activities.
     */
    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("id", getLogicalId());
        json.put("to", "A" + toId);
        if (completionCode != null)
            json.put("resultCode", completionCode);
        if (eventType != null)
            json.put("event", EventType.getEventTypeName(eventType));
        if (attributes != null && ! attributes.isEmpty())
            json.put("attributes", attributes.clean().getJson());
        return json;
    }

    @Override
    public Map<String,Object> getYaml() {
        Map<String,Object> yaml = Yamlable.create();
        yaml.put("id", getLogicalId());
        yaml.put("to", "A" + toId);
        if (eventType != null)
            yaml.put("event", EventType.getEventTypeName(eventType));
        if (completionCode != null)
            yaml.put("resultCode", completionCode);
        if (attributes != null && ! attributes.isEmpty()) {
            yaml.put("attributes", attributes.clean());
        }
        return yaml;
    }
}
