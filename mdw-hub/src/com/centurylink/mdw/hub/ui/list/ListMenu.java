/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.list;

import java.util.List;

import com.centurylink.mdw.hub.jsf.component.ActionMenu;
import com.centurylink.mdw.hub.ui.MenuBuilder;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.GroupingOption;

public class ListMenu extends ActionMenu {

    public ListMenu() {
    }

    public ListMenu(MenuBuilder builder) throws UIException {
        super(builder);
    }

    public ListMenu(String listId, List<GroupingOption> groupOptions, List<Integer> pageSizeOptions) throws UIException {
        super(new ListMenuBuilder(listId, groupOptions, pageSizeOptions));
    }
}