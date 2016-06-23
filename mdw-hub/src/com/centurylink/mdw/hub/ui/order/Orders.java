/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.order;

import javax.faces.model.DataModel;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.taskmgr.ui.data.PagedListDataModel;
import com.centurylink.mdw.taskmgr.ui.data.RowConverter;
import com.centurylink.mdw.taskmgr.ui.layout.ListUI;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.process.ProcessInstanceItem;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class Orders extends SortableList {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public Orders(ListUI listUI) {
        super(listUI);
    }

    /**
     * @see com.centurylink.mdw.taskmgr.ui.list.SortableList#retrieveItems()
     */
    public DataModel<ListItem> retrieveItems() throws UIException {
        try {
            PagedListDataModel pagedDataModel = new OrderDataModel(getListUI(), getFilter(), getUserPreferredDbColumns());

            pagedDataModel.setRowConverter(new RowConverter() {
                public Object convertRow(Object o) {
                    ProcessInstanceVO processInstance = (ProcessInstanceVO) o;
                    ProcessInstanceItem instance = new ProcessInstanceItem(processInstance);
                    return instance;
                }
            });

            return pagedDataModel;
        }
        catch (Exception ex) {
            String msg = "Problem retrieving Orders.";
            logger.severeException(msg, ex);
            throw new UIException(msg, ex);
        }
    }
    /**
     * @return the blvLink
     */
    public String getBlvLink()
    {
      return blvLink;
    }
    /**
     * @param blvLink the blvLink to set
     */
    public void setBlvLink(String blvLink)
    {
      this.blvLink = blvLink;
    }
    private String blvLink;
}
