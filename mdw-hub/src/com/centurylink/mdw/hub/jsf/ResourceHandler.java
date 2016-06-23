/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandlerWrapper;

public class ResourceHandler extends ResourceHandlerWrapper {
    
    private javax.faces.application.ResourceHandler wrappedHandler;
    
    public ResourceHandler(javax.faces.application.ResourceHandler wrappedHandler) {
        this.wrappedHandler = wrappedHandler;
    }
    
    @Override
    public Resource createResource(String resourceName) {
        if (resourceName.endsWith(".css"))
            return createResource(resourceName, null, "text/css");
        else
            return super.createResource(resourceName);
    }

    @Override
    public javax.faces.application.ResourceHandler getWrapped() {
        return wrappedHandler;
    }
}
