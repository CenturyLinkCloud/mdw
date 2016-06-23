/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

public class PanelRenderer extends Renderer {

    private static final String HAS_PANEL_COLLAPSE_JAVASCRIPT = "mdw_hasPanelCollapseJavascript";
    private static final String HAS_PANEL_COLLAPSE_JAVASCRIPT_HORIZONTAL = "mdw_hasPanelCollapseJavascriptHorizontal";
    private static final String PANEL_EXPANDED_DATA_ATTRIBUTE = "data-mdw-panel-expanded";

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        Panel panel = (Panel) component;
        ResponseWriter writer = facesContext.getResponseWriter();

        // toggle javascript
        if (panel.isCollapsible())
            renderPanelToggleScript(facesContext, panel);

        writer.startElement("div", panel); // outer
        writer.writeAttribute("id", panel.getClientId(), null);

        if (panel.getStyleClass() != null)
            writer.writeAttribute("class", panel.getStyleClass(), null);
        if (panel.getStyle() != null)
            writer.writeAttribute("style", panel.getStyle(), null);

        writer.startElement("div", panel);  // header
        writer.writeAttribute("id", panel.getClientId() + "_header", null);
        String headerClass = panel.getHeaderStyleClass();
        if (panel.isCollapsible() && panel.isCollapsed())
            headerClass += " " + panel.getHeaderCollapsedStyleClass();
        writer.writeAttribute("class", headerClass, null);

        if (panel.isCollapsible()) {
            if (panel.isCollapsed() && panel.isHorizontal()) {
                renderHeaderImage(facesContext, panel);
                renderHeaderLabel(facesContext, panel);
            }
            else {
                renderHeaderLabel(facesContext, panel);
                renderHeaderButtons(facesContext, panel);
                renderHeaderImage(facesContext, panel);
            }
        }
        else {
            renderHeaderLabel(facesContext, panel);
            renderHeaderButtons(facesContext, panel);
        }

        writer.endElement("div"); // header

        writer.startElement("div", panel);  // body
        writer.writeAttribute("id", panel.getClientId() + "_body", null);
        String bodyClass = panel.getBodyStyleClass();
        if (panel.isCollapsible() && panel.isCollapsed())
            bodyClass += " " + panel.getBodyCollapsedStyleClass();
        writer.writeAttribute("class", bodyClass, null);
        if (panel.isCollapsible())
            writer.writeAttribute(PANEL_EXPANDED_DATA_ATTRIBUTE, "" + !panel.isCollapsed(), null);

        if (panel.isCollapsible()) {
            writer.startElement("div", panel);  // body inner
            writer.writeAttribute("id", panel.getClientId() + "_bodyInner", null);
            writer.writeAttribute("style", "v-overflow:hidden;", null);
        }
    }

    protected void renderHeaderLabel(FacesContext facesContext, Panel panel) throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();
        // enclose label in div
        writer.startElement("div", panel);
        writer.writeAttribute("id", panel.getClientId() + "_labelDiv", null);
        String labelDivClass = panel.getLabelDivStyleClass();
        if (panel.isCollapsible() && panel.isCollapsed())
            labelDivClass += " " + panel.getLabelDivCollapsedClass();
        writer.writeAttribute("class", labelDivClass, null);
        writer.write(panel.getLabel());
        writer.endElement("div");  // label
    }

    protected void renderHeaderButtons(FacesContext facesContext, Panel panel) throws IOException {
        UIComponent facet = panel.getFacet(Panel.HEADER_BTN_FACET_NAME);
        if (facet != null) {
            facet.encodeAll(facesContext);
        }
    }

    protected void renderHeaderImage(FacesContext facesContext, Panel panel) throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();
        // header image
        writer.startElement("div", panel); // toggle
        writer.writeAttribute("id", panel.getClientId() + "_imgDiv", null);
        String imgDivClass = panel.getImageDivStyleClass();
        if (panel.isCollapsible() && panel.isCollapsed())
            imgDivClass += " " + panel.getImageDivCollapsedClass();
        writer.writeAttribute("class", imgDivClass, null);
        if (panel.isHorizontal())
            writer.writeAttribute("onclick", "toggleMdwPanelExpandHorizontal('" + panel.getClientId() + "');", null);
        else
            writer.writeAttribute("onclick", "toggleMdwPanelExpand('" + panel.getClientId() + "');", null);
        String image = panel.isCollapsed() ? panel.getExpandImage() : panel.getCollapseImage();
        writer.startElement("img", panel);
        writer.writeAttribute("id", panel.getClientId() + "_img", null);
        writer.writeAttribute("src", getBaseUrl(facesContext) + image, null);
        writer.writeAttribute("alt", panel.isCollapsed() ? "Expand" : "Collapse", null);
        if (panel.getImageStyleClass() != null)
            writer.writeAttribute("class", panel.getImageStyleClass(), null);
        writer.endElement("img");
        writer.endElement("div"); // toggle
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();
        if (((Panel)component).isCollapsible())
            writer.endElement("div"); // body inner
        writer.endElement("div"); // body
        writer.endElement("div"); // panel
    }

    /**
     * Assumes all vertically collapsible panels on the page are configured similarly to each other
     * and all horizontally collapsible panels on the page are also configured similarly to each other.
     * Renders only one function declaration for each orientation.
     */
    protected void renderPanelToggleScript(FacesContext facesContext, Panel panel) throws IOException {
        boolean horizontal = panel.isHorizontal();
        if (horizontal) {
            Object hasScript = facesContext.getExternalContext().getRequestMap().get(HAS_PANEL_COLLAPSE_JAVASCRIPT_HORIZONTAL);
            if (hasScript != null && (Boolean)hasScript)
                return; // already rendered for horizontal
        }
        else {
            Object hasScript = facesContext.getExternalContext().getRequestMap().get(HAS_PANEL_COLLAPSE_JAVASCRIPT);
            if (hasScript != null && (Boolean)hasScript)
                return; // already rendered for vertical
        }

        ResponseWriter writer = facesContext.getResponseWriter();
        writer.write("\n");
        writer.startElement("script", panel);
        writer.writeAttribute("type", "text/javascript", null);
        StringBuffer script = new StringBuffer();
        script.append("\n");
        if (horizontal)
            script.append("function toggleMdwPanelExpandHorizontal(panelId)\n");
        else
            script.append("function toggleMdwPanelExpand(panelId)\n");
        script.append("{\n");
        script.append("  //@ sourceURL=dynamic.js;\n");
        script.append("  var panelBody = document.getElementById(panelId + '_body');\n");
        script.append("  var panelHeader = document.getElementById(panelId + '_header');\n");
        script.append("  var bodyInner = document.getElementById(panelId + '_bodyInner');\n");
        script.append("  var panelImg = document.getElementById(panelId + '_img');\n");
        script.append("  var labelDiv = document.getElementById(panelId + '_labelDiv');\n");
        script.append("  var headerImgDiv = document.getElementById(panelId + '_imgDiv');\n");
        script.append("  var expanding = !(panelBody.getAttribute('" + PANEL_EXPANDED_DATA_ATTRIBUTE + "') === 'true');\n");
        String headerCollapsedClass = panel.getHeaderCollapsedStyleClass();
        if (headerCollapsedClass != null && headerCollapsedClass.trim().length() == 0)
            headerCollapsedClass = null;
        String labelCollapsedClass = panel.getLabelDivCollapsedClass();
        if (labelCollapsedClass != null && labelCollapsedClass.trim().length() == 0)
            labelCollapsedClass = null;
        String imageDivCollapsedClass = panel.getImageDivCollapsedClass();
        if (imageDivCollapsedClass != null && imageDivCollapsedClass.trim().length() == 0)
            imageDivCollapsedClass = null;
        if (horizontal) {
            script.append("  var oldWidth = panelBody.style.width;\n");
            script.append("  panelBody.style.width = bodyInner.offsetWidth + 'px';\n");
        }
        else {
            script.append("  var oldHeight = panelBody.style.height;\n");
            script.append("  panelBody.style.height = bodyInner.offsetHeight + 'px';\n");
        }
        script.append("  if (expanding)\n");
        script.append("  {\n");
        if (horizontal) {
            script.append("  // reverse order of elements\n");
            script.append("  panelHeader.removeChild(labelDiv);\n");
            script.append("  panelHeader.insertBefore(labelDiv, headerImgDiv);\n");
        }
        if (headerCollapsedClass != null)
            script.append("    panelHeader.classList.remove('" + headerCollapsedClass + "');\n");
        if (imageDivCollapsedClass != null)
            script.append("    headerImgDiv.classList.remove('" + imageDivCollapsedClass + "');\n");
        if (labelCollapsedClass != null)
            script.append("    labelDiv.classList.remove('" + labelCollapsedClass + "');\n");

        script.append("    setTimeout(function()\n");
        script.append("      {\n");
        script.append("        panelBody.classList.remove('" + panel.getBodyCollapsedStyleClass() + "');\n");
        if (horizontal)
            script.append("        panelBody.style.width = oldWidth;\n");
        else
            script.append("        panelBody.style.height = oldHeight;\n");
        script.append("      }, " + panel.getTransitionDuration() + ");\n");

        script.append("    panelImg.src = '" + getBaseUrl(facesContext) + panel.getCollapseImage() + "';\n");
        script.append("    panelImg.alt = 'Collapse';\n");
        script.append("  }\n");
        script.append("  else\n");
        script.append("  {\n");
        script.append("    setTimeout(function()\n");
        script.append("      {\n");
        if (horizontal)
            script.append("        panelBody.style.width = oldWidth;\n");
        else
            script.append("        panelBody.style.height = oldHeight;\n");
        script.append("        panelBody.classList.add('" + panel.getBodyCollapsedStyleClass() + "');\n");
        script.append("      }, 20);\n");



        if (headerCollapsedClass != null)
            script.append("    panelHeader.classList.add('" + headerCollapsedClass + "');\n");
        if (labelCollapsedClass != null)
            script.append("    labelDiv.classList.add('" + labelCollapsedClass + "');\n");
        if (imageDivCollapsedClass != null)
            script.append("    headerImgDiv.classList.add('" + imageDivCollapsedClass + "');\n");
        if (horizontal) {
            script.append("    // reverse order of elements\n");
            script.append("    panelHeader.removeChild(headerImgDiv);\n");
            script.append("    panelHeader.insertBefore(headerImgDiv, labelDiv);\n");
        }
        script.append("    panelImg.src = '" + getBaseUrl(facesContext) + panel.getExpandImage() + "';\n");
        script.append("    panelImg.alt = 'Expand';\n");
        script.append("  }\n");
        script.append("  panelBody.setAttribute('" + PANEL_EXPANDED_DATA_ATTRIBUTE + "', expanding);\n");
        script.append("}\n");
        writer.write(script.toString());
        writer.endElement("script");

        if (horizontal)
            facesContext.getExternalContext().getRequestMap().put(HAS_PANEL_COLLAPSE_JAVASCRIPT_HORIZONTAL, new Boolean(true));
        else
            facesContext.getExternalContext().getRequestMap().put(HAS_PANEL_COLLAPSE_JAVASCRIPT, new Boolean(true));
    }

    protected String getBaseUrl(FacesContext facesContext) {
        ExternalContext extCtx = facesContext.getExternalContext();
        String url = extCtx.getRequestScheme() + "://" + extCtx.getRequestServerName();
        if (extCtx.getRequestServerPort() > 0)
            url += ":" + extCtx.getRequestServerPort();
        url += extCtx.getRequestContextPath() + "/";
        return url;
    }

}
