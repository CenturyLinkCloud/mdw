/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;

public class Panel extends UIPanel {
    public static final String COMPONENT_TYPE = Panel.class.getName();

    public static final String DEFAULT_COLLAPSE_IMAGE = "images/panel_collapse.gif";
    public static final String DEFAULT_COLLAPSE_IMAGE_HORIZONTAL = "images/panel_collapse_horizontal.gif";
    public static final String DEFAULT_EXPAND_IMAGE = "images/panel_expand.gif";
    public static final String DEFAULT_EXPAND_IMAGE_HORIZONTAL = "images/panel_expand_horizontal.gif";
    public static final String DEFAULT_HEADER_STYLE_CLASS = "mdw_panelHeader";
    public static final String DEFAULT_BODY_STYLE_CLASS = "mdw_panelBody";
    public static final String DEFAULT_BODY_COLLAPSED_CLASS = "mdw_panelBodyCollapsed";
    public static final String DEFAULT_BODY_COLLAPSED_CLASS_HORIZONTAL = "mdw_panelBodyCollapsedHorizontal";
    public static final String DEFAULT_HEADER_COLLAPSED_CLASS = "";
    public static final String DEFAULT_HEADER_COLLAPSED_CLASS_HORIZONTAL = "mdw_panelHeaderCollapsedHorizontal";
    public static final String DEFAULT_LABEL_DIV_STYLE_CLASS = "mdw_panelLabel";
    public static final String DEFAULT_LABEL_DIV_COLLAPSED_CLASS = "";
    public static final String DEFAULT_LABEL_DIV_COLLAPSED_CLASS_HORIZONTAL = "mdw_panelLabelCollapsedHorizontal";
    public static final String DEFAULT_IMAGE_DIV_STYLE_CLASS = "mdw_panelImage";
    public static final String DEFAULT_IMAGE_DIV_COLLAPSED_CLASS = "";
    public static final String DEFAULT_IMAGE_DIV_COLLAPSED_CLASS_HORIZONTAL = "mdw_panelImageCollapsedHorizontal";
    public static final int DEFAULT_TRANSITION_DURATION = 300;
    public static final int DEFAULT_TRANSITION_DURATION_HORIZONTAL = 20;
    public static final String HEADER_BTN_FACET_NAME = "headerButtons";

    public String getLabel() {
        return (String) getStateHelper().eval("label");
    }
    public void setLabel(String label) {
        getStateHelper().put("label", label);
    }

    public String getStyle() {
        return (String) getStateHelper().eval("style");
    }
    public void setStyle(String style) {
        getStateHelper().put("style", style);
    }

    public String getStyleClass() {
        return (String) getStateHelper().eval("styleClass");
    }
    public void setStyleClass(String styleClass) {
        getStateHelper().put("styleClass", styleClass);
    }

    public String getHeaderStyleClass() {
        return (String) getStateHelper().eval("headerStyleClass", DEFAULT_HEADER_STYLE_CLASS);
    }
    public void setHeaderStyleClass(String headerStyleClass) {
        getStateHelper().put("headerStyleClass", headerStyleClass);
    }

    public String getLabelDivStyleClass() {
        return (String) getStateHelper().eval("labelDivStyleClass", DEFAULT_LABEL_DIV_STYLE_CLASS);
    }
    public void setLabelDivStyleClass(String labelDivStyleClass) {
        getStateHelper().put("labelDivStyleClass", labelDivStyleClass);
    }

    public String getLabelDivCollapsedClass() {
        String def = isHorizontal() ? DEFAULT_LABEL_DIV_COLLAPSED_CLASS_HORIZONTAL : DEFAULT_LABEL_DIV_COLLAPSED_CLASS;
        return (String) getStateHelper().eval("labelDivCollapsedClass", def);
    }
    public void setLabelDivCollapsedClass(String labelDivCollapsedClass) {
        getStateHelper().put("labelDivCollapsedClass", labelDivCollapsedClass);
    }

    public String getBodyStyleClass() {
        return (String) getStateHelper().eval("bodyStyleClass", DEFAULT_BODY_STYLE_CLASS);
    }
    public void setBodyStyleClass(String bodyStyleClass) {
        getStateHelper().put("bodyStyleClass", bodyStyleClass);
    }

    public boolean isCollapsible() {
        return (Boolean) getStateHelper().eval("collapsible", false);
    }
    public void setCollapsible(boolean collapsible) {
        getStateHelper().put("collapsible", collapsible);
    }

    public boolean isHorizontal() {
        return (Boolean) getStateHelper().eval("horizontal", false);
    }
    public void setHorizontal(boolean horizontal) {
        getStateHelper().put("horizontal", horizontal);
    }

    public boolean isCollapsed() {
        return (Boolean) getStateHelper().eval("collapsed", false);
    }
    public void setCollapsed(boolean collapsed) {
        getStateHelper().put("collapsed", collapsed);
    }

    public String getBodyCollapsedStyleClass() {
        String def = isHorizontal() ? DEFAULT_BODY_COLLAPSED_CLASS_HORIZONTAL : DEFAULT_BODY_COLLAPSED_CLASS;
        return (String) getStateHelper().eval("bodyCollapsedStyleClass", def);
    }
    public void setBodyCollapsedStyleClass(String bodyCollapsedStyleClass) {
        getStateHelper().put("bodyCollapsedStyleClass", bodyCollapsedStyleClass);
    }

    public String getHeaderCollapsedStyleClass() {
        String def = isHorizontal() ? DEFAULT_HEADER_COLLAPSED_CLASS_HORIZONTAL : DEFAULT_HEADER_COLLAPSED_CLASS;
        return (String) getStateHelper().eval("headerCollapsedStyleClass", def);
    }
    public void setHeaderCollapsedStyleClass(String headerCollapsedStyleClass) {
        getStateHelper().put("headerCollapsedStyleClass", headerCollapsedStyleClass);
    }

    public String getCollapseImage() {
        String def = isHorizontal() ? DEFAULT_COLLAPSE_IMAGE_HORIZONTAL : DEFAULT_COLLAPSE_IMAGE;
        return (String) getStateHelper().eval("collapseImage", def);
    }
    public void setCollapseImage(String collapseImage) {
        getStateHelper().put("collapseImage", collapseImage);
    }

    public String getExpandImage() {
        String def = isHorizontal() ? DEFAULT_EXPAND_IMAGE_HORIZONTAL : DEFAULT_EXPAND_IMAGE;
        return (String) getStateHelper().eval("expandImage", def);
    }
    public void setExpandImage(String expandImage) {
        getStateHelper().put("expandImage", expandImage);
    }

    public String getImageStyleClass() {
        return (String) getStateHelper().eval("imageStyleClass");
    }
    public void setImageStyleClass(String imageStyleClass) {
        getStateHelper().put("imageStyleClass", imageStyleClass);
    }

    public String getImageDivStyleClass() {
        return (String) getStateHelper().eval("imageDivStyleClass", DEFAULT_IMAGE_DIV_STYLE_CLASS);
    }
    public void setImageDivStyleClass(String imageDivStyleClass) {
        getStateHelper().put("imageDivStyleClass", imageDivStyleClass);
    }

    public String getImageDivCollapsedClass() {
        String def = isHorizontal() ? DEFAULT_IMAGE_DIV_COLLAPSED_CLASS_HORIZONTAL : DEFAULT_IMAGE_DIV_COLLAPSED_CLASS;
        return (String) getStateHelper().eval("imageDivCollapsedClass", def);
    }
    public void setImageDivCollapsedClass(String imageDivCollapsedClass) {
        getStateHelper().put("imageDivCollapsedClass", imageDivCollapsedClass);
    }

    /**
     * Set this to match the transition duration in your panel body style
     */
    public int getTransitionDuration() {
        int def = isHorizontal() ? DEFAULT_TRANSITION_DURATION_HORIZONTAL : DEFAULT_TRANSITION_DURATION;
        return (Integer) getStateHelper().eval("transitionDuration", def);
    }
    public void setTransitionDuration(int transitionDuration) {
        getStateHelper().put("transitionDuration", transitionDuration);
    }

    public UIComponent getHeaderButtons() {
        return (UIComponent) getFacets().get(HEADER_BTN_FACET_NAME);
    }

    public void setHeaderButtonsLast(UIComponent headerButtons) {
        getFacets().put(HEADER_BTN_FACET_NAME, headerButtons);
    }

}
