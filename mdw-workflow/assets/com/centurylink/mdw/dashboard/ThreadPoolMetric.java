package com.centurylink.mdw.dashboard;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.container.plugin.CommonThreadPool;
import com.centurylink.mdw.model.report.Metric;
import com.centurylink.mdw.model.system.SystemMetric;

import java.util.ArrayList;
import java.util.List;

@RegisteredService(SystemMetric.class)
public class ThreadPoolMetric implements SystemMetric {

    @Override
    public String getName() {
        return "ThreadPool";
    }

    private CommonThreadPool threadPool;

    @Override
    public List<Metric> collect() {
        if (threadPool == null)
            threadPool = (CommonThreadPool)ApplicationContext.getThreadPoolProvider();

        List<Metric> metrics = new ArrayList<>();
        metrics.add(new Metric("active", "Active", threadPool.getActiveThreadCount()));
        metrics.add(new Metric("queued", "Queued", threadPool.getCurrentQueueSize()));
        metrics.add(new Metric("poolSize", "Pool Size", threadPool.getMaxThreadPoolSize()));
        return metrics;
    }
}
