/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.db;

import java.net.ConnectException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.XAConnection;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.TransactionUtil;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;

public class CommonDataAccess {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected DatabaseAccess db;
    private int databaseVersion;
    private int supportedVersion;


    public CommonDataAccess() {
        this(null, DataAccess.currentSchemaVersion, DataAccess.supportedSchemaVersion);
    }

    protected CommonDataAccess(DatabaseAccess db, int databaseVersion, int supportedVersion) {
        this.db = db == null ? new DatabaseAccess(null) : db;
        this.databaseVersion = databaseVersion;
        this.supportedVersion = supportedVersion;
    }

    public int getDatabaseVersion() {
        return databaseVersion;
    }

    public int getSupportedVersion() {
        return supportedVersion;
    }

    /**
     * Should only be used with MDW data source
     * @return
     * @throws DataAccessException
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
                db.openConnection();// Get connection BEFORE beginning transaction to avoid transaction timeout (10 minutes) exceptions (Fail to stop the transaction)
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
                        db.openConnection();
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
     * @throws DataAccessException
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

    protected String dateConditionToMySQL(String value) {
        value = value.replaceAll("to_date", "str_to_date");
        value = value.replaceAll("mm/dd/yyyy hh24:mi:ss", "%m/%d/%Y %H:%i:%s");
        value = value.replaceAll("mm/dd/yyyy", "%m/%d/%Y");
        value = value.replaceAll("MON DD YYYY", "%M %D %Y");
        return value;
    }

    protected List<AttributeVO> getAttributes0(String ownerType, Long ownerId)
    throws SQLException {
        String query = "select ATTRIBUTE_ID, ATTRIBUTE_NAME, ATTRIBUTE_VALUE from ATTRIBUTE " +
                "where ATTRIBUTE_OWNER_ID=? and ATTRIBUTE_OWNER='" + ownerType + "'";
        ResultSet rs = db.runSelect(query, ownerId);
        List<AttributeVO> attribs = new ArrayList<AttributeVO>();
        while (rs.next()) {
            AttributeVO vo = new AttributeVO(rs.getString(2), rs.getString(3));
            vo.setAttributeId(new Long(rs.getLong(1)));
            attribs.add(vo);
        }
        return attribs;
    }

    /**
     * Same as getAttribute1 but handles overflow values
     * @param ownerType
     * @param ownerId
     * @return
     * @throws SQLException
     */
    protected List<AttributeVO> getAttributes1(String ownerType, Long ownerId)
    throws SQLException {
        List<AttributeVO> attrs = getAttributes0(ownerType, ownerId);
        if (attrs==null) return null;
        ResultSet rs;
        String query = "select RULE_SET_DETAILS from RULE_SET where RULE_SET_ID=?";
        for (AttributeVO attr : attrs) {
            String v = attr.getAttributeValue();
            if (v!=null && v.startsWith(RuleSetVO.ATTRIBUTE_OVERFLOW)) {
                Long rulesetId = new Long(v.substring(RuleSetVO.ATTRIBUTE_OVERFLOW.length()+1));
                rs = db.runSelect(query, rulesetId);
                if (rs.next()) {
                    attr.setAttributeValue(rs.getString(1));
                }
            }
        }
        return attrs;
    }

    protected AttributeVO getAttribute0(String ownerType, Long ownerId, String attrname)
        throws SQLException
    {
        db.openConnection();
        String query = "select ATTRIBUTE_ID, ATTRIBUTE_VALUE from ATTRIBUTE " +
                "where ATTRIBUTE_OWNER=? and ATTRIBUTE_OWNER_ID=? and ATTRIBUTE_NAME=?";
        Object[] args = new Object[3];
        args[0] = ownerType;
        args[1] = ownerId;
        args[2] = attrname;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) {
            AttributeVO vo = new AttributeVO(attrname, rs.getString(2));
            vo.setAttributeId(new Long(rs.getLong(1)));
            return vo;
        } else return null;
    }

    public void setAttributes0(String ownerType, Long ownerId, Map<String,String> attributes)
    throws SQLException {
        List<AttributeVO> attrs = null;
        if (attributes != null && !attributes.isEmpty()) {
            attrs = new ArrayList<AttributeVO>();
            for (String name : attributes.keySet()) {
                String value = attributes.get(name);
                if (value != null && !value.isEmpty())
                    attrs.add(new AttributeVO(name, value));
            }
        }
        deleteAttributes0(ownerType, ownerId);
        if (attrs != null)
          addAttributes0(ownerType, ownerId, attrs);
    }

    protected Long setAttribute0(String ownerType, Long ownerId,
            String attrname, String attrvalue)
            throws SQLException {
        String query = "select ATTRIBUTE_ID from ATTRIBUTE where " +
                    "ATTRIBUTE_OWNER=? and ATTRIBUTE_OWNER_ID=? and ATTRIBUTE_NAME=?";
        Object[] args = new Object[3];
        args[0] = ownerType;
        args[1] = ownerId;
        args[2] = attrname;
        ResultSet rs = db.runSelect(query, args);
        Long existingId = null;
        if (rs.next()) {
            Long attrid = rs.getLong(1);
            if (attrvalue==null) {
                query = "delete ATTRIBUTE where " +
                "ATTRIBUTE_OWNER=? and ATTRIBUTE_OWNER_ID=? and ATTRIBUTE_NAME=?";
            } else {
                query = "update ATTRIBUTE set ATTRIBUTE_VALUE=? where ATTRIBUTE_ID=?";
                args = new Object[2];
                args[0] = attrvalue;
                args[1] = attrid;
            }
            existingId = attrid;
        } else {
            query = "insert into ATTRIBUTE" +
                " (ATTRIBUTE_ID,ATTRIBUTE_OWNER,ATTRIBUTE_OWNER_ID,ATTRIBUTE_NAME,ATTRIBUTE_VALUE," +
                        "CREATE_DT,CREATE_USR)"
                + " values ("
                + (db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL") + ",?,?,?,?," + now() + ",'MDWEngine')";
            args = new Object[4];
            args[0] = ownerType;
            args[1] = ownerId;
            args[2] = attrname;
            args[3] = attrvalue;
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
        String query = "delete from value " +
            " where OWNER_TYPE = '" + ownerType + "' and OWNER_ID = ?";
        db.runUpdate(query, ownerId);
    }

    protected void deleteOverflowAttributes(String attrPrefix)
    throws SQLException {
        String query = "delete from RULE_SET where RULE_SET_NAME like ?";
        db.runUpdate(query, attrPrefix);
    }

    protected void addAttributes0(String pOwner, Long pOwnerId, List<AttributeVO> pAttributes)
    throws SQLException {
        String query = "insert into ATTRIBUTE"
            + " (attribute_id,attribute_owner,attribute_owner_id,attribute_name,attribute_value,"
            + " create_dt,create_usr) values ("
            + (db.isMySQL()?"null":"MDW_COMMON_ID_SEQ.NEXTVAL")
            + ",?,?,?,?,"+now()+",'MDWEngine')";
        db.prepareStatement(query);
        Object[] args = new Object[4];
        for (AttributeVO vo : pAttributes) {
            String v = vo.getAttributeValue();
            if (v==null||v.length()==0) continue;
            args[0] = pOwner;
            args[1] = pOwnerId;
            args[2] = vo.getAttributeName();
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
    public DocumentVO getDocument(Long documentId) throws DataAccessException {
        try {
            db.openConnection();
            return this.getDocument(documentId, false);
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to load document: " + documentId, ex);
        } finally {
            db.closeConnection();
        }
    }

    public DocumentVO getDocument(Long documentId, boolean forUpdate) throws SQLException {
        return loadDocument(documentId, forUpdate);
    }

    public DocumentVO loadDocument(Long documentId, boolean forUpdate)
            throws SQLException {
        String query = "select PROCESS_INST_ID, CREATE_DT, MODIFY_DT, DOCUMENT_TYPE, " +
            "SEARCH_KEY1, SEARCH_KEY2, OWNER_TYPE, OWNER_ID, CONTENT " +
            "from DOCUMENT where DOCUMENT_ID=?" + (forUpdate?" for update":"");
        ResultSet rs = db.runSelect(query, documentId);
        if (rs.next()) {
            DocumentVO vo = new DocumentVO();
            vo.setDocumentId(documentId);
            vo.setProcessInstanceId(rs.getLong(1));
            vo.setCreateDate(rs.getTimestamp(2));
            vo.setModifyDate(rs.getTimestamp(3));
            vo.setDocumentType(rs.getString(4));
            vo.setSearchKey1(rs.getString(5));
            vo.setSearchKey2(rs.getString(6));
            vo.setOwnerType(rs.getString(7));
            vo.setOwnerId(rs.getLong(8));
            vo.setContent(rs.getString(9));
            return vo;
        } else throw new SQLException("Document with ID " + documentId + " does not exist");
    }

    public ProcessVO getProcessBase0(String processName, int version)
            throws SQLException,DataAccessException {
        String query;
        if (version>0) {
            query = "select RULE_SET_ID, COMMENTS, VERSION_NO, MOD_DT, MOD_USR" +
                " from RULE_SET where RULE_SET_NAME=? and LANGUAGE='"+RuleSetVO.PROCESS+"' and VERSION_NO=" + version;
        } else {
            query = "select RULE_SET_ID, COMMENTS, VERSION_NO, MOD_DT, MOD_USR" +
                " from RULE_SET where RULE_SET_NAME=? and LANGUAGE='"+RuleSetVO.PROCESS+"' order by VERSION_NO desc";
        }
        ResultSet rs = db.runSelect(query, processName);
        String processComment;
        Long processId;
        if (rs.next()) {
            processId = new Long(rs.getLong(1));
            processComment = rs.getString(2);
            version = rs.getInt(3);
        } else throw new DataAccessException("Process does not exist; name=" + processName);
        ProcessVO retVO= new ProcessVO(processId, processName, processComment, null);   // external events - load later
        retVO.setVersion(version);
        retVO.setModifyDate(rs.getTimestamp(4));
        retVO.setModifyingUser(rs.getString(5));
        retVO.setInRuleSet(true);
        return retVO;
    }

    public List<DocumentVO> findDocuments0(Long procInstId, String type, String searchKey1, String searchKey2,
            String ownerType, Long ownerId, Date createDateStart, Date createDateEnd, String orderByClause)
            throws SQLException {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                List<DocumentVO> documentVOs = new ArrayList<DocumentVO>();
                StringBuffer query = new StringBuffer();
                List<Object> arglist = new ArrayList<Object>();
                boolean first = true;
                query.append("select DOCUMENT_ID, PROCESS_INST_ID, CREATE_DT, MODIFY_DT, DOCUMENT_TYPE, " +
                    "SEARCH_KEY1, SEARCH_KEY2, OWNER_TYPE, OWNER_ID from DOCUMENT where ");
                if (procInstId != null) {
                    first = false;
                    query.append("PROCESS_INST_ID=?");
                    arglist.add(procInstId);
                }
                if (type != null) {
                    if (first)
                        first = false;
                    else
                        query.append(" and ");
                    query.append("DOCUMENT_TYPE=?");
                    arglist.add(type);
                }
                if (searchKey1 != null) {
                    if (first)
                        first = false;
                    else
                        query.append(" and ");
                    query.append("SEARCH_KEY1=?");
                    arglist.add(searchKey1);
                }
                if (searchKey2 != null) {
                    if (first)
                        first = false;
                    else
                        query.append(" and ");
                    query.append("SEARCH_KEY2=?");
                    arglist.add(searchKey2);
                }
                if (ownerType != null) {
                    if (first)
                        first = false;
                    else
                        query.append(" and ");
                    query.append("OWNER_TYPE=?");
                    arglist.add(ownerType);
                }
                if (ownerId != null) {
                    if (first)
                        first = false;
                    else
                        query.append(" and ");
                    query.append("OWNER_ID=?");
                    arglist.add(ownerId);
                }
                if (createDateStart != null) {
                    if (first)
                        first = false;
                    else
                        query.append(" and ");
                    if (db.isMySQL())
                        query.append("CREATE_DT >= str_to_date('" + dateFormat.format(createDateStart) + "', '%m/%d/%Y')");
                    else query.append("CREATE_DT >= to_date('" + dateFormat.format(createDateStart) + "', 'MM/DD/YYYY')");
                }
                if (createDateEnd != null) {
                    if (first)
                        first = false;
                    else
                        query.append(" and ");
                    if (db.isMySQL())
                        query.append("CREATE_DT <= str_to_date('" + dateFormat.format(createDateEnd) + "', '%m/%d/%Y')");
                    else query.append("CREATE_DT <= to_date('" + dateFormat.format(createDateEnd) + "', 'MM/DD/YYYY')");
                }
                if (orderByClause != null)
                    query.append(" ").append(orderByClause);
                ResultSet rs = db.runSelect(query.toString(), arglist.toArray());
                while (rs.next()) {
                    DocumentVO vo = new DocumentVO();
                    vo.setDocumentId(rs.getLong(1));
                    vo.setProcessInstanceId(rs.getLong(2));
                    vo.setCreateDate(rs.getTimestamp(3));
                    vo.setModifyDate(rs.getTimestamp(4));
                    vo.setDocumentType(rs.getString(5));
                    vo.setSearchKey1(rs.getString(6));
                    vo.setSearchKey2(rs.getString(7));
                    vo.setOwnerType(rs.getString(8));
                    vo.setOwnerId(rs.getLong(9));
                    documentVOs.add(vo);
                }
                return documentVOs;

        }

    protected int countRows(String tableName, String keyElement, String whereCondition)
    throws SQLException {
        StringBuffer buff = new StringBuffer();

        buff.append("select count(").append(keyElement).append(") from ").append(tableName);
        if (whereCondition!=null) buff.append(" where ").append(whereCondition);
        String query = buff.toString();
        ResultSet rs = db.runSelect(query, null);
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
        ResultSet rs = db.runSelect(query, null);
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
        String query = "select " + sequenceName + ".NEXTVAL from dual";
        ResultSet rs = db.runSelect(query, null);
        rs.next();
        return new Long(rs.getString(1));
    }

    private DataAccessOfflineException dbOfflineException;
    public DataAccessOfflineException getDataAccessOfflineException() { return dbOfflineException; }

    private Boolean dbOnline;
    public boolean isOnline() throws DataAccessException {
        if (dbOnline == null) {
            try {
                db.openConnection();
                db.runSelect(db.isMySQL() ? "select now()" : "select sysdate from dual", null).next();
                dbOnline = true;
            }
            catch (SQLException ex) {
                // avoid vendor dependencies
                if ( (db.isMySQL() && ex.getCause() instanceof ConnectException)
                        || (db.isOracle() && ex.getCause() != null && "oracle.net.ns.NetException".equals(ex.getCause().getClass().getName())) ) {
                    dbOnline = false;
                    dbOfflineException = new DataAccessOfflineException("Database unavailable: " + db, ex);
                }
                else {
                    throw new DataAccessException(ex.getMessage(), ex);
                }
            }
            finally {
                db.closeConnection();
            }
        }
        return dbOnline;
    }

    public List<String> getValueNames(String ownerType) throws DataAccessException {
        try {
            List<String> names = new ArrayList<String>();
            db.openConnection();
            String sql = "select distinct name from value where owner_type = ?";
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
        String query = "select value from value where owner_type = ? and ownerId = ? and name = ?";
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
        String query = "select name, value from value where owner_type = ? and owner_id = ?";
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
        String query = "select name from value where " +
                    "OWNER_type=? and OWNER_ID=? and NAME=?";
        Object[] args = new Object[3];
        args[0] = ownerType;
        args[1] = ownerId;
        args[2] = name;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) {
            if (value==null) {
                query = "delete value where " +
                "OWNER_TYPE=? and OWNER_ID=? and NAME=?";
            } else {
                query = "update value set VALUE=? where OWNER_type=? and OWNER_ID=? and NAME=?";
                args = new Object[4];
                args[0] = value;
                args[1] = ownerType;
                args[2] = ownerId;
                args[3] = name;
            }
        } else {
            query = "insert into value" +
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
        String query = "insert into value"
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
                q = "select owner_id from value where name = ?";
                args = new Object[]{valueName};
            }
            else {
                if (valuePattern.contains("*")) {
                    q = "select owner_id from value where name = ? and value like '" + valuePattern.replace('*', '%') + "'";
                    args = new Object[]{valueName};
                }
                else {
                    q = "select owner_id from value where name = ? and value = ?";
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
                q = "select owner_id from value where owner_type = ? and name = ?";
                args = new Object[]{ownerType, valueName};
            }
            else {
                if (valuePattern.contains("*")) {
                    q = "select owner_id from value where owner_type = ? and name = ? and value like '" + valuePattern.replace('*', '%') + "'";
                    args = new Object[]{ownerType, valueName};
                }
                else {
                    q = "select owner_id from value where owner_type = ? and name = ? and value = ?";
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

}
