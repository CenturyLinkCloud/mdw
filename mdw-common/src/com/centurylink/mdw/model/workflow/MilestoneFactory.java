package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.monitor.MonitorAttributes;

import java.util.Date;

public class MilestoneFactory {

    private Process process;

    public MilestoneFactory(Process process) {
        this.process = process;
    }

    public Linked<Milestone> start() {
        return new Linked<>(new Milestone(process, process.getStartActivity(), "Start"));
    }

    public Milestone createMilestone(ProcessInstance processInstance) {
        Milestone milestone = new Milestone(process);
        milestone.setProcessInstance(processInstance);
        milestone.setMasterRequestId(processInstance.getMasterRequestId());
        return milestone;
    }

    public Milestone createMilestone(ProcessInstance processInstance, ActivityInstance activityInstance, String text) {
        Activity activity = process.getActivity(activityInstance.getActivityId());
        Milestone milestone = new Milestone(process, activity, text);
        milestone.setProcessInstance(processInstance);
        milestone.setActivityInstance(activityInstance);
        milestone.setMasterRequestId(processInstance.getMasterRequestId());
        Date startDate = new Date(activityInstance.getStartDate().getTime() + DatabaseAccess.getDbTimeDiff());
        milestone.setStart(startDate.toInstant());
        if (activityInstance.getEndDate() != null) {
            Date endDate = new Date(activityInstance.getEndDate().getTime() + DatabaseAccess.getDbTimeDiff());
            milestone.setEnd(endDate.toInstant());
        }
        milestone.setStatus(activityInstance.getStatus());
        return milestone;
    }

    /**
     * Linked milestones for process instance.
     * @return new parent
     */
    public Linked<Milestone> addMilestones(Linked<Milestone> parent, ProcessInstance processInstance) {
        return addMilestones(parent, processInstance, processInstance.getLinkedActivities(process));
    }

    /**
     * @return new parent
     */
    public Linked<Milestone> addMilestones(Linked<Milestone> parent, ProcessInstance processInstance,
            Linked<ActivityInstance> start) {
        ActivityInstance activityInstance = start.get();
        Activity activity = process.getActivity(activityInstance.getActivityId());
        Milestone milestone = getMilestone(activity);
        Linked<Milestone> newParent = parent;
        if (milestone != null) {
            Linked<Milestone> linkedMilestone = new Linked<>(milestone);
            linkedMilestone.setParent(parent);
            parent.getChildren().add(linkedMilestone);
            for (Linked<ActivityInstance> child : start.getChildren()) {
                newParent = addMilestones(linkedMilestone, processInstance, child);
            }
        }
        else {
            for (Linked<ActivityInstance> child : start.getChildren()) {
                newParent = addMilestones(parent, processInstance, child);
            }
        }
        return newParent;
    }

    /**
     * Linked milestones for this process definition.
     * @return new parent
     */
    public Linked<Milestone> addMilestones(Linked<Milestone> parent) {
        return addMilestones(parent, process.getLinkedActivities());
    }

    /**
     * @return new parent
     */
    public Linked<Milestone> addMilestones(Linked<Milestone> parent, Linked<Activity> start) {
        Activity activity = start.get();
        Milestone milestone = getMilestone(activity);
        if (milestone != null) {
            Linked<Milestone> linkedMilestone = new Linked<>(milestone);
            linkedMilestone.setParent(parent);
            parent.getChildren().add(linkedMilestone);
            for (Linked<Activity> child : start.getChildren()) {
                return addMilestones(linkedMilestone, child);
            }
            return linkedMilestone;
        }
        else {
            for (Linked<Activity> child : start.getChildren()) {
                return addMilestones(parent, child);
            }
            return parent;
        }
    }

    private Milestone getMilestone(Activity activity) {
        String monitorsAttr = activity.getAttribute(WorkAttributeConstant.MONITORS);
        if (monitorsAttr != null) {
            MonitorAttributes monitorAttributes = new MonitorAttributes(monitorsAttr);
            if (monitorAttributes.isEnabled(Milestone.MONITOR_CLASS)) {
                String text = monitorAttributes.getOptions(Milestone.MONITOR_CLASS);
                if (text == null || text.trim().isEmpty())
                    text = activity.oneLineName();
                return new Milestone(process, activity, text);
            }
        }
        return null;
    }
}
