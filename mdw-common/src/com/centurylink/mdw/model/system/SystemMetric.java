package com.centurylink.mdw.model.system;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.report.Metric;

import java.util.List;

public interface SystemMetric extends RegisteredService {

    String getName();

    /**
     * Executed at intervals to collect metrics.
     */
    List<Metric> collect();
}
