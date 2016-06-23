/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.reports;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.hub.report.ReportBean;
import com.centurylink.mdw.taskmgr.ui.layout.ReportUI;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

/**
 * Implements a persistence layout based on cookies
 *
 * @author aa70413
 *
 */
public class CookiePersistenceBean implements Serializable, PersistenceManager {

    public final static String DASHBOARD_LAYOUT_COOKIE = "mdw-dashboard-layout";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * <ul>
     * <li>Looks up the cookie "mdw-dashboard-layout" which holds Json data on
     * dashboard panels</li>
     *
     */
    @Override
    public List<ReportBean> loadReports() {
        List<ReportBean> finalList = new ArrayList<ReportBean>();
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext externalContext = fc.getExternalContext();
        Map<String, Object> cookies = externalContext.getRequestCookieMap();
        Cookie cookie = (Cookie) cookies.get(DASHBOARD_LAYOUT_COOKIE);
        if (cookie != null) {
            try {
                String unescaped = org.json.Cookie.unescape(cookie.getValue());
                JSONObject dashboardLayout = new JSONObject(unescaped);// org.json.Cookie.toJSONObject(cookie.getValue());
                /**
                 * {
                 * 'name':'myPanel1','clientHeight':clientHeight,'innerHeight':innerHe
                 * i g h t ,
                 * 'outerHeight':outerHeight,'left':position.left,'top':position.
                 * t o p }
                 */
                JSONArray reports = dashboardLayout.getJSONArray("reports");
                // Get the layout for each panel
                for (int i = 0; i < reports.length(); i++) {
                    JSONObject report = reports.getJSONObject(i);
                    String packageName = report.getString("packageName");
                    String reportName = report.getString("name");
                    double height = report.getDouble("height");
                    double width = report.getDouble("width");
                    double left = report.getDouble("left");
                    double top = report.getDouble("top");
                    // Create each panel with the saved layout
                    finalList
                            .add(new ReportBean(packageName, reportName, height, width, left, top));
                }
            }
            catch (JSONException ex) {
                // Problem reading from cookie
                logger.severe("Unable to read dashboard JSON data from cookie "
                        + DASHBOARD_LAYOUT_COOKIE + ":" + ex.getMessage());

            }
        }
        else {
            // Load the default layout from the hub view xml
            try {
                List<ReportUI> reports = ViewUI.getInstance().getReportsUI("dashboardReports")
                        .getReports();
                for (ReportUI report : reports) {
                    // Create each panel with the saved layout
                    finalList.add(new ReportBean(report.getPackageName(), report.getName(), report
                            .getHeight(), report.getWidth(), report.getLeft(), report.getTop()));

                }
            }
            catch (UIException ex) {
                // Problem reading from hubview xml
                logger.severe("Unable to read from hubview xml file : " + ex.getMessage());
            }

        }
        return finalList;

    }

    @Override
    public void saveReports(String dashboardLayout) {
        addCookie(DASHBOARD_LAYOUT_COOKIE, dashboardLayout, 365 * 24 * 60 * 60, "/");

    }

    private void addCookie(String name, String value, int expires, String path) {

        HttpServletResponse httpServletResponse = (HttpServletResponse) FacesContext
                .getCurrentInstance().getExternalContext().getResponse();
        Cookie layoutCookie = new Cookie(name, value);
        layoutCookie.setMaxAge(expires);
        // if (domain !=null) {
        // layoutCookie.setDomain(domain);
        // }
        if (path != null) {
            layoutCookie.setPath(path);
        }
        httpServletResponse.addCookie(layoutCookie);

    }

}
