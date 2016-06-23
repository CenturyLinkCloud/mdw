/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import java.io.IOException;
import java.util.Iterator;

import javax.faces.FacesException;
import javax.faces.application.NavigationHandler;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

import org.apache.myfaces.renderkit.ErrorPageWriter;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.web.ui.UIError;

public class UIExceptionHandler extends ExceptionHandlerWrapper {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private ExceptionHandler exceptionHandler;

    /**
     * @param exceptionHandler
     */
    public UIExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return exceptionHandler;
    }

    @Override
    public void handle() throws FacesException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        boolean isDebugPage = facesContext.getExternalContext().getRequestParameterMap().get("facelets.ui.DebugOutput") != null;
        Iterator<ExceptionQueuedEvent> iterator = getUnhandledExceptionQueuedEvents().iterator();
        while (iterator.hasNext()) {
            ExceptionQueuedEvent event = (ExceptionQueuedEvent) iterator.next();
            ExceptionQueuedEventContext exceptionContext = (ExceptionQueuedEventContext) event.getSource();
            Throwable throwable = exceptionContext.getException();
            logger.severeException("UIExceptionHandler: " + throwable.getMessage(), throwable);
            if (!(throwable instanceof AbortProcessingException)) {
                if (isDebugPage) {
                    // render the debug content instead of the standard error page
                    try {
                        ErrorPageWriter.debugHtml(facesContext.getExternalContext().getResponseOutputWriter(), facesContext, throwable);
                    }
                    catch (IOException ex) {
                        logger.severeException(ex.getMessage(), ex);
                        throw new FacesException(ex.getMessage(), ex);
                    }
                }
                else if (facesContext.getViewRoot() != null && !"/error.xhtml".equals(facesContext.getViewRoot().getViewId())) {  // avoid redirect loop
                    UIError error = new UIError(throwable.getMessage(), throwable);
                    facesContext.getExternalContext().getSessionMap().put("error", error);
                    try {
                        NavigationHandler navigationHandler = facesContext.getApplication().getNavigationHandler();
                        navigationHandler.handleNavigation(facesContext, null, "/error?faces-redirect=true");
                        facesContext.renderResponse();
                    }
                    finally {
                        iterator.remove();
                    }
                }
            }
        }

        getWrapped().handle();
    }
}
