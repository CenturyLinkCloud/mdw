package com.centurylink.mdw.testing;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.monitor.AdapterMonitor;

@Monitor(value="Stubbing Interceptor", category=AdapterMonitor.class)
public class AdapterStubbingMonitor implements AdapterMonitor {
    // TODO: implement
}
