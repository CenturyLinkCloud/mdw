package com.centurylink.mdw.services.system;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.WebSocketMessenger;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.report.MetricData;
import com.centurylink.mdw.model.report.MetricDataList;
import com.centurylink.mdw.model.system.SystemMetric;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.centurylink.mdw.config.PropertyManager.*;

public class SystemMetrics {

    private int period;      // capture interval (seconds)
    private int datapoints;  // datapoints to keep in memory
    private File outputDir;  // history output location
    private int outputSize;  // max output file size
    private String hostName;

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
            hostName = ApplicationContext.getHostname();
            int port = ApplicationContext.getServerPort();
            if (port != 0 && port != 8080)
                hostName += "_" + port;
            // seconds worth of data to keep in memory
            int retention = getIntegerProperty(PropertyNames.MDW_SYSTEM_METRICS_RETENTION, 3600);
            datapoints = retention / period;
            String logLoc = getProperty(PropertyNames.MDW_SYSTEM_METRICS_LOCATION);
            if (logLoc != null) {
                outputDir = new File(logLoc);
            }
            outputSize = getIntegerProperty(PropertyNames.MDW_SYSTEM_METRICS_BYTES, 10485760);
            systemMetrics = new TreeMap<>();
            metricDataLists = new HashMap<>();
            for (SystemMetric systemMetric :
                    MdwServiceRegistry.getInstance().getDynamicServices(SystemMetric.class)) {
                if (getBooleanProperty(PropertyNames.MDW_SYSTEM_METRICS_ENABLED + "." + systemMetric.getName(),
                        true)) {
                    systemMetrics.put(systemMetric.getName(), systemMetric);
                    metricDataLists.put(systemMetric.getName(), new MetricDataList(period, datapoints));
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
                        doCollect(time.minusSeconds(time.getSecond() % period).minusNanos(time.getNano()), systemMetric);
                        if (outputDir != null && (metricDataLists.get(systemMetric.getName()).getTotal() % datapoints == 0)) {
                            try {
                                doOutput(systemMetric, false);
                            }
                            catch (IOException ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    }
                }, period - (LocalDateTime.now().getSecond() % period), period, TimeUnit.SECONDS);
            }
        }
    }

    public synchronized void deactivate() {
        if (active) {
            active = false;
            scheduler.shutdown();
            for (String name : systemMetrics.keySet()) {
                try {
                    // log any remaining in-memory datapoints
                    doOutput(systemMetrics.get(name), true);
                }
                catch (IOException ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
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
            logger.trace("Collecting system metrics (" + time + "):"  + systemMetric.getName());

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

    private void doOutput(SystemMetric systemMetric, boolean isShutdown) throws IOException {
        if (!outputDir.isDirectory()) {
            outputDir.mkdirs();
        }
        String rootName = systemMetric.getName() + "_" + hostName;
        File file = new File(outputDir + "/" + rootName + ".csv");
        if (file.length() > outputSize && !isShutdown) {
            Files.copy(file.toPath(), new File(outputDir + "/" + rootName + "_old.csv").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.write(file.toPath(), new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
        }

        MetricDataList dataList = metricDataLists.get(systemMetric.getName());
        if (!file.isFile())
            Files.write(file.toPath(), dataList.getHeadings().getBytes(), StandardOpenOption.CREATE_NEW);
        else if (file.length() == 0)
            Files.write(file.toPath(), dataList.getHeadings().getBytes(), StandardOpenOption.APPEND);

        if (isShutdown) {
            Files.write(file.toPath(), dataList.getCsv(dataList.getTotal() % datapoints).getBytes(),
                    StandardOpenOption.APPEND);
        }
        else {
            Files.write(file.toPath(), dataList.getCsv().getBytes(), StandardOpenOption.APPEND);
        }
    }
}
