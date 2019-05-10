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

    public Milestone createMilestone(ProcessInstance processInstance, ActivityInstance activityInstance, String text) {
        Activity activity = process.getActivity(activityInstance.getActivityId());
        Milestone milestone = new Milestone(process, activity);
        milestone.setProcessInstance(processInstance);
        milestone.setActivityInstance(activityInstance);
        milestone.setMasterRequestId(processInstance.getMasterRequestId());
        Date startDate = new Date(activityInstance.getStartDate().getTime() + DatabaseAccess.getDbTimeDiff());
        milestone.setStart(startDate.toInstant());
        if (activityInstance.getEndDate() != null) {
            Date endDate = new Date(activityInstance.getEndDate().getTime() + DatabaseAccess.getDbTimeDiff());
            milestone.setFinish(endDate.toInstant());
        }
        milestone.setStatus(activityInstance.getStatus());
        milestone.setText(text);
        return milestone;
    }

    public void addMilestones(Linked<Milestone> parent, ProcessInstance processInstance) {
        addMilestones(parent, processInstance, processInstance.getLinkedActivities(process));
    }

    public void addMilestones(Linked<Milestone> parent, ProcessInstance processInstance,
            Linked<ActivityInstance> start) {
        for (Linked<ActivityInstance> childActivity : start.getChildren()) {
            ActivityInstance activityInstance = childActivity.get();
            Activity activity = process.getActivity(activityInstance.getActivityId());
            String monitorsAttr = activity.getAttribute(WorkAttributeConstant.MONITORS);
            if (monitorsAttr != null) {
                MonitorAttributes monitorAttributes = new MonitorAttributes(monitorsAttr);
                if (monitorAttributes.isEnabled(Milestone.MONITOR_CLASS)) {
                    String text = monitorAttributes.getOptions(Milestone.MONITOR_CLASS);
                    if (text == null)
                        text = activity.oneLineName();
                    Milestone milestone = createMilestone(processInstance, activityInstance, text);
                    Linked<Milestone> child = new Linked<>(milestone);
                    child.setParent(parent);
                    parent.getChildren().add(child);
                    addMilestones(child, processInstance, childActivity);
                }
            }
            else {
                addMilestones(parent, processInstance, childActivity);
            }
        }
    }

}
