/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao;

import java.sql.ResultSet;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.requests.Request;
import com.centurylink.mdw.model.value.requests.RequestList;

public class RequestsDAO extends VcsEntityDAO {

    public RequestsDAO() {
        super(null, DataAccess.currentSchemaVersion, DataAccess.supportedSchemaVersion);
    }

    public RequestList getMasterRequests(Query query) throws DataAccessException {

        try {
            db.openConnection();

            String where = getMasterRequestsWhere(query);

            StringBuilder count = new StringBuilder();
            count.append("select count(*)\n");
            count.append("from process_instance pi, document d\n");
            count.append(where);
            int total = 0;
            ResultSet countRs = db.runSelect(count.toString(), null);
            countRs.next();
            total = countRs.getInt(1);

            List<Request> requests = new ArrayList<Request>();
            RequestList requestList = new RequestList(RequestList.MASTER_REQUESTS, requests);

            //If the total count is 0, stop further execution to prevent it from erroring out while running to get the Responses query.
            if(total == 0){
              return requestList;
            }

            StringBuilder q = new StringBuilder(db.pagingQueryPrefix());
            q.append("select ").append(PROC_INST_COLS).append(", d.document_id, d.create_dt, d.owner_type\n");
            q.append("from process_instance pi, document d\n");

            q.append(where).append(buildOrderBy(query));
            if (query.getMax() != Query.MAX_ALL)
                q.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));

            Map<Long,Request> requestMap = new HashMap<Long,Request>();
            List<Long> listenerRequestIds = new ArrayList<Long>();
            ResultSet rs = db.runSelect(q.toString(), null);
            while (rs.next()) {
                ProcessInstanceVO pi = buildProcessInstance(rs);
                Request request = new Request(rs.getLong("d.document_id"));
                request.setCreated(rs.getTimestamp("d.create_dt"));
                request.setMasterRequestId(pi.getMasterRequestId());
                request.setProcessInstanceId(pi.getId());
                request.setProcessId(pi.getProcessId());
                request.setProcessName(pi.getProcessName());
                request.setProcessVersion(pi.getProcessVersion());
                request.setPackageName(pi.getPackageName());
                request.setProcessStatus(pi.getStatus());
                request.setProcessStart(rs.getTimestamp("pi.start_dt"));
                request.setProcessEnd(rs.getTimestamp("pi.end_dt"));
                requests.add(request);
                requestMap.put(request.getId(), request);
                if (OwnerType.LISTENER_REQUEST.equals(rs.getString("d.owner_type")))
                    listenerRequestIds.add(request.getId());
            }

            // This join takes forever on MySQL, so a separate query is used to populate response info:
            // -- left join document d2 on (d2.owner_id = d.document_id)
            if (query.getMax() != Query.MAX_ALL) {
                ResultSet respRs = db.runSelect(getResponsesQuery(OwnerType.LISTENER_RESPONSE, listenerRequestIds), null);
                while (respRs.next()) {
                    Request request = requestMap.get(respRs.getLong("owner_id"));
                    if (request != null) {
                        request.setResponseId(respRs.getLong("document_id"));
                        request.setResponded(respRs.getTimestamp("create_dt"));
                    }
                }
            }

            requestList.setTotal(total);
            requestList.setCount(requests.size());
            requestList.setRetrieveDate(DatabaseAccess.getDbDate());
            return requestList;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve master requests: ", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String getMasterRequestsWhere(Query query) throws DataAccessException {
        StringBuilder clause = new StringBuilder();
        clause.append("where pi.owner_id = d.document_id\n");
        clause.append("and pi.owner = 'DOCUMENT'\n");

        // status
        String status = query.getFilter("status");
        if (status != null) {
            if (status.equals(WorkStatus.STATUSNAME_ACTIVE)) {
                clause.append(" and pi.status_cd not in (")
                  .append(WorkStatus.STATUS_COMPLETED)
                  .append(",").append(WorkStatus.STATUS_FAILED)
                  .append(",").append(WorkStatus.STATUS_CANCELLED)
                  .append(",").append(WorkStatus.STATUS_PURGE)
                  .append(")\n");
            }
            else {
                clause.append(" and pi.status_cd = ").append(WorkStatuses.getCode(status)).append("\n");
            }
        }

        // receivedDate
        try {
            Date receivedDate = query.getDateFilter("receivedDate");
            if (receivedDate != null) {
                String formatedReceivedDate = getDateFormat().format(receivedDate);
                clause.append(" and d.create_dt >= STR_TO_DATE('").append(formatedReceivedDate).append("','%d-%M-%Y')\n");
            }
        }
        catch (ParseException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }

        String find = query.getFind();
        if (find != null)
            clause.append("and pi.master_request_id like '" + find + "%'\n");
        return clause.toString();
    }

    public Request getRequest(Long id) throws DataAccessException {
        try {
            db.openConnection();
            StringBuilder q = new StringBuilder(db.pagingQueryPrefix());
            q.append("select ").append(PROC_INST_COLS).append(", d.document_id, d.create_dt, d.owner_type\n");
            q.append("from process_instance pi, document d\n");
            q.append("where d.owner_type = '" + OwnerType.DOCUMENT + "'\n");
            q.append(" and pi.owner_id =  '" + id + "'\n"); // TODO:  Is this correct?

            ResultSet rs = db.runSelect(q.toString(), null);
            Request request = null;
            while (rs.next()) {
                ProcessInstanceVO pi = buildProcessInstance(rs);
                request = new Request(rs.getLong("d.document_id"));
                request.setCreated(rs.getTimestamp("d.create_dt"));
                request.setMasterRequestId(pi.getMasterRequestId());
                request.setProcessInstanceId(pi.getId());
                request.setProcessId(pi.getProcessId());
                request.setProcessName(pi.getProcessName());
                request.setProcessVersion(pi.getProcessVersion());
                request.setPackageName(pi.getPackageName());
                request.setProcessStatus(pi.getStatus());
                request.setProcessStart(rs.getTimestamp("pi.start_dt"));
                request.setProcessEnd(rs.getTimestamp("pi.end_dt"));
                request.setResponseId(rs.getLong("document_id"));
                request.setResponded(rs.getTimestamp("create_dt"));
            }
            return request;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve requestId: ", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public RequestList getInboundRequests(Query query) throws DataAccessException {

        try {
            db.openConnection();

            String where = getInboundRequestsWhere(query);

            StringBuilder count = new StringBuilder();
            count.append("select count(*)\n");
            count.append("from document d\n");
            count.append(where);
            int total = 0;
            ResultSet countRs = db.runSelect(count.toString(), null);
            countRs.next();
            total = countRs.getInt(1);

            StringBuilder q = new StringBuilder(db.pagingQueryPrefix());
            q.append("select d.document_id, d.create_dt\n");
            q.append("from document d\n");
            q.append(where).append(buildOrderBy(query));
            q.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));

            Map<Long,Request> requestMap = new HashMap<Long,Request>();
            List<Request> requests = new ArrayList<Request>();
            List<Long> requestIds = new ArrayList<Long>();
            ResultSet rs = db.runSelect(q.toString(), null);
            while (rs.next()) {
                Request request = new Request(rs.getLong("d.document_id"));
                request.setCreated(rs.getTimestamp("d.create_dt"));
                requestMap.put(request.getId(), request);
                requests.add(request);
                requestIds.add(request.getId());
            }

            // This join takes forever on MySQL, so a separate query is used to populate response info:
            // -- left join document d2 on (d2.owner_id = d.document_id)
            if (query.getMax() != Query.MAX_ALL) {
                ResultSet respRs = db.runSelect(getResponsesQuery(OwnerType.LISTENER_RESPONSE, requestIds), null);
                while (respRs.next()) {
                    Request request = requestMap.get(respRs.getLong("owner_id"));
                    if (request != null) {
                        request.setResponseId(respRs.getLong("document_id"));
                        request.setResponded(respRs.getTimestamp("create_dt"));
                    }
                }
            }

            RequestList requestList = new RequestList(RequestList.INBOUND_REQUESTS, requests);
            requestList.setTotal(total);
            requestList.setCount(requests.size());
            requestList.setRetrieveDate(DatabaseAccess.getDbDate());
            return requestList;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve master requests: ", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String getInboundRequestsWhere(Query query) {
        StringBuilder clause = new StringBuilder();
        clause.append("where d.owner_type = '" + OwnerType.LISTENER_REQUEST + "'\n");
        return clause.toString();
    }

    public RequestList getOutboundRequests(Query query) throws DataAccessException {

        try {
            db.openConnection();

            String where = getOutboundRequestsWhere(query);

            StringBuilder count = new StringBuilder();
            count.append("select count(*)\n");
            count.append("from document d\n");
            count.append(where);
            int total = 0;
            ResultSet countRs = db.runSelect(count.toString(), null);
            countRs.next();
            total = countRs.getInt(1);

            StringBuilder q = new StringBuilder(db.pagingQueryPrefix());
            q.append("select d.document_id, d.create_dt, d.owner_id\n");
            q.append("from document d\n");
            q.append(where).append(buildOrderBy(query));
            q.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));

            Map<Long,Request> requestMap = new HashMap<Long,Request>();
            List<Request> requests = new ArrayList<Request>();
            List<Long> activityIds = new ArrayList<Long>();
            ResultSet rs = db.runSelect(q.toString(), null);
            while (rs.next()) {
                Long activityId = rs.getLong("d.owner_id");
                Request request = new Request(rs.getLong("d.document_id"));
                request.setCreated(rs.getTimestamp("d.create_dt"));
                requestMap.put(activityId, request);
                requests.add(request);
                activityIds.add(activityId);
            }

            // This join takes forever on MySQL, so a separate query is used to populate response info:
            // -- left join document d2 on (d2.owner_id = d.document_id)
            if (query.getMax() != Query.MAX_ALL) {
                ResultSet respRs = db.runSelect(getResponsesQuery(OwnerType.ADAPTOR_RESPONSE, activityIds), null);
                while (respRs.next()) {
                    Request request = requestMap.get(respRs.getLong("owner_id"));
                    if (request != null) {
                        request.setResponseId(respRs.getLong("document_id"));
                        request.setResponded(respRs.getTimestamp("create_dt"));
                    }
                }
            }

            RequestList requestList = new RequestList(RequestList.OUTBOUND_REQUESTS, requests);
            requestList.setTotal(total);
            requestList.setCount(requests.size());
            requestList.setRetrieveDate(DatabaseAccess.getDbDate());
            return requestList;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve master requests: ", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String getOutboundRequestsWhere(Query query) {
        StringBuilder clause = new StringBuilder();
        clause.append("where d.owner_type = '" + OwnerType.ADAPTOR_REQUEST + "'\n");
        return clause.toString();
    }

    private String getResponsesQuery(String type, List<Long> ids) {
        StringBuilder resp = new StringBuilder("select document_id, owner_id, create_dt from document\n");
        resp.append("where owner_type = '" + type + "'\n");
        resp.append("and owner_id in (");
        int i = 0;
        for (Long id : ids) {
            resp.append(id);
            if (i < ids.size() - 1)
                resp.append(",");
            i++;
        }
        resp.append(")\n");
        return resp.toString();
    }

    private String buildOrderBy(Query query) {
        StringBuilder sb = new StringBuilder();
        sb.append(" order by d.document_id");
        if (query.isDescending())
            sb.append(" desc");
        sb.append("\n");
        return sb.toString();
    }
}