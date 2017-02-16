/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class Report extends WorkflowAsset {
    public Report() {
        super();
    }

    public Report(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        super(ruleSetVO, packageVersion);
    }

    public Report(Report cloneFrom) {
        super(cloneFrom);
    }

    @Override
    public String getTitle() {
        return "Report";
    }

    @Override
    public String getIcon() {
        return "report.gif";
    }

    @Override
    public String getDefaultExtension() {
        return ".rptdesign";
    }

    private static List<String> reportLanguages;

    @Override
    public List<String> getLanguages() {
        if (reportLanguages == null) {
            reportLanguages = new ArrayList<String>();
            reportLanguages.add("BIRT");
        }
        return reportLanguages;
    }

}