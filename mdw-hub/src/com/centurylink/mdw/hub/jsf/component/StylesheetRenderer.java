/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.renderkit.html.HtmlStylesheetRenderer;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

import com.centurylink.mdw.hub.jsf.FacesUtil;
import com.centurylink.mdw.hub.ui.MDW;

public class StylesheetRenderer extends HtmlStylesheetRenderer {

    private static final String RICHFACES_RES_PATH = "org.richfaces.staticResource/4.3.3.Final/PackedCompressed";
    private static final String RICHFACES_PACKED_CSS = "packed/packed.css";

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {

        boolean rfOpt = "true".equalsIgnoreCase(facesContext.getExternalContext().getInitParameter("org.richfaces.resourceOptimization.enabled"));
        if (rfOpt) {
            // RichFaces resource optimization renders packed.css many times due to this bug:
            // https://issues.jboss.org/browse/RF-12495  (which despite the status is NOT FIXED)
            Map<String,Object> componentAttributesMap = component.getAttributes();
            String libraryName = (String)componentAttributesMap.get(JSFAttr.LIBRARY_ATTR);
            String resourceName = (String)componentAttributesMap.get(JSFAttr.NAME_ATTR);
            if ("org.richfaces".equals(libraryName) || (resourceName != null && resourceName.startsWith("richfaces"))) {
                // instead render packed.css a single time to avoid the bug
                String packedCss = RICHFACES_RES_PATH + "/" + ((MDW)FacesUtil.getValue("mdw")).getSkin() + "/" + RICHFACES_PACKED_CSS;
                if (!ResourceUtils.isRenderedStylesheet(facesContext, null, packedCss)) {
                    super.encodeEnd(facesContext, component);
                    ResourceUtils.markStylesheetAsRendered(FacesContext.getCurrentInstance(), null, packedCss);
                }
                return;  // these will have been included in packed.css
            }
        }

        super.encodeEnd(facesContext, component);
    }
}
