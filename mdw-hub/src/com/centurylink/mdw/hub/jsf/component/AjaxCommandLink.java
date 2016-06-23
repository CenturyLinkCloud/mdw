/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import org.richfaces.component.UICommandLink;

/**
 * Replace RichFaces command link component to make this work with the MDW DataScroller.
 */
public class AjaxCommandLink extends UICommandLink {
    
    public static final String COMPONENT_TYPE = AjaxCommandLink.class.getName();

    @Override
    public String getRendererType() {
        return AjaxCommandLinkRenderer.RENDERER_TYPE;
    }
}
