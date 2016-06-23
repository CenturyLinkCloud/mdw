/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui;

import java.util.List;

import com.centurylink.mdw.web.ui.UIException;

public interface MenuBuilder {

    public enum MenuType {
        toolbar,
        button
    }

    public MenuType getType();
    public String getImage();
    public String getLabel();
    public String getStyleClass();


    /**
     * menu tree
     */
    public List<MenuItem> getMenu() throws UIException;

    /**
     * action handler managed bean
     */
    public String getHandler();
    public String getHandlerMethod();

    public String getExecute();
    public String getRender();

}
