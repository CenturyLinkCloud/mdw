/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.version5;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.SchemaTypeTranslator;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.ProcessExporter;
import com.centurylink.mdw.dataaccess.ProcessImporter;
import com.centurylink.mdw.dataaccess.version4.ProcessLoaderPersisterV4;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.LaneVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.PoolVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.model.data.task.TaskCategory;

/**
 * Process database loader/persister for MDW 5.* database
 */
public class ProcessLoaderPersisterV5 extends ProcessLoaderPersisterV4 {

    public ProcessLoaderPersisterV5(DatabaseAccess db, int databaseVersion, int supportedVersion, SchemaTypeTranslator schemaTypeTranslator) {
        super(db, databaseVersion, supportedVersion, schemaTypeTranslator);
    }

    /**
     * Expose this for task manager; may retire the task manager function later
     */
    public void createVariableMapping(Long ownerId, String ownerType, Long variableId, VariableVO vo)
            throws SQLException {
        super.createVariableMapping(ownerId, ownerType, variableId, vo);
    }

    /**
     * Expose this for task manager; may retire the task manager function later
     */
    public void updateVariableMapping(String ownerType, Long ownerId,
            Long oldVarId, VariableVO var) throws SQLException {
        super.updateVariableMapping(ownerType, ownerId, oldVarId, var);
    }

    /**
     * Expose this for task manager; may retire the task manager function later
     */
    public List<VariableVO> getVariablesForTask(Long taskId)
    throws SQLException {
        return super.getVariablesForOwner(OwnerType.TASK, taskId);
    }

    protected void persistPools(PackageVO packageVO) throws SQLException {
        String query;

        super.deletePools(packageVO.getPackageId());
        if (packageVO.getPools()==null) return;

        Object[] args = new Object[3];
        args[1] = packageVO.getPackageId();
        query = "insert into POOL "
            + "(POOL_ID, PACKAGE_ID, POOL_NAME) values "
            + "(?, ?, ?)";
        for (PoolVO pool :  packageVO.getPools()) {
            Long id = this.getNextId("MDW_COMMON_ID_SEQ");
            pool.setPoolId(id);
            args[0] = id;
            args[2] = pool.getPoolName();
            db.runUpdate(query, args);
        }

        query = "insert into LANE "
            + "(LANE_ID, POOL_ID, LANE_NAME) values "
            + "(?, ?, ?)";
        for (PoolVO pool :  packageVO.getPools()) {
            List<LaneVO> lanes = pool.getLanes();
            if (lanes==null) continue;
            for (LaneVO lane : lanes) {
                Long id = this.getNextId("MDW_COMMON_ID_SEQ");
                args[0] = id;
                args[1] = pool.getPoolId();
                args[2] = lane.getLaneName();
                lane.setLaneId(id);
                db.runUpdate(query, args);
            }
        }

        for (PoolVO pool :  packageVO.getPools()) {
            List<LaneVO> lanes = pool.getLanes();
            if (lanes==null) continue;
            for (LaneVO lane : lanes) {
                super.addAttributes(lane.getAttributes(), lane.getLaneId(), OwnerType.LANE, null);
            }
        }

    }

    @Override
    public List<RuleSetVO> getRuleSets() throws DataAccessException {
        try {
            db.openConnection();
            String query;
            if (db.isMySQL()) {
            	query = "select RULE_SET_ID,RULE_SET_NAME,LANGUAGE,rs.CREATE_USR,rs.CREATE_DT,rs.MOD_USR,rs.MOD_DT,rs.COMMENTS,VERSION_NO,"
                    + "ATTRIBUTE_ID,ATTRIBUTE_NAME,ATTRIBUTE_VALUE"
                	+ (getDatabaseVersion() >= DataAccess.schemaVersion55?",OWNER_ID,OWNER_TYPE\n":"\n")
                    + "from RULE_SET rs left join ATTRIBUTE attr \n"
                    + "on ( RULE_SET_ID = ATTRIBUTE_OWNER_ID and 'RULE_SET' = ATTRIBUTE_OWNER ) "
                    + "where LANGUAGE != ? "
                    + "order by lower(RULE_SET_NAME), VERSION_NO desc";
            } else {
            	query = "select RULE_SET_ID,RULE_SET_NAME,LANGUAGE,rs.CREATE_USR,rs.CREATE_DT,rs.MOD_USR,rs.MOD_DT,rs.COMMENTS,VERSION_NO,"
                + "ATTRIBUTE_ID,ATTRIBUTE_NAME,ATTRIBUTE_VALUE"
            	+ (getDatabaseVersion() >= DataAccess.schemaVersion55?",OWNER_ID,OWNER_TYPE\n":"\n")
                + "from RULE_SET rs, ATTRIBUTE attr\n"
                + "where RULE_SET_ID = ATTRIBUTE_OWNER_ID (+) and 'RULE_SET' = ATTRIBUTE_OWNER (+)"
                + " and LANGUAGE != ? "
                + "order by lower(RULE_SET_NAME), VERSION_NO desc";
            }
            ResultSet rs = db.runSelect(query, RuleSetVO.ATTRIBUTE_OVERFLOW);
            Map<Long,RuleSetVO> unique = new HashMap<Long,RuleSetVO>();
            List<RuleSetVO> ret = new ArrayList<RuleSetVO>();
            while (rs.next()) {
                Long rsId = rs.getLong(1);
                RuleSetVO vo = unique.get(rsId);
                if (vo == null) {
                    vo = new RuleSetVO();
                    vo.setId(rsId);
                    vo.setName(rs.getString(2));
                    vo.setLanguage(rs.getString(3));
                    vo.setCreateUser(rs.getString(4));
                    vo.setCreateDate(rs.getTimestamp(5));
                    vo.setModifyingUser(rs.getString(6));
                    vo.setModifyDate(rs.getTimestamp(7));
                    if (vo.getLanguage() == null)
                        vo.setLanguage(rs.getString(8));
                    vo.setComment(rs.getString(8));
                    vo.setVersion(rs.getInt(9));
                    vo.setRuleSet(null);
                    if (getDatabaseVersion() >= DataAccess.schemaVersion55) {
                        vo.setOwnerId(rs.getLong(13));
                        vo.setOwnerType(rs.getString(14));
                    }
                    ret.add(vo);
                    unique.put(rsId, vo);
                }

                Long attrId = rs.getLong(10);
                if (attrId != null) {
                    AttributeVO attrVO = new AttributeVO();
                    attrVO.setAttributeId(attrId);
                    attrVO.setAttributeName(rs.getString(11));
                    attrVO.setAttributeValue(rs.getString(12));
                    if (vo.getAttributes() == null)
                        vo.setAttributes(new ArrayList<AttributeVO>());
                    vo.getAttributes().add(attrVO);
                }
            }
            return ret;
        } catch (SQLException e) {
            throw new DataAccessException(0,"failed to load resource list", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<PackageVO> getPackageList(boolean deep, ProgressMonitor progressMonitor) throws DataAccessException {
        List<PackageVO> packageList = super.getPackageList(deep, progressMonitor);

        try {
            db.openConnection();

            // rule-sets
            if (progressMonitor != null)
                progressMonitor.subTask("Loading package assets");
            String query = "select prs.PACKAGE_ID, rs.RULE_SET_ID, rs.RULE_SET_NAME, rs.LANGUAGE,"
            	+ " rs.VERSION_NO, rs.COMMENTS, rs.CREATE_USR, rs.CREATE_DT, rs.MOD_DT, rs.MOD_USR\n"
                + "from PACKAGE_RULESETS prs, RULE_SET rs\n"
                + "where prs.RULE_SET_ID = rs.RULE_SET_ID";
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
            	String language = rs.getString(4);
                Long packageId = rs.getLong(1);
                Long ruleSetId = rs.getLong(2);
                if (RuleSetVO.PROCESS.equals(language)) {
                	if (getSupportedVersion()>=DataAccess.schemaVersion52) {
	                    Long processId = ruleSetId;
	                    String processName = rs.getString(3);
	                    String comment = rs.getString(6);
	                    ProcessVO procVO = new ProcessVO(processId, processName, comment, null);
	                    procVO.setVersion(rs.getInt(5));
	                    procVO.setCreateUser(rs.getString(7));
	                    procVO.setCreateDate(rs.getTimestamp(8));
	                    procVO.setModifyDate(rs.getTimestamp(9));
	                    procVO.setModifyingUser(rs.getString(10));
	                    for (PackageVO packageVO : packageList) {
		                    if (packageVO.getPackageId().equals(packageId)) {
		                    	packageVO.getProcesses().add(procVO);
		                    	break;
		                    }
	                    }
                	}
                } else {
	                RuleSetVO rsVO = new RuleSetVO();
	                rsVO.setId(ruleSetId);
	                rsVO.setName(rs.getString(3));
	                rsVO.setLanguage(language);
	                rsVO.setVersion(rs.getInt(5));
	                for (PackageVO packageVO : packageList) {
	                    if (packageVO.getPackageId().equals(packageId))
	                        packageVO.getRuleSets().add(rsVO);
	                }
                }
            }
            if (progressMonitor != null)
                progressMonitor.progress(10);

        }
        catch (SQLException e) {
            throw new DataAccessException(0, "failed to load package list", e);
        }
        finally {
            db.closeConnection();
        }

        return packageList;
    }

    @Override
    protected RuleSetVO getRuleSet0(Long id) throws SQLException {
    	String query = "select RULE_SET_NAME,RULE_SET_DETAILS,LANGUAGE,CREATE_USR,CREATE_DT,MOD_USR,MOD_DT,COMMENTS,VERSION_NO" +
    		(getDatabaseVersion()>=DataAccess.schemaVersion55?",OWNER_ID,OWNER_TYPE":"") +
    		" from RULE_SET " +
    		" where RULE_SET_ID=?";
    	ResultSet rs = db.runSelect(query, id.toString());
    	if (rs.next()) {
    		RuleSetVO ruleset = new RuleSetVO();
    		ruleset.setId(id);
    		ruleset.setName(rs.getString(1));
    		ruleset.setRuleSet(rs.getString(2));
    		ruleset.setLanguage(rs.getString(3));
    		ruleset.setCreateUser(rs.getString(4));
    		ruleset.setCreateDate(rs.getTimestamp(5));
    		ruleset.setModifyingUser(rs.getString(6));
    		ruleset.setModifyDate(rs.getTimestamp(7));
    		ruleset.setComment(rs.getString(8));
    		if (ruleset.getLanguage()==null) ruleset.setLanguage(ruleset.getComment());
    		ruleset.setVersion(rs.getInt(9));
    		if (getDatabaseVersion()>=DataAccess.schemaVersion55) {
    		    ruleset.setOwnerId(rs.getLong(10));
    		    ruleset.setOwnerType(rs.getString(11));
    		}
    		ruleset.setAttributes(getAttributes0("RULE_SET", ruleset.getId()));
    		ruleset.setLoadDate(new Date());
    		return ruleset;
    	} else {
    		return null;
    	}
    }

    public RuleSetVO getRuleSetForOwner(String ownerType, Long ownerId) throws DataAccessException {
        try {
            db.openConnection();
            return getRuleSetForOwner0(ownerType, ownerId);
        }
        catch (SQLException ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public RuleSetVO getRuleSetForOwner0(String ownerType, Long ownerId) throws SQLException {

        StringBuffer query = new StringBuffer();
        ResultSet rs = null;
        int rowCount = 0;

        if (getDatabaseVersion() >= DataAccess.schemaVersion55) {
            query.append("select RULE_SET_ID,RULE_SET_NAME,RULE_SET_DETAILS,LANGUAGE,CREATE_USR,CREATE_DT,MOD_USR,MOD_DT,COMMENTS,VERSION_NO");
            query.append(",OWNER_ID,OWNER_TYPE from RULE_SET where OWNER_ID=? and OWNER_TYPE = '").append(ownerType).append("'") ;
            rs = db.runSelect(query.toString(), ownerId.toString());
            if (rs.next()) ++rowCount;
        }

        // backward compatibility
        if (rowCount == 0 && getSupportedVersion() < DataAccess.schemaVersion55 && !db.isMySQL()) {
            query = new StringBuffer();
            query.append("select RULE_SET_ID,RULE_SET_NAME,RULE_SET_DETAILS,LANGUAGE,CREATE_USR,CREATE_DT,MOD_USR,MOD_DT,COMMENTS,VERSION_NO");
            if (getDatabaseVersion() >= DataAccess.schemaVersion55)  query.append(",OWNER_ID,OWNER_TYPE");
            query.append(" from RULE_SET where RULE_SET_ID=? and LANGUAGE = '").append(RuleSetVO.CONFIG).append("'");
            rs = db.runSelect(query.toString(), ownerId.toString());
            if (rs.next()) ++rowCount;
        }

        if (rowCount > 0) {
            RuleSetVO ruleset = new RuleSetVO();
            ruleset.setId(rs.getLong(1));
            ruleset.setName(rs.getString(2));
            ruleset.setRuleSet(rs.getString(3));
            ruleset.setLanguage(rs.getString(4));
            ruleset.setCreateUser(rs.getString(5));
            ruleset.setCreateDate(rs.getTimestamp(6));
            ruleset.setModifyingUser(rs.getString(7));
            ruleset.setModifyDate(rs.getTimestamp(8));
            ruleset.setComment(rs.getString(9));
            if (ruleset.getLanguage()==null)
                ruleset.setLanguage(ruleset.getComment());
            ruleset.setVersion(rs.getInt(10));
            if (getDatabaseVersion() >= DataAccess.schemaVersion55) {
                ruleset.setOwnerType(rs.getString("OWNER_TYPE"));
                ruleset.setOwnerId(rs.getLong("OWNER_ID"));
            }
            ruleset.setAttributes(getAttributes0("RULE_SET", ruleset.getId()));
            ruleset.setLoadDate(new Date());
            return ruleset;
        } else {
            return null;
        }

    }

    @Override
    protected RuleSetVO getRuleSet0(String name, String language, int version)
    		throws SQLException {
    	String query;
    	Object[] args;
    	if (language==null) {
    		query = "select RULE_SET_ID,RULE_SET_DETAILS,LANGUAGE,CREATE_USR,CREATE_DT,MOD_USR,MOD_DT,COMMENTS,VERSION_NO" +
                    " from RULE_SET " +
                    " where RULE_SET_NAME=? order by VERSION_NO desc";
    		args = new Object[1];
    		args[0] = name;
    	} else {
    		query = "select RULE_SET_ID,RULE_SET_DETAILS,LANGUAGE,CREATE_USR,CREATE_DT,MOD_USR,MOD_DT,COMMENTS,VERSION_NO" +
            		" from RULE_SET " +
            		" where RULE_SET_NAME=? and LANGUAGE=? order by VERSION_NO desc";
    		args = new Object[2];
    		args[0] = name;
    		args[1] = language;
    	}
    	ResultSet rs = db.runSelect(query, args);
    	while (rs.next()) {
    		int ver = rs.getInt(9);
    		if (version!=0 && version!=ver) continue;
    		RuleSetVO ruleset = new RuleSetVO();
    		ruleset.setId(rs.getLong(1));
    		ruleset.setName(name);
    		ruleset.setRuleSet(rs.getString(2));
    		ruleset.setLanguage(rs.getString(3));
    		ruleset.setCreateUser(rs.getString(4));
    		ruleset.setCreateDate(rs.getTimestamp(5));
    		ruleset.setModifyingUser(rs.getString(6));
    		ruleset.setModifyDate(rs.getTimestamp(7));
    		ruleset.setComment(rs.getString(8));
    		if (ruleset.getLanguage()==null) ruleset.setLanguage(ruleset.getComment());
    		ruleset.setVersion(ver);
    		ruleset.setAttributes(getAttributes0("RULE_SET", ruleset.getId()));
    		ruleset.setLoadDate(new Date());
    		return ruleset;
    	}
    	return null;
    }

    public RuleSetVO getRuleSet(String name, String language, int version) throws DataAccessException {
        try {
        	db.openConnection();
            return getRuleSet0(name, language, version);
        } catch (SQLException e) {
            throw new DataAccessException(0,"failed to load resource", e);
        } finally {
            db.closeConnection();
        }
    }

    public RuleSetVO getRuleSet(Long packageId, String name) throws DataAccessException {
        try {
            db.openConnection();
            return getRuleSet0(packageId, name);
        } catch (SQLException e) {
            throw new DataAccessException(0,"failed to load resource", e);
        } finally {
            db.closeConnection();
        }
    }

    protected RuleSetVO getRuleSet0(Long packageId, String name) throws SQLException {
        String query;
        Object[] args;
        query = "select rs.RULE_SET_ID,rs.RULE_SET_DETAILS,rs.LANGUAGE,rs.CREATE_USR,rs.CREATE_DT,rs.MOD_USR,rs.MOD_DT,rs.LANGUAGE,rs.COMMENTS,rs.VERSION_NO" +
                " from RULE_SET rs, PACKAGE_RULESETS pkg" +
                " where rs.RULE_SET_NAME=? and rs.RULE_SET_ID = pkg.RULE_SET_ID and pkg.PACKAGE_ID=?";
        args = new Object[2];
        args[0] = name;
        args[1] = packageId;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) {
            RuleSetVO ruleset = new RuleSetVO();
            ruleset.setId(rs.getLong(1));
            ruleset.setName(name);
            ruleset.setRuleSet(rs.getString(2));
            ruleset.setLanguage(rs.getString(3));
            ruleset.setCreateUser(rs.getString(4));
            ruleset.setCreateDate(rs.getTimestamp(5));
            ruleset.setModifyingUser(rs.getString(6));
            ruleset.setModifyDate(rs.getTimestamp(7));
            ruleset.setLanguage(rs.getString(8));
            ruleset.setComment(rs.getString(9));
            ruleset.setVersion(rs.getInt(10));
            ruleset.setAttributes(getAttributes0("RULE_SET", ruleset.getId()));
            ruleset.setLoadDate(new Date());
            return ruleset;
        }
        return null;
    }



    @Override
    protected Long createRuleSet0(RuleSetVO ruleset) throws SQLException {
    	String rules = ruleset.getRuleSet();
    	Long id = ruleset.getId();
    	String language = ruleset.getLanguage();
    	if (db.isMySQL()) id = null;
    	else if (id<=0) id = this.getNextId("MDW_COMMON_ID_SEQ");
    	if (this.getDatabaseVersion()>=DataAccess.schemaVersion52) {
    	    String createUser = ruleset.getCreateUser() == null ? "ProcessPersister" : ruleset.getCreateUser();

    	    StringBuffer insertQuery = new StringBuffer();
    	    insertQuery.append("insert into RULE_SET (RULE_SET_ID,RULE_SET_NAME,RULE_SET_DETAILS,LANGUAGE,CREATE_DT,CREATE_USR,MOD_USR,MOD_DT,REFRESH_INTERVAL,VERSION_NO,COMMENTS");

    	    if (this.getDatabaseVersion() >= DataAccess.schemaVersion55)
    	        insertQuery.append(",OWNER_ID,OWNER_TYPE");
    	    insertQuery.append(") values (?,?,?,?,"+now()+",?,?,"+now()+",?,?,?");
    	    if (this.getDatabaseVersion() >= DataAccess.schemaVersion55)
    	        insertQuery.append(",?,?");
    	    insertQuery.append(")");

    		Object[] args;
    		if (this.getDatabaseVersion() >= DataAccess.schemaVersion55)
    		    args = new Object[]{id, ruleset.getName(), rules, language, createUser,
                    ruleset.getModifyingUser(), 0, ruleset.getVersion(), ruleset.getComment(),
                    ruleset.getOwnerId(),ruleset.getOwnerType()};
    		else
    		    args =   new Object[]{id, ruleset.getName(), rules, language, createUser,
    				ruleset.getModifyingUser(), 0, ruleset.getVersion(), ruleset.getComment()};

    		if (db.isMySQL()) id = db.runInsertReturnId(insertQuery.toString(), args);
    		else db.runUpdate(insertQuery.toString(), args);
    		Date now = new Date(db.getDatabaseTime());
    		ruleset.setCreateDate(now);
    		ruleset.setModifyDate(now);
    	} else {
	    	String query = "insert into RULE_SET " +
	    				" (RULE_SET_ID,RULE_SET_NAME,RULE_SET_DETAILS,LANGUAGE,MOD_USR,MOD_DT," +
	    				"  REFRESH_INTERVAL,VERSION_NO,COMMENTS)" +
	    				" values (?,?,?,?,?,sysdate,?,?,?)";
	    	db.runUpdate(query, new Object[]{id, ruleset.getName(), rules, language,
	    				ruleset.getModifyingUser(), 0, ruleset.getVersion(), ruleset.getComment()});
    	}
    	ruleset.setId(id);
    	persistAttributes("RULE_SET", id, ruleset.getAttributes(), null);
    	return id;
    }

    @Override
    protected void updateRuleSet0(RuleSetVO ruleset) throws SQLException {
    	String rules = ruleset.getRuleSet();
    	Long id = ruleset.getId();
    	String language = ruleset.getLanguage();
    	if (this.getDatabaseVersion() >= DataAccess.schemaVersion55) {
            String query = "update RULE_SET set RULE_SET_NAME=?, RULE_SET_DETAILS=?," +
                    " LANGUAGE=?, MOD_USR=?, MOD_DT=" + now() + ", REFRESH_INTERVAL=?, VERSION_NO=?, COMMENTS=?," +
                    " OWNER_ID=?, OWNER_TYPE=? where RULE_SET_ID=?";
            db.runUpdate(query, new Object[]{ruleset.getName(), rules, language,
                        ruleset.getModifyingUser(), 0, ruleset.getVersion(), ruleset.getComment(), ruleset.getOwnerId(), ruleset.getOwnerType(), id});
            ruleset.setModifyDate(new Date(db.getDatabaseTime()));
    	} else if (this.getDatabaseVersion()>=DataAccess.schemaVersion52) {
	    	String query = "update RULE_SET set RULE_SET_NAME=?, RULE_SET_DETAILS=?," +
	    			" LANGUAGE=?, MOD_USR=?, MOD_DT=" + now() + ", REFRESH_INTERVAL=?, VERSION_NO=?, COMMENTS=?" +
	    			" where RULE_SET_ID=?";
	    	db.runUpdate(query, new Object[]{ruleset.getName(), rules, language,
	    				ruleset.getModifyingUser(), 0, ruleset.getVersion(), ruleset.getComment(), id});
	    	ruleset.setModifyDate(new Date(db.getDatabaseTime()));
    	} else {
	    	String query = "update RULE_SET set RULE_SET_NAME=?, RULE_SET_DETAILS=?," +
	    			" LANGUAGE=?, MOD_USR=?, MOD_DT=sysdate, REFRESH_INTERVAL=?, VERSION_NO=?, COMMENTS=?" +
	    			" where RULE_SET_ID=?";
	    	db.runUpdate(query, new Object[]{ruleset.getName(), rules, language,
	    				ruleset.getModifyingUser(), 0, ruleset.getVersion(), ruleset.getComment(), id});
    	}
    	persistAttributes("RULE_SET", id, ruleset.getAttributes(), null);
    }

    @Override
    protected void loadScriptsForPackage(PackageVO packageVO, boolean withContent)
    		throws SQLException, DataAccessException {
        String query;
        if (getDatabaseVersion()>=DataAccess.schemaVersion52) {
        	query = "select rs.RULE_SET_ID,rs.RULE_SET_NAME,rs.LANGUAGE,rs.VERSION_NO," +
        		"rs.CREATE_USR,rs.CREATE_DT,rs.MOD_USR,rs.MOD_DT,rs.COMMENTS" +
    			(withContent?",rs.RULE_SET_DETAILS":"") +
    			" from RULE_SET rs, PACKAGE_RULESETS pr " +
    			" where pr.PACKAGE_ID=? and pr.RULE_SET_ID=rs.RULE_SET_ID";
        } else {
        	query = "select rs.RULE_SET_ID,rs.RULE_SET_NAME,rs.LANGUAGE,rs.VERSION_NO" +
        		(withContent?",rs.RULE_SET_DETAILS":"") +
            	" from RULE_SET rs, PACKAGE_RULESETS pr " +
            	" where pr.PACKAGE_ID=? and pr.RULE_SET_ID=rs.RULE_SET_ID";
        }
        ResultSet rs = db.runSelect(query, packageVO.getPackageId());
        List<RuleSetVO> rulesets = new ArrayList<RuleSetVO>();
        List<RuleSetVO> ruleSetProcs = new ArrayList<RuleSetVO>();
        while (rs.next()) {
            RuleSetVO vo = new RuleSetVO();
            vo.setId(rs.getLong(1));
            vo.setName(rs.getString(2));
            vo.setLanguage(rs.getString(3));
            vo.setVersion(rs.getInt(4));
            if (getDatabaseVersion()>=DataAccess.schemaVersion52) {
                vo.setCreateUser(rs.getString(5));
                vo.setCreateDate(rs.getTimestamp(6));
            	vo.setModifyingUser(rs.getString(7));
            	vo.setModifyDate(rs.getTimestamp(8));
            	vo.setComment(rs.getString(9));
            	vo.setRuleSet(withContent?rs.getString(10):null);
            } else {
            	vo.setRuleSet(withContent?rs.getString(5):null);
            }
            if (RuleSetVO.PROCESS.equalsIgnoreCase(vo.getLanguage()))
                ruleSetProcs.add(vo);
            else
                rulesets.add(vo);
        }

        if (getSupportedVersion() >= DataAccess.schemaVersion52) { // else the processes are already loaded
            for (RuleSetVO ruleset : ruleSetProcs) {
                if (RuleSetVO.PROCESS.equalsIgnoreCase(ruleset.getLanguage())) {
                    List<ProcessVO> processes = packageVO.getProcesses();
                    ProcessVO processVO = new ProcessVO();
                    loadProcessBaseFromRuleSet(processVO, ruleset);
                    if (withContent) {
                        loadProcessContentFromRuleSet(processVO, ruleset.getRuleSet());
                        loadTaskDefinitions(processVO);
                    }
                    processes.add(processVO);
                }
            }
        }
        packageVO.setRuleSets(rulesets);
    }


    @Override
    protected void persistParticipants(PackageVO packageVO, PersistType persistType)
	throws SQLException,DataAccessException {
    	// TODO define participants table instead of using lane table, which does not
    	// match very well
    }

    @Override
    protected void persistScripts(PackageVO packageVO, PersistType persistType)
	throws SQLException,DataAccessException {
        if (packageVO.getRuleSets()==null) return;
        if (persistType==PersistType.IMPORT) {
        	for (RuleSetVO ei : packageVO.getRuleSets()) {
        		RuleSetVO existing = null;
        		if (getSupportedVersion() >= DataAccess.schemaVersion55) {
                    existing = getRuleSet0(packageVO.getId(), ei.getName());
                    if (existing != null && existing.getVersion() != ei.getVersion()) {
                        // remove the existing one from the package
                        String query = "delete from PACKAGE_RULESETS where RULE_SET_ID = ? AND PACKAGE_ID = ?\n";
                        db.runUpdate(query, new String[] {existing.getId().toString(), packageVO.getId().toString()});
                        existing = null; // let the new one be created in the package
                    }
        		}
                else
                    existing = getRuleSet0(ei.getName(), ei.getLanguage(), ei.getVersion());
                if (existing!=null) {
                    ei.setId(existing.getId());
                    ei.setVersion(existing.getVersion());
                    if (ei.getRuleSet()==null) ei.setRuleSet(existing.getRuleSet());
                    // above can happen when the script is existing and not overriding,
                    // and we still need to save the script for package.
                    this.updateRuleSet0(ei);    // update it
                } else {
                    ei.setId(-1L);
                    this.createRuleSet0(ei);
                }
        	}
        }
        if (packageVO.getVersion()>0 || persistType!=PersistType.IMPORT) {
            String query = "delete from PACKAGE_RULESETS where PACKAGE_ID=?";
            db.runUpdate(query, packageVO.getPackageId());
            query = "insert into PACKAGE_RULESETS "
                + "(PACKAGE_ID, RULE_SET_ID) values "
                + "(?, ?)";
            Object[] args = new Object[2];
            args[0] = packageVO.getPackageId();
            for (RuleSetVO ei : packageVO.getRuleSets()) {
            	if (!RuleSetVO.PROCESS.equals(ei.getLanguage())) {
            		args[1] = ei.getId();
            		db.runUpdate(query, args);
            	} // else handled in persistePackageProcessMapping
            }
        }
    }

    @Override
    protected void persistCustomAttributes(PackageVO packageVO)
    throws SQLException {
        if (packageVO.getCustomAttributes()==null) return;
        for (CustomAttributeVO customAttrVO : packageVO.getCustomAttributes()) {
          Long ownerId = customAttrVO.getOwnerId();
          setAttribute0(customAttrVO.getDefinitionAttrOwner(), ownerId, CustomAttributeVO.DEFINITION, customAttrVO.getDefinition());
          String rolesStr = customAttrVO.getRolesString();
          if (rolesStr != null)
            setAttribute0(customAttrVO.getRolesAttrOwner(), ownerId, CustomAttributeVO.ROLES, rolesStr);
        }
    }

    @Override
    public int deletePackage(Long packageId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from PACKAGE_RULESETS where PACKAGE_ID=?";
            int count = db.runUpdate(query, packageId);
            count += deletePackage0(packageId);
            query = "delete from RULE_SET where OWNER_ID=? AND OWNER_TYPE = '"+OwnerType.PACKAGE+"'";
            int deleteRuleSetCount = 0;
            if (getDatabaseVersion() >= DataAccess.schemaVersion55)
                deleteRuleSetCount = db.runUpdate(query, packageId);
            if (deleteRuleSetCount == 0  && !db.isMySQL()) {
                query = "delete from RULE_SET where RULE_SET_ID=?";
                deleteRuleSetCount = db.runUpdate(query, packageId);
            }
            count += deleteRuleSetCount;
            db.commit();
            return count;
        } catch(Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to delete package", e);
        } finally {
            db.closeConnection();
        }
    }

    public void deleteRuleSet(Long id) throws DataAccessException {
        try {
            db.openConnection();
            deleteReference("PACKAGE_RULESETS", "RULE_SET_ID", id);
            String query = "delete from RULE_SET where RULE_SET_ID=?";
            db.runUpdate(query, id);
            db.commit();
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to delete the rule set", e);
        }
        finally {
            db.closeConnection();
        }
    }

    /**
     * this is not needed for MDW 5, but cannot delete it for MDW 4
     * update backward compatibility (needed by Eclipse designer)
     */
    @Override
    protected void persistSla(Long pOwnerId, String pOwnerType,
            int newSlaSeconds, PersistType persistType)
        throws SQLException {
    }


    /**
     * not needed for MDW 5 since slas are stored as attributes
     */
    @Override
    protected void loadActivitySlas(List<ActivityVO> actVOs) throws SQLException {
    }

    /**
     * this is not needed for MDW 5, but cannot delete it for MDW 3 and early version of MDW 4
     * update backward compatibility (needed by Eclipse designer)
     */
    @Override
    protected void createSynchronizations(List<ActivityVO> pActArr)
    throws SQLException,DataAccessException {
    }

    /**
     * this is not needed for MDW 5, but cannot delete it for MDW 3 and early version of MDW 4
     * update backward compatibility (needed by Eclipse designer)
     */
    @Override
    protected void updateSynchronizations(List<ActivityVO> actList, Long processId)
    throws DataAccessException,SQLException {}

    /**
     * this is not needed for MDW 5, but cannot delete it for MDW 3 and early version of MDW 4
     * read backward compatibility
     */
    @Override
    protected void loadSynchronizations(Long processId, ProcessVO pProcessVO)
    throws SQLException {}

    @Override
    protected List<ExternalEventVO> loadExternalEvents(Long processId)
    throws SQLException {
    	return null;
    }

    @Override
    protected String getLanguageColumnName() {
    	return "LANGUAGE";
    }

    private Long getTaskIdFromLogicalId(String logicalId) {
    	try {
    		String query;
    		if (this.getDatabaseVersion()<=DataAccess.schemaVersion51) {	// for 5.1
    			query = "select TASK_ID from TASK where COMMENTS=? order by CREATE_DT desc";
    		} else {
    			query = "select TASK_ID from TASK where LOGICAL_ID=? order by CREATE_DT desc";
    		}
        	ResultSet rs = db.runSelect(query, logicalId);
        	if (rs.next()) return rs.getLong(1);
			else return null;
		} catch (SQLException e) {
			return null;
		}
    }

	@Override
	protected void loadTaskDefinition(ActivityVO actVO, ProcessVO mainProcess) {
		String logicalId = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_LOGICAL_ID);
		if (logicalId!=null) {
			try {
				Long taskId = getTaskIdFromLogicalId(logicalId);
				if (taskId!=null) loadTaskDefinitionSub(taskId, actVO, mainProcess);
			} catch (SQLException e) {
			}
		} // else assuming MDW 4 style, do not load from database
	}

	@Override
	protected void saveTaskDefinition(ActivityVO actVO, Long actId,
			ProcessVO mainProcess, AttributeBatch batch) throws SQLException,
			DataAccessException {
		if (this.getDatabaseVersion()<=DataAccess.schemaVersion5) {	// for 5.0
			super.saveTaskDefinition(actVO, actId, mainProcess, batch);
			return;
		}
		Long taskId;
		String logicalId = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_LOGICAL_ID);
		if (logicalId==null||logicalId.length()==0) {
			logicalId = mainProcess.getProcessName() + ":" + actVO.getAttribute(WorkAttributeConstant.LOGICAL_ID);
			actVO.setAttribute(TaskActivity.ATTRIBUTE_TASK_LOGICAL_ID, logicalId);
			taskId = null;
		} else taskId = this.getTaskIdFromLogicalId(logicalId);
    	saveTaskDefinitionSub(actVO, taskId!=null?taskId:db.isMySQL()?null:getNextId("MDW_COMMON_ID_SEQ"),
    			taskId!=null, mainProcess, batch);
	}

	@Override
	protected void persistTask(TaskVO task, PersistType persistType, Map<String,Long> categories,
    		AttributeBatch batch) throws SQLException, DataAccessException {
		if (this.getDatabaseVersion()<=DataAccess.schemaVersion5) {	// for 5.0
			super.persistTask(task, persistType, categories, batch);
			return;
		}
    	Long categoryId = categories.get(task.getTaskCategory());
    	if (categoryId==null) {
    	    try{
        		categoryId = createCategory(task.getTaskCategory());
        		categories.put(task.getTaskCategory(), categoryId);
            }
            catch (SQLException ex){
                throw new DataAccessException(0,"Please check if TASK_CATEGORY_ID in TASK_CATEGORY and TASK table is set to NUMBER(20)", ex);
            }
    	}
    	Long taskId = task.getTaskId();
    	if (persistType==PersistType.CREATE) {
    		taskId = this.createTask(task.getLogicalId(), task.getTaskName(),
    				task.getTaskTypeId(), categoryId, task.getComment());
    		task.setTaskId(taskId);
    	} else {
    		updateTask(taskId, task.getLogicalId(), task.getTaskName(),
    				task.getTaskTypeId(), categoryId, task.getComment());
    	}
    	persistAttributes(OwnerType.TASK, taskId, task.getAttributes(), batch);
    	if (!task.isGeneralTask() && getSupportedVersion() < DataAccess.schemaVersion52) {
    		super.persistTaskGroupsAndVariables(task, persistType);
    	}
    }

	private Long createTask(String logicalId, String taskName,
			Integer taskTypeId, Long categoryId, String comment) throws SQLException {
		Long taskId = db.isMySQL()?null:getNextId("MDW_COMMON_ID_SEQ");
		if (this.getDatabaseVersion()<DataAccess.schemaVersion52) {
			super.createTask(taskId, taskName, taskTypeId, categoryId, logicalId);
			return taskId;
		}
		String query = "insert into TASK" +
			" (TASK_ID,TASK_NAME,TASK_TYPE_ID,TASK_CATEGORY_ID,LOGICAL_ID,COMMENTS,CREATE_DT,CREATE_USR)" +
			" values (?,?,?,?,?,?,"+now()+",'ProcessPersister')";
		Object[] args = new Object[6];
		args[0] = taskId;
		args[1] = taskName;
		args[2] = taskTypeId;
		args[3] = categoryId;
		args[4] = logicalId;
		args[5] = comment;
		if (db.isMySQL()) taskId = db.runInsertReturnId(query, args);
		else db.runUpdate(query, args);
		return taskId;
	}

	private void updateTask(Long id, String logicalId, String taskName,
			Integer taskType, Long category, String comments) throws SQLException
	{
		if (this.getDatabaseVersion()<DataAccess.schemaVersion52) {
			super.updateTask(id, taskName, taskType, category, logicalId);
			return;
		}
		String query = "update TASK" +
			" set TASK_NAME=?, TASK_TYPE_ID=?, TASK_CATEGORY_ID=?, LOGICAL_ID=?, COMMENTS=?" +
			" where TASK_ID=?";
		Object[] args = new Object[6];
		args[0] = taskName;
		args[1] = taskType;
		args[2] = category;
		args[3] = logicalId;
		args[4] = comments;
		args[5] = id;
		db.runUpdate(query, args);
	}

	@Override
	protected PackageVO loadPackage0(Long packageId, boolean withProcesses)
    throws SQLException,DataAccessException {
		PackageVO pkg = super.loadPackage0(packageId, withProcesses);
		RuleSetVO pkgXml = this.getRuleSetForOwner0(OwnerType.PACKAGE, packageId);
		if (pkgXml!=null) pkg.setMetaContent(pkgXml.getRuleSet());
		return pkg;
	}

	@Override
	protected Long persistPackageProper(PackageVO packageVO, PersistType persistType)
    throws SQLException, DataAccessException, XmlException {
		Long pkgId = super.persistPackageProper(packageVO, persistType);
		if (!StringHelper.isEmpty(packageVO.getMetaContent())) {
		    String pkgDefXml = packageVO.getMetaContent();
		    try {
		        if (schemaTypeTranslator != null) {
		            pkgDefXml = schemaTypeTranslator.getOldProcessDefinition(packageVO.getMetaContent());
		        }
		    } catch (Exception ex) {
		    }
			RuleSetVO pkgXml = this.getRuleSetForOwner0(OwnerType.PACKAGE, pkgId);
			if (pkgXml==null) {
				pkgXml = new RuleSetVO();
				pkgXml.setId(pkgId);
				pkgXml.setName(packageVO.getPackageName() + " Package Definition");
				pkgXml.setLanguage(RuleSetVO.CONFIG);
				pkgXml.setVersion(packageVO.getVersion());
				pkgXml.setRuleSet(pkgDefXml);
				pkgXml.setOwnerId(pkgId);
				pkgXml.setOwnerType(OwnerType.PACKAGE);
				this.createRuleSet0(pkgXml);
			} else {
				pkgXml.setName(packageVO.getPackageName() + " Package Definition");
				pkgXml.setRuleSet(pkgDefXml);
				if (pkgXml.getOwnerId() == null) {
				    pkgXml.setOwnerId(pkgId);
				    pkgXml.setOwnerType(OwnerType.PACKAGE);
				}
				this.updateRuleSet0(pkgXml);
			}
		}
		return pkgId;
	}

	@Override
    protected TaskVO getTask(Long taskId, Map<Long,TaskCategory> categories)
			throws SQLException {
		String query;
		if (this.getDatabaseVersion()<=DataAccess.schemaVersion51) {	// for 5.1
			query = "select TASK_NAME, TASK_TYPE_ID, COMMENTS, TASK_CATEGORY_ID" +
				" from TASK where TASK_ID=?";
		} else {
			query = "select TASK_NAME, TASK_TYPE_ID, COMMENTS, TASK_CATEGORY_ID, LOGICAL_ID" +
				" from TASK where TASK_ID=?";
		}
		ResultSet rs = db.runSelect(query, taskId);
		if (!rs.next()) return null;
		TaskVO task = new TaskVO();
		Long categoryId = rs.getLong(4);
		task.setTaskId(taskId);
		Integer taskType = rs.getInt(2);
		task.setTaskTypeId(taskType);
		task.setTaskCategory(categories.get(categoryId).getCode());
		task.setTaskName(rs.getString(1));
		if (this.getDatabaseVersion()>=DataAccess.schemaVersion52) {
			task.setLogicalId(rs.getString(5));
			task.setComment(rs.getString(3));
		} else if (task.isTemplate()) {		// MDW 5.1 template task backward compatibility
			task.setLogicalId(rs.getString(3));
		} else {							// MDW 4.*/5.0 task
			task.setComment(rs.getString(3));
		}
		task.setAttributes(getAttributes1(OwnerType.TASK, taskId));
		if (task.isAutoformTask()) {
			task.setVariablesFromAttribute();
		} else if (!task.isGeneralTask()) {
			// backward compatibility for classic tasks (templated or not)
			task.setVariables(getVariablesForOwner(OwnerType.TASK, taskId));
		}
		if (!task.isTemplate() && task.getAttribute(TaskActivity.ATTRIBUTE_TASK_SLA)==null) {
			// for MDW 3 and early versions of MDW 4 tasks
			task.setSlaSeconds(getServiceLevelAgreement(OwnerType.TASK, taskId));
		}
		if (!(task.isTemplate() && task.isGeneralTask() && task.getAttribute(TaskActivity.ATTRIBUTE_TASK_GROUPS)!=null)) {
			// backward compatibility for MDW 4.* and 5.0 tasks
			task.setUserGroups(getGroupsForTask(taskId));
		}
		return task;
	}

	@Override
    protected void saveProcessInRuleSet(ProcessVO processVO, Long process_id,
    		PersistType persistType, int actualVersion, AttributeBatch batch) throws SQLException, DataAccessException, XmlException {
        // save task definition must be before exporting, as it may add task logical ID attributes
        saveTaskDefinitions(processVO, batch);
        processVO.removeEmptyAttributes();
    	ProcessExporter exporter = DataAccess.getProcessExporter(DataAccess.currentSchemaVersion, schemaTypeTranslator);
    	String procdefXml = exporter.exportProcess(processVO, getDatabaseVersion(), null);
    	RuleSetVO ruleset = new RuleSetVO();
    	ruleset.setId(process_id);
    	ruleset.setLanguage(RuleSetVO.PROCESS);
    	ruleset.setName(processVO.getProcessName());
    	ruleset.setComment(processVO.getProcessDescription());
    	ruleset.setRuleSet(procdefXml);
    	ruleset.setVersion(actualVersion);
    	ruleset.setCreateUser(processVO.getCreateUser());
    	ruleset.setModifyingUser(processVO.getModifyingUser());
    	if (persistType==PersistType.UPDATE || persistType==PersistType.SAVE) {
    		updateRuleSet0(ruleset);
    	} else {
    		Long id = createRuleSet0(ruleset);
    		if (getSupportedVersion()>=DataAccess.schemaVersion52)
    			processVO.setProcessId(id);
    	}
    }

    private void saveTaskDefinitions(ProcessVO processVO, AttributeBatch batch) throws DataAccessException, SQLException {
    	for (ActivityVO act : processVO.getActivities()) {
    	    ActivityImplementorVO impl = getActivityImpl(act);
    		if (impl != null && impl.isManualTask()) {
            	saveTaskDefinition(act, null, processVO, batch);
    		}
    	}
    	if (processVO.getSubProcesses()!=null) {
    		for (ProcessVO subproc : processVO.getSubProcesses()) {
    			for (ActivityVO act : subproc.getActivities()) {
    	            ActivityImplementorVO impl = getActivityImpl(act);
    	    		if (impl != null && impl.isManualTask()) {
    	            	saveTaskDefinition(act, null, processVO, batch);
    	    		}
    	    	}
    		}
    	}
    	if (batch != null)
    	 db.runBatchUpdate();
    }

    private void loadProcessBaseFromRuleSet(ProcessVO processVO, RuleSetVO ruleset) {
    	processVO.setProcessId(ruleset.getId());
		processVO.setProcessName(ruleset.getName());
		processVO.setVersion(ruleset.getVersion());
		processVO.setInRuleSet(true);
		if (ruleset.getModifyDate()==null)
			processVO.setModifyDate(ruleset.getCreateDate());
		else processVO.setModifyingUser(ruleset.getModifyingUser());
		processVO.setProcessDescription(ruleset.getComment());
    }

    private void loadProcessContentFromRuleSet(ProcessVO processVO, String xml)
    		throws DataAccessException {
    	ProcessImporter importer = DataAccess.getProcessImporter(DataAccess.currentSchemaVersion);
    	ProcessVO procdef = importer.importProcess(xml);
    	processVO.setVariables(procdef.getVariables());
    	processVO.setTransitions(procdef.getTransitions());
    	processVO.setActivities(procdef.getActivities());
    	processVO.setAttributes(procdef.getAttributes());
    	processVO.setSubProcesses(procdef.getSubProcesses());
    	processVO.setTextNotes(procdef.getTextNotes());
    }

    protected void loadProcessFromRuleSet(ProcessVO processVO)
    		throws SQLException, DataAccessException {
    	RuleSetVO ruleset = getRuleSet0(processVO.getProcessId());
    	if (getSupportedVersion()>=DataAccess.schemaVersion52)
    		loadProcessBaseFromRuleSet(processVO, ruleset);
    	loadProcessContentFromRuleSet(processVO, ruleset.getRuleSet());
    	loadTaskDefinitions(processVO);
    }

    private void loadTaskDefinitions(ProcessVO processVO) throws DataAccessException, SQLException {
    	for (ActivityVO act : processVO.getActivities()) {
        	// cannot use act.getActivityType(), which is not populated at this point
            ActivityImplementorVO impl = getActivityImpl(act);
    		if (impl != null && impl.isManualTask()) {
            	loadTaskDefinition(act, processVO);
    		}
    	}
    	if (processVO.getSubProcesses()!=null) {
    		for (ProcessVO subproc : processVO.getSubProcesses()) {
    			for (ActivityVO act : subproc.getActivities()) {
    		         ActivityImplementorVO impl = getActivityImpl(act);
    	    		if (impl != null && impl.isManualTask()) {
    	            	loadTaskDefinition(act, processVO);
    	    		}
    	    	}
    		}
    	}
    }

	@Override
	public List<ProcessVO> getProcessList() throws DataAccessException {
		if (getSupportedVersion()<DataAccess.schemaVersion52)
			return super.getProcessList();
        List<ProcessVO> processList = new ArrayList<ProcessVO>();
        try {
            db.openConnection();
            String query = "select RULE_SET_ID, RULE_SET_NAME, "
            	+ "VERSION_NO, COMMENTS, CREATE_DT, MOD_DT, MOD_USR "
                + "from RULE_SET where LANGUAGE='" + RuleSetVO.PROCESS + "' "
                + "order by upper(RULE_SET_NAME), VERSION_NO";
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
                Long processId = new Long(rs.getLong(1));
                String processName = rs.getString(2);
                int version = rs.getInt(3);
                String processDesc = rs.getString(4);
                Date createDate = rs.getTimestamp(5);
                Date modifyDate = rs.getTimestamp(6);
                String modifyUser = rs.getString(7);
                ProcessVO vo = new ProcessVO();
                vo.setProcessId(processId);
                vo.setProcessName(processName);
                vo.setVersion(version);
                vo.setProcessDescription(processDesc);
                vo.setCreateDate(createDate);
                vo.setModifyDate(modifyDate);
                vo.setModifyingUser(modifyUser);
                vo.setInRuleSet(true);
                processList.add(vo);
            }
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to load process", e);
        } finally {
            db.closeConnection();
        }
        return processList;
	}

	@Override
	protected void persistPackageProcessMapping(PackageVO packageVO,
			PersistType persistType) throws SQLException {
		if (getSupportedVersion()<DataAccess.schemaVersion52) {
			super.persistPackageProcessMapping(packageVO, persistType);
			return;
		}
		// no need to delete existing entries as that is already done by persistScripts
        if (packageVO.getProcesses()!=null) {
            String query = "insert into PACKAGE_RULESETS "
                + "(PACKAGE_ID, RULE_SET_ID) values "
                + "(?, ?)";
            Object[] args = new Object[2];
            args[0] = packageVO.getPackageId();
            for (ProcessVO vo : packageVO.getProcesses()) {
            	args[1] = vo.getProcessId();
                db.runUpdate(query, args);
            }
        }
	}

    public List<ProcessVO> findCallingProcesses(ProcessVO subproc) throws DataAccessException {
        if (getSupportedVersion() < DataAccess.schemaVersion52)
          throw new UnsupportedOperationException("Not supported for MDW 4.x style processes");

        ProcessImporter importer = DataAccess.getProcessImporter(DataAccess.currentSchemaVersion);
        try {
            List<Long> potentialCallingIds = new ArrayList<Long>();
            db.openConnection();
            String query = "select rule_set_id\n"
                + "from rule_set\n"
                + "where language = '" + RuleSetVO.PROCESS + "'\n"
                + "and (rule_set_details like '%Attribute Name=\"processname\" Value=\"%" + subproc.getName() + "\"%'\n"
                + "or rule_set_details like '%Attribute Name=\"processname\" Value=\"" + subproc.getName() + "\"%'\n"
                + "or rule_set_details like '%Attribute Name=\"processmap\" Value=\"%" + subproc.getName() + "%\"%')";
            ResultSet rs = db.runSelect(query, null);
            while (rs.next())
                potentialCallingIds.add(rs.getLong(1));

            List<ProcessVO> callers = new ArrayList<ProcessVO>();
            for (Long id : potentialCallingIds) {
                RuleSetVO ruleset = getRuleSet0(id);
                ProcessVO procdef = importer.importProcess(ruleset.getRuleSet());
                procdef.setProcessId(id);
                for (ActivityVO activity : procdef.getActivities()) {
                    if (activityInvokesProcess(activity, subproc) && !callers.contains(procdef))
                        callers.add(procdef);
                }
                for (ProcessVO embedded : procdef.getSubProcesses()) {
                    for (ActivityVO activity : embedded.getActivities()) {
                        if (activityInvokesProcess(activity, subproc) && !callers.contains(procdef))
                            callers.add(procdef);
                    }
                }
            }
            return callers;

        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to load process", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<ProcessVO> findCalledProcesses(ProcessVO mainproc) throws DataAccessException {
        // make sure process is loaded
        mainproc = loadProcess(mainproc.getId(), false);
        return findInvoked(mainproc, getProcessList());
    }

    public boolean activityInvokesProcess(ActivityVO activity, ProcessVO subproc) {
        String procName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
        if (procName != null && (procName.equals(subproc.getName()) || procName.endsWith("/" + subproc.getName()))) {
            String verSpec = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
            try {
                // compatibility
                int ver = Integer.parseInt(verSpec);
                verSpec = RuleSetVO.formatVersion(ver);
            }
            catch (NumberFormatException ex) {}

            if (subproc.meetsVersionSpec(verSpec))
                return true;
        }
        else {
            String procMap = activity.getAttribute(WorkAttributeConstant.PROCESS_MAP);
            if (procMap != null) {
                List<String[]> procmap = StringHelper.parseTable(procMap, ',', ';', 3);
                for (int i = 0; i < procmap.size(); i++) {
                    String nameSpec = procmap.get(i)[1];
                    if (nameSpec != null && (nameSpec.equals(subproc.getName()) || nameSpec.endsWith("/" + subproc.getName()))) {
                        String verSpec = procmap.get(i)[2];
                        try {
                            // compatibility
                            int ver = Integer.parseInt(verSpec);
                            verSpec = RuleSetVO.formatVersion(ver);
                        }
                        catch (NumberFormatException ex) {}

                        if (subproc.meetsVersionSpec(verSpec))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public List<ProcessVO> findInvoked(ProcessVO caller, List<ProcessVO> processes) {
        List<ProcessVO> called = new ArrayList<ProcessVO>();
        if (caller.getActivities() != null) {
            for (ActivityVO activity : caller.getActivities()) {
                String procName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
                if (procName != null) {
                    String verSpec = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
                    if (verSpec != null) {
                        try {
                            // compatibility
                            int ver = Integer.parseInt(verSpec);
                            verSpec = RuleSetVO.formatVersion(ver);
                        }
                        catch (NumberFormatException ex) {}

                        ProcessVO latestMatch = null;
                        for (ProcessVO process : processes) {
                            if ((procName.equals(process.getName()) || procName.endsWith("/" + process.getName()))
                            && (process.meetsVersionSpec(verSpec) && (latestMatch == null || latestMatch.getVersion() < process.getVersion()))) {
                                latestMatch = process;
                            }
                        }
                        if (latestMatch != null && !called.contains(latestMatch))
                            called.add(latestMatch);
                    }
                }
                else {
                    String procMap = activity.getAttribute(WorkAttributeConstant.PROCESS_MAP);
                    if (procMap != null) {
                        List<String[]> procmap = StringHelper.parseTable(procMap, ',', ';', 3);
                        for (int i = 0; i < procmap.size(); i++) {
                            String nameSpec = procmap.get(i)[1];
                            if (nameSpec != null) {
                                String verSpec = procmap.get(i)[2];
                                if (verSpec != null) {
                                    try {
                                        // compatibility
                                        int ver = Integer.parseInt(verSpec);
                                        verSpec = RuleSetVO.formatVersion(ver);
                                    }
                                    catch (NumberFormatException ex) {}

                                ProcessVO latestMatch = null;
                                for (ProcessVO process : processes) {
                                    if ((nameSpec.equals(process.getName()) || nameSpec.endsWith("/" + process.getName()))
                                      && (process.meetsVersionSpec(verSpec) && (latestMatch == null || latestMatch.getVersion() < process.getVersion()))) {
                                        latestMatch = process;
                                    }
                                }
                                if (latestMatch != null && !called.contains(latestMatch))
                                    called.add(latestMatch);
                                }
                            }
                        }
                    }
                }
            }
        }

        return called;
    }

	/**
	 * This is expensive, but it's deselected by default in Designer.
	 */
	@Override
    public List<ActivityImplementorVO> getReferencedImplementors(PackageVO packageVO)
    throws DataAccessException {
        List<ActivityImplementorVO> ret = new ArrayList<ActivityImplementorVO>();
        if (getSupportedVersion() < DataAccess.schemaVersion52)
            ret.addAll(super.getReferencedImplementors(packageVO));
        try {
            db.openConnection();
            loadExistingImplementors();
            for (ProcessVO process : packageVO.getProcesses()) {
                ProcessVO loaded = getProcessBase0(process.getId());
                if (loaded.isInRuleSet()) {
                    // load to get the activities
                    loadProcessFromRuleSet(loaded);
                    for (ActivityVO activity : loaded.getActivities()) {
                        ActivityImplementorVO activityImpl = getActivityImpl(activity);
                        if (activityImpl != null && !ret.contains(activityImpl))
                            ret.add(activityImpl);
                    }
                    if (process.getSubProcesses() != null) {
                        for (ProcessVO subproc : process.getSubProcesses()) {
                            for (ActivityVO activity : subproc.getActivities()) {
                                ActivityImplementorVO activityImpl = getActivityImpl(activity);
                                if (activityImpl != null && !ret.contains(activityImpl))
                                    ret.add(activityImpl);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to get referenced implementors", e);
        } finally {
            db.closeConnection();
        }
        return ret;
    }

    /**
     * This method is just here to shadow super.getActivityImplementorVOsForProcess().
     * The only place in modern MDW where this info is used is for inference during
     * export, which is handled in getReferencedImplementors().
     */
    @Override
    protected List<ActivityImplementorVO> getActivityImplementorVOsForProcess(Long processId)
    throws SQLException {
        List<ActivityImplementorVO> retVOs = new ArrayList<ActivityImplementorVO>();
        if (getSupportedVersion() < DataAccess.schemaVersion52)
          retVOs.addAll(super.getActivityImplementorVOsForProcess(processId));
        return retVOs;
    }

	@Override
    public List<ProcessVO> getProcessListForImplementor(Long implementorId, String implementorClass)
	throws DataAccessException {
        List<ProcessVO> processList = new ArrayList<ProcessVO>();
		if (getSupportedVersion() < DataAccess.schemaVersion52)
			processList.addAll(super.getProcessListForImplementor(implementorId, implementorClass));
		if (getDatabaseVersion() >= DataAccess.schemaVersion52) {
		    // add rule_set processes
            try {
                db.openConnection();
                String query = "select rule_set_id, rule_set_name, version_no, comments, mod_dt, mod_usr\n"
                    + "from rule_set\n"
                    + "where language = '" + RuleSetVO.PROCESS + "'\n"
                    + "and rule_set_details like '%Implementation=\"" + implementorClass + "\"%'\n";
                ResultSet rs = db.runSelect(query, null);
                while (rs.next()) {
        			ProcessVO procdef = new ProcessVO();
        			procdef.setProcessId(rs.getLong("rule_set_id"));
        			procdef.setProcessName(rs.getString("rule_set_name"));
        			procdef.setVersion(rs.getInt("version_no"));
        			procdef.setProcessDescription(rs.getString("comments"));
        			procdef.setModifyDate(rs.getTimestamp("mod_dt"));
        			procdef.setModifyingUser(rs.getString("mod_usr"));
                    processList.add(procdef);
                }
            } catch (SQLException e) {
                throw new DataAccessException(0, "failed to load process", e);
            } finally {
                db.closeConnection();
            }
		}
        return processList;
    }
}
