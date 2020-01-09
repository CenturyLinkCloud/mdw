package com.centurylink.mdw.servicenow;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a ServiceNow incident.  Used to build the API request for creating an incident.
 */
public class Incident implements Jsonable {

    public Incident() {
    }

    public Incident(JSONObject json) {

        shortDescription = json.optString("short_description", null);
        description = json.optString("description", null);
        assignmentGroup = json.optString("assignment_group", null);
        if (json.has("group_list"))
            groupList = Arrays.asList(json.getString("group_list").split(","));
        if (json.has("impact"))
            impact = Level.of(json.getInt("impact"));
        if (json.has("urgency"))
            urgency = Level.of(json.getInt("urgency"));
        category = json.optString("category", null);
        caller = json.optString("caller_id", null);
        if (json.has("due_date"))
            dueDate = new GlideDateTime(json.getString("due_date")).getLocalDateTime();
        correlationId = json.optString("correlation_id", null);
        correlationDisplay = json.optString("correlation_display", null);
    }

    public enum Level {
        High(1),
        Medium(2),
        Low(3);

        public final int level;
        Level(int level) {
            this.level = level;
        }

        public static Level of(int level) {
            for (Level l : Level.values()) {
                if (l.level == level)
                    return l;
            }
            return null;
        }

        public String toString() {
            return level + " - " + super.toString();
        }
    }

    /**
     * Maps to "Task Name" in MDW task
     */
    private String shortDescription;
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    private String description;
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /**
     * Ref corresponding to MDW workgroup (attribute of Workgroup)
     */
    private String assignmentGroup;
    public String getAssignmentGroup() { return assignmentGroup; }
    public void setAssignmentGroup(String assignmentGroup) { this.assignmentGroup = assignmentGroup; }

    /**
     * List of group refs if multiple
     */
    private List<String> groupList;
    public List<String> getGroupList() { return groupList; }
    public void setGroupList(List<String> groupList) { this.groupList = groupList; }

    /**
     * Effect incident has on business
     */
    private Level impact;
    public Level getImpact() { return impact; }
    public void setImpact(Level impact) { this.impact = impact; }

    /**
     * Extent to which the incident's resolution can bear delay
     * (maps to "Priority" of MDW tasks)
     */
    private Level urgency;
    public Level getUrgency() { return urgency; }
    public void setUrgency(Level urgency) { this.urgency = urgency; }

    /**
     * How quickly the service desk should address the incident
     * (calculated based on impact and urgency)
     * <a href="https://docs.servicenow.com/bundle/istanbul-it-service-management/page/product/incident-management/reference/r_PrioritizationOfIncidents.html">https://docs.servicenow.com/bundle/istanbul-it-service-management/page/product/incident-management/reference/r_PrioritizationOfIncidents.html</a>
     */
    private int priority;
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    private String category;
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    private LocalDateTime dueDate;
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    /**
     * Populated with the MDW task instance id or process instance id
     */
    private String correlationId;
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    /**
     * Either "Process Instance" or "Task Instance"
     */
    private String correlationDisplay;
    public String getCorrelationDisplay() { return correlationDisplay; }
    public void setCorrelationDisplay(String correlationDisplay) { this.correlationDisplay = correlationDisplay; }

    /**
     * Name of MDW User who is considered incident creator
     */
    private String caller;
    public String getCaller() { return caller; }
    public void setCaller(String caller) { this.caller = caller; }

    @Override
    public JSONObject getJson() {
        JSONObject json = create();
        json.put("short_description", shortDescription);
        json.putOpt("description", description);
        json.putOpt("assignment_group", assignmentGroup);
        if (groupList != null && !groupList.isEmpty())
            json.put("group_list", String.join(",", groupList));
        if (impact != null)
            json.put("impact", impact.level);
        if (urgency != null)
            json.put("urgency", urgency.level);
        json.putOpt("category", category);
        json.putOpt("caller_id", caller);
        if (dueDate != null)
            json.put("due_date", new GlideDateTime(dueDate).toString());
        json.putOpt("correlation_id", correlationId);
        json.putOpt("correlation_display", correlationDisplay);

        return json;
    }

}
