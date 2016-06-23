/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import javax.faces.context.ExternalContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import com.centurylink.mdw.hub.ui.MDW;

public class UiPhaseListener implements PhaseListener {

    static final String MOBILE_EMULATION = "mdw.mobile";

    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    public void beforePhase(PhaseEvent event) {
        ExternalContext externalContext = event.getFacesContext().getExternalContext();

        // determine whether the request is from MDW Mobile
        boolean isMobile = false;

        String mobileEmulation = externalContext.getRequestParameterMap().get(MOBILE_EMULATION);
        if ("true".equalsIgnoreCase(mobileEmulation)) {
            isMobile = true;
        }
        else {
            String userAgent = externalContext.getRequestHeaderMap().get("User-Agent");
            if (userAgent != null && userAgent.indexOf("Android") >= 0)
                isMobile = true;
        }

        MDW mdw = (MDW) FacesUtil.getValue("mdw");
        mdw.setMobile(isMobile);
    }

    public void afterPhase(PhaseEvent event) {
    }

}
