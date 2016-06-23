/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.history;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.model.value.history.HistoryList;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.services.HistoryServices;
import com.centurylink.mdw.services.dao.process.EngineDataAccessDB;

public class HistoryServicesImpl implements HistoryServices {
    public HistoryList getHistory(int historyLength) throws ServiceException {
        List<UserActionVO> historyVOList = new ArrayList<UserActionVO>();
        HistoryList historyList;
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            historyVOList = edao.getAuditLogs("CREATE_DT", false, null, null, 0, historyLength, true);
        }
        catch (SQLException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        catch (DataAccessException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        finally {
            try {
                edao.stopTransaction(transaction);
            }
            catch (DataAccessException e) {
                throw new ServiceException(e.getMessage(), e);
            }
        }
            Collections.sort(historyVOList);
            historyList = new HistoryList(HistoryList.ALL_HISTORY, historyVOList);
            historyList.setRetrieveDate(new Date()); // TODO db date

        return historyList;
    }
}
