/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.web.jsf.components.Filter;

public class FilterRenderer extends com.centurylink.mdw.web.jsf.renderers.FilterRenderer {

    public static final String RENDERER_TYPE = FilterRenderer.class.getName();

    protected void renderUserPrefsButton(FacesContext facesContext, Filter filter) throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement("input", filter);
        writer.writeAttribute("id", "filterPrefsButton", null);
        writer.writeAttribute("type", "button", null);
        writer.writeAttribute("onclick", "displayFilterPrefsPopup();return false;", null);
        writer.writeAttribute("class", "mdw_listButton", null);
        String bg = "background-color:transparent;background-repeat:no-repeat;";
        writer.writeAttribute("style", "background-image:url('" + ApplicationContext.getTaskManagerUrl() + "/images/prefs.gif');" + bg, null);
        writer.writeAttribute("value", "", null);
        writer.writeAttribute("title", "Filter Preferences", null);
        writer.endElement("input");
    }
}
