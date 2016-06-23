/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.io.IOException;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.datascroller.HtmlDataScroller;
import org.richfaces.component.UIDropDownMenu;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.jsf.components.DataScroller;

/**
 * Custom datascroller renderer for desired look-and-feel.
 */
public class DataScrollerRenderer extends com.centurylink.mdw.web.jsf.renderers.DataScrollerRenderer {
    public static final String RENDERER_TYPE = DataScrollerRenderer.class.getName();

    protected UICommand getCommandLink(FacesContext facesContext, HtmlDataScroller scroller, String facetName) {
        UICommand link = isAjaxEnabled(scroller) ? getAjaxLink(facesContext, scroller, facetName) : super.getLink(facesContext, scroller, facetName);
        link.setImmediate(true);
        return link;
    }

    @Override
    protected void renderListButtons(FacesContext facesContext, DataScroller scroller) throws IOException {
        UIDropDownMenu listMenu = null;
        UIDropDownMenu actionMenu = null;
        for (UIComponent child : scroller.getChildren()) {
            if (child instanceof UIDropDownMenu) {
                if (child.getId().endsWith("_menu"))
                    listMenu = (UIDropDownMenu) child;
                else if (child.getId().endsWith("_actions"))
                    actionMenu = (UIDropDownMenu) child;
            }
        }
        if (actionMenu != null) {
            actionMenu.encodeBegin(facesContext);
            actionMenu.encodeChildren(facesContext);
            actionMenu.encodeEnd(facesContext);
        }
        if (listMenu != null) {
            listMenu.encodeBegin(facesContext);
            listMenu.encodeChildren(facesContext);
            listMenu.encodeEnd(facesContext);
        }

        if (isRefreshable(scroller))
            renderRefreshButton(facesContext, scroller);
        if (isExportable(scroller))
            renderExportButton(facesContext, scroller);
        if (hasPreferences(scroller))
            renderPrefsButton(facesContext, scroller);

        renderCustomListButtons(facesContext, scroller);

        if (isSearchable(scroller))
            renderSearchButton(facesContext, scroller);

    }

    @Override
    protected UICommand renderPrefsButton(FacesContext facesContext, DataScroller scroller) throws IOException {
        FacesVariableUtil.setValue("filterId", scroller.getAttributes().get("filterId"));
        return renderListButton(facesContext, scroller, "listPrefsButton", "List Preferences", "prefs.gif", false, false, "displayListPrefsPopup();return false;", null);
    }

    @Override
    protected boolean shouldRenderChild(DataScroller scroller, UIComponent child) {
        return super.shouldRenderChild(scroller, child) && !(child instanceof UIDropDownMenu);
    }

    @Override
    protected String getImageBaseUrl() throws IOException {
        return getProperty(PropertyNames.MDW_HUB_URL) + "/images";
    }

}
