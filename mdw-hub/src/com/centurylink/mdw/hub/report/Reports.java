/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.report;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.taskmgr.ui.Lister;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class Reports extends SortableList implements Lister {

    private static List<String> packages;

    public Reports(ListUI listUI) {
        super(listUI);
    }

    protected DataModel<ListItem> retrieveItems() throws UIException {
        List<ListItem> list = new ArrayList<ListItem>();
        packages = new ArrayList<String>();  // refresh package list

        // shows only the latest versions
        List<RuleSetVO> ruleSetVOs = RuleSetCache.getRuleSets(RuleSetVO.BIRT);

        for (RuleSetVO ruleSetVO : ruleSetVOs) {
            ReportItem reportItem = new ReportItem(ruleSetVO);
            try {
                PackageVO packageVO = PackageVOCache.getRuleSetPackage(ruleSetVO.getId());
                if (packageVO != null) {
                    reportItem.setPackage(packageVO.getPackageName());
                    packages.add(packageVO.getPackageName());
                }
            }
            catch (CachingException ex) {
                throw new UIException(ex.getMessage(), ex);
            }

            list.add(reportItem);
        }
        return new ListDataModel<ListItem>(list);
    }

    /**
     * lister for packages
     */
    public List<SelectItem> list() {

        List<SelectItem> packageNameSelects = new ArrayList<SelectItem>();
        packageNameSelects.add(new SelectItem(" "));
        if (packages != null) {
            for (String pkg : packages) {
                packageNameSelects.add(new SelectItem(pkg));
            }
        }
        return packageNameSelects;
    }

    /**
     * No-arg constructor used when instantiating for Lister interface
     */
    public Reports() {
        super(null);
    }


}
