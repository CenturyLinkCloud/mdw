/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.reports;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.richfaces.model.Filter;

import com.centurylink.mdw.hub.report.ReportItem;
import com.centurylink.mdw.hub.report.Reports;
import com.centurylink.mdw.hub.ui.ListManager;
import com.centurylink.mdw.web.ui.UIException;

/**
 * Supports adding a filter of "package" to the header of a datatable
 * @author aa70413
 *
 */
public class ReportsFilterBean implements Serializable {
    private String packageFilter;

    public Filter<?> getFilterPackage() {
        return new Filter<ReportItem>() {
            public boolean accept(ReportItem t) {
                String pack = getPackageFilter();
                if (pack == null || pack.trim().length() == 0 || pack.equals(t.getPackage())) {
                    return true;
                }
                return false;
            }
        };
    }

    public String getPackageFilter() {
        return packageFilter;
    }

    public void setPackageFilter(String packageFilter) {
        this.packageFilter = packageFilter;
    }
    /**
     * Uses the existing ListManager.getList().list() call
     * and removes duplicates
     * @see ListManager.getList().list()
     * @return a set of packages with reports
     */
    public Set<String> getReportPackages() {
        Reports reports = null;
        try {
            reports = (Reports)ListManager.getInstance().getList("reportsList");
        }
        catch (UIException e) {
            return null;
        }

        // Remove duplicates
        // It would be quicker to use the LinkedHashSet constructor
        // but SelectItem doesn't have an equals method
        // so it won't work, so just create a new set
        Set<String> set = new LinkedHashSet<String>();
        for (SelectItem item: reports.list()) {
                if (!set.contains(item.getValue())) {
                    set.add((String)item.getValue());
                }

            }
        return set;
    }

}
