/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.HashMap;
import java.util.Map;

public class ProcessInstanceSort {
    private String sort = "Instance ID";

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    private boolean ascending;

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean asc) {
        this.ascending = asc;
    }

    public String getOrderBy() {
        String orderBy = "order by " + getDbColumns().get(sort);
        if (!ascending)
            orderBy += " desc";
        return orderBy;
    }

    private static Map<String, String> dbColumns;

    public static Map<String, String> getDbColumns() {
        if (dbColumns == null) {
            dbColumns = new HashMap<String, String>();
            dbColumns.put("Instance ID", "PROCESS_INSTANCE_ID");
            dbColumns.put("Master Request ID", "MASTER_REQUEST_ID");
            dbColumns.put("Owner", "OWNER");
            dbColumns.put("Owner ID", "OWNER_ID");
            dbColumns.put("Status", "STATUS_CD");
            dbColumns.put("Start", "START_DT");
            dbColumns.put("End", "END_DT");
        }
        return dbColumns;
    }

}
