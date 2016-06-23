/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import org.richfaces.component.UIMenuItem;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.list.SortableList;

public class ListMenuController implements ActionListener {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    
    public static final String MENU = "menu";
    public static final String GROUP_BY_MENU = MENU + "_groupBy";
    public static final String PAGE_SIZE_MENU = MENU + "_pageSize";

    public Object doAction() {
        return null;
    }
    
    public void processAction(ActionEvent actionEvent) throws AbortProcessingException {
        try {
            String action = String.valueOf(((UIMenuItem)actionEvent.getComponent()).getId());
            String listId = String.valueOf(((UIMenuItem)actionEvent.getComponent()).getData());
            ListManager listMgr = (ListManager) ListManager.getInstance();
            SortableList list = listMgr.getList(listId);
            if (action.startsWith(listId + "_" + GROUP_BY_MENU)) {
                String groupBy = action.substring(action.lastIndexOf('_') + 1);
                list.setGroupBy(groupBy);
                listMgr.setGroupBy(listId, groupBy); // remember for the session
            }
            else if (action.startsWith(listId + "_" + PAGE_SIZE_MENU)) {
                Integer pageSize = Integer.parseInt(action.substring(action.lastIndexOf('_') + 1)); 
                list.setDisplayRows(pageSize);
            }
            listMgr.clearList(listId);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            FacesVariableUtil.addMessage(ex.toString());
            throw new AbortProcessingException(ex.getMessage(), ex);
        }
        
    }

}
