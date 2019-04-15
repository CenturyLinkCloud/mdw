package com.centurylink.mdw.model.report;

import com.centurylink.mdw.model.JsonObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Rolling, fixed size list for holding metric data.
 */
public class MetricDataList {

    public static final int PERIOD = 10; // seconds

    private int max;
    private List<MetricData> dataList;

    public MetricDataList(int size) {
        this.max = size;
        dataList = new ArrayList<>();
    }

    public void add(MetricData metricData) {
        while (dataList.size() >= max) {
            dataList.remove(0);
        }
        dataList.add(metricData);
    }

    public MetricData get(int i) {
        return dataList.get(i);
    }

    public JSONObject getJson(int span) {
        return getJson(span, DateTimeFormatter.ISO_DATE_TIME);
    }

    public JSONObject getJson(int span, DateTimeFormatter formatter) {
        JSONObject json = new JsonObject();
        for (MetricData metricData : getData(span)) {
            JSONArray jsonArray = new JSONArray();
            for (Metric metric : metricData.getMetrics()) {
                jsonArray.put(metric.getJson());
            }
            json.put(formatter.format(metricData.getTime()), jsonArray);
        }
        return json;
    }

    /**
     * Returns a left-padded list.
     */
    public List<MetricData> getData(int span) {
        int count = span / PERIOD;
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
                    pads.add(new Metric(metric.getName(), 0));
                }
                LocalDateTime time = first.getTime();
                while (padded.size() < count) {
                    time = time.plusSeconds(PERIOD);
                    padded.add(0, new MetricData(time, pads));
                }
                return padded;
            }
        }
        else {
            return dataList.subList(dataList.size() - count, dataList.size() - 1);
        }
    }

    public int size() {
        return dataList.size();
    }
}
