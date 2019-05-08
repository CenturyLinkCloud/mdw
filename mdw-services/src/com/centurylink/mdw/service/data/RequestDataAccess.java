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

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.request.RequestList;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatuses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class RequestDataAccess extends CommonDataAccess {

    public RequestList getMasterRequests(Query query) throws DataAccessException {

        try {
            db.openConnection();

            String where = getMasterRequestsWhere(query);

            int total;
            String count = "select count(*)\nfrom PROCESS_INSTANCE pi, DOCUMENT d\n" + where;
            ResultSet countRs = db.runSelect(count);
            countRs.next();
            total = countRs.getInt(1);

            List<Request> requests = new ArrayList<>();
            RequestList requestList = new RequestList(RequestList.MASTER_REQUESTS, requests);

            StringBuilder q = new StringBuilder(db.pagingQueryPrefix());
            q.append("select ").append(PROC_INST_COLS).append(", d.document_id, d.create_dt, d.owner_type, d.status_code, d.status_message, d.path\n");
            q.append("from PROCESS_INSTANCE pi, DOCUMENT d\n");

            q.append(where).append(buildOrderBy(query));
            if (query.getMax() != Query.MAX_ALL)
                q.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));

            Map<Long,Request> requestMap = new HashMap<>();
            List<Long> listenerRequestIds = new ArrayList<>();
            ResultSet rs = db.runSelect(q.toString());
            while (rs.next()) {
                ProcessInstance pi = buildProcessInstance(rs);
                Request request = new Request(rs.getLong("document_id"));
                request.setCreated(rs.getTimestamp("create_dt"));
                request.setStatusCode(rs.getInt("status_code"));
                String statusMessage = rs.getString("status_message");
                if (statusMessage != null && !statusMessage.isEmpty())
                    request.setStatusMessage(statusMessage);
                request.setPath(rs.getString("path"));
                request.setMasterRequestId(pi.getMasterRequestId());
                request.setProcessInstanceId(pi.getId());
                request.setProcessId(pi.getProcessId());
                request.setProcessName(pi.getProcessName());
                request.setProcessVersion(pi.getProcessVersion());
                request.setPackageName(pi.getPackageName());
                request.setProcessStatus(pi.getStatus());
                request.setProcessStart(rs.getTimestamp("start_dt"));
                request.setProcessEnd(rs.getTimestamp("end_dt"));
                requests.add(request);
                requestMap.put(request.getId(), request);
                if (OwnerType.LISTENER_REQUEST.equals(rs.getString("owner_type")))
                    listenerRequestIds.add(request.getId());
            }

            // This join takes forever on MySQL, so a separate query is used to populate response info:
            // -- left join document d2 on (d2.owner_id = d.document_id)
            if (query.getMax() != Query.MAX_ALL && listenerRequestIds.size() > 0) {
                ResultSet respRs = db.runSelect(getResponsesQuery(OwnerType.LISTENER_RESPONSE, listenerRequestIds));
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
            throw new DataAccessException("Failed to retrieve master requests: (" + query + ")", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String getMasterRequestsWhere(Query query) throws DataAccessException {
        StringBuilder clause = new StringBuilder();
        clause.append("where pi.owner_id = d.document_id\n");
        clause.append("and pi.owner = 'DOCUMENT'\n");

        String find = query.getFind();
        String masterRequestId = query.getFilter("masterRequestId");
        String path = query.getFilter("path");
        if (find != null) {
            // ignore other criteria
            clause.append(" and pi.master_request_id like '").append(find).append("%' or d.path like '").append(find).append("%' \n");
        }
        else if (masterRequestId != null) {
            // ignore other criteria
            clause.append(" and pi.master_request_id = '").append(masterRequestId).append("' \n");
        }
        else if (path != null) {
            clause.append(" or d.path = '").append(path).append("' \n");
        }
        else {
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
                    String formatedReceivedDate = getOracleDateFormat().format(receivedDate);
                    if (db.isMySQL()){
                        clause.append(" and d.create_dt >= STR_TO_DATE('").append(formatedReceivedDate).append("','%d-%M-%Y')\n");
                    }else{
                        clause.append(" and d.create_dt >= to_date('").append(formatedReceivedDate).append("','DD-Mon-yyyy')\n");
                    }
                }
            }
            catch (ParseException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }

            // TODO: respond date
        }

        return clause.toString();
    }

    public Request getMasterRequest(String masterRequestId, boolean withContent, boolean withResponseContent)
    throws DataAccessException {
        StringBuilder query = new StringBuilder();
        query.append("select document_id, process_instance_id from PROCESS_INSTANCE pi, DOCUMENT d\n");
        query.append("where pi.owner_id = d.document_id\n");
        // query.append("and pi.owner = 'DOCUMENT'\n");  (eg: 'TESTER')
        query.append("and pi.master_request_id = ?");
        Long requestId;
        Long processInstanceId;
        try {
            db.openConnection();
            ResultSet rs = db.runSelect(query.toString(), masterRequestId);
            if (rs.next()) {
                requestId = rs.getLong("document_id");
                processInstanceId = rs.getLong("process_instance_id");
            }
            else
                return null;
        }
        catch (SQLException ex) {
            throw new DataAccessException("Error retrieving masterRequestId: " + masterRequestId, ex);
        }
        finally {
            db.closeConnection();
        }
        Request request = getRequest(requestId, withContent, withResponseContent);
        request.setMasterRequestId(masterRequestId);
        request.setProcessInstanceId(processInstanceId);
        return request;
    }

    public Request getRequest(Long id, boolean withContent, boolean withResponseContent)
    throws DataAccessException {
        try {
            String query = "select create_dt, owner_type, owner_id, path";
            query += " from DOCUMENT where document_id = ?";
            db.openConnection();
            ResultSet rs = db.runSelect(query, id);
            Request request;
            String ownerType;
            Long ownerId;
            if (rs.next()) {
                request = new Request(id);
                request.setCreated(rs.getTimestamp("create_dt"));
                ownerType = rs.getString("owner_type");
                ownerId = rs.getLong("owner_id");
                request.setPath(rs.getString("path"));
                if (withContent) {
                    // Get META info as well
                    Long metaId = 0L;
                    String owner_type_meta = ownerType + "_META";
                    query = "select document_id from DOCUMENT where owner_id = ? and owner_type = '" + owner_type_meta + "'";
                    db.openConnection();
                    rs = db.runSelect(query, id);
                    if (rs.next()) {
                        metaId = rs.getLong("document_id");
                    }
                    boolean foundInDocDb = false;
                    if (getDocumentDbAccess().hasDocumentDb()) {
                        String docContent = getDocumentDbAccess().getDocumentContent(ownerType, id);
                        if (docContent != null) {
                            request.setContent(docContent);
                            foundInDocDb = true;
                            // Get META
                            if (metaId > 0L) {
                                String metaContent = getDocumentDbAccess().getDocumentContent(owner_type_meta, metaId);
                                if (metaContent != null) {
                                    request.setMeta(new JsonObject(metaContent));
                                }
                            }
                        }
                    }
                    if (!foundInDocDb) {
                        query = "select content from DOCUMENT_CONTENT where document_id = ?";
                        rs = db.runSelect(query, id);
                        if (rs.next())
                            request.setContent(rs.getString("content"));
                        //Get META
                        if (metaId > 0L) {
                            rs = db.runSelect(query, metaId);
                            if (rs.next())
                                request.setMeta(new JsonObject(rs.getString("content")));
                        }
                    }
                }
            }
            else {
                return null;
            }

            ResultSet responseRs = null;
            String responseQuery = "select document_id, create_dt, status_code, status_message, path";
            String responseOwnerType = null;
            if (OwnerType.ADAPTER_REQUEST.equals(ownerType)) {
                responseOwnerType = OwnerType.ADAPTER_RESPONSE;
                request.setOutbound(true);
                responseQuery += " from DOCUMENT where owner_type='" + responseOwnerType + "' and owner_id = ?";
                responseRs = db.runSelect(responseQuery, ownerId);
            }
            else if (OwnerType.LISTENER_REQUEST.equals(ownerType)) {
                responseOwnerType = OwnerType.LISTENER_RESPONSE;
                responseQuery += " from DOCUMENT where owner_type='" + responseOwnerType + "' and owner_id = ?";
                responseRs = db.runSelect(responseQuery, id);
            }
            if (responseRs != null && responseRs.next()) {
                request.setResponseId(responseRs.getLong("document_id"));
                request.setResponded(responseRs.getTimestamp("create_dt"));
                request.setStatusCode(responseRs.getInt("status_code"));
                request.setStatusMessage(responseRs.getString("status_message"));

                if (withResponseContent) {
                    Response response = new Response();
                    // Get META info as well
                    Long metaId = 0L;
                    String owner_type_meta = responseOwnerType + "_META";
                    query = "select document_id from DOCUMENT where owner_id = ? and owner_type = '" + owner_type_meta + "'";
                    db.openConnection();
                    rs = db.runSelect(query, request.getResponseId());
                    if (rs.next()) {
                        metaId = rs.getLong("document_id");
                    }
                    boolean foundInDocDb = false;
                    if (getDocumentDbAccess().hasDocumentDb()) {
                        String responseDocContent = getDocumentDbAccess().getDocumentContent(responseOwnerType, request.getResponseId());
                        if (responseDocContent != null) {
                            response.setContent(responseDocContent);
                            foundInDocDb = true;
                            if (metaId > 0L) {
                                String responseMetaContent = getDocumentDbAccess().getDocumentContent(owner_type_meta, metaId);
                                if (responseMetaContent != null) {
                                    response.setMeta(new JsonObject(responseMetaContent));
                                }
                            }
                        }
                    }
                    if (!foundInDocDb) {
                        query = "select content from DOCUMENT_CONTENT where document_id = ?";
                        responseRs = db.runSelect(query, request.getResponseId());
                        if (responseRs.next())
                            response.setContent(responseRs.getString("content"));
                        //Get META
                        if (metaId > 0L) {
                            rs = db.runSelect(query, metaId);
                            if (rs.next())
                                response.setMeta(new JsonObject(rs.getString("content")));
                        }
                    }

                    request.setResponse(response);
                }

                if (request.getResponseId() != null && request.getResponseId() > 0) {
                    query = "select elapsed_ms from INSTANCE_TIMING where owner_type = ? and instance_id = ?";
                    String timingOwner = responseOwnerType.equals(OwnerType.LISTENER_RESPONSE) ? OwnerType.LISTENER_RESPONSE : OwnerType.ADAPTER;
                    Long timingInstanceId = responseOwnerType.equals(OwnerType.LISTENER_RESPONSE) ? request.getResponseId() : ownerId;
                    rs = db.runSelect(query, new Object[]{timingOwner, timingInstanceId});
                    if (rs.next()) {
                        request.setResponseMs(rs.getLong("elapsed_ms"));
                    }
                }
            }

            if (ownerId > 0 && (withContent || withResponseContent)) {
                if (OwnerType.ADAPTER_REQUEST.equals(ownerType)) {
                    query = "select activity_instance_id, process_instance_id from ACTIVITY_INSTANCE where activity_instance_id = ?";
                    rs = db.runSelect(query, ownerId);
                    if (rs.next()) {
                        request.setActivityInstanceId(rs.getLong("activity_instance_id"));
                        request.setProcessInstanceId(rs.getLong("process_instance_id"));
                    }
                }
                else if (OwnerType.LISTENER_REQUEST.equals(ownerType)) {
                    request.setProcessInstanceId(ownerId);
                }
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

            int total;
            String count = "select count(*)\nfrom DOCUMENT d\n" +
                    "left join DOCUMENT d2 on d2.owner_id = d.document_id\n" + where;
            ResultSet countRs = db.runSelect(count);
            countRs.next();
            total = countRs.getInt(1);

            List<Request> requests = new ArrayList<>();
            String q = db.pagingQueryPrefix() + "select d.document_id, d.create_dt, d.path, " +
                    "d2.document_id response_id, d2.create_dt responded, d2.status_code, d2.status_message\n" +
                    "from DOCUMENT d\n" +
                    "left join DOCUMENT d2 on d2.owner_id = d.document_id\n" +
                    where + buildOrderBy(query) +
                    db.pagingQuerySuffix(query.getStart(), query.getMax());
            ResultSet rs = db.runSelect(q);
            while (rs.next()) {
                Request request = new Request(rs.getLong("document_id"));
                request.setCreated(rs.getTimestamp("create_dt"));
                request.setStatusCode(rs.getInt("status_code"));
                String statusMessage = rs.getString("status_message");
                if (statusMessage != null && !statusMessage.isEmpty())
                    request.setStatusMessage(statusMessage);
                else
                    request.setStatusMessage(StatusResponse.getMessage(request.getStatusCode()));
                request.setPath(rs.getString("path"));
                request.setResponseId(rs.getLong("response_id"));
                request.setResponded(rs.getTimestamp("responded"));
                request.setStatusCode(rs.getInt("status_code"));
                requests.add(request);
            }

            RequestList requestList = new RequestList(RequestList.INBOUND_REQUESTS, requests);
            requestList.setTotal(total);
            requestList.setCount(requests.size());
            requestList.setRetrieveDate(DatabaseAccess.getDbDate());
            return requestList;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve inbound requests: (" + query + ")", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String getInboundRequestsWhere(Query query) {
        StringBuilder clause = new StringBuilder();
        clause.append("where d.owner_type = '" + OwnerType.LISTENER_REQUEST + "'\n");
        clause.append("and d2.owner_type = '" + OwnerType.LISTENER_RESPONSE + "'\n");
        boolean healthCheck = query.getBooleanFilter("healthCheck");
        if (!healthCheck)
            clause.append(" and d.path != 'AppSummary' and d.path != 'GetAppSummary");

        String find = query.getFind();
        Long id = query.getLongFilter("id");
        String path = query.getFilter("path");
        if (find != null) {
            try {
                long docId = Long.parseLong(find);
                clause.append(" and d.document_id like '").append(docId).append("%'\n");
            }
            catch (NumberFormatException e) {
                clause.append(" and d.path like '").append(find).append("%'\n");
            }
        }
        else if (id > 0) {
            clause.append(" and d.document_id = ").append(id).append("\n");
        }
        else if (path != null) {
            clause.append(" and d.path like '").append(path).append("%'\n");
        }
        else if (query.getFilter("ownerId") != null) {
            clause.append(" and d.owner_id = ").append(query.getLongFilter("ownerId")).append("\n");
        }
        int status = query.getIntFilter("status");
        if (status > 0) {
            clause.append(" and d2.status_code = ").append(status).append("\n");
        }
        try {
            Date date = query.getDateFilter("receivedDate");
            if (date != null) {
                String start = db.isMySQL() ? getMySqlDt(date) : getOracleDt(date);
                date.setTime(date.getTime() + 86400000);
                String end = db.isMySQL() ? getMySqlDt(date) : getOracleDt(date);;
                clause.append(" and d.create_dt >= '").append(start).append("' and d.create_dt < '").append(end).append("'\n");
            }
        }
        catch (ParseException ex) {
        }

        return clause.toString();
    }

    public RequestList getOutboundRequests(Query query) throws DataAccessException {

        try {
            db.openConnection();

            String where = getOutboundRequestsWhere(query);

            int total;
            String count = "select count(*)\nfrom DOCUMENT d\n" +
                    "left join DOCUMENT d2 on d2.owner_id = d.document_id\n" + where;
            ResultSet countRs = db.runSelect(count);
            countRs.next();
            total = countRs.getInt(1);

            List<Request> requests = new ArrayList<>();
            String q = db.pagingQueryPrefix() + "select d.document_id, d.create_dt, d.path, " +
                    "d2.document_id response_id, d2.create_dt responded, d2.status_code, d2.status_message\n" +
                    "from DOCUMENT d\n" +
                    "left join DOCUMENT d2 on d2.owner_id = d.owner_id\n" +
                    where + buildOrderBy(query) +
                    db.pagingQuerySuffix(query.getStart(), query.getMax());
            ResultSet rs = db.runSelect(q);
            while (rs.next()) {
                Request request = new Request(rs.getLong("document_id"));
                request.setCreated(rs.getTimestamp("create_dt"));
                request.setStatusCode(rs.getInt("status_code"));
                String statusMessage = rs.getString("status_message");
                if (statusMessage != null && !statusMessage.isEmpty())
                    request.setStatusMessage(statusMessage);
                else
                    request.setStatusMessage(StatusResponse.getMessage(request.getStatusCode()));
                request.setPath(rs.getString("path"));
                request.setOutbound(true);
                requests.add(request);
            }

            RequestList requestList = new RequestList(RequestList.OUTBOUND_REQUESTS, requests);
            requestList.setTotal(total);
            requestList.setCount(requests.size());
            requestList.setRetrieveDate(DatabaseAccess.getDbDate());
            return requestList;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve outbound requests: (" + query + ")", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String getOutboundRequestsWhere(Query query) {
        StringBuilder clause = new StringBuilder();
        clause.append("where d.owner_type = '" + OwnerType.ADAPTER_REQUEST + "'\n");
        clause.append("and d2.owner_type = '" + OwnerType.ADAPTER_RESPONSE + "'\n");

        String find = query.getFind();
        Long id = query.getLongFilter("id");
        String path = query.getFilter("path");
        if (find != null) {
            try {
                Long docId = Long.parseLong(find);
                clause.append(" and d.document_id like '").append(docId).append("%'\n");
            }
            catch (NumberFormatException e) {
                clause.append(" and d.path like '").append(find).append("%'\n");
            }
        }
        else if (id > 0) {
            clause.append(" and d.document_id = ").append(id).append("\n");
        }
        else if (path != null) {
            clause.append(" and d.path like '%").append(path).append("%'\n");
        }
        else if (query.getFilter("ownerId") != null) {
            clause.append(" and d.owner_id = ").append(query.getLongFilter("ownerId")).append("\n");
        }
        else {
            Long[] ownerIds = query.getLongArrayFilter("ownerIds");
            if (ownerIds != null) {
                clause.append(" and d.owner_id in (");
                for (int i = 0; i < ownerIds.length; i++) {
                    clause.append(ownerIds[i]);
                    if (i < ownerIds.length - 1)
                        clause.append(", ");
                }
                clause.append(")\n");
            }
        }
        int status = query.getIntFilter("status");
        if (status > 0) {
            clause.append(" and d2.status_code = ").append(status).append("\n");
        }
        try {
            Date date = query.getDateFilter("receivedDate");
            if (date != null) {
                String start = db.isMySQL() ? getMySqlDt(date) : getOracleDt(date);
                date.setTime(date.getTime() + 86400000);
                String end = db.isMySQL() ? getMySqlDt(date) : getOracleDt(date);;
                clause.append(" and d.create_dt >= '").append(start).append("' and d.create_dt < '").append(end).append("'\n");
            }
        }
        catch (ParseException ex) {
        }

        return clause.toString();
    }

    private String getResponsesQuery(String type, List<Long> ids) {
        StringBuilder resp = new StringBuilder("select document_id, owner_id, create_dt from DOCUMENT\n");
        resp.append("where owner_type = '").append(type).append("'\n");
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