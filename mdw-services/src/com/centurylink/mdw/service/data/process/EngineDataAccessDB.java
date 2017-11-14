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
package com.centurylink.mdw.service.data.process;

import static com.mongodb.client.model.Filters.eq;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.cache.impl.VariableTypeCache;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.TableSequenceName;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.monitor.CertifiedMessage;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.monitor.UnscheduledEvent;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.Transition;
import com.centurylink.mdw.model.workflow.TransitionInstance;
import com.centurylink.mdw.model.workflow.TransitionStatus;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatuses;
import com.centurylink.mdw.util.StringHelper;
import com.mongodb.client.MongoCollection;

/**
 * TODO: Remove non-engine-related data access from this class.
 */
public class EngineDataAccessDB extends CommonDataAccess implements EngineDataAccess {

    private static Map<String,String> _ExternalEventInstanceQueryMap = new HashMap<String, String>();

    static {
        _ExternalEventInstanceQueryMap.put("eventName", "d.OWNER_TYPE");
        _ExternalEventInstanceQueryMap.put("externalEventInstanceId", "d.DOCUMENT_ID");
        _ExternalEventInstanceQueryMap.put("createdDate", "d.CREATE_DT");
        _ExternalEventInstanceQueryMap.put("eventData", "d.CONTENT");
        _ExternalEventInstanceQueryMap.put("processInstanceId", "d.PROCESS_INST_ID");
        _ExternalEventInstanceQueryMap.put("processId", "pi.PROCESS_ID");
        _ExternalEventInstanceQueryMap.put("processInstanceStatus", "pi.STATUS_CD");
        _ExternalEventInstanceQueryMap.put("masterRequestId", "pi.MASTER_REQUEST_ID");
    }

    public EngineDataAccessDB() {
        super(new DatabaseAccess(null), DataAccess.currentSchemaVersion,
                DataAccess.supportedSchemaVersion);
    }

    public DatabaseAccess getDatabaseAccess() {
        return db;
    }

    public int getPerformanceLevel() {
        return 1;
    }

    protected Long getNextId(String sequenceName) throws SQLException {
        String query = "select " + sequenceName + ".NEXTVAL from dual";
        ResultSet rs = db.runSelect(query, null);
        rs.next();
        return new Long(rs.getString(1));
    }

    public Long createVariableInstance(VariableInstance var, Long procInstId) throws SQLException {
        Long varInstId = db.isMySQL()?null:this.getNextId("VARIABLE_INST_ID_SEQ");
        String query = "insert into VARIABLE_INSTANCE " +
            "(VARIABLE_INST_ID, VARIABLE_ID, PROCESS_INST_ID, VARIABLE_VALUE, VARIABLE_NAME, VARIABLE_TYPE_ID, " +
            "CREATE_DT, CREATE_USR) values (?, ?, ?, ?, ?, ?, "+nowPrecision()+",'MDWEngine')";
        Object[] args = new Object[6];
        args[0] = varInstId;
        args[1] = var.getVariableId();
        args[2] = procInstId;
        args[3] = var.getStringValue();
        args[4] = var.getName();
        args[5] = VariableTypeCache.getTypeId(var.getType());
        if (db.isMySQL()) varInstId = db.runInsertReturnId(query, args);
        else db.runUpdate(query, args);
        var.setInstanceId(varInstId);
        return varInstId;
    }

    public void updateVariableInstance(VariableInstance var) throws SQLException {
//        db.openConnection();
        String query = "update VARIABLE_INSTANCE set VARIABLE_VALUE=?, MOD_DT="+nowPrecision()+" where VARIABLE_INST_ID=?";
        Object[] args = new Object[2];
        args[0] = var.getStringValue();
        args[1] = var.getInstanceId();
        db.runUpdate(query, args);
    }

    public VariableInstance getVariableInstance(Long varInstId) throws SQLException {
        String query = "select VARIABLE_VALUE, VARIABLE_NAME, VARIABLE_TYPE_ID, PROCESS_INST_ID " +
                    "from VARIABLE_INSTANCE vi " +
                    "where VARIABLE_INST_ID=?";
        ResultSet rs = db.runSelect(query, varInstId);
        if (!rs.next()) return null;
        VariableInstance var = new VariableInstance();
        var.setInstanceId(varInstId);
        var.setStringValue(rs.getString(1));
        var.setName(rs.getString(2));
        var.setType(VariableTypeCache.getTypeName(rs.getLong(3)));
        var.setProcessInstanceId(rs.getLong(4));
        return var;
    }

    public VariableInstance getVariableInstance(Long procInstId, String varname) throws SQLException {
         String query = "select VARIABLE_INST_ID, VARIABLE_VALUE, VARIABLE_TYPE_ID " +
             " from VARIABLE_INSTANCE" +
             " where PROCESS_INST_ID=? and VARIABLE_NAME=?";
         Object[] args = new Object[2];
         args[0] = procInstId;
         args[1] = varname;
         ResultSet rs = db.runSelect(query, args);
         if (rs.next()) {
             VariableInstance var = new VariableInstance();
             var.setInstanceId(rs.getLong(1));
             var.setStringValue(rs.getString(2));
             var.setName(varname);
             var.setType(VariableTypeCache.getTypeName(rs.getLong(3)));
             var.setProcessInstanceId(procInstId);
             return var;
         } else {
             return null;
         }
    }

    public Long createActivityInstance(ActivityInstance act) throws SQLException {
        Long actInstId = db.isMySQL()?null:this.getNextId("ACTIVITY_INSTANCE_ID_SEQ");
        String query = "insert into ACTIVITY_INSTANCE " +
            "(ACTIVITY_INSTANCE_ID, ACTIVITY_ID, PROCESS_INSTANCE_ID, STATUS_CD, START_DT, CREATE_DT, CREATE_USR) " +
            "values (?, ?, ?, ?, " + nowPrecision() + ", " + nowPrecision() + ", 'MDWEngine')";
        Object[] args = new Object[4];
        args[0] = actInstId;
        args[1] = act.getActivityId();
        args[2] = act.getProcessInstanceId();
        args[3] = act.getStatusCode();
        if (db.isMySQL())
            actInstId = db.runInsertReturnId(query, args);
        else
            db.runUpdate(query, args);
        act.setId(actInstId);
        return actInstId;
    }

    public ActivityInstance getActivityInstance(Long actInstId) throws SQLException {
        String query = "select STATUS_CD,START_DT,END_DT,STATUS_MESSAGE,ACTIVITY_ID,PROCESS_INSTANCE_ID" +
            " from ACTIVITY_INSTANCE where ACTIVITY_INSTANCE_ID=?";
        ResultSet rs = db.runSelect(query, actInstId);
        if (!rs.next()) throw new SQLException("Activity instance does not exist: " + actInstId);
        ActivityInstance vo = new ActivityInstance();
        vo.setId(actInstId);
        vo.setStatusCode(rs.getInt(1));
        vo.setStartDate(rs.getTimestamp(2));
        vo.setEndDate(rs.getTimestamp(3));
        vo.setMessage(rs.getString(4));
        vo.setActivityId(rs.getLong(5));
        vo.setProcessInstanceId(rs.getLong(6));
        return vo;
    }

    public void setActivityInstanceStatus(ActivityInstance actInst,
            Integer status, String status_message)
        throws SQLException {
        String query;
        if (status.equals(WorkStatus.STATUS_CANCELLED)
                || status.equals(WorkStatus.STATUS_COMPLETED)
                || status.equals(WorkStatus.STATUS_FAILED)) {
            query = "update ACTIVITY_INSTANCE set STATUS_CD=?, STATUS_MESSAGE=?, END_DT="+nowPrecision() +
                " where ACTIVITY_INSTANCE_ID=?";
        } else {
            query = "update ACTIVITY_INSTANCE set STATUS_CD=?, STATUS_MESSAGE=?" +
                " where ACTIVITY_INSTANCE_ID=?";
        }
        Object[] args = new Object[3];
        if (actInst.getMessage()!=null) {
            if (status_message==null) status_message = actInst.getMessage();
            else status_message = actInst.getMessage() + "\n" + status_message;
            if (status_message.length()>3960) {
                status_message = status_message.substring(0,3960)
                    + "\n\nTruncated to 3960 characters\n";
            }
        }
        args[0] = status;
        args[1] = status_message;
        args[2] = actInst.getId();
        db.runUpdate(query, args);
    }

    public Long createTransitionInstance(TransitionInstance trans) throws SQLException {
        Long transInstId = db.isMySQL()?null:this.getNextId("WORK_TRANS_INST_ID_SEQ");
        String query = "insert into WORK_TRANSITION_INSTANCE " +
            "(WORK_TRANS_INST_ID, WORK_TRANS_ID, PROCESS_INST_ID, STATUS_CD, START_DT, DEST_INST_ID, CREATE_DT, CREATE_USR) " +
            "values (?, ?, ?, ?, "+nowPrecision()+", ?, "+nowPrecision()+", 'MDWEngine')";
        Object[] args = new Object[5];
        args[0] = transInstId;
        args[1] = trans.getTransitionID();
        args[2] = trans.getProcessInstanceID();
        args[3] = trans.getStatusCode();
        args[4] = trans.getDestinationID();
        if (db.isMySQL()) transInstId = db.runInsertReturnId(query, args);
        else db.runUpdate(query, args);
        trans.setTransitionInstanceID(transInstId);
        return transInstId;
    }

    public void completeTransitionInstance(Long transInstId, Long toActInstId) throws SQLException {
        String query = "update WORK_TRANSITION_INSTANCE " +
                "set STATUS_CD=?, END_DT="+nowPrecision()+", DEST_INST_ID=? " +
                "where WORK_TRANS_INST_ID=?";
        Object[] args = new Object[3];
        args[0] = TransitionStatus.STATUS_COMPLETED;
        args[1] = toActInstId;
        args[2] = transInstId;
        db.runUpdate(query, args);
    }

    public Long createProcessInstance(ProcessInstance pi) throws SQLException {
        Long procInstId = db.isMySQL()?null:this.getNextId("MDW_COMMON_INST_ID_SEQ");
        String query = "insert into PROCESS_INSTANCE " +
            "(PROCESS_INSTANCE_ID, PROCESS_ID, OWNER, OWNER_ID, SECONDARY_OWNER, " +
            "SECONDARY_OWNER_ID, STATUS_CD, MASTER_REQUEST_ID, START_DT, COMMENTS, CREATE_DT, CREATE_USR) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, " + nowPrecision() + ", ?, " + nowPrecision() + ", 'MDWEngine')";
        Object[] args = new Object[9];
        args[0] = procInstId;
        args[1] = pi.getProcessId();
        args[2] = pi.getOwner();
        args[3] = pi.getOwnerId();
        args[4] = pi.getSecondaryOwner();
        args[5] = pi.getSecondaryOwnerId();
        args[6] = pi.getStatusCode();
        args[7] = pi.getMasterRequestId();
        args[8] = pi.getComment();
        if (db.isMySQL()) procInstId = db.runInsertReturnId(query, args);
        else db.runUpdate(query, args);
        pi.setId(procInstId);
        return procInstId;
    }

    public void setProcessInstanceStatus(Long procInstId, Integer status)
        throws SQLException {
        String query;
        if (status.equals(WorkStatus.STATUS_COMPLETED) ||
                status.equals(WorkStatus.STATUS_CANCELLED) ||
                status.equals(WorkStatus.STATUS_FAILED)) {
            query = "update PROCESS_INSTANCE set STATUS_CD=?, END_DT="+nowPrecision()+" " +
                " where PROCESS_INSTANCE_ID=?";
        } else if (status.equals(WorkStatus.STATUS_PENDING_PROCESS)) {
            status = WorkStatus.STATUS_IN_PROGRESS;
            query = "update PROCESS_INSTANCE set STATUS_CD=?, START_DT="+nowPrecision()+" " +
                " where PROCESS_INSTANCE_ID=?";
        } else {
            query = "update PROCESS_INSTANCE set STATUS_CD=?" +
                " where PROCESS_INSTANCE_ID=?";
        }
        Object[] args = new Object[2];
        args[0] = status;
        args[1] = procInstId;
        db.runUpdate(query, args);
    }

    public Long createDocument(Document doc) throws SQLException {
        return createDocument(doc, null);
    }

    boolean hasMongo() { return (DatabaseAccess.getMongoDb() != null); }
    public Long createDocument(Document doc, Package pkg) throws SQLException {
        Long docId = db.isMySQL() ? null : getNextId("MDW_COMMON_INST_ID_SEQ");
        String query = "insert into DOCUMENT " +
            "(DOCUMENT_ID, CREATE_DT, DOCUMENT_TYPE, OWNER_TYPE, OWNER_ID, STATUS_CODE, STATUS_MESSAGE) " +
            "values (?, " + nowPrecision() + ", ?, ?, ?, ?, ?)";
        Object[] args = new Object[6];
        args[0] = docId;
        args[1] = doc.getDocumentType();
        args[2] = doc.getOwnerType();
        args[3] = doc.getOwnerId();
        if (doc.getStatusCode() == null)
            args[4] = 0;
        else
            args[4] = doc.getStatusCode();
        if (doc.getStatusMessage() == null)
            args[5] = "";
        else
            args[5] = doc.getStatusMessage();

        if (db.isMySQL())
            docId = db.runInsertReturnId(query, args);
        else if(db.isOracle())
            db.runUpdate(query, args);
        else
            db.runUpdate(query, String.valueOf(args));
        doc.setDocumentId(docId);
        if (hasMongo()) {
            MongoCollection<org.bson.Document> collection = DatabaseAccess.getMongoDb().getCollection(doc.getOwnerType());
            org.bson.Document myDoc = null;
            if (doc.getContent(pkg).trim().startsWith("{") && doc.getContent(pkg).trim().endsWith("}")) {
                try {
                    org.bson.Document myJsonDoc = org.bson.Document.parse(doc.getContent(pkg)); // Parse JSON to create BSON CONTENT Document
                    if (!myJsonDoc.isEmpty()) {
                        if (doc.getContent(pkg).contains(".") || doc.getContent(pkg).contains("$"))
                            myJsonDoc = DatabaseAccess.encodeMongoDoc(myJsonDoc);
                        myDoc = new org.bson.Document("CONTENT", myJsonDoc).append("_id", docId).append("isJSON", true); // Plus append _id and isJSON:true field
                    }
                }
                catch (Throwable ex) {myDoc=null;}  // Assume not JSON then
            }
            if (myDoc == null)   // Create BSON document with Raw content if it wasn't JSON, plus append _id and isJSON:false
                myDoc = new org.bson.Document("CONTENT", doc.getContent(pkg)).append("_id", docId).append("isJSON", false);

            collection.insertOne(myDoc);
        }
        else {
            // store in DOCUMENT_CONTENT
            query = "insert into DOCUMENT_CONTENT (DOCUMENT_ID, CONTENT) values (?, ?)";
            args = new Object[2];
            args[0] = docId;
            args[1] = doc.getContent(pkg);
            db.runUpdate(query, args);
        }
        return docId;
    }

    public void updateDocumentContent(Long documentId, String content) throws SQLException {
        String selectQuery = "select OWNER_TYPE from DOCUMENT where DOCUMENT_ID = ?";
        String owner_type = "";
        ResultSet rs = db.runSelect(selectQuery, documentId);
        if (rs.next())
            owner_type = rs.getString("OWNER_TYPE");

        String query = "update DOCUMENT set MODIFY_DT = " + nowPrecision() + " where DOCUMENT_ID = ?";
        db.runUpdate(query, documentId);
        boolean inMongo = false;  // not found (compatibility)
        if (hasMongo() && owner_type.length() > 0) {
            MongoCollection<org.bson.Document> collection = DatabaseAccess.getMongoDb().getCollection(owner_type);
            org.bson.Document myDoc = null;
            if (content.trim().startsWith("{") && content.trim().endsWith("}")) {
                try {
                    org.bson.Document myJsonDoc = org.bson.Document.parse(content); // Parse JSON to create BSON CONTENT Document
                    if (!myJsonDoc.isEmpty())
                        if (content.contains(".") || content.contains("$"))
                            myJsonDoc = DatabaseAccess.encodeMongoDoc(myJsonDoc);
                        myDoc = new org.bson.Document("CONTENT", myJsonDoc).append("isJSON", true); // Plus append isJSON:true field
                }
                catch (Throwable ex) {myDoc=null;}  // Assume not JSON then
            }
            if (myDoc == null)   // Create BSON document with Raw content if it wasn't JSON plus append isJSON:false
                myDoc = new org.bson.Document("CONTENT", content).append("isJSON", false);
            if (collection.findOneAndReplace(eq("_id", documentId), myDoc) != null)
                inMongo = true;
        }

        if (!inMongo) {
            query = "update DOCUMENT_CONTENT set CONTENT = ? where DOCUMENT_ID = ?";
            Object[] args = new Object[2];
            args[0] = content;
            args[1] = documentId;
            db.runUpdate(query, args);
        }

    }

    public void updateDocumentInfo(Document docvo) throws SQLException {
        String query = "update DOCUMENT set DOCUMENT_TYPE=?, OWNER_TYPE=?, OWNER_ID=?, STATUS_CODE=?, STATUS_MESSAGE=? where DOCUMENT_ID=?";
        Object[] args = new Object[6];
        args[0] = docvo.getDocumentType();
        args[1] = docvo.getOwnerType();
        args[2] = docvo.getOwnerId();
        args[3] = docvo.getStatusCode();
        args[4] = docvo.getStatusMessage();
        args[5] = docvo.getDocumentId();
        db.runUpdate(query, args);
    }

    public void createEventInstance(String eventName,
            Long documentId, Integer status, Date consumeDate,
            String auxdata, String reference, int preserveSeconds)
            throws SQLException {
        String query = "insert into EVENT_INSTANCE " +
            "(EVENT_NAME, DOCUMENT_ID, STATUS_CD, CREATE_DT, CONSUME_DT, AUXDATA, REFERENCE, PRESERVE_INTERVAL) " +
            "values (?, ?, ?, "+nowPrecision()+", ?, ?, ?, ?)";
        Object[] args = new Object[7];
        args[0] = eventName;
        args[1] = documentId;
        args[2] = status;
        args[3] = consumeDate;
        args[4] = auxdata;
        args[5] = reference;
        args[6] = new Integer(preserveSeconds);
        db.runUpdate(query, args);
    }

    public EventInstance lockEventInstance(String eventName)
            throws SQLException {
        String query = "select DOCUMENT_ID, STATUS_CD, " +
            " CREATE_DT, CONSUME_DT, PRESERVE_INTERVAL from EVENT_INSTANCE " +
            " where EVENT_NAME=? for update";
        ResultSet rs = db.runSelect(query, eventName);
        if (rs.next()) {
            EventInstance vo = new EventInstance();
            vo.setEventName(eventName);
            vo.setData(null);
            long docid = rs.getLong(1);
            vo.setDocumentId(docid==0L?null:docid);
            vo.setStatus(rs.getInt(2));
            vo.setCreateDate(rs.getTimestamp(3));
            vo.setConsumeDate(rs.getTimestamp(4));
            vo.setPreserveSeconds(rs.getInt(5));
            return vo;
        } else return null;
    }

    private void consumeEventInstance(EventInstance vo, int preserveSeconds) throws SQLException {
        String query = "update EVENT_INSTANCE set DOCUMENT_ID=?, STATUS_CD=?, " +
            " CONSUME_DT="+nowPrecision()+", PRESERVE_INTERVAL=? where EVENT_NAME=?";
        Object[] args = new Object[4];
        args[0] = vo.getDocumentId();
        args[1] = EventInstance.STATUS_CONSUMED;
        args[2] = Math.max(vo.getPreserveSeconds(),preserveSeconds);
        args[3] = vo.getEventName();
        db.runUpdate(query, args);
    }

    public void updateEventInstance(String eventName,
            Long documentId, Integer status, Date consumeDate,
            String auxdata, String reference, int preserveSeconds, String comments)
            throws SQLException {
        StringBuffer sb = new StringBuffer();
        List<Object> argl = new ArrayList<Object>();
        sb.append("update EVENT_INSTANCE set ");
        if (documentId!=null) {
            if (argl.size()>0) sb.append(", ");
            sb.append("DOCUMENT_ID=?");
            argl.add(documentId);
        }
        if (status!=null) {
            if (argl.size()>0) sb.append(", ");
            sb.append("STATUS_CD=?");
            argl.add(status);
        }
        if (consumeDate!=null) {
            if (argl.size()>0) sb.append(", ");
            sb.append("CONSUME_DT=?");
            argl.add(consumeDate);
        }
        if (auxdata!=null) {
            if (argl.size()>0) sb.append(", ");
            sb.append("AUXDATA=?");
            argl.add(auxdata);
        }
        if (reference!=null) {
            if (argl.size()>0) sb.append(", ");
            sb.append("REFERENCE=?");
            argl.add(reference);
        }
        if (preserveSeconds>0) {
            if (argl.size()>0) sb.append(", ");
            sb.append("PRESERVE_INTERVAL=?");
            argl.add(new Integer(preserveSeconds));
        }
        if (comments != null) {
            if (argl.size()>0) sb.append(", ");
            sb.append("COMMENTS=?");
            argl.add(comments);
        }
        sb.append(" where EVENT_NAME=?");
        argl.add(eventName);
        int count = db.runUpdate(sb.toString(), argl.toArray());
        if (count==0) throw new SQLException("The event does not exist");
    }

    public int deleteEventInstance(String eventName) throws SQLException {
        String query = "delete from EVENT_INSTANCE where EVENT_NAME = ?";
        return db.runUpdate(query, eventName);
    }

    public EventInstance getEventInstance(String eventName)
            throws SQLException {
        String query = "select DOCUMENT_ID, STATUS_CD, " +
            " CREATE_DT, CONSUME_DT, PRESERVE_INTERVAL, COMMENTS from EVENT_INSTANCE " +
            " where EVENT_NAME=?";
        ResultSet rs = db.runSelect(query, eventName);
        if (rs.next()) {
            EventInstance vo = new EventInstance();
            vo.setEventName(eventName);
            vo.setData(null);
            long docid = rs.getLong(1);
            vo.setDocumentId(docid==0L?null:docid);
            vo.setStatus(rs.getInt(2));
            vo.setCreateDate(rs.getTimestamp(3));
            vo.setConsumeDate(rs.getTimestamp(4));
            vo.setPreserveSeconds(rs.getInt(5));
            vo.setComments(rs.getString("COMMENTS"));
            return vo;
        } else {
            return null;
        }
    }

    private List<EventWaitInstance> getEventWaitInstances(String eventName) throws SQLException {
        List<EventWaitInstance> waiters = new ArrayList<EventWaitInstance>();
        String query = "select EVENT_WAIT_INSTANCE_OWNER_ID, WAKE_UP_EVENT, CREATE_DT " +
                "from EVENT_WAIT_INSTANCE " +
                "where EVENT_NAME=? order by EVENT_WAIT_INSTANCE_OWNER_ID, CREATE_DT desc";
        ResultSet rs = db.runSelect(query, eventName);
        while (rs.next()) {
            EventWaitInstance vo = new EventWaitInstance();
            vo.setActivityInstanceId(rs.getLong(1));
            vo.setCompletionCode(rs.getString(2));
            // Only return the most recent registration of the event per unique activity_instance_id
            // This situation would occur when calling the /Tasks REST service multiple times to register the same event for the same task_instance_id
            if (waiters.size() == 0 || !(waiters.get(waiters.size()-1).getActivityInstanceId().equals(vo.getActivityInstanceId())))
                waiters.add(vo);
        }
        return waiters;
    }

    public void createEventWaitInstance(Long actInstId, String eventName, String compCode)
            throws SQLException {
        Long id = db.isMySQL()?null:this.getNextId("EVENT_WAIT_INSTANCE_ID_SEQ");
        String query = "insert into EVENT_WAIT_INSTANCE " +
            "(EVENT_WAIT_INSTANCE_ID, EVENT_NAME, EVENT_WAIT_INSTANCE_OWNER_ID, " +
            "EVENT_WAIT_INSTANCE_OWNER, EVENT_SOURCE, WORK_TRANS_INSTANCE_ID, WAKE_UP_EVENT," +
            "STATUS_CD, CREATE_DT, CREATE_USR) " +
            "values (?, ?, ?, ?, ?, ?, ? ,?, "+nowPrecision()+", 'MDWEngine')";
        Object[] args = new Object[8];
        args[0] = id;
        args[1] = eventName;
        args[2] = actInstId;
        args[3] = OwnerType.ACTIVITY_INSTANCE;
        args[4] = EventLog.STANDARD_EVENT_SOURCE;
        args[5] = new Long(1);
        args[6] = compCode;
        args[7] = EventInstance.STATUS_WAITING;
        db.runUpdate(query, args);
        this.recordEventHistory(eventName, EventLog.SUBCAT_REGISTER, OwnerType.ACTIVITY_INSTANCE, actInstId, null);
    }

    public List<EventWaitInstance> recordEventArrive(String eventName, Long documentId) throws SQLException {
        boolean hasWaiters;
        try {
            this.recordEventHistory(eventName, EventLog.SUBCAT_ARRIVAL, OwnerType.DOCUMENT, documentId, null);
            createEventInstance(eventName, documentId, EventInstance.STATUS_ARRIVED, null, null, null, 0);
            hasWaiters = false;
        } catch (SQLException e) {
            if (db.isMySQL()) db.commit();
            EventInstance event = lockEventInstance(eventName);
            if (event == null)
                throw e;  // throw original SQLException
            hasWaiters = true;
            if (event.getStatus().equals(EventInstance.STATUS_WAITING)) { // Recurring type event - only 1 waiter is allowed
                deleteEventInstance(eventName); // Cleanup for next time same event is being registered for
                List<EventWaitInstance> eventWaits = getEventWaitInstances(eventName);
                if (eventWaits != null && eventWaits.size() > 0) // There is a waiter, so return it
                    return eventWaits;
                else // Event wait got removed from event_wait_instance table when waiter's process completed, but not from event_instance - recreate event arrival
                    return recordEventArrive(eventName, documentId);
            } else if (event.getStatus().equals(EventInstance.STATUS_WAITING_MULTIPLE)) {
                event.setDocumentId(documentId);
                consumeEventInstance(event, 0);
            }
        }
        return hasWaiters?this.getEventWaitInstances(eventName):null;
    }

    public Long recordEventWait(String eventName, boolean multipleRecepients,
            int preserveSeconds, Long actInstId, String compCode) throws SQLException {
        Long documentId;
        try {
            createEventInstance(eventName, null,
                    multipleRecepients?EventInstance.STATUS_WAITING_MULTIPLE:EventInstance.STATUS_WAITING,
                    null, null, null, preserveSeconds);
            documentId = null;
        } catch (SQLException e) {
            if (db.isMySQL()) db.commit();
            EventInstance event = lockEventInstance(eventName);
            if (event.getStatus().equals(EventInstance.STATUS_WAITING)) {
                if (multipleRecepients) {
                    throw new SQLException("The event has been waited by a single recepient");
                } else {
                    this.removeEventWait(eventName);    // deregister existing waiters
                }
                documentId = null;
            } else if (event.getStatus().equals(EventInstance.STATUS_WAITING_MULTIPLE)) {
                if (!multipleRecepients) {
                    throw new SQLException("The event has been waited by multiple recepients");
                }
                documentId = null;
            } else if (event.getStatus().equals(EventInstance.STATUS_ARRIVED)) {
                if (multipleRecepients) {
                    consumeEventInstance(event, preserveSeconds);
                    documentId = event.getDocumentId();
                } else {
                    deleteEventInstance(eventName);
                    documentId = event.getDocumentId();
                }
            } else if (event.getStatus().equals(EventInstance.STATUS_CONSUMED)) {
                if (multipleRecepients) {
                    documentId = event.getDocumentId();
                } else {
                    throw new SQLException("The event has been waited by multiple recepients");
                }
            } else {        // STATUS_FLAG
                throw new SQLException("The event is already recorded as a FLAG");
            }
        }
        createEventWaitInstance(actInstId, eventName, compCode);
        return documentId;
    }

    public boolean recordEventFlag(String eventName, int preserveSeconds) throws SQLException {
        boolean recorded;
        try {
            createEventInstance(eventName, null, EventInstance.STATUS_FLAG,
                    new Date(DatabaseAccess.getCurrentTime()), null, null, preserveSeconds);
            recorded = false;
        } catch (SQLException e) {
            if (db.isMySQL()) db.commit();
            EventInstance event = lockEventInstance(eventName);
            if (event.getStatus().equals(EventInstance.STATUS_FLAG)) {
                recorded = true;
            } else {
                throw new SQLException("The event is already recorded but not a FLAG");
            }
        }
        return recorded;
    }

    /**
     * remove other wait instances when an activity receives one event
     */
    public void removeEventWaitForActivityInstance(Long activityInstanceId, String reason) throws SQLException {
        String query = "delete from EVENT_WAIT_INSTANCE where EVENT_WAIT_INSTANCE_OWNER_ID=?";
        db.runUpdate(query, activityInstanceId);
        this.recordEventHistory("All Events", EventLog.SUBCAT_DEREGISTER,
                OwnerType.ACTIVITY_INSTANCE, activityInstanceId, reason);
        if (db.isMySQL())  //Commit since JMS message to resume activity was already sent, in case next activity to notify causes deadlock
            db.commit();
    }

    /**
     * remove existing waiters when a new waiter is registered for the same event
     * @param eventName
     * @throws SQLException
     */
    private void removeEventWait(String eventName) throws SQLException {
        String query = "delete from EVENT_WAIT_INSTANCE where EVENT_NAME=?";
        db.runUpdate(query, eventName);
        this.recordEventHistory(eventName, EventLog.SUBCAT_DEREGISTER,
                "N/A", 0L, "Deregister all existing waiters");
    }

    public void removeEventWaitForProcessInstance(Long processInstanceId) throws SQLException {
        String query;
        if (db.isMySQL()) // This is to separate the row locking from delete statement of impacted rows, to avoid deadlock in MySQL
            query = "delete E1 from EVENT_WAIT_INSTANCE E1 " +
                "join EVENT_WAIT_INSTANCE E2 using (EVENT_WAIT_INSTANCE_ID) " +
                "where E2.EVENT_WAIT_INSTANCE_OWNER_ID in " +
                "(select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID=?)";
        else
            query = "delete from EVENT_WAIT_INSTANCE " +
                "where EVENT_WAIT_INSTANCE_OWNER_ID in " +
                "  (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID=?)";
        db.runUpdate(query, processInstanceId);
        this.recordEventHistory("All Events", EventLog.SUBCAT_DEREGISTER,
                OwnerType.PROCESS_INSTANCE, processInstanceId, "process completed or cancelled");
    }

    public Long recordEventLog(String name, String category, String subCategory,
            String source, String ownerType, Long ownerId, String user, String modUser, String comments) throws SQLException {
        Long id = db.isMySQL()?null:this.getNextId("EVENT_LOG_ID_SEQ");
        String query = "insert into EVENT_LOG " +
            "(EVENT_LOG_ID, EVENT_NAME, EVENT_CATEGORY, EVENT_SUB_CATEGORY, " +
            "EVENT_SOURCE, EVENT_LOG_OWNER, EVENT_LOG_OWNER_ID, CREATE_USR, CREATE_DT, MOD_USR, COMMENTS, STATUS_CD) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, "+nowPrecision()+", ?, ?, '1')";
        Object[] args = new Object[10];
        args[0] = id;
        args[1] = name;
        args[2] = category;
        args[3] = subCategory;
        args[4] = source;
        args[5] = ownerType;
        args[6] = ownerId;
        args[7] = user == null ? "MDW" : user;
        args[8] = modUser;
        args[9] = comments;
        if (db.isMySQL()) id = db.runInsertReturnId(query, args);
        else db.runUpdate(query, args);
        return id;
    }

    public void recordScheduledJobHistory(String jobName, Date scheduledTime, String wlsServerName) throws SQLException {
        String subCategory = "Execute";
        String comments = "Scheduled time " + StringHelper.dateToString(scheduledTime);
        recordEventLog(jobName, EventLog.CATEGORY_SCHEDULED_JOB_HISTORY,
                subCategory, wlsServerName, "N/A", 0L, "MDW", null, comments);
    }

    private void recordEventHistory(String eventName, String subcat,
            String ownerType, Long ownerId, String comments) throws SQLException {
        recordEventLog(eventName, EventLog.CATEGORY_EVENT_HISTORY,
                subcat, "N/A", ownerType, ownerId, "MDW", null, comments);
    }

    public int countAuditLogs(List<String> searchDBClmns, Object searchKey) throws SQLException {
        String query = "select count(distinct EVENT_LOG_ID) from EVENT_LOG\n"
            + "WHERE EVENT_CATEGORY = ? \n"
            + (searchKey != null ? getAuditLogsSearchClause(searchDBClmns, searchKey) : "");

        ResultSet rs = db.runSelect(query, EventLog.CATEGORY_AUDIT);
        if (rs.next())
            return rs.getInt(1);
        else
            return 0;
    }

    public List<UserAction> getAuditLogs(String orderBy, boolean ascending, List<String> searchDBClmns, Object searchKey,
            int start, int end, boolean allRowsMode) throws SQLException {

        List<UserAction> userActions = new ArrayList<UserAction>();
        StringBuffer auditLogsQuery = new StringBuffer();
        String query = null;
        auditLogsQuery.append(" select EVENT_LOG_ID, EVENT_NAME, EVENT_SOURCE, EVENT_LOG_OWNER, EVENT_LOG_OWNER_ID, CREATE_USR, CREATE_DT, COMMENTS\n ")
                .append("  from EVENT_LOG where EVENT_CATEGORY = ?\n");
        if (searchKey != null)
            auditLogsQuery.append(getAuditLogsSearchClause(searchDBClmns, searchKey));
        if (orderBy != null)
            auditLogsQuery.append(" order by " + orderBy + " " + (ascending ? "" : "desc"));

        if (allRowsMode && end == 0) {
            query = auditLogsQuery.toString();
        }
        else {
            query = db.pagingQueryPrefix() + auditLogsQuery.toString() + db.pagingQuerySuffix(start, end - start);
        }

        ResultSet rs = db.runSelect(query, EventLog.CATEGORY_AUDIT);
        while (rs.next()) {
            UserAction userAction = new UserAction();
            userAction.setId(rs.getLong(1));
            String actionName = rs.getString(2);
            Action action = UserAction.getAction(actionName);
            userAction.setAction(action);
            if (action.equals(Action.Other))
                userAction.setExtendedAction(actionName);
            userAction.setSource(rs.getString(3));
            userAction.setEntity(UserAction.getEntity(rs.getString(4)));
            userAction.setEntityId(rs.getLong(5));
            userAction.setUser(rs.getString(6));
            userAction.setDate(rs.getTimestamp(7));
            userAction.setDescription(rs.getString(8));
            userActions.add(userAction);
        }
        return userActions;
    }

    private String getAuditLogsSearchClause(List<String> searchDBClmns, Object searchKey) {
        StringBuffer searchClause = new StringBuffer();
        String searchValue = searchKey.toString().toUpperCase();
        searchClause.append(" AND (");
        for (String column : searchDBClmns) {
            if (!"CREATE_DT".equals(column))
            searchClause.append("upper(").append(column).append(") like '%").append(searchValue).append("%' OR ");
        }
        searchClause.replace(searchClause.length() - 3, searchClause.length(), ")");
        return searchClause.toString();
     }

    /**
     * Method that returns distinct event log event names
     * @return String[]
     *
     */
    public String[] getDistinctEventLogEventNames() throws SQLException {
        String sql = "select distinct EVENT_NAME from EVENT_LOG";
        List<String> retList = new ArrayList<String>();
        ResultSet rs = db.runSelect(sql.toString(), null);
        while (rs.next()) {
            String evName = rs.getString(1);
            retList.add(evName);
        }
        return retList.toArray(new String[retList.size()]);
    }

    /**
     * Method that returns distinct event log sources
     * @return String[]
     *
     */
    public String[] getDistinctEventLogEventSources() throws SQLException {
        String sql = "select distinct EVENT_SOURCE from EVENT_LOG";
        List<String> retList = new ArrayList<String>();
        ResultSet rs = db.runSelect(sql.toString(), null);
        while (rs.next()) {
            String evName = rs.getString(1);
            retList.add(evName);
        }
        return retList.toArray(new String[retList.size()]);
    }

    private static final String PROCINST_QUERY_FIELDS =
        "PROCESS_INSTANCE_ID,PROCESS_ID,OWNER,OWNER_ID,MASTER_REQUEST_ID," +
            "STATUS_CD,SECONDARY_OWNER,SECONDARY_OWNER_ID,COMPCODE,COMMENTS";

    private ProcessInstance getProcessInstanceSub(ResultSet rs) throws SQLException {
         ProcessInstance pi = new ProcessInstance(rs.getLong(2), null);
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

    /**
     * different from V4: include completion code; add flag to say
     * if variable instances are needed
     */
    public ProcessInstance getProcessInstance(Long procInstId)
            throws SQLException {
        String query = "select " + PROCINST_QUERY_FIELDS +
            " from PROCESS_INSTANCE where PROCESS_INSTANCE_ID=?";
        ResultSet rs = db.runSelect(query, procInstId);
        if (!rs.next()) throw new SQLException("Failed to load process instance: " + procInstId);
        return this.getProcessInstanceSub(rs);
    }

    public List<VariableInstance> getProcessInstanceVariables(Long processInstanceId)
            throws SQLException {
        Map<Long,VariableInstance> oldVarInsts = null;
        List<VariableInstance> variableDataList = new ArrayList<VariableInstance>();
        String query = "select VARIABLE_INST_ID, VARIABLE_VALUE, VARIABLE_NAME, VARIABLE_TYPE_ID from VARIABLE_INSTANCE "
                + " where PROCESS_INST_ID = ?";
        ResultSet rs = db.runSelect(query, processInstanceId);
        while (rs.next()) {
            VariableInstance data = new VariableInstance();
            data.setInstanceId(rs.getLong(1));
            data.setStringValue(rs.getString(2));
            data.setName(rs.getString(3));
            data.setType(VariableTypeCache.getTypeName(rs.getLong(4)));
            if (data.getName()==null) {
                if (oldVarInsts==null) oldVarInsts = new HashMap<Long,VariableInstance>();
                oldVarInsts.put(data.getInstanceId(), data);;
            }
            variableDataList.add(data);
        }
        if (oldVarInsts!=null) {
            StringBuffer sb = new StringBuffer();
            sb.append("select vi.VARIABLE_INST_ID, v.VARIABLE_NAME, vt.VARIABLE_TYPE_NAME ");
            sb.append("from VARIABLE_INSTANCE vi, VARIABLE v, VARIABLE_TYPE vt ");
            sb.append("where vi.VARIABLE_INST_ID in (");
            boolean first=true;
            for (Long varInstId : oldVarInsts.keySet()) {
                if (first) first = false;
                else sb.append(",");
                sb.append(varInstId.toString());
            }
            sb.append(") and vi.VARIABLE_ID=v.VARIABLE_ID and v.VARIABLE_TYPE_ID=vt.VARIABLE_TYPE_ID");
            rs = db.runSelect(sb.toString(), null);
            while (rs.next()) {
                Long varInstId = rs.getLong(1);
                VariableInstance var = oldVarInsts.get(varInstId);
                var.setName(rs.getString(2));
                var.setType(rs.getString(3));
            }
        }
        return variableDataList;
    }

    public List<ProcessInstance> getChildProcessInstances(Long procInstId)
        throws SQLException {
        String query = "select " + PROCINST_QUERY_FIELDS +
            " from PROCESS_INSTANCE" +
            " where OWNER_ID = ? and OWNER=?" +
            "     order by CREATE_DT asc";
        Object[] args = new Object[2];
        args[0] = procInstId;
        args[1] = OwnerType.PROCESS_INSTANCE;
        ResultSet rs = db.runSelect(query, args);
        List<ProcessInstance> ret = new ArrayList<ProcessInstance>();
        while (rs.next()) {
            ret.add(getProcessInstanceSub(rs));
        }
        return ret;
    }

    public void setProcessInstanceCompletionCode(Long procInstId, String completionCode)
        throws SQLException {
        String query = "update PROCESS_INSTANCE set COMPCODE=?" +
                " where PROCESS_INSTANCE_ID=?";
        Object[] args = new Object[2];
        args[0] = completionCode;
        args[1] = procInstId;
        db.runUpdate(query, args);
    }

    public TransitionInstance getWorkTransitionInstance(Long transInstId) throws SQLException
    {
        String query = "select PROCESS_INST_ID,STATUS_CD,START_DT,END_DT,WORK_TRANS_ID,DEST_INST_ID" +
            " from WORK_TRANSITION_INSTANCE" +
            " where WORK_TRANS_INST_ID=?";
        ResultSet rs = db.runSelect(query, transInstId);
        if (rs.next()) {
            TransitionInstance workTransInstance = new TransitionInstance();
            workTransInstance.setTransitionInstanceID(transInstId);
            workTransInstance.setProcessInstanceID(rs.getLong(1));
            workTransInstance.setStatusCode(rs.getInt(2));
            workTransInstance.setStartDate(StringHelper.dateToString(rs.getTimestamp(3)));
            workTransInstance.setEndDate(StringHelper.dateToString(rs.getTimestamp(4)));
            workTransInstance.setTransitionID(rs.getLong(5));
            workTransInstance.setDestinationID(rs.getLong(6));
            return workTransInstance;
        } else throw new SQLException("Cannot find transition instance with ID " + transInstId);
    }

    public List<ActivityInstance> getActivityInstances(Long actId, Long procInstId,
            boolean activeOnly, boolean isSynchActivity) throws SQLException {
        String query = "select STATUS_CD,START_DT,END_DT,STATUS_MESSAGE,ACTIVITY_INSTANCE_ID" +
            " from ACTIVITY_INSTANCE where ACTIVITY_ID=? and PROCESS_INSTANCE_ID=?";
        if (activeOnly) {
             query = query + " and STATUS_CD in ("
                 + (isSynchActivity?(WorkStatus.STATUS_COMPLETED.intValue() + ","):"")
                 + WorkStatus.STATUS_IN_PROGRESS.intValue() + ","
                 + WorkStatus.STATUS_WAITING.intValue() + ","
                 + WorkStatus.STATUS_HOLD.intValue() + ")";
        }
        Object[] args = new Object[2];
        args[0] = actId;
        args[1] = procInstId;
        ResultSet rs = db.runSelect(query, args);
        List<ActivityInstance> ret = new ArrayList<ActivityInstance>();
        while (rs.next()) {
            ActivityInstance vo = new ActivityInstance();
            vo.setId(rs.getLong(5));
            vo.setStatusCode(rs.getInt(1));
            vo.setStartDate(rs.getTimestamp(2));
            vo.setEndDate(rs.getTimestamp(3));
            vo.setMessage(rs.getString(4));
            vo.setActivityId(actId);
            vo.setProcessInstanceId(procInstId);
            ret.add(vo);
        }
        return ret;
    }

    public List<ActivityInstance> getActivityInstancesForProcessInstance(Long procInstId) throws SQLException {
        String query = "select STATUS_CD,START_DT,END_DT,STATUS_MESSAGE,ACTIVITY_INSTANCE_ID,ACTIVITY_ID" +
            " from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID=?";
        ResultSet rs = db.runSelect(query, procInstId);
        List<ActivityInstance> ret = new ArrayList<ActivityInstance>();
        while (rs.next()) {
            ActivityInstance vo = new ActivityInstance();
            vo.setId(rs.getLong(5));
            vo.setStatusCode(rs.getInt(1));
            vo.setStartDate(rs.getTimestamp(2));
            vo.setEndDate(rs.getTimestamp(3));
            vo.setMessage(rs.getString(4));
            vo.setActivityId(rs.getLong(6));
            vo.setProcessInstanceId(procInstId);
            vo.setStatus(WorkStatuses.getWorkStatuses().get(vo.getStatusCode()));
            ret.add(vo);
        }
        return ret;
    }

    public void cancelTransitionInstances(Long procInstId, String comment, Long transId)
        throws SQLException {
        if (transId!=null) {
            String query = "update WORK_TRANSITION_INSTANCE set STATUS_CD = 10, END_DT = "+nowPrecision()+"," +
                    " MOD_DT = "+nowPrecision()+", COMMENTS = ? " +
                    "where PROCESS_INST_ID = ? and STATUS_CD in (1, 2, 4, 7) and WORK_TRANS_ID = ?";
            Object[] args = new Object[3];
            args[0] = comment;
            args[1] = procInstId;
            args[2] = transId;
            db.runUpdate(query, args);
        } else {
            String query = "update WORK_TRANSITION_INSTANCE set STATUS_CD = 10, END_DT = "+nowPrecision()+"," +
                " MOD_DT = "+nowPrecision()+", COMMENTS = ? " +
                "where PROCESS_INST_ID = ? and STATUS_CD in (1, 2, 4, 7)";
            Object[] args = new Object[2];
            args[0] = comment;
            args[1] = procInstId;
            db.runUpdate(query, args);
        }
    }

    public int countTransitionInstances(Long pProcInstId, Long pWorkTransId)
        throws SQLException
    {
        StringBuffer sql = new StringBuffer();
        sql.append("select count(STATUS_CD) from WORK_TRANSITION_INSTANCE ");
        sql.append("where PROCESS_INST_ID = ? and WORK_TRANS_ID = ?");
        Object[] args = new Object[2];
        args[0] = pProcInstId;
        args[1] = pWorkTransId;
        ResultSet rs = db.runSelect(sql.toString(), args);
        if (rs.next()) return rs.getInt(1);
        else return 0;
    }

    public int countActivityInstances(Long procInstId, Long activityId, Integer[] statuses)
        throws SQLException
    {
        StringBuffer sql = new StringBuffer();
        sql.append("select count(STATUS_CD) from ACTIVITY_INSTANCE ");
        sql.append("where PROCESS_INSTANCE_ID = ? and ACTIVITY_ID = ? and STATUS_CD in (");
        for (int i=0; i<statuses.length; i++) {
            if (i>0) sql.append(",");
            sql.append(statuses[i]);
        }
        sql.append(")");
        Object[] args = new Object[2];
        args[0] = procInstId;
        args[1] = activityId;
        ResultSet rs = db.runSelect(sql.toString(), args);
        if (rs.next()) return rs.getInt(1);
        else return 0;
    }

    public void determineCompletedTransitions(Long pProcInstId, List<Transition> transitions)
        throws SQLException {
        String sql = "select STATUS_CD from WORK_TRANSITION_INSTANCE where WORK_TRANS_ID=?"
            + " and PROCESS_INST_ID=? and STATUS_CD=?";
        ResultSet rs;
        Object[] args = new Object[3];
        args[1] = pProcInstId;
        args[2] = TransitionStatus.STATUS_COMPLETED;
        for (Transition trans : transitions) {
            args[0] = trans.getId();
            rs = db.runSelect(sql, args);
            if (rs.next()) trans.setEventType(EventType.FINISH);
            else trans.setEventType(EventType.START);
        }
    }

    public List<ProcessInstance> getProcessInstances(Long procId, String ownerType, Long ownerId)
        throws SQLException {
        String query = "select " + PROCINST_QUERY_FIELDS +
            " from PROCESS_INSTANCE where PROCESS_ID = ? and OWNER = ? and OWNER_ID = ?";
        Object[] args = new Object[3];
        args[0] = procId;
        args[1] = ownerType;
        args[2] = ownerId;
        ResultSet rs = db.runSelect(query, args);
        List<ProcessInstance> ret = new ArrayList<ProcessInstance>();
        while (rs.next()) {
            ret.add(this.getProcessInstanceSub(rs));
        }
        return ret;
    }

    public List<ProcessInstance> getProcessInstancesByMasterRequestId(String masterRequestId, Long processId)
            throws SQLException {
        List<ProcessInstance> ret = new ArrayList<ProcessInstance>();
        ResultSet rs;
        if (processId==null) {
            String query = "select " + PROCINST_QUERY_FIELDS +
                " from PROCESS_INSTANCE where MASTER_REQUEST_ID = ?";
            rs = db.runSelect(query, masterRequestId);
        } else {
            String query = "select " + PROCINST_QUERY_FIELDS +
                " from PROCESS_INSTANCE where MASTER_REQUEST_ID = ? and PROCESS_ID=?";
            rs = db.runSelect(query, new Object[]{masterRequestId, processId});
        }
        while (rs.next()) {
            ret.add(this.getProcessInstanceSub(rs));
        }
        return ret;
    }

    /**
     * Load all internal event and scheduled jobs before cutoff time.
     * If cutoff time is null, load only unscheduled events
     * @param cutofftime a date or null
     * @return
     * @throws SQLException
     */
    public List<ScheduledEvent> getScheduledEventList(Date cutofftime) throws SQLException {
        StringBuffer query = new StringBuffer();
        query.append("select EVENT_NAME,CREATE_DT,CONSUME_DT,AUXDATA,REFERENCE,COMMENTS ");
        query.append("from EVENT_INSTANCE ");
        query.append("where STATUS_CD in (");
        query.append(EventInstance.STATUS_SCHEDULED_JOB).append(",");
        query.append(EventInstance.STATUS_INTERNAL_EVENT).append(")");
        ResultSet rs;
        if (cutofftime==null) {
            query.append(" and CONSUME_DT is null");
            rs = db.runSelect(query.toString(), null);
        } else {
            query.append(" and CONSUME_DT < ?");
            rs = db.runSelect(query.toString(), cutofftime);
        }
        List<ScheduledEvent> ret = new ArrayList<ScheduledEvent>();
        while (rs.next()) {
            ScheduledEvent de = new ScheduledEvent();
            de.setName(rs.getString(1));
            de.setCreateTime(rs.getTimestamp(2));
            de.setScheduledTime(rs.getTimestamp(3));
            de.setMessage(rs.getString(4));
            de.setReference(rs.getString(5));
            if (de.getMessage()==null) de.setMessage(rs.getString(6));
            ret.add(de);
        }
        return ret;
    }

    /**
     * Load all internal events start at the specified age and scheduled jobs before cutoff time.
     * If cutoff time is null, load only unscheduled events
     * @param cutofftime a date or null
     * @return
     * @throws SQLException
     */
    public List<UnscheduledEvent> getUnscheduledEventList(Date cutoffTime, int maxRows) throws SQLException {
        StringBuffer query = new StringBuffer();
        query.append("select EVENT_NAME,CREATE_DT,AUXDATA,REFERENCE,COMMENTS ");
        query.append("from EVENT_INSTANCE ");
        query.append("where STATUS_CD = " + EventInstance.STATUS_INTERNAL_EVENT + " ");
        query.append("and CREATE_DT < ? ");
        query.append("and CONSUME_DT is null ");
        if(!db.isMySQL())
            query.append("and ROWNUM <= " + maxRows + " ");
        query.append("order by CREATE_DT");
        if (db.isMySQL())
            query.append(" LIMIT " + maxRows + " ");
        List<UnscheduledEvent> ret = new ArrayList<UnscheduledEvent>();
        ResultSet rs = db.runSelect(query.toString(), cutoffTime);
        while (rs.next()) {
            UnscheduledEvent ue = new UnscheduledEvent();
            ue.setName(rs.getString("EVENT_NAME"));
            ue.setCreateTime(rs.getTimestamp("CREATE_DT"));
            ue.setMessage(rs.getString("AUXDATA"));
            ue.setReference(rs.getString("REFERENCE"));
            if (ue.getMessage() == null)
                ue.setMessage(rs.getString("COMMENTS"));
            ret.add(ue);
        }
        return ret;
    }

    public ScheduledEvent lockScheduledEvent(String name) {
        String query = "select EVENT_NAME,CREATE_DT,CONSUME_DT,AUXDATA,COMMENTS " +
                        "from EVENT_INSTANCE " +
                        "where EVENT_NAME = ? for update";
        if (!db.isMySQL()) query = query + " nowait";
        try {
            ResultSet rs = db.runSelect(query, name);
            if (rs.next()) {
                ScheduledEvent de = new ScheduledEvent();
                de.setName(rs.getString(1));
                de.setCreateTime(rs.getTimestamp(2));
                de.setScheduledTime(rs.getTimestamp(3));
                de.setMessage(rs.getString(4));
                if (de.getMessage()==null) de.setMessage(rs.getString(5));
                return de;
            } else return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public void offerScheduledEvent(ScheduledEvent event) throws SQLException {
        createEventInstance(event.getName(), null,
                event.isScheduledJob()?EventInstance.STATUS_SCHEDULED_JOB:EventInstance.STATUS_INTERNAL_EVENT,
                event.getScheduledTime(), event.getMessage(), event.getReference(), 3600);
    }

    /**
     * Get the variable name for a task instance with 'Referred as' name
     * @param taskInstId
     * @param name
     * @return
     * @throws SQLException
     */
    public String getVariableNameForTaskInstance(Long taskInstId, String name) throws SQLException {
        String query = "select v.VARIABLE_NAME " +
            "from VARIABLE v, VARIABLE_MAPPING vm, TASK t, TASK_INSTANCE ti " +
            "where ti.TASK_INSTANCE_ID = ?" +
            "    and ti.TASK_ID = t.TASK_ID" +
            "    and vm.MAPPING_OWNER = 'TASK'" +
            "    and vm.MAPPING_OWNER_ID = t.TASK_ID" +
            "    and v.VARIABLE_ID = vm.VARIABLE_ID" +
            "    and (v.VARIABLE_NAME = ? or vm.VAR_REFERRED_AS = ?)";
        Object[] args = new Object[3];
        args[0] = taskInstId;
        args[1] = name;
        args[2] = name;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) {
            /*if (rs.isLast())*/ return rs.getString(1);
            //else throw new SQLException("getVariableNameForTaskInstance returns non-unique result");
        } else throw new SQLException("getVariableNameForTaskInstance returns no result");
    }

    public Document getDocument(DocumentReference docref, boolean forUpdate) throws SQLException {
        return super.getDocument(docref.getDocumentId(), forUpdate);
    }

    public Integer lockActivityInstance(Long actInstId) throws SQLException {
        String query =
                "select STATUS_CD from ACTIVITY_INSTANCE where ACTIVITY_INSTANCE_ID=? for update";
        ResultSet rs = db.runSelect(query, actInstId);
        if (rs.next()) return new Integer(rs.getInt(1));
        throw new SQLException("Activity instance does not exist: " + actInstId);
    }

    public Integer lockProcessInstance(Long procInstId) throws SQLException {
        String query =
                "select STATUS_CD from PROCESS_INSTANCE where PROCESS_INSTANCE_ID=? for update";
        ResultSet rs = db.runSelect(query, procInstId);
        if (rs.next()) return new Integer(rs.getInt(1));
        throw new SQLException("Process instance does not exist: " + procInstId);
    }

    public void updateActivityInstanceEndTime(Long actInstId, Date endtime) throws SQLException {
        String query = "update ACTIVITY_INSTANCE set END_DT=? where ACTIVITY_INSTANCE_ID=?";
        Object[] args = new Object[2];
        args[0] = endtime;
        args[1] = actInstId;
        db.runUpdate(query, args);
    }

    public List<CertifiedMessage> getCertifiedMessageList() throws SQLException {
        String query = "select EVENT_NAME,DOCUMENT_ID,CREATE_DT,STATUS_CD,PRESERVE_INTERVAL,AUXDATA,REFERENCE,COMMENTS " +
                        "from EVENT_INSTANCE " +
                        "where STATUS_CD = ?";
        ResultSet rs = db.runSelect(query, EventInstance.STATUS_CERTIFIED_MESSAGE);
        List<CertifiedMessage> ret = new ArrayList<CertifiedMessage>();
        Date now = new Date(DatabaseAccess.getCurrentTime());
        while (rs.next()) {
            CertifiedMessage message = new CertifiedMessage();
            long docid = rs.getLong(2);
            message.setDocumentId(docid==0L?null:docid);
            message.setContent(null);
            message.setInitiateTime(rs.getTimestamp(3));
            message.setStatus(rs.getInt(4));
            message.setTryCount(rs.getInt(5));
            message.setPropertyString(rs.getString(6));
            message.setReference(rs.getString(7));
            if (message.getPropertyString()==null) message.setPropertyString(rs.getString(8));
            message.setNextTryTime(now);
            ret.add(message);
        }
        return ret;
    }

    public void recordCertifiedMessage(CertifiedMessage message)
    throws SQLException {
        createEventInstance(message.getId(), message.getDocumentId(),
                EventInstance.STATUS_CERTIFIED_MESSAGE, null,
                message.getPropertyString(), message.getReference(), message.getTryCount());
    }

    public void consumeCertifiedMessage(String msgid)
    throws SQLException {
        long now = DatabaseAccess.getCurrentTime();
        createEventInstance(msgid, null,
                EventInstance.STATUS_CERTIFIED_MESSAGE_RECEIVED,
                new Date(now), null, null, 3600*24*365);
    }

    public CertifiedMessage lockCertifiedMessage(String msgid)
    throws SQLException {
        String query = "select EVENT_NAME,CONSUME_DT,STATUS_CD,PRESERVE_INTERVAL " +
            "from EVENT_INSTANCE " +
            "where EVENT_NAME = ? for update";
        if (!db.isMySQL()) query = query + " nowait";
        ResultSet rs = db.runSelect(query, msgid);
        if (rs.next()) {
            CertifiedMessage message = new CertifiedMessage();
            message.setNextTryTime(rs.getTimestamp(2));
            message.setStatus(rs.getInt(3));
            message.setTryCount(rs.getInt(4));
            return message;
        } else return null;
    }

    public void updateCertifiedMessageStatus(String msgid, Integer status, int tryCount, Date consumeTime)
    throws SQLException {
        StringBuffer query = new StringBuffer();
        query.append("update EVENT_INSTANCE set STATUS_CD=?, CONSUME_DT=?");
        if (status.equals(EventInstance.STATUS_CERTIFIED_MESSAGE))
            query.append(", PRESERVE_INTERVAL=").append(tryCount);    // restart count
        query.append(" where EVENT_NAME = ?");
        Object[] args = new Object[3];
        args[0] = status;
        args[1] = consumeTime;
        args[2] = msgid;
        db.runUpdate(query.toString(), args);
    }

    public int getTableRowCount(String tableName, String whereClause) throws SQLException {
        StringBuffer buff = new StringBuffer();
        buff.append("select count(*) from ").append(tableName);
        if (whereClause!=null) buff.append(" where ").append(whereClause);
        ResultSet rs = db.runSelect(buff.toString(), null);
        rs.next();
        return rs.getInt(1);
    }

    public List<String[]> getTableRowList(String tableName, Class<?>[] types, String[] fields,
            String whereClause, String orderby, boolean descending, int startRow, int rowCount)
            throws SQLException {
        StringBuffer buff = new StringBuffer();
        if (rowCount>0) buff.append(db.pagingQueryPrefix());
        buff.append("  select ");
        for (int j=0; j<fields.length; j++) {
            if (j>0) buff.append(",");
            buff.append(fields[j]);
        }
        buff.append("  from ").append(tableName);
        if (whereClause!=null) buff.append(" where ").append(whereClause);
        if (orderby!=null && orderby.length()>0) {
            buff.append(" order by ").append(orderby);
            if (descending) buff.append(" desc");
        }
        if (rowCount>0) buff.append(db.pagingQuerySuffix(startRow-1, rowCount));
        String query = buff.toString();
        ResultSet rs = db.runSelect(query, null);
        List<String[]> ret = new ArrayList<String[]>();
        while (rs.next()) {
            String[] row = new String[types.length];
            for (int j=0; j<types.length; j++) {
                if (types[j]==Date.class) {
                    Date d = rs.getTimestamp(j+1);
                    if (d!=null) row[j] = StringHelper.dateToString(d);
                } else if (types[j]==Long.class) {
                    long v = rs.getLong(j+1);
                    if (v!=0L) row[j] = Long.toString(v);
                } else if (types[j]==Integer.class) {
                    int v = rs.getInt(j+1);
                    row[j] = Integer.toString(v);
                } else row[j] = rs.getString(j+1);
            }
            ret.add(row);
        }
        return ret;
    }

    public int deleteTableRow(String tableName, String fieldName, Object fieldValue)
    throws SQLException {
        String query = "delete from " + tableName + " where " + fieldName + "=?";
        return db.runUpdate(query, fieldValue);
    }

    public Long createTableRow(String tableName, String[] fieldNames, Object[] fieldValues)
            throws SQLException {
        StringBuffer sb = new StringBuffer();
        Long id = null;
        sb.append("insert into ").append(tableName).append(" (");
        for (int i=0; i<fieldNames.length; i++) {
            if (i>0) sb.append(",");
            sb.append(fieldNames[i]);
        }
        sb.append(") values (");
        if (db.isMySQL()) {
            for (int i=0; i<fieldNames.length; i++) {
                if (i>0) sb.append(",");
                sb.append("?");
                if (fieldValues[i] instanceof TableSequenceName) {
                    fieldValues[i] = null;
                }
            }
            sb.append(")");
            String query = sb.toString();
            id = db.runInsertReturnId(query, fieldValues);
        } else {
            for (int i=0; i<fieldNames.length; i++) {
                if (i>0) sb.append(",");
                sb.append("?");
                if (fieldValues[i] instanceof TableSequenceName) {
                    id = this.getNextId(((TableSequenceName)fieldValues[i]).getSequenceName());
                    fieldValues[i] = id;
                }
            }
            sb.append(")");
            String query = sb.toString();
            db.runUpdate(query, fieldValues);
        }
        return id;
    }

    public int updateTableRow(String tableName, String keyName, Object keyValue,
            String[] fieldNames, Object[] fieldValues)
            throws SQLException {
        StringBuffer sb = new StringBuffer();
        sb.append("update ").append(tableName).append(" set ");
        Object[] args = new Object[fieldNames.length+1];
        for (int j=0; j<fieldNames.length; j++) {
            if (j>0) sb.append(", ");
            sb.append(fieldNames[j]).append("=?");
            args[j] = fieldValues[j];
        }
        sb.append(" where ").append(keyName).append("=?");
        args[fieldNames.length] = keyValue;
        return db.runUpdate(sb.toString(), args);
    }

    public void persistInternalEvent(String eventId, String message)
            throws SQLException {
        createEventInstance(eventId, null, EventInstance.STATUS_INTERNAL_EVENT,
                    null, message, EventInstance.ACTIVE_INTERNAL_EVENT, 3600);
    }

    public Long setAttribute(String ownerType, Long ownerId,
            String attrname, String attrvalue)
    throws SQLException {
        return super.setAttribute0(ownerType, ownerId, attrname, attrvalue);
    }

    public void setAttributes(String ownerType, Long ownerId, Map<String,String> attributes) throws SQLException {
        super.setAttributes0(ownerType, ownerId, attributes);
    }
}
