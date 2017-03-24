/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
