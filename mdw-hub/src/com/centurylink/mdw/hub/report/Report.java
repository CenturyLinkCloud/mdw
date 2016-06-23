/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.report;

import com.centurylink.mdw.web.ui.filter.Filter;

public class Report {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String title;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    private Filter filter;
    public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }

    public Report(String name) {
      this.name = name;
    }

    public Report() {

    }
}