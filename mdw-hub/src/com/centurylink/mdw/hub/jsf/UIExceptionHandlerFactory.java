/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

public class UIExceptionHandlerFactory extends ExceptionHandlerFactory {
    
    private ExceptionHandlerFactory wrappedFactory;

    public UIExceptionHandlerFactory(ExceptionHandlerFactory parent) {
        super();
        this.wrappedFactory = parent;
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new UIExceptionHandler(wrappedFactory.getExceptionHandler()); 
    }

}
