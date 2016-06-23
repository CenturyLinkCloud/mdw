/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.order;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.value.process.OrderVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.services.BamManager;
import com.centurylink.mdw.services.OrderServices;
import com.centurylink.mdw.services.bam.BamManagerBean;
import com.centurylink.mdw.services.dao.process.EngineDataAccessDB;
import com.centurylink.mdw.services.dao.process.ProcessDAO;

// TODO add OrderNotes services based on InstanceNotes
public class OrderServicesImpl implements OrderServices {

    private ProcessDAO getProcessDAO() {
        DatabaseAccess db = new DatabaseAccess(null);
        return new ProcessDAO(db);
    }

    public List<OrderVO> getOrders() throws DataAccessException {
        ProcessInstanceVO[] instArr = null;
        List<OrderVO> returnOrders = new ArrayList<OrderVO>();

        Map<String,String> criteria = new HashMap<String,String>();
        CodeTimer timer = new CodeTimer("EventManagerBean.getProcessInstances()", true);
        ProcessDAO pdao = getProcessDAO();

        TransactionWrapper transaction = null;
        try {
            transaction = pdao.startTransaction();
            List<ProcessInstanceVO> daoResults = null;
            daoResults = pdao.getProcessInstancesByCriteria(criteria, null, true, 0, 50);

            instArr = daoResults.toArray(new ProcessInstanceVO[daoResults.size()]);
            timer.stopAndLogTiming("EventManagerBean.getProcessInstances()");

            for (int i = 0; i < instArr.length; i++) {
                ProcessInstanceVO[] pVo = new ProcessInstanceVO[1];
                pVo[0] = instArr[i];
                OrderVO orderVo =  (new OrderVO(pVo[0]));
                orderVo.setProcessInstanceVOs(pVo);
                returnOrders.add(orderVo);
            }

        }
        catch (DataAccessException e) {
            throw new DataAccessException(0, "Failed to get process instances", e);
        }
        finally {
            pdao.stopTransaction(transaction);
        }
        return returnOrders;
    }

    /**
     * Returns tasks associated with the specified user's workgroups.
     */
    public OrderVO getOrder(String masterRequestId)  throws DataAccessException {
        if (masterRequestId == null)
            throw new DataAccessException("Unknown masterRequestId: " + masterRequestId);

        EngineDataAccessDB processDao = new EngineDataAccessDB();

        BamManager bamMgr = new BamManagerBean(new DatabaseAccess(null));
        Long processId = bamMgr.getMainProcessId(masterRequestId);
        List<ProcessInstanceVO> processVo;
        try {
            processVo = processDao.getProcessInstancesByMasterRequestId(masterRequestId, processId);
        }
        catch (SQLException e) {
            throw new DataAccessException(0, "Failed to get process instances", e);
        }
        OrderVO orderVo = new OrderVO();
        orderVo.setProcessInstanceVOs(processVo.toArray(new ProcessInstanceVO[processVo.size()]));
        return orderVo;
    }

}
