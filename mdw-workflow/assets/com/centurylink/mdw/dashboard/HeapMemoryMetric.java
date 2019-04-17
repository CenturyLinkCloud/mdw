package com.centurylink.mdw.dashboard;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.model.report.Metric;
import com.centurylink.mdw.model.system.SystemMetric;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

@RegisteredService(SystemMetric.class)
public class HeapMemoryMetric implements SystemMetric {

    @Override
    public String getName() {
        return "HeapMemory";
    }

    private MemoryMXBean memoryMxBean;

    @Override
    public List<Metric> collect() {
        if (memoryMxBean == null)
            memoryMxBean = ManagementFactory.getMemoryMXBean();

        MemoryUsage heapUsage = memoryMxBean.getHeapMemoryUsage();
        List<Metric> metrics = new ArrayList<>();
        metrics.add(new Metric("used", "Used", heapUsage.getUsed() / 1000000));
        metrics.add(new Metric("heap", "Heap", heapUsage.getCommitted() / 1000000));
        return metrics;
    }
}
