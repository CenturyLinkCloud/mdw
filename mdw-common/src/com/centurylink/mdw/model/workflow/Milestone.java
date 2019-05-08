package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Jsonable;

import java.time.Instant;

/**
 * Milestone definition.
 */
public class Milestone implements Jsonable {

    private Activity activity;
    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }

    private Process process;
    public Process getProcess() { return process; }
    public void setProcess(Process process) { this.process = process; }

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

    private ActivityInstance activityInstance;
    public ActivityInstance getActivityInstance() { return activityInstance; }
    public void setActivityInstance(ActivityInstance activityInstance) { this.activityInstance = activityInstance; }

    private ProcessInstance processInstance;
    public ProcessInstance getProcessInstance() { return processInstance; }
    public void setProcessInstance(ProcessInstance processInstance) { this.processInstance = processInstance; }



}
