/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import java.util.List;

import javax.el.MethodExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.event.ActionListener;

import org.richfaces.component.Positioning;
import org.richfaces.component.UIDropDownMenu;
import org.richfaces.component.UIMenuGroup;
import org.richfaces.component.UIMenuItem;

import com.centurylink.mdw.hub.ui.MenuBuilder;
import com.centurylink.mdw.hub.ui.MenuBuilder.MenuType;
import com.centurylink.mdw.hub.ui.MenuItem;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class ActionMenu extends UIDropDownMenu {

    private MenuBuilder builder;

    public ActionMenu(MenuBuilder builder) throws UIException {
        this.builder = builder;
        buildMenuComponent();
    }

    public ActionMenu() {
    }

    /**
     * Should not be overridden due to consequences of importing JSF classes into Dynamic Java client code.
     */
    protected void buildMenuComponent() throws UIException {
        if (builder.getType() == MenuType.toolbar) {
            HtmlGraphicImage labelImg = new HtmlGraphicImage();
            labelImg.setValue(builder.getImage());
            labelImg.setAlt(builder.getLabel());
            getFacets().put("label", labelImg);
            if (builder.getStyleClass() != null)
                labelImg.setStyleClass(builder.getStyleClass());
        }
        else if (builder.getType() == MenuType.button) {
            HtmlCommandButton labelBtn = new HtmlCommandButton();
            labelBtn.setValue(builder.getLabel());
            labelBtn.setType("button");
            labelBtn.setOnclick("return false;");
            if (builder.getStyleClass() != null)
                labelBtn.setStyleClass(builder.getStyleClass());
        }

        buildMenu(this, builder.getMenu());
    }

    protected void buildMenu(UIComponent parent, List<MenuItem> items) {
        for (MenuItem item : items) {
            if (item.getChildren() != null) {
                UIMenuGroup menuGroup = new UIMenuGroup();
                menuGroup.setId(item.getId());
                menuGroup.setLabel(item.getLabel());
                if (item.getImage() != null)
                    menuGroup.setIcon(item.getImage());
                menuGroup.getAttributes().put("nonAlias", item.getAction());
                menuGroup.setDirection(Positioning.autoRight);
                menuGroup.setJointPoint(Positioning.autoRight);
                parent.getChildren().add(menuGroup);
                buildMenu(menuGroup, item.getChildren());
            }
            else {
                UIMenuItem menuItem = buildItem(item);
                parent.getChildren().add(menuItem);
            }
        }
    }

    protected UIMenuItem buildItem(MenuItem item) {
        UIMenuItem menuItem = new UIMenuItem();
        menuItem.setId(item.getId());
        menuItem.setLabel(item.getLabel());
        if (item.getImage() != null)
            menuItem.setIcon(item.getImage());
        if (item.getData() != null)
            menuItem.setData(item.getData());
        else
            menuItem.setData(item.getAction());
        menuItem.setRender(builder.getRender());
        menuItem.setExecute(builder.getExecute());
        MethodExpression me = FacesVariableUtil.createMethodExpression("#{" + builder.getHandler() + "." + builder.getHandlerMethod() + "}", String.class, null);
        menuItem.setActionExpression(me);
        menuItem.addActionListener((ActionListener) FacesVariableUtil.getValue(builder.getHandler()));
        menuItem.setImmediate(item.isSubmit());
        if (item.isRequireComment())
            menuItem.setOnclick("showCommentDialog('" + item.getAction() + "', '" + item.getData() + "'); return false;");
        return menuItem;
    }
}
