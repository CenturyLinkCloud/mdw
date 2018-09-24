package com.centurylink.mdw.base;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.monitor.ProcessMonitor;

/**
 * TODO: https://github.com/CenturyLinkCloud/mdw/issues/516
 */
@Monitor(value="Due Date Monitor", category=ProcessMonitor.class)
public class ProcessDueDateMonitor implements ProcessMonitor {
}
