package com.centurylink.mdw.dashboard;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.model.report.Metric;
import com.centurylink.mdw.model.system.SystemMetric;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.commons.dbcp2.BasicDataSource;

import java.util.ArrayList;
import java.util.List;

@RegisteredService(SystemMetric.class)
public class DbConnectionsMetric  implements SystemMetric {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public String getName() {
        return "DbConnections";
    }

    private Exception exception; // avoid repeated attempts
    private BasicDataSource dataSource;

    @Override
    public List<Metric> collect() {
        if (exception != null)
            return new ArrayList<>();

        if (dataSource == null) {
            try {
                dataSource = (BasicDataSource)SpringAppContext.getInstance().getBean("MDWDataSource");
            }
            catch (Exception ex) {
                exception = ex;
                logger.error(ex.getMessage(), ex);
            }
        }

        List<Metric> metrics = new ArrayList<>();
        if (dataSource != null) {
            metrics.add(new Metric("active", "Active", dataSource.getNumActive()));
            metrics.add(new Metric("idle", "Idle", dataSource.getNumIdle()));
            metrics.add(new Metric("poolSize", "Pool Size", dataSource.getMaxTotal()));
        }
        return metrics;
    }
}
