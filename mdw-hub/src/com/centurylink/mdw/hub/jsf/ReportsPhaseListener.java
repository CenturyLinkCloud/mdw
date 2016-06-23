/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import java.util.Map;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.birt.report.engine.api.EXCELRenderOption;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.hub.jsf.FacesUtil;
import com.centurylink.mdw.hub.report.Report;
import com.centurylink.mdw.hub.report.birt.BirtFilter;
import com.centurylink.mdw.hub.report.birt.BirtReport;
import com.centurylink.mdw.hub.report.birt.BirtReportFactory;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.input.Input;

public class ReportsPhaseListener implements PhaseListener
{
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    /**
     * If request is for a report, set the managed bean.
     */
    public void beforePhase(PhaseEvent event) {
        FacesContext facesContext = event.getFacesContext();
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, String> params = externalContext.getRequestParameterMap();
        String reportName = params.get("mdwReport");

        if (reportName != null) {
            try {
                Report birtReport = BirtReportFactory.loadReport(reportName, params);
                FacesUtil.setValue("mdwReport", birtReport);
            }
            catch (UIException ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new FacesException(ex.getMessage(), ex);
            }
        }

        boolean exportFlag = false;
        for (String paramName : params.keySet()) {
            if ("exportReportToExcel".equals(params.get(paramName)))
                exportFlag = true;
        }

        if (exportFlag) {
            BirtReport birtReport = (BirtReport) FacesUtil.getValue("mdwReport");
            Object contextResponse = externalContext.getResponse();
            // TODO portlet support
            if (contextResponse instanceof HttpServletResponse) {
                HttpServletResponse response = (HttpServletResponse) contextResponse;
                response.setContentType("application/vnd.ms-excel");

                try {
                    IReportRunnable reportDesign = birtReport.getReportDesign();
                    IRunAndRenderTask task = BirtReportFactory.getReportEngine().createRunAndRenderTask(reportDesign);

                    BirtFilter birtFilter = (BirtFilter) birtReport.getFilter();
                    boolean paramsValid = true;
                    if (birtFilter != null) {
                        // set parameter values and validate
                        for (Input input : birtFilter.getCriteriaList()) {
                            Object paramValue = input.getValue();
                            if (input.isInputTypeDate()) {
                                if (paramValue instanceof String) {
                                    paramValue = java.sql.Date.valueOf((String) paramValue);
                                }
                                else {
                                    paramValue = new java.sql.Date(
                                            ((java.util.Date) paramValue).getTime());
                                }
                            }
                            else if (input.isInputTypeDigit() && paramValue instanceof String) {
                                paramValue = Integer.parseInt((String) paramValue);
                            }

                            task.setParameterValue(input.getAttribute(), paramValue);
                        }
                        paramsValid = task.validateParameters();
                    }

                    if (paramsValid) {
                        EXCELRenderOption options = new EXCELRenderOption();
                        options.setOutputFormat("xls");
                        options.setOutputStream(response.getOutputStream());
                        options.closeOutputStreamOnExit(true);
                        task.setRenderOption(options);
                        task.run();
                        task.close();
                    }
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                    throw new FacesException(ex.getMessage(), ex);
                }

                facesContext.responseComplete();
            }
        }
    }

    public void afterPhase(PhaseEvent event) {
    }
}
