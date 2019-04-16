package com.centurylink.mdw.dashboard;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.model.report.Metric;
import com.centurylink.mdw.model.system.SystemMetric;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

@RegisteredService(SystemMetric.class)
public class CpuMetric implements SystemMetric {

    @Override
    public String getName() {
        return "CPU";
    }

    private OperatingSystemMXBean osMxBean;

    @Override
    public List<Metric> collect() {
        if (osMxBean == null)
            osMxBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long process = Math.round(osMxBean.getProcessCpuLoad() * 100);
        long other = Math.round(osMxBean.getSystemCpuLoad() * 100) - process;
        List<Metric> metrics = new ArrayList<>();
        metrics.add(new Metric("jvm", "JVM", process));
        metrics.add(new Metric("other", "Other", other));
        return metrics;
    }
}
