/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.reports;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.richfaces.event.DropEvent;
import org.richfaces.event.DropListener;
import org.richfaces.function.RichFunction;

import com.centurylink.mdw.hub.jsf.component.Panel;
import com.centurylink.mdw.hub.report.ReportBean;
import com.centurylink.mdw.hub.report.ReportItem;

/**
 * Supports Richfaces drag and drop for reports
 *
 * @author aa70413
 *
 */
public class DragDropBean implements Serializable, DropListener {

    // Holds the reports items dragged onto the dashboard
    private List<ReportBean> reports;
    // HashMap of panelId/DashboardPanelHolder - used to check the status of
// panels
    private Map<String, DashboardPanelHolder> panels;
    // Used to re-render the correct panel and not rerender the parent
    private List<String> droppedPanelNames;
    // Once a panel is closed, ensure it is no longer visible
    private String closedPanelName;
    private String dashboardLayout;
     private boolean loadLayout;
    // Initial list of panel ids (TBD refactor)
    private List<String> panelIds;
    private PersistenceManager persistenceBean;

    /**
     * Initialize the lists
     */
    public DragDropBean() {
        initList();
    }

    /**
     * Reset the lists TBD for future clearing
     */
    public void reset() {
        initList();
    }

    /**
     * Initialize the report items and panel Ids
     */
    private void initList() {
        reports = new ArrayList<ReportBean>();
        panelIds = new ArrayList<String>();
        droppedPanelNames = new ArrayList<String>();
        for (int i = 0; i < 50; i++) {
            String panelId = new String("myPanel" + String.valueOf(i));
            panelIds.add(panelId);
            // createPanel(panelId);
        }

    }

    /**
     * Loads the layout from the persistence store. from xml asset
     *
     * @throws JSONException
     */

    public void loadLayout() {
        if (isLoadLayout()) {
            setLoadLayout(false);
            initList();
            List<ReportBean> reports = getPersistenceBean().loadReports();
            for (ReportBean report : reports) {
                // Create each panel with the saved layout
                addReport(new ReportBean(report.getPackageName(), report.getName(),
                        report.getHeight(), report.getWidth(), report.getLeft(), report.getTop()));

            }

        }

    }

    /**
     * Main function that does the following:
     * <p>
     * Builds the panel, makes it draggable, resizable and includes the report
     * content Note that it wasn't possible to make the panel
     * draggable/resizable in the xhtml so we do it here
     * </p>
     */

    @Override
    public void processDrop(DropEvent event) {
        ReportItem report = (ReportItem) event.getDragValue();
        if (report != null) {
            droppedPanelNames = new ArrayList<String>();
            addReport(toReportBean(report));
        }

    }

    /**
     * Convert the REportItem to a more usable ReportBean
     *
     * @param report
     * @return a ReportBean
     */
    private ReportBean toReportBean(ReportItem report) {
        ReportBean reportBean = new ReportBean();
        reportBean.setId(report.getId());
        reportBean.setName(report.getName());
        reportBean.setPackageName(report.getPackage());
        reportBean.setType(report.getType());
        reportBean.setVersion(report.getVersion());
        return reportBean;
    }

    /**
     * @param report
     */
    private void addReport(ReportBean report) {
        if (report != null) {
            // Add the report that the user just dragged
            reports.add(report);
            // Get a panel id

            populatePanel(report, reports.size() - 1);
        }

    }

    /**
     * @param report
     * @param report
     * @param panelId
     * @return
     */
    private void populatePanel(ReportBean report, int index) {
        String panelId = "myPanel" + index;
        Panel panel = getAssignedPanel(panelId);
// panel.setParent(RichFunction.findComponent("dashboardPanel"));
        /**
         * Store the dropped panel for rerendering Note that this has to be the
         * full clientId of the panel
         */
        addDroppedPanelName(RichFunction.clientId(panel.getClientId()));
        // FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add(RichFunction.clientId(panel.getClientId()));
        // Update the panel with necessary stuff
        panel.setLabel(report.getName());
        // Add the report
        panel.getChildren().add(getReportComponent(report, index));
        // Make it resizeable
       // panel.getChildren().add(getResizeable(panelId));
        // Make it draggable
       // panel.getChildren().add(getDraggable(panelId));

        // Set the width/height and position based on saved layout
        StringBuffer styleSB = new StringBuffer();
        /**
         * // Try to set position if (report.getWidth() != 0.0) {
         * styleSB.append(
         * "width:").append(Double.valueOf(report.getWidth()).intValue())
         * .append("px;"); } if (report.getHeight() != 0.0) {
         * styleSB.append("height:"
         * ).append(Double.valueOf(report.getHeight()).intValue())
         * .append("px;"); }
         *
         * if (report.getLeft() != 0.0) {
         * styleSB.append("position:absolute;left:")
         * .append(Double.valueOf(report.getLeft()).intValue()).append("px;"); }
         * if (report.getTop() != 0.0) {
         * styleSB.append("position:absolute;top:")
         * .append(Double.valueOf(report.getTop()).intValue()).append("px;"); }
         */
        styleSB.append("width:100;display:block;");
        panel.setStyle(styleSB.toString());
        getPanels().put(panel.getId(), new DashboardPanelHolder(panelId, true, report));
    }

    /**
     * Create a draggable panel
     *
     * @param panelId
     * @return
     *
    private UIComponent getDraggable(String panelId) {
        FacesContext fc = FacesContext.getCurrentInstance();
        Application application = fc.getApplication();
        Draggable draggable = (Draggable) application.createComponent(fc,
                "org.primefaces.component.Draggable", "org.primefaces.component.DraggableRenderer");
        draggable.setFor(panelId);
        draggable.setContainment("parent");
        return draggable;
    }

    /**
     * Create a resizeable panel
     *
     * @param panelId
     * @return
     *
    private UIComponent getResizeable(String panelId) {
        FacesContext fc = FacesContext.getCurrentInstance();
        Application application = fc.getApplication();
        Resizable resizable = (Resizable) application.createComponent(fc,
                "org.primefaces.component.Resizable", "org.primefaces.component.ResizableRenderer");
        resizable.setFor(panelId);
        return resizable;
    }
*/
    /**
     * TODO Remove this iFrame and replace with something else
     *
     * @param report
     * @return a UIOutput component
     */
    private UIComponent getReportComponent(ReportBean report, int index) {
        String htmlText = "<div id='drillDownPopup'></div><input type='hidden' name='reportName"
                + index
                + "' id='reportName"
                + index
                + "' value='"
                + report.getName()
                + "'/><input type='hidden' name='packageName"
                + index
                + "' id='packageName"
                + index
                + "' value='"
                + report.getPackageName()
                + "'/><iframe src=\"birt_inline.jsf?mdwReport="
                + report.getPackageName()
                + "/"
                + report.getName()
                + "\" class=\"iframe\" frameborder=\"0\" onload='javascript:resizeIframe(this);'></iframe>";
        UIOutput verbatim = new UIOutput();
        verbatim.setRendererType("javax.faces.Text");
        verbatim.getAttributes().put("escape", false);
        verbatim.setValue(htmlText);
        return verbatim;
    }

    /**
     * @param panelId
     * @return
     */
    private Panel getAssignedPanel(String panelId) {
        return (Panel) RichFunction.findComponent(panelId);
    }

    public Map<String, DashboardPanelHolder> getPanels() {
        if (panels == null) {
            panels = new HashMap<String, DashboardPanelHolder>();
        }
        return panels;
    }

    public void setPanels(Map<String, DashboardPanelHolder> panels) {
        this.panels = panels;
    }

    public List<ReportBean> getReports() {
        return reports;
    }

    public String getClosedPanelName() {
        return closedPanelName;
    }

    public void setClosedPanelName(String closedPanelName) {
        if (StringUtils.isEmpty(closedPanelName))
            return;
        this.closedPanelName = closedPanelName;
        DashboardPanelHolder panelHolder = getPanels().get(closedPanelName);
        if (panelHolder != null) {
            panelHolder.setVisible(false);
        }
    }

    public List<String> getPanelIds() {
        if (panelIds == null) {
            initList();
        }
        return panelIds;
    }

    public void setPanelIds(List<String> panelIds) {
        this.panelIds = panelIds;
    }

    public List<String> getDroppedPanelNames() {
        return droppedPanelNames;
    }

    public void setDroppedPanelNames(List<String> droppedPanelNames) {
        this.droppedPanelNames = droppedPanelNames;
    }

    public void addDroppedPanelName(String panelName) {
        if (droppedPanelNames == null) {
            droppedPanelNames = new ArrayList<String>();
        }
        droppedPanelNames.add(panelName);
    }

    public String getDashboardLayout() {
        return dashboardLayout;
    }

    public void setDashboardLayout(String dashboardLayout) {
        /**
         * Set a cookie
         */
        this.dashboardLayout = dashboardLayout;
        getPersistenceBean().saveReports(dashboardLayout);
    }
    public boolean isLoadLayout() {
        return loadLayout;
    }

    public void setLoadLayout(boolean loadLayout) {
        this.loadLayout = loadLayout;
    }

    public void beforePhase(PhaseEvent event) {
        if (event.getPhaseId() == PhaseId.RENDER_RESPONSE && isLoadLayout()) {
            // Runs right before the RENDER_RESPONSE.
                loadLayout();
         }
    }

    public PersistenceManager getPersistenceBean() {
        return persistenceBean;
    }

    public void setPersistenceBean(PersistenceManager persistenceBean) {
        this.persistenceBean = persistenceBean;
    }
}
