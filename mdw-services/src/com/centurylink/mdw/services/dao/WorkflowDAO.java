/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;

public class WorkflowDAO extends VcsEntityDAO {

    public WorkflowDAO() {
        super(null, DataAccess.currentSchemaVersion, DataAccess.supportedSchemaVersion);
    }

    public ProcessList getProcessInstances(Query query) throws DataAccessException {
        try {
            List<ProcessInstanceVO> procInsts = new ArrayList<ProcessInstanceVO>();
            db.openConnection();
            long count = -1;
            String where;
            if (query.getFind() != null) {
                try {
                    // numeric value means instance id
                    long findInstId = Long.parseLong(query.getFind());
                    where = "where pi.process_instance_id like '" + findInstId + "%'\n";
                }
                catch (NumberFormatException ex) {
                    // otherwise master request id
                    where = "where pi.master_request_id like '" + query.getFind() + "%'\n";
                }
            }
            else {
                where = buildWhere(query);
            }
            String countSql = "select count(process_instance_id) from process_instance pi\n" + where;
            ResultSet rs = db.runSelect(countSql, null);
            if (rs.next())
                count = rs.getLong(1);

            String orderBy = buildOrderBy(query);
            StringBuilder sql = new StringBuilder();
            if (query.getMax() != -1)
              sql.append(db.pagingQueryPrefix());
            sql.append("select ").append(PROC_INST_COLS).append(" from process_instance pi\n").append(where).append(orderBy);
            if (query.getMax() != -1)
                sql.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));
            rs = db.runSelect(sql.toString(), null);
            while (rs.next())
                procInsts.add(buildProcessInstance(rs));

            ProcessList list = new ProcessList(ProcessList.PROCESS_INSTANCES, procInsts);
            list.setTotal(count);
            list.setRetrieveDate(DatabaseAccess.getDbDate());
            return list;
        }
        catch (SQLException ex) {
            throw new DataAccessException("Failed to retrieve Processes", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String buildWhere(Query query) throws DataAccessException {
        long instanceId = query.getLongFilter("instanceId");
        if (instanceId > 0)
            return "where pi.process_instance_id = " + instanceId + "\n"; // ignore other criteria
        String masterRequestId = query.getFilter("masterRequestId");
        if (masterRequestId != null)
            return "where pi.master_request_id = '" + masterRequestId + "'\n"; // ignore other criteria

        StringBuilder sb = new StringBuilder();
        sb.append("where 1 = 1 ");
        // master or not
        sb.append(" and pi.owner != '").append(OwnerType.MAIN_PROCESS_INSTANCE).append("'\n");
        if ("true".equals(query.getFilter("master"))) {
            sb.append(" and pi.owner != '").append(OwnerType.PROCESS_INSTANCE).append("'\n");
        }
        // processId
        String processId = query.getFilter("processId");
        if (processId != null) {
            sb.append(" and pi.process_id = ").append(processId).append("\n");
        }
        // status
        String status = query.getFilter("status");
        if (status != null) {
            if (status.equals(WorkStatus.STATUSNAME_ACTIVE)) {
                sb.append(" and pi.status_cd not in (")
                  .append(WorkStatus.STATUS_COMPLETED)
                  .append(",").append(WorkStatus.STATUS_FAILED)
                  .append(",").append(WorkStatus.STATUS_CANCELLED)
                  .append(",").append(WorkStatus.STATUS_PURGE)
                  .append(")\n");
            }
            else {
                sb.append(" and pi.status_cd = ").append(WorkStatuses.getCode(status)).append("\n");
            }
        }
        // startDate
        try {
            Date startDate = query.getDateFilter("startDate");
            if (startDate != null) {
                String start = getDateFormat().format(startDate);
                if (db.isMySQL())
                    sb.append(" and pi.start_dt >= STR_TO_DATE('").append(start).append("','%d-%M-%Y')\n");
                else
                    sb.append(" and pi.start_dt >= '").append(start).append("'\n");
            }
        }
        catch (ParseException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        return sb.toString();
    }

    private String buildOrderBy(Query query) {
        StringBuilder sb = new StringBuilder();
        sb.append(" order by process_instance_id");
        if (query.isDescending())
            sb.append(" desc");
        sb.append("\n");
        return sb.toString();
    }

}