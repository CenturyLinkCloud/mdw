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

    public Milestone getMilestone(Activity activity) {
        String monitorsAttr = activity.getAttribute(WorkAttributeConstant.MONITORS);
        if (monitorsAttr != null) {
            MonitorAttributes monitorAttributes = new MonitorAttributes(monitorsAttr);
            if (monitorAttributes.isEnabled(Milestone.MONITOR_CLASS)) {
                String text = monitorAttributes.getOptions(Milestone.MONITOR_CLASS);
                if (text == null || text.trim().isEmpty())
                    text = activity.getName();
                else
                    text = text.replaceAll("\\\\n", "\n");
                return new Milestone(process, activity, text);
            }
        }
        return null;
    }
}
