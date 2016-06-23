/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.reports;

import java.util.List;

import com.centurylink.mdw.hub.report.ReportBean;

/**
 * Interface that describes how to load and save dashboard layouts.
 * Designed to be a pluggable Spring bean
 * @author aa70413
 *
 */
public interface PersistenceManager {

    public List<ReportBean> loadReports();
    //For the moment dashboardLayout is a JSON string, may change to be more consistent later
    public void saveReports(String dashboardLayout);

}
