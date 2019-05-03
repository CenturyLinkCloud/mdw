package com.centurylink.mdw.milestones;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.monitor.ActivityMonitor;

@Monitor(value="Milestone", category=ActivityMonitor.class)
public class ActivityMilestone implements ActivityMonitor {
}
