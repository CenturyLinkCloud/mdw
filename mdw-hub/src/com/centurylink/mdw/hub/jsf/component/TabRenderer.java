/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorContext;
import javax.faces.component.behavior.ClientBehaviorHint;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared.renderkit.html.HtmlLinkRendererBase;
import org.apache.myfaces.shared.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.FormInfo;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

import com.centurylink.mdw.web.jsf.components.NavOutputLink;
import com.centurylink.mdw.web.jsf.components.Tab;
import com.centurylink.mdw.web.jsf.components.TabLink;
import com.centurylink.mdw.web.jsf.components.TabPanel;

/**
 * HTML renderer for a tab.
 * Reimplemented for HTML5 engine.
 */
public class TabRenderer extends HtmlLinkRendererBase {
    public static final String RENDERER_TYPE = TabRenderer.class.getName();

    @Override
    protected void renderCommandLinkStart(FacesContext facesContext, UIComponent component,
            String clientId, Object value, String style, String styleClass) throws IOException {

        FormInfo formInfo = findNestingForm(component, facesContext);
        if (formInfo == null)
            throw new IOException("No containing form for component ID: " + component.getId());

        ResponseWriter writer = facesContext.getResponseWriter();

        String[] passthroughAttrs;
        Map<String,List<ClientBehavior>> behaviors = ((ClientBehaviorHolder)component).getClientBehaviors();

        if (isDisabled(component)) {
            renderMainTagStart(facesContext, writer, component, style, styleClass);
            writer.writeAttribute(HTML.ONCLICK_ATTR, ((HtmlCommandLink) component).getOnclick(), null);
        }
        else {
            renderBehaviorizedJavaScriptTagStart(facesContext, writer, component, clientId, behaviors, formInfo, style, styleClass);
        }

        if (!behaviors.isEmpty()) {
            HtmlRendererUtils.writeIdAndName(writer, component, facesContext);
        }
        else {
            HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
        }
        HtmlRendererUtils.renderBehaviorizedEventHandlersWithoutOnclick(facesContext, writer, component, behaviors);
        HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect( facesContext, writer, component, behaviors);
        passthroughAttrs = HTML.UNIVERSAL_ATTRIBUTES_WITHOUT_STYLE;

        HtmlRendererUtils.renderHTMLAttributes(writer, component, passthroughAttrs);
    }

    @Override
    protected void renderOutputLinkStart(FacesContext facesContext, UIOutput uiOutput) throws IOException {
        renderMainTagStart(facesContext, facesContext.getResponseWriter(), uiOutput, getStyle(facesContext, uiOutput), getStyleClass(facesContext, uiOutput));
    }

    @Override
    protected void renderOutputLinkEnd(FacesContext facesContext, UIComponent component) throws IOException {
        renderMainTagEnd(facesContext, facesContext.getResponseWriter(), component);
    }

    // TODO move to base class
    protected void renderMainTagStart(FacesContext facesContext, ResponseWriter writer, UIComponent component, String style, String styleClass)
    throws IOException {
        TabPanel tabPanel = (TabPanel)component.getParent();

        writer.startElement(HTML.DIV_ELEM, component);
        if (styleClass != null) {
            writer.writeAttribute(HTML.CLASS_ATTR, styleClass, null);
        }
        else if (tabPanel.getTabClass() != null) {
            writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getTabClass(), null);
        }
        if (style != null)
            writer.writeAttribute(HTML.STYLE_ATTR, style, null);

        writer.startElement(HTML.DIV_ELEM, component);
        writer.startElement(HTML.ANCHOR_ELEM, component);

        if (component instanceof TabLink)
            writer.writeAttribute(HTML.HREF_ATTR, ((TabLink)component).getAction(), null);
        else
            writer.writeAttribute(HTML.HREF_ATTR, "#", null);

        String tabIndex = null;
        if (component instanceof Tab)
            tabIndex = ((Tab)component).getTabindex();
        else if (component instanceof TabLink)
            tabIndex = ((TabLink)component).getTabindex();
        if (tabIndex != null)
            writer.writeAttribute(HTML.TABINDEX_ATTR, tabIndex, null);
    }

    protected void renderMainTagEnd(FacesContext facesContext, ResponseWriter writer, UIComponent component)
    throws IOException {
        TabPanel tabPanel = (TabPanel)component.getParent();

        if (component instanceof Tab)
            writer.writeText(((Tab)component).getLabel(), "label");
        else if (component instanceof NavOutputLink)
            writer.writeText(((NavOutputLink)component).getLabel(), "label");
        else
            throw new IllegalStateException("Invalid component: " + component);
        writer.endElement(HTML.ANCHOR_ELEM);
        writer.endElement(HTML.DIV_ELEM);

        // active tab image
        if (component instanceof Tab && ((Tab)component).isActive() && tabPanel.getActiveTabImage() != null) {
            writer.startElement(HTML.IMG_ELEM, component);
            writer.writeAttribute(HTML.SRC_ATTR, facesContext.getExternalContext().getRequestContextPath() + tabPanel.getActiveTabImage(), null);
            writer.writeAttribute(HTML.ALT_ATTR, "Selected", null);
            if (tabPanel.getActiveTabImageClass() != null)
                writer.writeAttribute(HTML.CLASS_ATTR, tabPanel.getActiveTabImageClass(), null);
            writer.endElement(HTML.IMG_ELEM);
        }

        writer.endElement(HTML.DIV_ELEM);
    }

    @Override
    protected void renderCommandLinkEnd(FacesContext facesContext, UIComponent component)
    throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();
        renderMainTagEnd(facesContext, writer, component);
    }

    protected void renderBehaviorizedJavaScriptTagStart(FacesContext facesContext, ResponseWriter writer,  UIComponent component,
            String clientId, Map<String, List<ClientBehavior>> behaviors, FormInfo formInfo, String style, String styleClass) throws IOException {
        String commandOnclick = ((HtmlCommandLink) component).getOnclick();

        // Calculate the script necessary to submit form
        String serverEventCode = buildServerOnclick(facesContext, component, clientId, formInfo);

        String onclick = null;

        if (commandOnclick == null
                && (behaviors.isEmpty() || (!behaviors.containsKey(ClientBehaviorEvents.CLICK) && !behaviors
                        .containsKey(ClientBehaviorEvents.ACTION)))) {
            // render only the submit script
            onclick = serverEventCode;
        }
        else {
            boolean hasSubmittingBehavior = hasSubmittingBehavior(behaviors,
                    ClientBehaviorEvents.CLICK)
                    || hasSubmittingBehavior(behaviors, ClientBehaviorEvents.ACTION);
            if (!hasSubmittingBehavior) {
                // ensure required resource javascript is available
                ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, writer);
            }

            // render javascript that chains the related client code
            Collection<ClientBehaviorContext.Parameter> paramList = HtmlRendererUtils
                .getClientBehaviorContextParameters(HtmlRendererUtils.mapAttachedParamsToStringValues(facesContext, component));

            onclick = HtmlRendererUtils.buildBehaviorChain(facesContext, component,
                ClientBehaviorEvents.CLICK, paramList, ClientBehaviorEvents.ACTION, paramList,
                behaviors, commandOnclick, hasSubmittingBehavior ? null : serverEventCode);
        }

        renderMainTagStart(facesContext, writer, component, style, styleClass);
        writer.writeAttribute(HTML.ONCLICK_ATTR, onclick, null);
    }

    private boolean hasSubmittingBehavior(Map<String, List<ClientBehavior>> clientBehaviors, String eventName) {
        List<ClientBehavior> eventBehaviors = clientBehaviors.get(eventName);
        if (eventBehaviors != null && !eventBehaviors.isEmpty()) {
            for (ClientBehavior behavior : eventBehaviors) {
                if (behavior.getHints().contains(ClientBehaviorHint.SUBMITTING)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isDisabled(UIComponent component) {
        Object obj = component.getAttributes().get("disabled");
        if (obj instanceof String)
            return new Boolean((String) obj);
        else if (obj instanceof Boolean)
            return ((Boolean) obj).booleanValue();
        else
            return false;
    }

}
