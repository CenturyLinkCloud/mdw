/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;

public class ProcessDAO extends CommonDataAccess {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    public static Boolean hasRuleSetTable = null;

    public ProcessDAO(DatabaseAccess db) {
        super(db, DataAccess.currentSchemaVersion, DataAccess.supportedSchemaVersion);
        if (hasRuleSetTable == null) {
            hasRuleSetTable = !ApplicationContext.isFileBasedAssetPersist();
        }
    }

    public List<ProcessInstanceVO> getProcessInstancesByCriteria(Map<String, String> criteria,
            String orderBy, boolean ascendingOrder, int startIndex, int endIndex)
                    throws DataAccessException {
        long start = System.currentTimeMillis();
        List<ProcessInstanceVO> ret = new ArrayList<ProcessInstanceVO>();
        ResultSet rs;

        StringBuffer query = new StringBuffer();
        query.append(db.pagingQueryPrefix());
        query.append("select pi.PROCESS_INSTANCE_ID, pi.PROCESS_ID, pi.OWNER, pi.OWNER_ID, pi.MASTER_REQUEST_ID, pi.STATUS_CD,");
        query.append(" pi.SECONDARY_OWNER, pi.SECONDARY_OWNER_ID, pi.COMPCODE, pi.COMMENTS, pi.START_DT,  pi.END_DT");
        if (hasRuleSetTable) {
            query.append(" , rs.rule_set_name ");
        }
        query.append(" from PROCESS_INSTANCE pi");
        if (hasRuleSetTable) {
            query.append(" , rule_set rs where rs.rule_set_id = pi.process_id ");
        }
        String whereClause = buildWhereClause(criteria);
        if (!StringHelper.isEmpty(whereClause.toString())) {
            query.append(" where ").append(whereClause);
        }
        if (orderBy != null && orderBy.length() > 0) {
            query.append(" order by ").append(orderBy);
            if (!ascendingOrder)
                query.append(" desc");
        }
        query.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("getProcessInstancesByCriteria() Query --> " + query.toString());
        }

        try {
            rs = db.runSelect(query.toString(), null);
            while (rs.next()) {
                ProcessInstanceVO pi = this.getProcessInstanceSub(rs);
                pi.setStartDate(rs.getString(11));
                pi.setEndDate(rs.getString(12));
                if (hasRuleSetTable) {
                    pi.setProcessName(rs.getString(13));
                }
                else {
                    if (ProcessVOCache.getProcessVO(pi.getProcessId()) == null) {
                        pi.setProcessName(rs.getString(10));
                    } else
                    pi.setProcessName(ProcessVOCache.getProcessVO(pi.getProcessId()).getProcessName());
                }
                ret.add(pi);
            }
        }
        catch (Exception e) {
            throw new DataAccessException(0, "failed to query process instances by criteria=" + criteria.values(), e);
        }
        finally {
            if (logger.isMdwDebugEnabled()) {
                long elapsed = System.currentTimeMillis() - start;
                logger.mdwDebug("getProcessInstancesByCriteria() Elapsed-->" + elapsed + " ms");
            }
        }
        return ret;
    }

    public int queryProcessInstancesCount(Map<String, String> criteria) throws DataAccessException {
        long start = System.currentTimeMillis();
        StringBuffer buff = new StringBuffer();
        buff.append("select count(process_instance_id) row_count ");
        buff.append(" from PROCESS_INSTANCE pi ");
        if (hasRuleSetTable) {
            buff.append(" , rule_set rs where rs.rule_set_id = pi.process_id   ");
        }
        String whereClause = buildWhereClause(criteria);
        if (!StringHelper.isEmpty(whereClause.toString())) {
            buff.append(" where ").append(whereClause);
        }
        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("queryTaskInstancesCount() Query --> " + buff.toString());
        }
        try {
            ResultSet rs = db.runSelect(buff.toString(), null);
            if (rs.next()) {
                int count = rs.getInt(1);
                return count;
            }
            else
                return 0;
        }
        catch (Exception e) {
            throw new DataAccessException(0, "failed to query process instances count", e);
        }
        finally {
            if (logger.isMdwDebugEnabled()) {
                long elapsed = System.currentTimeMillis() - start;
                logger.mdwDebug("queryProcessInstancesCount() Elapsed-->" + elapsed + " ms");
            }
        }
    }

    /**
     * Creates the where clause based on the passed in params
     *
     * @param criteria
     * @param pPersistable
     *            Class Impl
     * @param pTableAlias
     */
    private String buildWhereClause(Map<String, String> criteria) {
        StringBuffer buff = new StringBuffer();
        if (criteria.isEmpty()) {
            return "";
        }

        String pre = "";
        for (String key : criteria.keySet()) {

            String value = criteria.get(key);
            if (value == null)
                buff.append(pre + key +  " is null ");
            if (key.equals("startDatefrom") && value != null) {
                buff.append(pre+" start_dt >= to_date('" + value + "', 'DD-Mon-YYYY' )");
            }
            else if (key.equals("startDateto") && value != null) {
                buff.append(pre+" start_dt < to_date('" + value + "', 'DD-Mon-YYYY' )");
            }
            if (key.equals("endDatefrom") && value != null) {
                buff.append(pre+" end_dt >= to_date('" + value + "', 'DD-Mon-YYYY' )");
            }
            else if (key.equals("endDateto") && value != null) {
                buff.append(pre+" end_dt < to_date('" + value + "', 'DD-Mon-YYYY' )");
            }
            else if (key.equals("masterRequestId") && value != null) {
                buff.append(pre+" MASTER_REQUEST_ID like '" + value + "'");
            }
            else if (key.equals("processName") && value != null) {
                if (hasRuleSetTable) {
                    buff.append(pre+" RULE_SET_NAME like '" + value + "'");
                }
            }
           pre = buff.length() > 0 ? " and ": "";

        }
        // add this once if there's criteria
        if (buff.length() > 0) {
            buff.append(" and owner NOT IN ('PROCESS_INSTANCE', 'MAIN_PROCESS_INSTANCE') ");
        }


        return buff.toString();
    }

    private ProcessInstanceVO getProcessInstanceSub(ResultSet rs) throws SQLException {
        ProcessInstanceVO pi = new ProcessInstanceVO(rs.getLong(2), null);
        pi.setId(rs.getLong(1));
        pi.setOwner(rs.getString(3));
        pi.setOwnerId(rs.getLong(4));
        pi.setMasterRequestId(rs.getString(5));
        pi.setStatusCode(rs.getInt(6));
        pi.setSecondaryOwner(rs.getString(7));
        pi.setSecondaryOwnerId(rs.getLong(8));
        pi.setCompletionCode(rs.getString(9));
        pi.setComment(rs.getString(10));
        return pi;
   }
}
