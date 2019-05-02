package com.centurylink.mdw.milestones;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.monitor.ProcessMonitor;

@Monitor(value="Milestone", category=ProcessMonitor.class)
public class ProcessMilestone implements ProcessMonitor {
}
