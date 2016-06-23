/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.renderkit.html.HtmlScriptRenderer;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

public class ScriptRenderer extends HtmlScriptRenderer {

    private static final String RICHFACES_PACKED = "org.richfaces.staticResource/4.3.3.Final/PackedCompressed/packed/packed.js";

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {

        boolean rfOpt = "true".equalsIgnoreCase(facesContext.getExternalContext().getInitParameter("org.richfaces.resourceOptimization.enabled"));
        if (rfOpt) {
            // RichFaces resource optimization renders packed.js many times due to this bug:
            // https://issues.jboss.org/browse/RF-12495  (which despite the status is NOT FIXED)
            Map<String,Object> componentAttributesMap = component.getAttributes();
            String libraryName = (String)componentAttributesMap.get(JSFAttr.LIBRARY_ATTR);
            String resourceName = (String)componentAttributesMap.get(JSFAttr.NAME_ATTR);
            if ("org.richfaces".equals(libraryName) || (resourceName != null && resourceName.startsWith("richfaces"))) {
                // instead render packed.js a single time to avoid the bug
                if (!ResourceUtils.isRenderedScript(facesContext, null, RICHFACES_PACKED)) {
                    // TODO: why does the following call add an extra packed.js?
                    // super.encodeEnd(facesContext, component);
                    ResourceUtils.markScriptAsRendered(FacesContext.getCurrentInstance(), null, RICHFACES_PACKED);
                }
                return;  // these will have been included in packed.js
            }
        }

        super.encodeEnd(facesContext, component);
    }
}
