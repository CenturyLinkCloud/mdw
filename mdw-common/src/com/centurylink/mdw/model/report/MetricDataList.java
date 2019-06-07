package com.centurylink.mdw.model.report;

import com.centurylink.mdw.model.JsonObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rolling, fixed size list for holding metric data.
 */
public class MetricDataList {

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private int period;    // capture interval
    private int max;       // max datapoints to keep in memory
    private int total;     // total datapoints collected
    public int getTotal() { return total; }

    private List<MetricData> dataList;

    public MetricDataList(int period, int max) {
        this.period = period;
        this.max = max;
        dataList = new ArrayList<>();
    }

    public void add(MetricData metricData) {
        while (dataList.size() >= max) {
            dataList.remove(0);
        }
        dataList.add(metricData);
        total++;
    }

    public MetricData get(int i) {
        return dataList.get(i);
    }

    public JSONObject getJson(int span) {
        JSONObject json = new JsonObject();
        for (MetricData metricData : getData(span)) {
            JSONArray jsonArray = new JSONArray();
            for (Metric metric : metricData.getMetrics()) {
                jsonArray.put(metric.getJson());
            }
            json.put(timeFormatter.format(metricData.getTime()), jsonArray);
        }
        return json;
    }

    /**
     * Accumulated averages.
     */
    public List<Metric> getAverages(int span) {
        Map<String,Metric> accum = new LinkedHashMap<>();
        int count = span / period;
        if (count > dataList.size())
            count = dataList.size();
        for (int i = dataList.size() - count; i < dataList.size(); i++) {
            MetricData metricData = dataList.get(i);
            for (Metric metric : metricData.getMetrics()) {
                Metric total = accum.get(metric.getName());
                if (total == null) {
                    total = new Metric(metric.getId(), metric.getName(), metric.getValue());
                    accum.put(metric.getName(), total);
                }
                else {
                    total.setValue(total.getValue() + metric.getValue());
                }
            }
        }
        for (Metric metric : accum.values()) {
            metric.setValue(Math.round((double)metric.getValue() / count));
        }
        return new ArrayList<>(accum.values());
    }

    /**
     * Returns a left-padded list.
     */
    public List<MetricData> getData(int span) {
        int count = span / period;
        if (dataList.size() < count) {
            if (dataList.isEmpty()) {
                return dataList;
            }
            else {
                // left-pad
                List<MetricData> padded = new ArrayList<>(dataList);
                MetricData first = dataList.get(0);
                List<Metric> pads = new ArrayList<>();
                for (Metric metric : first.getMetrics()) {
                    pads.add(new Metric(metric.getId(), metric.getName(), 0));
                }
                LocalDateTime time = first.getTime();
                while (padded.size() < count) {
                    time = time.minusSeconds(period);
                    padded.add(0, new MetricData(time, pads));
                }
                return padded;
            }
        }
        else {
            return dataList.subList(dataList.size() - count, dataList.size() - 1);
        }
    }

    public String getHeadings() {
        StringBuilder sb = new StringBuilder();
        sb.append("Time");
        if (!dataList.isEmpty()) {
            for (Metric metric : dataList.get(0).getMetrics()) {
                sb.append(",").append(metric.getName());
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public String getCsv() {
        return getCsv(0);
    }

    /**
     * @param datapoints zero for all
     */
    public String getCsv(int datapoints) {
        StringBuilder sb = new StringBuilder();
        List<MetricData> list;
        if (datapoints > 0) {
            list = getData(datapoints * period);
        }
        else {
            list = dataList;
        }
        for (MetricData data : list) {
            sb.append(data.getTime());
            for (Metric metric : data.getMetrics()) {
                sb.append(",").append(metric.getValue());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public int size() {
        return dataList.size();
    }
}
