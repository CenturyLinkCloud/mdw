/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.list;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.hub.ui.MenuBuilder;
import com.centurylink.mdw.hub.ui.MenuItem;
import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.GroupingOption;

public class ListMenuBuilder implements MenuBuilder {

    public static final String MENU = "menu";
    public static final String GROUP_BY = "groupBy";
    public static final String PAGE_SIZE = "pageSize";

    private String listId;
    private List<GroupingOption> groupOptions;
    private List<Integer> pageSizeOptions;

    public ListMenuBuilder(String listId, List<GroupingOption> groupOptions, List<Integer> pageSizeOptions) {
        this.listId = listId;
        this.groupOptions = groupOptions;
        this.pageSizeOptions = pageSizeOptions;
    }

    public MenuType getType() {
        return MenuType.toolbar;
    }

    public String getImage() {
        return "/images/list.gif";
    }

    public String getLabel() {
        return "List Menu";
    }

    public String getStyleClass() {
        return null;
    }

    public String getHandler() {
        return "listMenuController";
    }

    public String getHandlerMethod() {
        return "doAction";
    }

    public String getExecute() {
        return "@form";
    }

    public String getRender() {
        return "@form";
    }

    public List<MenuItem> getMenu() throws UIException {

        String menuId = listId + "_" + MENU;

        List<MenuItem> items = new ArrayList<MenuItem>();

        if (groupOptions != null) {
            String groupSel = ListManager.getInstance().getGroupBy(listId);
            String groupMenuId = menuId + "_" + GROUP_BY;
            MenuItem item = new MenuItem(groupMenuId, "Group By", GROUP_BY);
            item.setImage("/images/group_by.gif");
            List<MenuItem> children = new ArrayList<MenuItem>();
            for (GroupingOption groupOption : getGroupOptions(groupOptions)) {
                MenuItem child = new MenuItem(groupMenuId + "_" + groupOption.getId(), groupOption.getLabel(), GROUP_BY);
                if (groupOption.getId().equals(groupSel) || (groupSel == null && "none".equals(groupOption.getId())))
                    child.setImage("/images/checked.gif");
                else
                    child.setImage("/images/unchecked.gif");
                child.setData(listId); // listId
                child.setSubmit(true);
                children.add(child);
            }
            item.setChildren(children);
            items.add(item);
        }

        if (pageSizeOptions != null) {
            Integer pageSel = ListManager.getInstance().getDisplayRows(listId);
            String pageMenuId = menuId + "_" + PAGE_SIZE;
            MenuItem item = new MenuItem(pageMenuId, "Display Rows", PAGE_SIZE);
            item.setImage("/images/grid.png");
            List<MenuItem> children = new ArrayList<MenuItem>();
            for (Integer pageSizeOption : getPageSizeOptions(pageSizeOptions)) {
                String label = pageSizeOption.intValue() == 0 ? "All" : String.valueOf(pageSizeOption);
                MenuItem child = new MenuItem(pageMenuId + "_" + pageSizeOption, label, PAGE_SIZE);
                if (pageSizeOption.equals(pageSel))
                    child.setImage("/images/checked.gif");
                else
                    child.setImage("/images/unchecked.gif");
                child.setData(listId); // listId
                child.setSubmit(true);
                children.add(child);
            }
            item.setChildren(children);
            items.add(item);
        }

        return items;
    }

    protected List<Integer> getPageSizeOptions(List<Integer> pageSizeOptions) {
        if (pageSizeOptions != null)
            pageSizeOptions.add(0);
        return pageSizeOptions;
    }

    protected List<GroupingOption> getGroupOptions(List<GroupingOption> groupOptions) {
        if (groupOptions != null)
            groupOptions.add(new GroupingOption("none", "(None)"));
        return groupOptions;
    }


}
