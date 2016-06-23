/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.report;

import java.util.Date;

import javax.faces.FacesException;

import org.apache.commons.lang.StringUtils;

import com.centurylink.mdw.hub.report.birt.BirtReportFactory;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class ReportItem  extends ListItem {
    private RuleSetVO ruleSetVO;
    private Report report;

    public ReportItem(RuleSetVO ruleSetVO) {
        this.ruleSetVO = ruleSetVO;
    }

    public Long getId() {
        return ruleSetVO.getId();
    }

    public String getName() {
        return ruleSetVO.getName();
    }

    public String getVersion() {
        return ruleSetVO.getVersionString();
    }

    public int getVersionInt() {
        return ruleSetVO.getVersion();
    }

    public String getType() {
        return ruleSetVO.getLanguage();
    }

    public String getComments() {
        return ruleSetVO.getComment();
    }

    public String getLockedTo() {
        return ruleSetVO.getModifyingUser();
    }

    public Date getLockDate() {
        return ruleSetVO.getModifyDate();
    }

    private String rsPackage;
    public String getPackage() { return rsPackage; }
    public void setPackage(String rsPackage) { this.rsPackage = rsPackage; }
    public Report getReport() {
        try {
            report = BirtReportFactory.loadReport(getPackage()+"/"+getName(), null);
            return report;
        }
        catch (Exception ex) {
            throw new FacesException(ex.getMessage(), ex);
       }


    }
    public void setReport(Report report) {
        this.report = report;
    }
}
