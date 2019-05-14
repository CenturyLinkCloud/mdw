package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONObject;

import java.time.Instant;

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
        this.text = text;
    }

    private Process process;
    public Process getProcess() { return process; }
    public void setProcess(Process process) { this.process = process; }

    private Activity activity;
    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }

    private String text;
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

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

    private Instant finish;
    public Instant getFinish() { return finish; }
    public void setFinish(Instant finish) { this.finish = finish; }

    private ProcessInstance processInstance;
    public ProcessInstance getProcessInstance() { return processInstance; }
    public void setProcessInstance(ProcessInstance processInstance) { this.processInstance = processInstance; }

    private ActivityInstance activityInstance;
    public ActivityInstance getActivityInstance() { return activityInstance; }
    public void setActivityInstance(ActivityInstance activityInstance) { this.activityInstance = activityInstance; }

    @Override
    public JSONObject getJson() {
        JSONObject json = create();
        json.put("process", process.getSummaryJson());
        if (activity != null)
            json.put("activity", activity.getSummaryJson());
        if (text != null)
            json.put("text", text);
        if (masterRequestId != null)
            json.put("masterRequestId", masterRequestId);
        if (status != null)
            json.put("status", status);
        if (start != null)
            json.put("start", start.toString());
        if (finish != null)
            json.put("finish", finish.toString());
        if (processInstance != null)
            json.put("processInstance", processInstance.getSummaryJson());
        if (activityInstance != null)
            json.put("activityInstance", activityInstance.getSummaryJson());

        return json;
    }

    @Override
    public JSONObject getSummaryJson() {
        return getJson();
    }

    @Override
    public String getQualifiedLabel() {
        return text;
    }
}
