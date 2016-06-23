/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.task;

import javax.faces.component.UIComponent;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.hub.jsf.component.ActionMenu;
import com.centurylink.mdw.hub.ui.MenuBuilder;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.web.ui.UIException;

public class WorkgroupTasks extends com.centurylink.mdw.taskmgr.ui.tasks.list.WorkgroupTasks {

    public WorkgroupTasks(ListUI listUI) {
        super(listUI);
    }

    private UIComponent actionMenu;
    public UIComponent getActionMenu() throws UIException, CachingException {
        if (isEmpty())
            return null;
        if (actionMenu == null) {
            MenuBuilder menuBuilder = new TaskListMenuBuilder(getId(), getTaskActions());
            actionMenu = new ActionMenu(menuBuilder);
        }
        return actionMenu;
    }

}
