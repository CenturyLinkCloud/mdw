package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.config.PropertyGroup;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.monitor.MonitorAttributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class MilestoneFactory {

    public static final List<PropertyGroup> DEFAULT_GROUPS = new ArrayList<>();
    public static final PropertyGroup START_GROUP;
    public static final PropertyGroup STOP_GROUP;
    public static final PropertyGroup PAUSE_GROUP;
    public static final PropertyGroup OTHER_GROUP;
    static {
        String startRoot = PropertyNames.MDW_MILESTONE_GROUPS + ".Start";
        Properties startProps = new Properties();
        startProps.setProperty(startRoot + ".color", "#aff7a2");
        START_GROUP = new PropertyGroup("Start", startRoot, startProps);
        DEFAULT_GROUPS.add(START_GROUP);

        String stopRoot = PropertyNames.MDW_MILESTONE_GROUPS + ".Stop";
        Properties stopProps = new Properties();
        stopProps.setProperty(stopRoot + ".color", "#f0928a");
        STOP_GROUP = new PropertyGroup("Stop", stopRoot, stopProps);
        DEFAULT_GROUPS.add(STOP_GROUP);

        String pauseRoot = PropertyNames.MDW_MILESTONE_GROUPS + ".Pause";
        Properties pauseProps = new Properties();
        pauseProps.setProperty(pauseRoot + ".color", "#fffc7c");
        PAUSE_GROUP = new PropertyGroup("Pause", pauseRoot, pauseProps);
        DEFAULT_GROUPS.add(PAUSE_GROUP);

        String otherRoot = PropertyNames.MDW_MILESTONE_GROUPS + ".Other";
        Properties otherProps = new Properties();
        otherProps.setProperty(otherRoot + ".color", "#4cafea");
        OTHER_GROUP = new PropertyGroup("Other", otherRoot, otherProps);
        DEFAULT_GROUPS.add(OTHER_GROUP);
    }

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
                return new Milestone(process, activity, text);
            }
        }
        return null;
    }
}
