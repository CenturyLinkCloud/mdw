package com.centurylink.mdw.base;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.monitor.ProcessMonitor;

/**
 * TODO: Implement this ().
 */

@Monitor(value="Due Date Monitor", category=ProcessMonitor.class, defaultOptions="24 hours")
public class ProcessDueDateMonitor {
}
