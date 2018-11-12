/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.service.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatuses;

public class WorkflowDataAccess extends CommonDataAccess {

    public WorkflowDataAccess() {
        super(null, DataAccess.currentSchemaVersion, DataAccess.supportedSchemaVersion);
    }

    public ProcessList getProcessInstances(Query query) throws DataAccessException {
        try {
            List<ProcessInstance> procInsts = new ArrayList<ProcessInstance>();
            db.openConnection();
            long count = -1;
            String where;
            if (query.getFind() != null) {
                try {
                    // numeric value means instance id or master request id
                    long findInstId = Long.parseLong(query.getFind());
                    where = "where (pi.process_instance_id like '" + findInstId
                            + "%' or pi.master_request_id like '" + query.getFind() + "%')\n";
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
            ResultSet rs = db.runSelect(countSql);
            if (rs.next())
                count = rs.getLong(1);

            String orderBy = buildOrderBy(query);
            StringBuilder sql = new StringBuilder();
            if (query.getMax() != Query.MAX_ALL)
              sql.append(db.pagingQueryPrefix());
            sql.append("select ").append(PROC_INST_COLS).append(" from process_instance pi\n").append(where).append(orderBy);
            if (query.getMax() != Query.MAX_ALL)
                sql.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));
            rs = db.runSelect(sql.toString());
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

        StringBuilder sb = new StringBuilder();
        sb.append("where 1 = 1 ");

        // masterRequestId
        String masterRequestId = query.getFilter("masterRequestId");
        if (masterRequestId != null)
            sb.append(" and pi.master_request_id = '" + masterRequestId + "'\n");

        String owner = query.getFilter("owner");
        if (owner == null) {
            // default excludes embedded subprocs - unless searching for activityInstanceId
            if (!(query.getLongFilter("activityInstanceId") > 0L))
                sb.append(" and pi.owner != '").append(OwnerType.MAIN_PROCESS_INSTANCE).append("'\n");
            if ("true".equals(query.getFilter("master")))
                sb.append(" and pi.owner NOT IN ( '").append(OwnerType.PROCESS_INSTANCE).append("' , '").append(OwnerType.ERROR).append("' )\n");
        }
        else {
            String ownerId = query.getFilter("ownerId");
            sb.append(" and pi.owner = '").append(owner).append("' and pi.owner_id = ").append(ownerId).append("\n");
        }

        // processId
        String processId = query.getFilter("processId");
        if (processId != null) {
            sb.append(" and pi.process_id = ").append(processId).append("\n");
        }
        else {
            // processIds
            String[] processIds = query.getArrayFilter("processIds");
            if (processIds != null && processIds.length > 0) {
                sb.append(" and pi.process_id in (");
                for (int i = 0; i < processIds.length; i++) {
                    sb.append(processIds[i]);
                    if (i < processIds.length - 1)
                        sb.append(",");
                }
                sb.append(")\n");
            }
        }
        // activityInstanceId
        long activityInstanceId = query.getLongFilter("activityInstanceId");
        if (activityInstanceId > 0) {
            sb.append(" and pi.process_instance_id in (select process_instance_id from activity_instance where activity_instance_id =");
            sb.append(activityInstanceId).append(")\n");
        }
        // status
        String status = query.getFilter("status");
        if (status != null && !status.equals("[Any]")) {
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
        // template
        String template = query.getFilter("template");
        if (template != null)
            sb.append(" and template = '" + template + "'");
        // values
        Map<String,String> values = query.getMapFilter("values");
        if (values != null) {
            for (String varName : values.keySet()) {
                String varValue = values.get(varName);
                sb.append("\n and exists (select vi.variable_inst_id from variable_instance vi ");
                sb.append(" where vi.process_inst_id = pi.process_instance_id and vi.variable_name = '").append(varName).append("'");
                sb.append(" and vi.variable_value = '").append(varValue).append("')");
            }
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