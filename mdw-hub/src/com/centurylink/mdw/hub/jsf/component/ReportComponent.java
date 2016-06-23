/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import com.centurylink.mdw.hub.jsf.FacesUtil;
import com.centurylink.mdw.hub.report.Report;

public class ReportComponent extends UIComponentBase {
    public static final String COMPONENT_TYPE = "com.centurylink.mdw.hub.jsf.component.ReportComponent";
    public static final String COMPONENT_FAMILY = "javax.faces.Panel";

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    private Report _reportEntity;
    public void setReportEntity(Report r) { _reportEntity = r; }
    public Report getReportEntity() {
        if (_reportEntity != null)
            return _reportEntity;
        return (Report) FacesUtil.getObject(getValueExpression("reportEntity"));
    }

    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
        values[0] = super.saveState(context);
        values[1] = _reportEntity;
        return values;
    }

    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        _reportEntity = (Report) values[1];
    }
}
