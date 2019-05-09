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
package com.centurylink.mdw.dataaccess.db;

import com.centurylink.mdw.cache.impl.VariableTypeCache;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.DocumentDbAccess;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetHeader;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.WorkStatuses;
import com.centurylink.mdw.util.TransactionUtil;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import javax.sql.XAConnection;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommonDataAccess {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static final String MYSQL_DT_FORMAT = "yyyy-MM-dd HH:mm:ss"; // 2018-12-08 00:00:00
    private static final String ORACLE_DT_FORMAT = "dd-MMM-yyyy hh:mm:ss aa"; // 08-DEC-2018 12.00.00 AM

    protected static final String PROC_INST_COLS = "pi.master_request_id, pi.process_instance_id, pi.process_id, pi.owner, pi.owner_id, " +
            "pi.status_cd, pi.start_dt, pi.end_dt, pi.compcode, pi.comments, pi.template";

    protected DatabaseAccess db;
    protected DocumentDbAccess documentDbAccess;
    public DocumentDbAccess getDocumentDbAccess() { return documentDbAccess; }

    public CommonDataAccess() {
        this(null);
    }

    protected CommonDataAccess(DatabaseAccess db) {
        this.db = db == null ? new DatabaseAccess(null) : db;
        this.documentDbAccess = new DocumentDbAccess(DatabaseAccess.getDocumentDb());
    }

    /**
     * Should only be used with MDW data source
     */
    public TransactionWrapper startTransaction() throws DataAccessException {
        TransactionWrapper transaction = new TransactionWrapper();
        TransactionUtil transUtil = TransactionUtil.getInstance();
        TransactionManager transManager = transUtil.getTransactionManager();
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("startTransaction - transaction manager=" + transManager.hashCode() + " (status=" + transManager.getStatus() + ")");
            }
            transaction.setDatabaseAccess(db);
            if (transManager.getStatus()==Status.STATUS_NO_TRANSACTION) {
                transaction.setTransactionAlreadyStarted(false);
                // Get connection BEFORE beginning transaction to avoid transaction timeout (10 minutes) exceptions (Fail to stop the transaction)
                db.openConnection().setAutoCommit(false); // Also set autoCommit to false
                transManager.begin();
                transUtil.setCurrentConnection(db.getConnection());
            } else {
                if (logger.isTraceEnabled())
                    logger.trace("   ... transaction already started, status=" + transManager.getStatus());
                transaction.setTransactionAlreadyStarted(true);
                if (db.connectionIsOpen()) {
                    transaction.setDatabaseConnectionAlreadyOpened(true);
                } else {
                    if (logger.isTraceEnabled())
                        logger.trace("   ... but database is not open");
                    // not opened through this DatabaseAccess
                    transaction.setDatabaseConnectionAlreadyOpened(false);
                    if (transUtil.getCurrentConnection() == null) {
                        db.openConnection().setAutoCommit(false);  // Set autoCommit to false
                        transUtil.setCurrentConnection(db.getConnection());
                    } else {
                        db.setConnection(transUtil.getCurrentConnection());
                    }
                }
            }
            transaction.setTransaction(transManager.getTransaction());
            return transaction;
        } catch (Throwable e) {
            if (transaction.getTransaction()!=null) stopTransaction(transaction);
            throw new DataAccessException(0, "Fail to start transaction", e);
        }
    }

    /**
     * Should only be used with MDW data source
     * @param transaction
     */
    public void stopTransaction(TransactionWrapper transaction) throws DataAccessException {
        if (logger.isTraceEnabled()) {
            logger.trace("stopTransaction");
        }
        if (transaction==null) {
            if (logger.isTraceEnabled())
                logger.trace("   ... transaction is null");
            return;
        }
        if (!transaction.isTransactionAlreadyStarted()) {
            DataAccessException exception = null;
            if (!transaction.isDatabaseConnectionAlreadyOpened()) {
                if (!(db.getConnection() instanceof XAConnection)) {
                    if (transaction.isRollbackOnly()) db.rollback();
                    else {
                        try {
                            db.commit();
                        } catch (SQLException e) {
                            exception = new DataAccessException(0, "Fail to commit", e);
                        }
                    }
                }
                db.closeConnection();
            } else {
                if (logger.isTraceEnabled())
                    logger.trace("   ... database opened by others");
            }
            try {
                TransactionUtil transUtil = TransactionUtil.getInstance();
                TransactionManager transManager = transUtil.getTransactionManager();
                if (transaction.isRollbackOnly()) transManager.rollback();
                else transManager.commit();
                transUtil.setCurrentConnection(null);
                //transUtil.clearCurrentConnection();
            } catch (Exception e) {
                throw new DataAccessException(0, "Fail to stop the transaction", e);
            }
            if (exception!=null) throw exception;
        } else {
            if (logger.isTraceEnabled())
                logger.trace("   ... transaction started by others");
        }
    }

    public void rollbackTransaction(TransactionWrapper transaction) {
        if (transaction!=null) {
            transaction.setRollbackOnly(true);
            if (transaction.getTransaction()!=null) {
                try {
                    transaction.getTransaction().setRollbackOnly();
                } catch (Exception e) {
                    StandardLogger logger = LoggerUtil.getStandardLogger();
                    logger.severeException("Fail to rollback", e);
                }
            }
        }
    }

    protected String now() {
        return db.isMySQL()?"now()":"sysdate";
    }

    protected String nowPrecision() {
        return db.isPrecisionSupport()?db.isMySQL()?"now(6)":"systimestamp":now();
    }

    public Map<String,String> getAttributes(String ownerType, Long ownerId)
    throws SQLException {
        try {
            db.openConnection();
            List<Attribute> attrs = getAttributes1(ownerType, ownerId);
            if (attrs == null)
                return null;
            Map<String,String> attributes = new HashMap<String,String>();
            for (Attribute attr : attrs)
                attributes.put(attr.getName(), attr.getValue());
            return attributes;
        }
        finally {
            db.closeConnection();
        }
    }

    protected List<Attribute> getAttributes0(String ownerType, Long ownerId)
    throws SQLException {
        String query = "select ATTRIBUTE_ID, ATTRIBUTE_NAME, ATTRIBUTE_VALUE from ATTRIBUTE " +
                "where ATTRIBUTE_OWNER_ID=? and ATTRIBUTE_OWNER='" + ownerType + "'";
        ResultSet rs = db.runSelect(query, ownerId);
        List<Attribute> attribs = new ArrayList<Attribute>();
        while (rs.next()) {
            attribs.add(new Attribute(rs.getString(2), rs.getString(3)));
        }
        return attribs;
    }

    /**
     * Same as getAttribute1 but handles overflow values
     * @param ownerType
     * @param ownerId
     * @return list of Attributes
     */
    protected List<Attribute> getAttributes1(String ownerType, Long ownerId)
    throws SQLException {
        List<Attribute> attrs = getAttributes0(ownerType, ownerId);
        if (attrs==null) return null;
        ResultSet rs;
        String query = "select RULE_SET_DETAILS from RULE_SET where RULE_SET_ID=?";
        for (Attribute attr : attrs) {
            String v = attr.getValue();
            if (v!=null && v.startsWith(Asset.ATTRIBUTE_OVERFLOW)) {
                Long assetId = new Long(v.substring(Asset.ATTRIBUTE_OVERFLOW.length()+1));
                rs = db.runSelect(query, assetId);
                if (rs.next()) {
                    attr.setValue(rs.getString(1));
                }
            }
        }
        return attrs;
    }

    public void setAttributes(String ownerType, Long ownerId, Map<String,String> attributes)
    throws SQLException {
        try {
            db.openConnection();
            setAttributes0(ownerType, ownerId, attributes);
            db.commit();
        }
        finally {
            db.closeConnection();
        }
    }

    public void setAttributes0(String ownerType, Long ownerId, Map<String,String> attributes)
    throws SQLException {
        List<Attribute> attrs = null;
        if (attributes != null && !attributes.isEmpty()) {
            attrs = new ArrayList<Attribute>();
            for (String name : attributes.keySet()) {
                String value = attributes.get(name);
                if (value != null && !value.isEmpty())
                    attrs.add(new Attribute(name, value));
            }
        }
        deleteAttributes0(ownerType, ownerId);
        if (attrs != null)
          addAttributes0(ownerType, ownerId, attrs);
    }

    public Long setAttribute(String ownerType, Long ownerId,
            String attrName, String attrValue) throws SQLException {
        try {
            db.openConnection();
            Long id = setAttribute0(ownerType, ownerId, attrName, attrValue);
            db.commit();
            return id;
        }
        finally {
            db.closeConnection();
        }
    }

    public Long setAttribute0(String ownerType, Long ownerId,
            String attrName, String attrValue)
            throws SQLException {
        String query = "select ATTRIBUTE_ID from ATTRIBUTE where " +
                    "ATTRIBUTE_OWNER=? and ATTRIBUTE_OWNER_ID=? and ATTRIBUTE_NAME=?";
        Object[] args = new Object[3];
        args[0] = ownerType;
        args[1] = ownerId;
        args[2] = attrName;
        ResultSet rs = db.runSelect(query, args);
        Long existingId = null;
        if (rs.next()) {
            Long attrId = rs.getLong(1);
            if (attrValue==null) {
                query = "delete ATTRIBUTE where " +
                "ATTRIBUTE_OWNER=? and ATTRIBUTE_OWNER_ID=? and ATTRIBUTE_NAME=?";
            } else {
                query = "update ATTRIBUTE set ATTRIBUTE_VALUE=? where ATTRIBUTE_ID=?";
                args = new Object[2];
                args[0] = attrValue;
                args[1] = attrId;
            }
            existingId = attrId;
        } else {
            query = "insert into ATTRIBUTE" +
                " (ATTRIBUTE_ID,ATTRIBUTE_OWNER,ATTRIBUTE_OWNER_ID,ATTRIBUTE_NAME,ATTRIBUTE_VALUE," +
                        "CREATE_DT,CREATE_USR)"
                + " values ("
                + (db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL") + ",?,?,?,?," + now() + ",'MDWEngine')";
            args = new Object[4];
            args[0] = ownerType;
            args[1] = ownerId;
            args[2] = attrName;
            args[3] = attrValue;
        }
        db.runUpdate(query, args);
        return existingId;
    }

    protected void deleteAttributes0(String ownerType, Long ownerId)
    throws SQLException {
        String query = "delete from ATTRIBUTE " +
            " where ATTRIBUTE_OWNER='" + ownerType + "' and ATTRIBUTE_OWNER_ID=?";
        db.runUpdate(query, ownerId);
    }

    protected void deleteValues0(String ownerType, String ownerId)
    throws SQLException {
        String query = "delete from VALUE " +
            " where OWNER_TYPE = '" + ownerType + "' and OWNER_ID = ?";
        db.runUpdate(query, ownerId);
    }

    protected void deleteOverflowAttributes(String attrPrefix)
    throws SQLException {
        String query = "delete from RULE_SET where RULE_SET_NAME like ?";
        db.runUpdate(query, attrPrefix);
    }

    protected void addAttributes0(String pOwner, Long pOwnerId, List<Attribute> pAttributes)
    throws SQLException {
        String query = "insert into ATTRIBUTE"
            + " (attribute_id,attribute_owner,attribute_owner_id,attribute_name,attribute_value,"
            + " create_dt,create_usr) values ("
            + (db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL")
            + ",?,?,?,?,"+now()+",'MDWEngine')";
        db.prepareStatement(query);
        Object[] args = new Object[4];
        for (Attribute vo : pAttributes) {
            String v = vo.getValue();
            if (v==null||v.length()==0) continue;
            args[0] = pOwner;
            args[1] = pOwnerId;
            args[2] = vo.getName();
            args[3] = v;
            db.addToBatch(args);
        }
        db.runBatchUpdate();
    }

    protected void updateMembersById(Long id, Long[] members,
            String selectQuery, String deleteQuery, String insertQuery, String errmsg)
        throws DataAccessException {
        try {
            db.openConnection();
            ResultSet rs = db.runSelect(selectQuery, id);
            List<Long> existing = new ArrayList<Long>();
            while (rs.next()) {
                existing.add(rs.getLong(1));
            }
            Object[] args = new Object[2];
            args[0] = id;
            for (Long e : existing) {
                boolean found = false;
                for (Long m : members) {
                    if (m.equals(e)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    args[1] = e;
                    db.runUpdate(deleteQuery, args);
                }
            }
            for (Long m : members) {
                boolean found = false;
                for (Long e : existing) {
                    if (m.equals(e)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    args[1] = m;
                    db.runUpdate(insertQuery, args);
                }
            }
            db.commit();
        } catch(Exception ex){
            db.rollback();
            throw new DataAccessException(-1, errmsg, ex);
        } finally {
            db.closeConnection();
        }
    }

    /**
     * Not for update.  Opens a new connection.
     */
    public Document getDocument(Long documentId) throws DataAccessException {
        try {
            db.openConnection();
            return this.getDocument(documentId, false);
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to load document: " + documentId, ex);
        } finally {
            db.closeConnection();
        }
    }

    public Document getDocument(Long documentId, boolean forUpdate) throws SQLException {
        return loadDocument(documentId, forUpdate);
    }

    public boolean isDocument(Long id) throws SQLException {
        String query = "select DOCUMENT_ID from DOCUMENT where DOCUMENT_ID = ?";
        try {
            db.openConnection();
            return db.runSelect(query, id).next();
        }
        finally {
            db.closeConnection();
        }
    }

    public Long getDocumentId(String ownerType, Long ownerId) throws SQLException {
        String query = "select DOCUMENT_ID from DOCUMENT where OWNER_TYPE = ? and OWNER_ID = ?";
        try {
            db.openConnection();
            Object[] args = { ownerType, ownerId };
            ResultSet rs = db.runSelect(query, args);
            if (rs.next()) {
                return rs.getLong("DOCUMENT_ID");
            }
        }
        finally {
            db.closeConnection();
        }
        return null;
    }

    public Document loadDocument(Long documentId, boolean forUpdate)
            throws SQLException {
        String query = "select CREATE_DT, MODIFY_DT, DOCUMENT_TYPE, OWNER_TYPE, OWNER_ID " +
            "from DOCUMENT where DOCUMENT_ID = ?" + (forUpdate ? " for update" : "");
        ResultSet rs = db.runSelect(query, documentId);
        if (rs.next()) {
            Document vo = new Document();
            vo.setDocumentId(documentId);
            vo.setCreateDate(rs.getTimestamp("CREATE_DT"));
            vo.setModifyDate(rs.getTimestamp("MODIFY_DT"));
            vo.setDocumentType(rs.getString("DOCUMENT_TYPE"));
            vo.setOwnerType(rs.getString("OWNER_TYPE"));
            vo.setOwnerId(rs.getLong("OWNER_ID"));
            boolean foundInDocDb = false;
            if (documentDbAccess.hasDocumentDb()) {
                String docDbContent = documentDbAccess.getDocumentContent(vo.getOwnerType(), vo.getDocumentId());
                if (docDbContent != null) {
                    vo.setContent(docDbContent);
                    foundInDocDb = true;
                }
            }
            if (!foundInDocDb) {
                query = "select CONTENT from DOCUMENT_CONTENT where DOCUMENT_ID = ?";
                rs = db.runSelect(query, documentId);
                if (rs.next())
                    vo.setContent(rs.getString("CONTENT"));
            }
            return vo;
        }
        else {
            throw new SQLException("Document with ID " + documentId + " does not exist");
        }
    }

    public Process getProcessBase0(String processName, int version)
            throws SQLException,DataAccessException {
        String query;
        if (version>0) {
            query = "select RULE_SET_ID, COMMENTS, VERSION_NO, MOD_DT, MOD_USR" +
                " from RULE_SET where RULE_SET_NAME=? and LANGUAGE='"+Asset.PROCESS+"' and VERSION_NO=" + version;
        } else {
            query = "select RULE_SET_ID, COMMENTS, VERSION_NO, MOD_DT, MOD_USR" +
                " from RULE_SET where RULE_SET_NAME=? and LANGUAGE='"+Asset.PROCESS+"' order by VERSION_NO desc";
        }
        ResultSet rs = db.runSelect(query, processName);
        String processComment;
        Long processId;
        if (rs.next()) {
            processId = new Long(rs.getLong(1));
            processComment = rs.getString(2);
            version = rs.getInt(3);
        } else throw new DataAccessException("Process does not exist; name=" + processName);
        Process retVO= new Process(processId, processName, processComment, null);   // external events - load later
        retVO.setVersion(version);
        retVO.setModifyDate(rs.getTimestamp(4));
        retVO.setModifyingUser(rs.getString(5));
        return retVO;
    }

    protected int countRows(String tableName, String keyElement, String whereCondition)
    throws SQLException {
        StringBuffer buff = new StringBuffer();

        buff.append("select count(").append(keyElement).append(") from ").append(tableName);
        if (whereCondition!=null) buff.append(" where ").append(whereCondition);
        String query = buff.toString();
        ResultSet rs = db.runSelect(query);
        if (rs.next()) {
            return rs.getInt(1);
        } else throw new SQLException("Failed to count rows");
    }

    protected List<String[]> queryRows(String tableName, String[] fields, String whereCondition,
            String sortOn, int startIndex, int endIndex)
    throws SQLException {
        StringBuffer buff = new StringBuffer();
        buff.append(db.pagingQueryPrefix());
        buff.append("select ");
        int n = fields.length;
        for (int i=0; i<n; i++) {
            if (i>0) buff.append(",");
            buff.append(fields[i]);
        }
        buff.append(" from ").append(tableName);
        if (whereCondition!=null) buff.append(" where ").append(whereCondition);
        if (sortOn!=null) {
            boolean desc = false;
            if (sortOn.startsWith("-")) {
                desc = true;
                sortOn = sortOn.substring(1);
            }
            buff.append(" order by ").append(sortOn);
            if (desc) buff.append(" desc");
        }
        buff.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));
        String query = buff.toString();
        ResultSet rs = db.runSelect(query);
        List<String[]> result = new ArrayList<String[]>();
        while (rs.next()) {
            String[] one = new String[n];
            for (int i=0; i<n; i++) {
                one[i] = rs.getString(i+1);
            }
            result.add(one);
        }
        return result;
    }

    protected Long getNextId(String sequenceName) throws SQLException {
        String query = "select " + sequenceName + ".NEXTVAL from DUAL";
        ResultSet rs = db.runSelect(query);
        rs.next();
        return new Long(rs.getString(1));
    }

    public List<String> getValueNames(String ownerType) throws DataAccessException {
        try {
            List<String> names = new ArrayList<String>();
            db.openConnection();
            String sql = "select distinct name from VALUE where owner_type = ?";
            ResultSet rs = db.runSelect(sql, ownerType);
            while (rs.next())
                names.add(rs.getString(1));
            return names;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve value names for ownerType: " + ownerType);
        }
        finally {
            db.closeConnection();
        }
    }

    public String getValue(String ownerType, String ownerId, String name) throws SQLException {
        try {
            db.openConnection();
            return getValue0(ownerType, ownerId, name);
        }
        finally {
            db.closeConnection();
        }
    }

    protected String getValue0(String ownerType, String ownerId, String name) throws SQLException {
        String query = "select value from VALUE where owner_type = ? and ownerId = ? and name = ?";
        Object[] args = new Object[3];
        args[0] = ownerType;
        args[1] = ownerId;
        args[2] = name;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next())
            return rs.getString(1);
        else
            return null;
    }

    public Map<String,String> getValues(String ownerType, String ownerId) throws SQLException {
        try {
            db.openConnection();
            return getValues0(ownerType, ownerId);
        }
        finally {
            db.closeConnection();
        }
    }

    public Map<String,String> getValues0(String ownerType, String ownerId) throws SQLException {
        Map<String,String> values = null;
        String query = "select name, value from VALUE where owner_type = ? and owner_id = ?";
        Object[] args = new Object[2];
        args[0] = ownerType;
        args[1] = ownerId;
        ResultSet rs = db.runSelect(query, args);
        while (rs.next()) {
            if (values == null)
                values = new HashMap<String,String>();
            values.put(rs.getString(1), rs.getString(2));
        }
        return values;
    }

    public void setValues(String ownerType, String ownerId, Map<String,String> values) throws SQLException {
        try {
            db.openConnection();
            setValues0(ownerType, ownerId, values);
            db.commit();
        }
        finally {
            db.closeConnection();
        }
    }

    protected void setValues0(String ownerType, String ownerId, Map<String,String> values)
    throws SQLException {
        deleteValues0(ownerType, ownerId);
        if (values != null && !values.isEmpty())
            addValues0(ownerType, ownerId, values);
    }

    public void setValue(String ownerType, String ownerId, String name, String value) throws SQLException {
        try {
            db.openConnection();
            setValue0(ownerType, ownerId, name, value);
            db.commit();
        }
        finally {
            db.closeConnection();
        }
    }

    protected void setValue0(String ownerType, String ownerId, String name, String value)
            throws SQLException {
        String query = "select name from VALUE where " +
                    "OWNER_type=? and OWNER_ID=? and NAME=?";
        Object[] args = new Object[3];
        args[0] = ownerType;
        args[1] = ownerId;
        args[2] = name;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) {
            if (value==null) {
                query = "delete VALUE where " +
                "OWNER_TYPE=? and OWNER_ID=? and NAME=?";
            } else {
                query = "update VALUE set VALUE=? where OWNER_type=? and OWNER_ID=? and NAME=?";
                args = new Object[4];
                args[0] = value;
                args[1] = ownerType;
                args[2] = ownerId;
                args[3] = name;
            }
        } else {
            query = "insert into VALUE" +
                " (OWNER_TYPE,OWNER_ID,NAME,VALUE," +
                        "CREATE_DT,CREATE_USR)"
                + " values (?,?,?,?," + now() + ",'MDWEngine')";
            args = new Object[4];
            args[0] = ownerType;
            args[1] = ownerId;
            args[2] = name;
            args[3] = value;
        }
        db.runUpdate(query, args);
        return;
    }

    public void addValues(String ownerType, String ownerId, Map<String,String> values) throws SQLException {
        try {
            db.openConnection();
            addValues0(ownerType, ownerId, values);
            db.commit();
        }
        finally {
            db.closeConnection();
        }
    }

    protected void addValues0(String ownerType, String ownerId, Map<String,String> values)
    throws SQLException {
        String query = "insert into VALUE"
            + " (owner_type, owner_id, name, value,"
            + " create_dt,create_usr) values (?,?,?,?,"+now()+",'MDWEngine')";
        db.prepareStatement(query);
        Object[] args = new Object[4];

        for (String name : values.keySet()) {
            String v = values.get(name);
            if (v==null||v.length()==0) continue;
            args[0] = ownerType;
            args[1] = ownerId;
            args[2] = name;
            args[3] = v;
            db.addToBatch(args);
        }
        db.runBatchUpdate();
    }

    public List<String> getValueOwnerIds(String valueName, String valuePattern) throws SQLException {
        try {
            db.openConnection();
            String q;
            Object[] args;
            if (valuePattern == null) {
                q = "select owner_id from VALUE where name = ?";
                args = new Object[]{valueName};
            }
            else {
                if (valuePattern.contains("*")) {
                    q = "select owner_id from VALUE where name = ? and value like '" + valuePattern.replace('*', '%') + "'";
                    args = new Object[]{valueName};
                }
                else {
                    q = "select owner_id from VALUE where name = ? and value = ?";
                    args = new Object[]{valueName, valuePattern};
                }
            }
            ResultSet rs = db.runSelect(q, args);
            List<String> ids = new ArrayList<String>();
            while (rs.next())
                ids.add(rs.getString("owner_id"));
            return ids;
        }
        finally {
            db.closeConnection();
        }
    }

    public List<String> getValueOwnerIds(String ownerType, String valueName, String valuePattern) throws SQLException {
        try {
            db.openConnection();
            String q;
            Object[] args;
            if (valuePattern == null) {
                q = "select owner_id from VALUE where owner_type = ? and name = ?";
                args = new Object[]{ownerType, valueName};
            }
            else {
                if (valuePattern.contains("*")) {
                    q = "select owner_id from VALUE where owner_type = ? and name = ? and value like '" + valuePattern.replace('*', '%') + "'";
                    args = new Object[]{ownerType, valueName};
                }
                else {
                    q = "select owner_id from VALUE where owner_type = ? and name = ? and value = ?";
                    args = new Object[]{ownerType, valueName, valuePattern};
                }
            }
            ResultSet rs = db.runSelect(q, args);
            List<String> ids = new ArrayList<String>();
            while (rs.next())
                ids.add(rs.getString("owner_id"));
            return ids;
        }
        finally {
            db.closeConnection();
        }
    }

    public Long createVariable(Long processInstanceId, VariableInstance variableInstance) throws SQLException {
        try {
            db.openConnection();
            Long varInstId = createVariable0(processInstanceId, variableInstance);
            db.commit();
            return varInstId;
        }
        finally {
            db.closeConnection();
        }
    }

    protected Long createVariable0(Long processInstanceId, VariableInstance variableInstance) throws SQLException {
        Long varInstId = db.isMySQL() ? null : getNextId("VARIABLE_INST_ID_SEQ");
        String query = "insert into VARIABLE_INSTANCE " +
            "(VARIABLE_INST_ID, VARIABLE_ID, PROCESS_INST_ID, VARIABLE_VALUE, VARIABLE_NAME, VARIABLE_TYPE_ID, " +
            "CREATE_DT, CREATE_USR) values (?, ?, ?, ?, ?, ?, "+nowPrecision()+",'MDWEngine')";
        Object[] args = new Object[6];
        args[0] = varInstId;
        args[1] = variableInstance.getVariableId();
        args[2] = processInstanceId;
        args[3] = variableInstance.getStringValue();
        args[4] = variableInstance.getName();
        args[5] = VariableTypeCache.getTypeId(variableInstance.getType());
        if (db.isMySQL())
            varInstId = db.runInsertReturnId(query, args);
        else
            db.runUpdate(query, args);
        variableInstance.setInstanceId(varInstId);
        return varInstId;
    }

    public void updateVariable(VariableInstance variableInstance) throws SQLException {
        try {
            db.openConnection();
            updateVariable0(variableInstance);
            db.commit();
        }
        finally {
            db.closeConnection();
        }
    }

    protected void updateVariable0(VariableInstance variableInstance) throws SQLException {
        String query = "update VARIABLE_INSTANCE set VARIABLE_VALUE=?, MOD_DT=" + nowPrecision() + " where VARIABLE_INST_ID=?";
        Object[] args = new Object[2];
        args[0] = variableInstance.getStringValue();
        args[1] = variableInstance.getInstanceId();
        db.runUpdate(query, args);
    }

    public void deleteVariable(VariableInstance variableInstance) throws SQLException {
        try {
            db.openConnection();
            deleteVariable0(variableInstance);
            db.commit();
        }
        finally {
            db.closeConnection();
        }
    }

    protected void deleteVariable0(VariableInstance variableInstance) throws SQLException {
        String query = "delete from VARIABLE_INSTANCE where VARIABLE_INST_ID=?";
        db.runUpdate(query, variableInstance.getInstanceId());
    }

    public void setElapsedTime(String ownerType, Long instanceId, Long elapsedTime) throws SQLException {
        if (db.connectionIsOpen()) {
            setElapsedTime0(ownerType, instanceId, elapsedTime);
        }
        else {
            try {
                db.openConnection();
                setElapsedTime0(ownerType, instanceId, elapsedTime);
            } finally {
                db.closeConnection();
            }
        }
    }

    protected void setElapsedTime0(String ownerType, Long instanceId, Long elapsedTime) throws SQLException{
        String query = "insert into INSTANCE_TIMING " +
                "(INSTANCE_ID, OWNER_TYPE, ELAPSED_MS) "+
                 "values (?, ?, ?)";
        Object[] args = new Object[3];
        args[0] = instanceId;
        args[1] = ownerType;
        args[2] = elapsedTime;
        db.runUpdate(query, args);
    }

    protected Long getProcessElapsedTime0(Long processInstanceId) throws SQLException {
        String query;
        if (db.isOracle()) {
            query = "SELECT EXTRACT(day FROM DIFF)*24*60*60*1000 + " +
                    "EXTRACT(hour FROM DIFF)*60*60*1000 + " +
                    "EXTRACT(minute FROM DIFF)*60*1000 + " +
                    "EXTRACT(second FROM DIFF)*1000 as ELAPSED_MS" +
                    " FROM (SELECT (" + nowPrecision() + " - START_DT) AS DIFF" +
                    " from PROCESS_INSTANCE where PROCESS_INSTANCE_ID=?)";
        }
        else {
            query = "select TIMESTAMPDIFF(MICROSECOND,START_DT," + nowPrecision() + ")/1000"
                    + " from PROCESS_INSTANCE where PROCESS_INSTANCE_ID=? ";
        }
        Object[] args = new Object[1];
        args[0] = processInstanceId;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) {
            return rs.getLong(1);
        }
        else {
            throw new SQLException("Cannot find START_DT for process instance: " + processInstanceId);
        }
    }

    protected Long getActivityElapsedTime0(Long activityInstanceId) throws SQLException {
        String query;
        if (db.isOracle()) {
            query = "SELECT EXTRACT(day FROM DIFF)*24*60*60*1000 + " +
                    "EXTRACT(hour FROM DIFF)*60*60*1000 + " +
                    "EXTRACT(minute FROM DIFF)*60*1000 + " +
                    "EXTRACT(second FROM DIFF)*1000 as ELAPSED_MS" +
                    " FROM (SELECT (" + nowPrecision() + " - START_DT) AS DIFF" +
                    " from ACTIVITY_INSTANCE where ACTIVITY_INSTANCE_ID=?)";
        }
        else {
            query = "select TIMESTAMPDIFF(MICROSECOND,START_DT," + nowPrecision() + ")/1000"
                    + " from ACTIVITY_INSTANCE where ACTIVITY_INSTANCE_ID=? ";
        }
        ResultSet rs = db.runSelect(query, new Object[]{activityInstanceId});
        if (rs.next()) {
            return rs.getLong(1);
        }
        else {
            throw new SQLException("Cannot find START_DT for activity: " + activityInstanceId);
        }
    }

    public Long getRequestCompletionTime(String ownerType, Long ownerId) throws SQLException {
        if (db.connectionIsOpen())
            return getRequestCompletionTime0(ownerType, ownerId);
        else {
            try {
                db.openConnection();
                return getRequestCompletionTime0(ownerType, ownerId);
            } finally {
                db.closeConnection();
            }
        }
    }

    protected Long getRequestCompletionTime0(String ownerType, Long ownerId) throws SQLException {
        String responseOwner = ownerType + "_RESPONSE";
        String requestOwner = ownerType + "_REQUEST";
        String query;
        if (db.isOracle()) {
            query="SELECT EXTRACT(day FROM DIFF)*24*60*60*1000 + " +
                    "EXTRACT(hour FROM DIFF)*60*60*1000 + " +
                    "EXTRACT(minute FROM DIFF)*60*1000 + " +
                    "EXTRACT(second FROM DIFF)*1000 as ELAPSED_MS" +
                    " FROM (SELECT (t1.CREATE_DT - t2.CREATE_DT) as DIFF" +
                    " from  DOCUMENT t1" +
                    " left join DOCUMENT t2" +
                    " on t2.OWNER_ID=t1.OWNER_ID" +
                    " and t2.OWNER_TYPE='" + requestOwner + "'" +
                    " and t1.OWNER_TYPE='" + responseOwner + "'" +
                    " where t1.OWNER_ID=?)";
        }
        else {
          query="select TIMESTAMPDIFF(MICROSECOND,(select t2.CREATE_DT"+
            " from DOCUMENT t2" +
            " where OWNER_TYPE='" + requestOwner + "'" +
            " and t2.OWNER_ID=t1.OWNER_ID),t1.CREATE_DT)/1000" +
            " from DOCUMENT t1 where t1.OWNER_ID=?" +
            " and t1.OWNER_TYPE='" + responseOwner + "' ";
        }
        Object[] args = new Object[1];
        args[0] = ownerId;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) {
            return rs.getLong(1);
        }
        else {
            throw new SQLException("Unable to find CREATE_DT for " + ownerType + ": " + ownerId);
        }
    }



    /**
     * Assumes pi.* table prefix.
     */
    protected ProcessInstance buildProcessInstance(ResultSet rs) throws SQLException {
        ProcessInstance pi = new ProcessInstance();
        pi.setMasterRequestId(rs.getString("master_request_id"));
        pi.setId(rs.getLong("process_instance_id"));
        pi.setProcessId(rs.getLong("process_id"));
        pi.setOwner(rs.getString("owner"));
        pi.setOwnerId(rs.getLong("owner_id"));
        int statusCode = rs.getInt("status_cd");
        pi.setStatus(WorkStatuses.getWorkStatuses().get(statusCode));
        pi.setStartDate(rs.getTimestamp("start_dt"));
        pi.setEndDate(rs.getTimestamp("end_dt"));
        pi.setCompletionCode(rs.getString("compcode"));
        pi.setComment(rs.getString("comments"));
        pi.setTemplate(rs.getString("template"));
        // avoid loading into ProcessCache
        if (pi.getComment() != null) {
            AssetHeader assetHeader = new AssetHeader(pi.getComment());
            pi.setProcessName(assetHeader.getName());
            pi.setProcessVersion(assetHeader.getVersion());
            pi.setPackageName(assetHeader.getPackageName());
        }
        if (pi.getTemplate() != null) {
            AssetHeader templateHeader = new AssetHeader(pi.getTemplate());
            pi.setTemplate(templateHeader.getName());
            pi.setTemplatePackage(templateHeader.getPackageName());
            pi.setTemplateVersion(templateHeader.getVersion());
        }
        return pi;
    }

    protected static DateFormat getOracleDateFormat() {
        return new SimpleDateFormat("dd-MMM-yyyy");
    }

    public long getDatabaseTime() throws SQLException {
        return db.getDatabaseTime();
    }

    protected static DateFormat getMySqlDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd");
    }

    protected DateFormat getDateFormat() {
        return db.isMySQL() ? getMySqlDateFormat() : getOracleDateFormat();
    }

    protected static String getMySqlDt(Date date) {
        return new SimpleDateFormat(MYSQL_DT_FORMAT).format(date);
    }

    protected static String getOracleDt(Date date) {
        return new SimpleDateFormat(ORACLE_DT_FORMAT).format(date);
    }

    protected String getDt(Date date) {
        return db.isMySQL() ? getMySqlDt(date) : getOracleDt(date);
    }

    protected String getSelectDate(String column) {
        if (db.isOracle())
            return "select to_char(" + column + ",'DD-Mon-yyyy')";
        else
            return "select date(" + column + ")";
    }

    @SuppressWarnings("deprecation")
    protected static Date getRoundDate(Date date) {
        Date roundDate = new Date(date.getTime());
        roundDate.setHours(0);
        roundDate.setMinutes(0);
        roundDate.setSeconds(0);
        roundDate.setTime((roundDate.getTime() / 1000) * 1000);
        return roundDate;
    }
}
