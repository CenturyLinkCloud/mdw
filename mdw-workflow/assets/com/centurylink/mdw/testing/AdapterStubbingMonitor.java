package com.centurylink.mdw.testing;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.monitor.AdapterMonitor;

/**
 * TODO: https://github.com/CenturyLinkCloud/mdw/issues/518
 */
@Monitor(value="Stubbing Interceptor", category=AdapterMonitor.class)
public class AdapterStubbingMonitor implements AdapterMonitor {
}
