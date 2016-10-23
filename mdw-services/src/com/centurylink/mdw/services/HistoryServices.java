/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.event.HistoryList;

/**
 * Services for getting the audit log in mdw-admin.
 */
public interface HistoryServices {
    public HistoryList getHistory(int l) throws ServiceException;
}
