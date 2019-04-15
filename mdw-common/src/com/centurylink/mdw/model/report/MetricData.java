package com.centurylink.mdw.model.report;

import java.time.LocalDateTime;
import java.util.List;

public class MetricData implements Comparable<MetricData> {

    private LocalDateTime time;
    public LocalDateTime getTime() { return time; }

    private List<Metric> metrics;
    public List<Metric> getMetrics() { return metrics; }

    public MetricData(LocalDateTime time, List<Metric> metrics) {
        this.time = time;
        this.metrics = metrics;
    }

    @Override
    public int compareTo(MetricData other) {
        return time.compareTo(other.time);
    }
}
