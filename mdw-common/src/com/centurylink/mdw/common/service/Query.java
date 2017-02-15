/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Query {
    public static final int DEFAULT_MAX = 50;
    public static final int MAX_ALL = -1;

    public Query() {

    }

    public Query(String path, Map<String,String> parameters) {

        this.path = path;

        String countParam = parameters.get("count");
        if (countParam != null)
            setCount(Boolean.parseBoolean(countParam));

        setFind(parameters.get("find"));

        String startParam = parameters.get("start");
        if (startParam != null)
            setStart(Integer.parseInt(startParam));

        String maxParam = parameters.get("max");
        if (maxParam != null)
            setMax(Integer.parseInt(maxParam));

        setSort(parameters.get("sort"));

        String descendingParam = parameters.get("descending");
        if (descendingParam != null)
            setDescending(Boolean.parseBoolean(descendingParam));

        for (String key : parameters.keySet()) {
            if (!"count".equals(key) && !"find".equals(key) && !"start".equals(key) && !"max".equals(key)
                    && !"sort".equals(key) && !"descending".equals(key) && !"ascending".equals(key) && !"app".equals(key)
                    && !"DownloadFormat".equals(key))
                setFilter(key, parameters.get(key));
        }
    }

    private boolean count; // count only -- no retrieval
    public boolean isCount() { return count; }
    public void setCount(boolean count) { this.count = true; }

    private String find;
    public String getFind() { return find; }
    public void setFind(String find) { this.find = find;}

    private int start = 0;
    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }

    private int max = DEFAULT_MAX;
    public int getMax() { return max; }
    public void setMax(int max) { this.max = max; }

    private String sort;
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    private boolean descending;
    public boolean isDescending() { return descending; }
    public void setDescending(boolean descending) { this.descending = descending; }

    private String path;
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    /**
     * All parameters are filters except count, find, start, max, sort, descending, ascending and app
     */
    private Map<String,String> filters = new HashMap<String,String>();
    public Map<String,String> getFilters() { return filters; }
    public void setFilters(Map<String,String> filters) { this.filters = filters; }
    public boolean hasFilters() { return !filters.isEmpty(); }

    public void setFilter(String key, String value) {
        filters.put(key, value);
    }
    public String getFilter(String key) {
        return filters.get(key);
    }

    /**
     * Empty list returns null;
     */
    public String[] getArrayFilter(String key) {
        String value = filters.get(key);
        if (value == null)
            return null;
        String[] array = new String[0];
        if (value.startsWith("[")) {
            if (value.length() > 2)
                array = value.substring(1, value.length() - 1).split(",");
        }
        else if (value.length() > 1) {
            array = value.split(",");
        }
        for (int i = 0; i < array.length; i++) {
            if (array[i].charAt(0) == '"')
                array[i] = array[i].substring(1, array[i].length() - 2);
        }
        return array;
    }
    public void setArrayFilter(String key, String[] array) {
        if (array == null || array.length == 0) {
            filters.remove(key);
        }
        else {
            String value = "[";
            for (int i = 0; i < array.length; i++) {
                value += array[i];
                if (i < array.length - 1)
                    value += ",";
            }
            value += "]";
            filters.put(key, value);
        }
    }

    public Long[] getLongArrayFilter(String key) {
        String[] strArr = getArrayFilter(key);
        if (strArr == null)
            return null;
        Long[] longArr = new Long[strArr.length];
        for (int i = 0; i < strArr.length; i++)
            longArr[i] = Long.parseLong(strArr[i]);
        return longArr;
    }

    public boolean getBooleanFilter(String key) {
        String value = filters.get(key);
        return "true".equalsIgnoreCase(value);
    }
    public void setFilter(String key, boolean value) {
        setFilter(key, String.valueOf(value));
    }

    public int getIntFilter(String key) {
        String value = filters.get(key);
        return value == null ? -1 : Integer.parseInt(value);
    }
    public void setFilter(String key, int value) {
        setFilter(key, String.valueOf(value));
    }

    public long getLongFilter(String key) {
        String value = filters.get(key);
        return value == null ? -1 : Long.parseLong(value);
    }
    public void setFilter(String key, long value) {
        setFilter(key, String.valueOf(value));
    }

    public Date getDateFilter(String key) throws ParseException {
        return getDate(filters.get(key));
    }
    public void setFilter(String key, Date date) {
        String value = getString(date);
        if (value == null)
            filters.remove(key);
        else
            filters.put(key, value);
    }

    private static DateFormat dateTimeFormat;
    protected static DateFormat getDateTimeFormat() {
        if (dateTimeFormat == null)
            dateTimeFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        return dateTimeFormat;
    }

    private static DateFormat dateFormat;
    protected static DateFormat getDateFormat() {
        if (dateFormat == null)
            dateFormat = new SimpleDateFormat("yyyy-MMM-dd"); // does not require url-encoding
        return dateFormat;
    }

    public static Date getDate(String str) throws ParseException {
        if (str == null)
            return null;
        else if (str.length() > 11)
            return getDateTimeFormat().parse(str);
        else
            return getDateFormat().parse(str);
    }

    public static String getString(Date date) {
        if (date == null)
            return null;
        else {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            if (c.get(Calendar.HOUR_OF_DAY) == 0 && c.get(Calendar.MINUTE) == 0 && c.get(Calendar.SECOND) == 0)
                return getDateFormat().format(date);
            else
                return getDateTimeFormat().format(date);
        }
    }
    /**
     * Support ISO8601 format for a Date YYYY-MM-dd
     * @param str
     * @return a Date object
     * @throws ParseException
     */
    public static Date getISO8601Date(String str) throws ParseException {
        // Use thread safe Java 8 version
        return Date.from(LocalDate.parse(str, DateTimeFormatter.ISO_DATE).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }
    /**
     * Support ISO8601 format for a DateTime yyyy-MM-dd'T'HH:mm:ss'Z'
     * @param str
     * @return a Date object
     * @throws ParseException
     */
    public static Date getISO8601DateTime(String str) throws ParseException {
        // Use thread safe Java 8 version
        return Date.from(LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME).atZone(ZoneId.systemDefault()).toInstant());
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(path).append(": ");
        sb.append("[count=").append(count);
        sb.append(", ").append("find=").append(find);
        sb.append(", ").append("start=").append(start);
        sb.append(", ").append("max=").append(max);
        sb.append(", ").append("sort=").append(sort);
        sb.append(", ").append("descending=").append(descending);

        if (filters != null) {
            for (String key : filters.keySet())
                sb.append(", ").append(key).append("=").append(filters.get(key));
        }

        sb.append("]");
        return sb.toString();
    }
}