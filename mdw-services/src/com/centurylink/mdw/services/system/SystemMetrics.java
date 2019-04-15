package com.centurylink.mdw.services.system;

import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.report.MetricData;
import com.centurylink.mdw.model.report.MetricDataList;
import com.centurylink.mdw.model.system.SystemMetric;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.centurylink.mdw.model.report.MetricDataList.PERIOD;

public class SystemMetrics {

    private static final int RETAIN = 3600 / PERIOD;  // one hour of data

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static volatile SystemMetrics instance;
    public static SystemMetrics getInstance() {
        if (instance == null) {
            synchronized (SystemMetrics.class) {
                if (instance == null) {
                    instance = new SystemMetrics();
                    instance.initialize();
                }
            }
        }
        return instance;
    }

    private SystemMetrics() {}

    private Map<String,SystemMetric> systemMetrics;
    private Map<String,MetricDataList> metricDataLists;
    private ScheduledExecutorService scheduler;

    private void initialize() {
        systemMetrics = new TreeMap<>();
        metricDataLists = new HashMap<>();
        for (SystemMetric systemMetric :
                MdwServiceRegistry.getInstance().getDynamicServices(SystemMetric.class)) {
            systemMetrics.put(systemMetric.getName(), systemMetric);
            metricDataLists.put(systemMetric.getName(), new MetricDataList(RETAIN));
        }
        scheduler = Executors.newScheduledThreadPool(systemMetrics.size());
    }

    private boolean active;
    public boolean isActive() { return active; }
    public synchronized void activate() {
        if (!active) {
            active = true;
            for (String name : systemMetrics.keySet()) {
                final SystemMetric systemMetric = systemMetrics.get(name);
                scheduler.scheduleAtFixedRate(() -> {
                    if (active) {
                        LocalDateTime time = LocalDateTime.now();
                        doCollect(time.minusSeconds(time.getSecond() % PERIOD), systemMetric);
                    }
                }, PERIOD - (LocalDateTime.now().getSecond() % PERIOD), PERIOD, TimeUnit.SECONDS);
            }
        }
    }
    public synchronized void deactivate() {
        if (active) {
            active = false;
            scheduler.shutdown();
        }
    }

    public MetricDataList getData(String name) throws ServiceException {
        SystemMetric systemMetric = systemMetrics.get(name);
        if (systemMetric == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "SystemMetric not found: " + name);
        if (!isActive()) {
            activate();
        }
        return metricDataLists.get(name);
    }

    private void doCollect(LocalDateTime time, SystemMetric systemMetric) {
        if (logger.isTraceEnabled())
            logger.trace("Collecting system metrics: " + systemMetric.getName());

        MetricData data = new MetricData(time, systemMetric.collect());
        metricDataLists.get(systemMetric.getName()).add(data);
    }
}
