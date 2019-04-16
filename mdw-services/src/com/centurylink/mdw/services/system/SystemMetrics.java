package com.centurylink.mdw.services.system;

import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.WebSocketMessenger;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.report.MetricData;
import com.centurylink.mdw.model.report.MetricDataList;
import com.centurylink.mdw.model.system.SystemMetric;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.centurylink.mdw.config.PropertyManager.getBooleanProperty;
import static com.centurylink.mdw.config.PropertyManager.getIntegerProperty;

public class SystemMetrics {

    private int period;

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
        period = getIntegerProperty(PropertyNames.MDW_SYSTEM_METRICS_PERIOD, 5);
        if (period > 0) {
            int retain = getIntegerProperty(PropertyNames.MDW_SYSTEM_METRICS_RETENTION, 3600) / period;
            systemMetrics = new TreeMap<>();
            metricDataLists = new HashMap<>();
            for (SystemMetric systemMetric :
                    MdwServiceRegistry.getInstance().getDynamicServices(SystemMetric.class)) {
                if (getBooleanProperty(PropertyNames.MDW_SYSTEM_METRICS_ENABLED + "." + systemMetric.getName(),
                        true)) {
                    systemMetrics.put(systemMetric.getName(), systemMetric);
                    metricDataLists.put(systemMetric.getName(), new MetricDataList(period, retain));
                }
            }
            scheduler = Executors.newScheduledThreadPool(systemMetrics.size());
        }
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
                        doCollect(time.minusSeconds(time.getSecond() % period), systemMetric);
                    }
                }, period - (LocalDateTime.now().getSecond() % period), period, TimeUnit.SECONDS);
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
        MetricDataList dataList = metricDataLists.get(systemMetric.getName());
        dataList.add(data);
        try {
            WebSocketMessenger.getInstance().send("/System/metrics/" + systemMetric.getName(),
                    dataList.getJson(300).toString()); // TODO param
        } catch (IOException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }
}
