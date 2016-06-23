/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import com.centurylink.mdw.hub.ui.MDW;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

/**
 * Extended JSF utility class for HTML5/JSF 2.1 rendering.
 * Any refactoring of this class should take into account that this
 * is used to determine whether the HTML5 rendering engine is active
 * in the FacesVariableUtil method isHtml5Rendering().
 */
public class FacesUtil extends FacesVariableUtil {

    public static final String DEV_WEB_ROOT = "mdw.hub.dev.web.root";

    public static String getDevWebRoot() {
        return System.getProperty(DEV_WEB_ROOT);
    }

    public static void setMobile(boolean isMobile) {
        ((MDW)getValue("mdw")).setMobile(isMobile);
    }

}
