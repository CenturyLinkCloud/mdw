/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui;

import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

/**
 * Replaces the old heavier MDW managed bean.
 */
public class MDW {

    private String skin;
    public String getSkin() {
        if (skin == null) {
            skin = PropertyManager.getProperty("mdw.richfaces.skin");
            if (skin == null)
                skin = "blueSky";    // emeraldTown or plain
        }
        return skin;
    }
    public void setSkin(String skin) {
        this.skin = skin;
    }

    private String theme;
    public String getPrimefacesTheme() {
        // avoid rendering primefaces css on login page to prevent CT from redirecting to CSS resource after login
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        if (viewId != null && (viewId.endsWith("login.xhtml") || viewId.endsWith("loginError.xhtml") || viewId.endsWith("mdwLogin.xhtml") || viewId.endsWith("mdwLoginError.xhtml")))
            return "none";
        if (theme == null) {
            theme = PropertyManager.getProperty("mdw.primefaces.theme");
            if (theme == null)
                theme = "aristo";
        }
        return theme;
    }
    public void setPrimefacesTheme(String theme) {
        this.theme = theme;
    }

    private boolean mobile;
    public boolean isMobile() {
        return mobile;
    }
    public void setMobile(boolean mobile) {
        this.mobile = mobile;
    }

    public boolean isOsgi() {
        return ApplicationContext.isOsgi();
    }

    public String getAppName() {
        return ApplicationContext.getApplicationName();
    }

    public String getAppVersion() {
        return ApplicationContext.getApplicationVersion();
    }

    public String getMdwVersion() {
      return ApplicationContext.getMdwVersion();
    }

    /**
     * Map for facelets properties access.
     */
    public Map<String, String> getProperties() {
        return new HashMap<String, String>() {
            public String get(Object key) {
                if (key == null)
                    return null;
                else
                    return FacesVariableUtil.getProperty(key.toString());
            }
        };
    }

    public boolean isHasBirt() {
        try {
            new org.eclipse.birt.core.exception.BirtException();
            return true;
        }
        catch (NoClassDefFoundError ex) {
            return false;
        }
    }
}
