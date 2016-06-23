/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.HTMLServerImageHandler;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.hub.report.birt.BirtFilter;
import com.centurylink.mdw.hub.report.birt.BirtReport;
import com.centurylink.mdw.hub.report.birt.BirtReportFactory;
import com.centurylink.mdw.web.ui.input.Input;

public class BirtReportRenderer extends Renderer {
    public static final String RENDERER_TYPE = "com.centurylink.mdw.hub.jsf.component.ReportRenderer";

    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        ReportComponent report = (ReportComponent) component;
        BirtReport birtReport = (BirtReport) report.getReportEntity();
        BirtFilter birtFilter = (BirtFilter) birtReport.getFilter();

        // make sure the temp directories exist
        File tempImageDir = new File(ApplicationContext.getTempDirectory() + "/images");
        if (!tempImageDir.exists()) {
            boolean tempDirCreated = tempImageDir.mkdirs();
            if (!tempDirCreated)
                throw new IOException("Unable to create temp image dir: " + tempImageDir);
        }

        IRunAndRenderTask runAndRenderTask = null;
        try {
            IReportRunnable reportDesign = birtReport.getReportDesign();

            // TODO: sync BIRT report engine versions between plug-in and webapp
            // to allow separate run and render tasks with IReportDocument and data extraction:
            // http://www.eclipse.org/birt/phoenix/deploy/reportEngineAPI.php#idataextractiontask
            // (this will enable us to support pagination and showing count of total rows)

            runAndRenderTask = BirtReportFactory.getReportEngine().createRunAndRenderTask( reportDesign);

            boolean paramsValid = true;
            if (birtFilter != null) {
                // set parameter values and validate
                for (Input input : birtFilter.getCriteriaList()) {
                    Object paramValue = input.getValue();
                    if (input.isInputTypeDate()) {
                        if (paramValue instanceof String) {
                            paramValue = java.sql.Date.valueOf((String) paramValue);
                        }
                        else if (paramValue != null) {
                            paramValue = new java.sql.Date(((java.util.Date) paramValue).getTime());
                        }
                    }
                    else if (input.isInputTypeDigit() && paramValue instanceof String) {
                        if (((String) paramValue).toString().trim().length() != 0)
                            paramValue = Integer.parseInt((String) paramValue);
                    }

                    runAndRenderTask.setParameterValue(input.getAttribute(), paramValue);
                }
                paramsValid = runAndRenderTask.validateParameters();
            }

            // don't save prefs render html if required parameters have not been entered
            if (paramsValid) {
                // save current filter selections as user defaults
                if (birtFilter != null)
                    birtFilter.saveFilterPrefs();

                // render report to HTML
                OutputStream outStream = new ByteArrayOutputStream();
                HTMLRenderOption options = new HTMLRenderOption();
                options.setImageHandler(new HTMLServerImageHandler());
                options.setBaseImageURL(ApplicationContext.getMdwWebUrl() + "/resources/images/");
                options.setImageDirectory(ApplicationContext.getTempDirectory() + "/images");
                options.setOutputFormat("html");
                options.setEmbeddable(true);
                options.setSupportedImageFormats("PNG");
                options.setOutputStream(outStream);
                options.setEnableAgentStyleEngine(true);
                runAndRenderTask.setRenderOption(options);

                // runAndRenderTask.setPageHandler(new IPageHandler() {
                //     public void onPage(int pageNumber, boolean checkpoint, IReportDocumentInfo reportInfoDoc) {
                //         System.out.println("HANDLING PAGE: " + pageNumber);
                //     }
                // });

                runAndRenderTask.run();

                String html = outStream.toString();
                facesContext.getResponseWriter().write(html);
            }
            else {
                ResponseWriter writer = facesContext.getResponseWriter();
                writer.write("Please enter required filter parameters");
            }
        }
        catch (Exception ex) {
            throw new IOException(ex.getMessage(), ex);
        }
        finally {
            if (runAndRenderTask != null)
                runAndRenderTask.close();
        }
    }
}
