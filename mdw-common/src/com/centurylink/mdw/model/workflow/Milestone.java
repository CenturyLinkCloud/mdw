package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.project.Data;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Date;

/**
 * Milestone definition.
 */
public class Milestone implements Linkable, Jsonable {

    public static final String MONITOR_CLASS = "com.centurylink.mdw.milestones.ActivityMilestone";

    public Milestone(Process process) {
        this.process = process;
    }

    public Milestone(Process process, Activity activity, String text) {
        this.process = process;
        this.activity = activity;
        this.label = activity.getName();
        if (text != null) {
            String l = parseLabel(text);
            if (l != null)
                this.label = l;
            String g = parseGroup(text);
            if (g != null) {
                this.group = g;
            }
            else if (Data.Implementors.START_IMPL.equals(activity.getImplementor())) {
                this.group = MilestoneFactory.START_GROUP.getName();
            }
            else if (Data.Implementors.STOP_IMPL.equals(activity.getImplementor())) {
                this.group = MilestoneFactory.STOP_GROUP.getName();
            }
            else if (Data.Implementors.PAUSE_IMPL.equals(activity.getImplementor())) {
                this.group = MilestoneFactory.PAUSE_GROUP.getName();
            }
        }
    }

    private Process process;
    public Process getProcess() { return process; }
    public void setProcess(Process process) { this.process = process; }

    private Activity activity;
    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }

    private String label;
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public static String parseLabel(String text) {
        String t = text.trim();
        if (!t.isEmpty()) {
            int bracket = t.indexOf('[');
            if (bracket > 0) {
                t = t.substring(0, bracket);
            }
            return t.trim().replaceAll("\\\\n", "\n");
        }
        return null;
    }

    private String group;
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public String parseGroup(String text) {
        String t = text.trim();
        if (!t.isEmpty()) {
            int bracket = t.indexOf('[');
            if (bracket >= 0) {
                String g = t.substring(bracket + 1);
                bracket = g.indexOf(']');
                if (bracket > 0)
                    g = g.substring(0, bracket);
                return g.trim();
            }
        }
        return null;
    }

    // below is for instance-level
    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String masterRequestId) { this.masterRequestId = masterRequestId; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private Instant start;
    public Instant getStart() { return start; }
    public void setStart(Instant start) { this.start = start; }

    private Instant end;
    public Instant getEnd() { return end; }
    public void setEnd(Instant end) { this.end = end; }

    private ProcessInstance processInstance;
    public ProcessInstance getProcessInstance() { return processInstance; }
    public void setProcessInstance(ProcessInstance processInstance) { this.processInstance = processInstance; }

    private ActivityInstance activityInstance;
    public ActivityInstance getActivityInstance() { return activityInstance; }
    public void setActivityInstance(ActivityInstance activityInstance) {
        this.activityInstance = activityInstance;
        this.start = new Date(activityInstance.getStartDate().getTime() + DatabaseAccess.getDbTimeDiff()).toInstant();
        if (activityInstance.getEndDate() != null) {
            this.end = new Date(activityInstance.getEndDate().getTime() + DatabaseAccess.getDbTimeDiff()).toInstant();
        }
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = create();
        json.put("process", process.getSummaryJson());
        if (activity != null)
            json.put("activity", activity.getSummaryJson());
        if (label != null)
            json.put("label", label);
        if (group != null)
            json.put("group", group);
        if (masterRequestId != null)
            json.put("masterRequestId", masterRequestId);
        if (status != null)
            json.put("status", status);
        if (start != null)
            json.put("start", start.toString());
        if (end != null)
            json.put("end", end.toString());
        if (processInstance != null)
            json.put("processInstance", processInstance.getSummaryJson());
        if (activityInstance != null)
            json.put("activityInstance", activityInstance.getSummaryJson());

        return json;
    }

    @Override
    public JSONObject getSummaryJson(int detail) {
        return getJson();
    }

    @Override
    public String getQualifiedLabel() {
        String qlabel = process.getQualifiedLabel();
        if (activity != null)
            qlabel += " " + activity.getLogicalId() + ":'";
        qlabel += label + "'";
        return qlabel;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Milestone))
            return false;
        Milestone otherMilestone = (Milestone) other;
        if (activityInstance != null) {
            return activityInstance.equals(otherMilestone.activityInstance);
        }
        return activity.equals(otherMilestone.activity);
    }
}
