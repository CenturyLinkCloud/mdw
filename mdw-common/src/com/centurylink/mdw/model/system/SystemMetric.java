package com.centurylink.mdw.model.system;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.report.Metric;

import java.util.List;

public interface SystemMetric extends RegisteredService {

    /**
     * Should match path segment[2] on REST request.
     * And this path is the same used for websocket subscribe.
     */
    String getName();

    /**
     * Executed at intervals to collect metrics.
     */
    List<Metric> collect();

    default boolean isEnabled() { return false; }
}
