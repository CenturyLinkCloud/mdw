/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.NavigationCase;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.myfaces.config.element.FacesConfig;
import org.apache.myfaces.config.element.NavigationRule;
import org.apache.myfaces.spi.FacesConfigurationProvider;
import org.apache.myfaces.spi.FacesConfigurationProviderFactory;

import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.hub.ui.FilterManager;
import com.centurylink.mdw.hub.ui.ListManager;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.navigation.MDWNavigationHandler;

/**
 * Provides the ability to override navigation cases from META-INF faces configs
 * which doesn't work in MyFaces NavigationHandlerImpl due to unordered Set impl
 * (although technically the JSF spec considers it an error to have two conflicting cases).
 */
public class NavigationHandler extends MDWNavigationHandler {

    private Map<String,Set<NavigationCase>> navCases;

    public NavigationHandler(javax.faces.application.NavigationHandler wrapped) {
        super(wrapped, false);
    }

    @Override
    public Map<String,Set<NavigationCase>> getNavigationCases() {
        if (navCases == null) {
            navCases = new HashMap<String,Set<NavigationCase>>();
            Map<String,Set<NavigationCase>> superNavCases = super.getNavigationCases();
            // replace with ordered list implementation (local webapp cases last)
            for (String key : superNavCases.keySet()) {
                Set<NavigationCase> superCases = superNavCases.get(key);
                List<NavigationCase> webappCasesList = new ArrayList<NavigationCase>();
                List<NavigationCase> foreignCasesList = new ArrayList<NavigationCase>();
                for (NavigationCase superCase : superCases) {
                    if (isWebAppNavCase(superCase))
                        webappCasesList.add(superCase);
                    else
                        foreignCasesList.add(0, superCase);
                }
                Set<NavigationCase> orderedCases = new LinkedHashSet<NavigationCase>();
                for (NavigationCase navCase : foreignCasesList)
                    orderedCases.add(navCase);
                for (NavigationCase navCase : webappCasesList)
                    orderedCases.add(navCase);
                navCases.put(key, orderedCases);
            }
        }
        return navCases;
    }

    /**
     * @return the nav rules declared in this webapp
     */
    private List<NavigationRule> getWebAppNavigationRules() {
        ExternalContext extCtx = FacesContext.getCurrentInstance().getExternalContext();
        FacesConfigurationProvider facesConfigProvider
          = FacesConfigurationProviderFactory.getFacesConfigurationProviderFactory(extCtx).getFacesConfigurationProvider(extCtx);
        FacesConfig webAppFacesConfig = facesConfigProvider.getWebAppFacesConfig(extCtx);
        return webAppFacesConfig.getNavigationRules();
    }

    private List<NavigationRule> webAppNavRules;
    private boolean isWebAppNavCase(NavigationCase navCase) {
        if (webAppNavRules == null)
            webAppNavRules = getWebAppNavigationRules();

        if (webAppNavRules != null) {
            for (NavigationRule navRule : webAppNavRules) {
                for (org.apache.myfaces.config.element.NavigationCase webAppNavCase : navRule.getNavigationCases()) {
                    if (navCase.equals(getApiNavCase(navRule.getFromViewId(), webAppNavCase)))
                        return true;
                }
            }
        }
        return false;
    }


    /**
     * Create a nav case from a config element.
     */
    private NavigationCase getApiNavCase(String fromViewId, org.apache.myfaces.config.element.NavigationCase navCaseElement) {
        if (navCaseElement.getRedirect() != null) {
            String includeViewParamsAttribute = navCaseElement.getRedirect().getIncludeViewParams();
            boolean includeViewParams = false; // default value is false
            if (includeViewParamsAttribute != null)
                includeViewParams = new Boolean(includeViewParamsAttribute);
            return new NavigationCase(fromViewId, navCaseElement.getFromAction(),
                    navCaseElement.getFromOutcome(), navCaseElement.getIf(),
                    navCaseElement.getToViewId(), navCaseElement.getRedirect().getViewParams(),
                    true, includeViewParams);
        }
        else {
            return new NavigationCase(fromViewId, navCaseElement.getFromAction(),
                    navCaseElement.getFromOutcome(), navCaseElement.getIf(),
                    navCaseElement.getToViewId(), null, false, false);
        }
    }

    // TODO : Test this approach throughly
    @Override
    public void handleNavigation(FacesContext facesContext, String fromAction, String outcome) {
        if (outcome != null && outcome.startsWith("navigation")) {
            String toPageQualifiedName = outcome.substring("navigation/".length());
            if (toPageQualifiedName != null) {
                String packageName = null;
                String pageName = toPageQualifiedName;
                PackageVO packageVO = null;
                if (toPageQualifiedName.indexOf("/") > 0) {
                    packageName = toPageQualifiedName.substring(0, toPageQualifiedName.indexOf("/", 1));
                }
                try {
                    if (packageName != null) {
                        packageVO = PackageVOCache.getPackageVO(packageName);
                        PackageVO currentPackage = FacesVariableUtil.getCurrentPackage();
                        if (currentPackage == null || !packageVO.getPackageName().equals(currentPackage.getPackageName())) {
                            ListManager.getInstance().invalidate();
                            FilterManager.getInstance().invalidate();
                            FacesVariableUtil.setValue("mdwPackage", packageVO);
                            FacesVariableUtil.setSkin(ViewUI.getInstance().getSkin()); // reset skin
                        }
                    }
                    FacesVariableUtil.setValue("pageName", URLEncoder.encode(pageName, "UTF-8"));
                    ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
                    UIViewRoot viewRoot = viewHandler.createView(facesContext, "/page.jsf");
                    facesContext.setViewRoot(viewRoot);
                    facesContext.renderResponse();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        else {
            super.handleNavigation(facesContext, fromAction, outcome);
        }
    }
}
