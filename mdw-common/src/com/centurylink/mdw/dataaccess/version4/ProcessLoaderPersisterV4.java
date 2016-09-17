/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.version4;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.SchemaTypeTranslator;
import com.centurylink.mdw.common.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.model.data.monitor.ServiceLevelAgreement;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.data.task.TaskType;
import com.centurylink.mdw.model.data.work.WorkType;
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
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;

public class ProcessLoaderPersisterV4 extends CommonDataAccess implements ProcessPersister, ProcessLoader {

    private static final String ATTRIBUTE_ICONNAME = "ICONNAME";
    private static final String ATTRIBUTE_LABEL = "LABEL";
    private static final String ATTRIBUTE_ATTRDESC = "ATTRDESC";
    private static final String ATTRIBUTE_BASECLASS = "BASECLASS";
    private static final String ATTRIBUTE_MDW_VERSION = "MDWVERSION";

    protected static final String ATTRIBUTE_OVERFLOW_PREFIX = RuleSetVO.ATTRIBUTE_OVERFLOW + "_";

    private Map<Long,TaskCategory> categoryId2Cat;       // used by loader/persister
    private Map<String,Long> categoryCode2Id;       // used by persister/getTaskCategorySet
    protected Map<Long,Long> workNameRef = new HashMap<Long,Long>();  // used by persister
    private Map<String,Long> processNametoProcessId = new HashMap<String, Long>();
                        // used by persister
    protected Map<Long,ActivityImplementorVO> implementorId2Obj = null;   // used by loader/persister
    protected Map<String,ActivityImplementorVO> implementorName2Obj = null;   // used by persister

    protected SchemaTypeTranslator schemaTypeTranslator;  // used by persister

    public ProcessLoaderPersisterV4(DatabaseAccess db, int databaseVersion, int supportedVersion, SchemaTypeTranslator schemaTypeTranslator) {
        super(db, databaseVersion, supportedVersion);
        categoryId2Cat = null;
        categoryCode2Id = null;
        this.schemaTypeTranslator = schemaTypeTranslator;
    }

    private boolean tableExist(String table_name) throws SQLException {
        // determine if the DB contains MDW 3 tables
        String query;
        if (db.isMySQL()) {
            query = "select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_NAME=?";
        } else {
            query = "select TABLE_NAME from ALL_TABLES where TABLE_NAME=?";
        }
        ResultSet rs = db.runSelect(query, table_name);
        return rs.next();
    }

    private int deleteTransitions(String transitionQueryWhere, Long processId) throws SQLException {
        // delete transition attributes
        String query = "delete from ATTRIBUTE where ATTRIBUTE_OWNER='WORK_TRANSITION' " +
            "and ATTRIBUTE_OWNER_ID in (select WORK_TRANS_ID from WORK_TRANSITION where " + transitionQueryWhere + ")";
        int n = db.runUpdate(query, processId);
        int count = n;
        System.out.println("Deleted transition attributes: " + n);

        // delete transitions
        query = "delete from WORK_TRANSITION where " + transitionQueryWhere;
        n = db.runUpdate(query, processId);
        count += n;
        System.out.println("Deleted transitions: " + n + " (where " + transitionQueryWhere +")");
        return count;
    }

    protected int deleteReference(String tablename, String fkeyname, Long id)
            throws SQLException {
        int n;
        if (tableExist(tablename)) {
            String query = "delete from " + tablename + " where " + fkeyname + "=?";
            n = db.runUpdate(query, id);
//            System.out.println("Deleted " + tablename + ": " + n);
        } else n = 0;
        return n;
    }

    private int deleteProcessTwo(Long processId) throws SQLException {
        int count = 0;
        String query;

        count += deleteReference("POOL", "PROCESS_ID", processId);

        if (getSupportedVersion()<DataAccess.schemaVersion52) {
            count += deleteReference("PACKAGE_PROCESS", "PROCESS_ID", processId);
            query = "delete from PROCESS where PROCESS_ID=?";
            count += db.runUpdate(query, processId);
            query = "delete from WORK where WORK_ID=?";
            count += db.runUpdate(query, processId);
        }
        else {
            count += deleteReference("PACKAGE_RULESETS", "RULE_SET_ID", processId);
        }
        query = "delete from RULE_SET where RULE_SET_ID=?";
        count += db.runUpdate(query, processId);

        return count;
    }

    private int deleteProcessOne(Long processId) throws SQLException {
        int count = 0, n;

        String query, activityQuery, transitionQueryWhere;
        ResultSet rs;
        transitionQueryWhere = "PROCESS_ID=?";
        activityQuery = "select unique(ACTIVITY_ID) from ACTIVITY w, WORK_TRANSITION t " +
                "where (w.ACTIVITY_ID=t.FROM_WORK_ID or w.ACTIVITY_ID=t.TO_WORK_ID) and t.PROCESS_ID=?";

        // collect subprocesses, but can't delete them yet
        List<Long> embeddedSubProcesses = new ArrayList<Long>();
        query = "select p.PROCESS_ID from PROCESS p, WORK_TRANSITION t " +
                "where p.PROCESS_ID=t.TO_WORK_ID and t.PROCESS_ID=?";
        rs = db.runSelect(query, processId);
        while (rs.next()) {
            embeddedSubProcesses.add(new Long(rs.getString(1)));
        }

        // remember activities, as we must delete transitions first
        query = activityQuery;
        List<String> activityList = new ArrayList<String>();
        rs = db.runSelect(query, processId);
        while (rs.next()) activityList.add(rs.getString(1));

        try {
            // delete work synchronization
            if (tableExist("WORK_SYNCHRONIZATION")) {
                query = "delete from WORK_SYNCHRONIZATION where ACTIVITY_ID in (" + activityQuery + ")";
                n = db.runUpdate(query, processId);
                System.out.println("Deleted Work synchronization: " + n);
                count += n;
            }

            // delete variable mappings
            query = "delete from VARIABLE_MAPPING where MAPPING_OWNER='PROCESS' and MAPPING_OWNER_ID=?";
            n = db.runUpdate(query, processId);
            count += n;
            System.out.println("Deleted variable mappings: " + n);

            // delete variables without mappings; also need to delete variable instances first
            query = "delete from VARIABLE_INSTANCE where VARIABLE_ID not in (select VARIABLE_ID from VARIABLE_MAPPING)";
            n = db.runUpdate(query, null);
            query = "delete from VARIABLE where VARIABLE_ID!=0 and VARIABLE_ID not in (select VARIABLE_ID from VARIABLE_MAPPING)";
            n = db.runUpdate(query, null);
            count += n;
            System.out.println("Deleted variables without mappings: " + n);

            // delete attributes
            query = "delete from ATTRIBUTE where ATTRIBUTE_OWNER='PROCESS' and ATTRIBUTE_OWNER_ID=?";
            n = db.runUpdate(query, processId);
            System.out.println("Deleted process attributes: " + n);
            count += n;
            query = "delete from ATTRIBUTE where ATTRIBUTE_OWNER='ACTIVITY' " +
                    "and ATTRIBUTE_OWNER_ID in (" + activityQuery + ")";
            n = db.runUpdate(query, processId);
            count += n;
            System.out.println("Deleted activity attributes: " + n);

            // delete transitions in full (attributes, dependency, validation are deleted as well)
            count += deleteTransitions(transitionQueryWhere, processId);

            // delete activities
            query = "delete from ACTIVITY where ACTIVITY_ID=?";
            db.prepareStatement(query);
            n = 0;
            for (String one : activityList) {
                db.runUpdateWithPreparedStatement(one);
                n++;
            }
            count += n;
            System.out.println("Deleted activities in ACTIVITY table: " + n);

            // delete the process references from package and pool
            count += deleteReference("PACKAGE_PROCESS", "PROCESS_ID", processId);
            count += deleteReference("POOL", "PROCESS_ID", processId);

            query = "delete from PROCESS where PROCESS_ID=?";
            n = db.runUpdate(query, processId);
            count += n;
            System.out.println("Deleted the process from process table: " + n);

            // delete process and activities from work table
            query = "delete from WORK where WORK_ID=?";
            db.prepareStatement(query);
            n = 0;
            for (String one : activityList) {
                db.runUpdateWithPreparedStatement(one);
                n++;
            }
            db.runUpdate(query, processId);
            count += n+1;
            System.out.println("Deleted the activity/process from work table " + n);
        } catch (SQLException e) {
            StringBuffer errmsg = new StringBuffer();
            guessFailedReason(processId, activityList, errmsg);
            if (errmsg.length()>0) throw new SQLException(errmsg.toString());
            else throw e;
        }

        for (Long subprocId : embeddedSubProcesses) {
            System.out.println("......Delete subprocess " + subprocId.toString());

            // delete link from other processes (can happen with MDW 3 processes)
//            count += this.deleteTransitions("TO_WORK_ID=?", subprocId);

            n = this.deleteProcessOne(subprocId);
            count += n;
        }

        return count;

    }


    private void guessFailedReason(Long processId, List<String> activityList, StringBuffer msg) {
        try {
            String query = "select WORK_TRANS_ID,PROCESS_ID from WORK_TRANSITION where FROM_WORK_ID=? or TO_WORK_ID=?";
            Object[] args = new Object[2];
            args[0] = processId;
            args[1] = processId;
            ResultSet rs = db.runSelect(query, args);
            while (rs.next()) {
                msg.append("There is a transition ")
                    .append(rs.getString(1))
                    .append(" from the process ")
                    .append(rs.getString(2))
                    .append(" refers to the process ")
                    .append(processId)
                    .append('\n');
            }
            for (String one : activityList) {
                args[0] = one;
                args[1] = one;
                rs = db.runSelect(query, args);
                while (rs.next()) {
                    msg.append("There is a transition ")
                        .append(rs.getString(1))
                        .append(" from the process ")
                        .append(rs.getString(2))
                        .append(" refers to the activity ")
                        .append(one)
                        .append(" in process ")
                        .append(processId)
                        .append('\n');
                }
            }

        } catch (Exception e) {
        }
    }

    public void deleteProcess(ProcessVO processVO) throws DataAccessException {
        try {
            db.openConnection();
            if (processVO.isInRuleSet())
                deleteProcessTwo(processVO.getProcessId());
            else
                deleteProcessOne(processVO.getProcessId());
            db.commit();
        } catch(Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to delete process", e);
        } finally {
            db.closeConnection();
        }
    }

    protected int deletePools(Long packageId) throws SQLException {
        int count = 0;
        String query = "delete from ATTRIBUTE where ATTRIBUTE_OWNER='LANE' " +
            " and ATTRIBUTE_OWNER_ID in " +
            " (select l.LANE_ID from LANE l, POOL p" +
            "  where l.POOL_ID = p.POOL_ID and p.PACKAGE_ID = ?)";
        int n = db.runUpdate(query, packageId);

        query = "delete from LANE where POOL_ID in " +
                "(select POOL_ID from POOL where PACKAGE_ID=?)";
        n = db.runUpdate(query, packageId);
        count += n;

        query = "delete from POOL where PACKAGE_ID=?";
        n = db.runUpdate(query, packageId);
        count += n;

        return count;
    }

    public int deletePackage(Long packageId) throws DataAccessException {
        try {
            db.openConnection();
            int count = deletePackage0(packageId);
            db.commit();
            return count;
        } catch(Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to delete package", e);
        } finally {
            db.closeConnection();
        }
    }

    protected int deletePackage0(Long packageId) throws DataAccessException, SQLException {
        int count = 0;
        String query = "delete from PACKAGE_ACTIVITY_IMPLEMENTORS where PACKAGE_ID=?";
        int n = db.runUpdate(query, packageId);
        count += n;

        query = "delete from PACKAGE_EXTERNAL_EVENTS where PACKAGE_ID=?";
        n = db.runUpdate(query, packageId);
        count += n;

        if (getDatabaseVersion() < DataAccess.schemaVersion52) {
            query = "delete from PACKAGE_PROCESS where PACKAGE_ID=?";
            n = db.runUpdate(query, packageId);
            count += n;

            count += deletePools(packageId);
        }

        query = "delete from " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " where PACKAGE_ID=?";
        n = db.runUpdate(query, packageId);
        count += n;

        return count;
    }

    private int getCurrentVersionForProcess(String name) throws SQLException {
        String query = "SELECT decode(max(VERSION_NO), null , 0, max(VERSION_NO)) MAX FROM PROCESS p, WORK w "
            + " WHERE w.WORK_NAME = ? AND w.WORK_ID = p.PROCESS_ID";
        ResultSet rs = db.runSelect(query, name);
        if (rs.next()) return rs.getInt(1);
        else return 0;
    }

    private int getCurrentVersionForPackage(String name) throws SQLException {
        String query = "select DATA_VERSION from " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " where PACKAGE_NAME=?"
            + " order by DATA_VERSION desc";
        ResultSet rs = db.runSelect(query, name);
        if (rs.next()) return rs.getInt(1);
        else return 0;
    }

    //////////////////////////////

    public List<ProcessVO> getProcessList()
        throws DataAccessException {
        List<ProcessVO> processList = new ArrayList<ProcessVO>();
        try {
            db.openConnection();
            String query = "select ATTRIBUTE_OWNER_ID,ATTRIBUTE_VALUE from ATTRIBUTE "
                + "where ATTRIBUTE_OWNER='PROCESS' and ATTRIBUTE_NAME=?";
            ResultSet rs = db.runSelect(query, WorkAttributeConstant.PROCESS_VISIBILITY);
            HashMap<Long,String> proctypes = new HashMap<Long,String>();
            while (rs.next()) {
                proctypes.put(rs.getLong(1), rs.getString(2));
            }
            query = "select wr.WORK_ID, wr.WORK_NAME, wr.COMMENTS,"
                + "pr.VERSION_NO, wr.CREATE_DT, wr.MOD_DT, wr.MOD_USR, pr.PROCESS_TYPE_ID"
                + " from WORK wr, PROCESS pr"
                + " where wr.WORK_TYPE = 1 AND wr.WORK_ID = pr.PROCESS_ID"
                + " order by upper(wr.WORK_NAME), pr.VERSION_NO";
            rs = db.runSelect(query, null);
            while (rs.next()) {
                Long processId = new Long(rs.getLong(1));
                String procType = proctypes.get(processId);
                Integer procTypeId = rs.getInt(8);
                if (procType==null && procTypeId.equals(ProcessVO.PROCESS_TYPE_CONCRETE)) continue;
                if (ProcessVisibilityConstant.EMBEDDED.equals(procType)) continue;
                String processName = rs.getString(2);
                String processDesc = rs.getString(3);
                int version = rs.getInt(4);
                Date createDate = rs.getTimestamp(5);
                Date modifyDate = rs.getTimestamp(6);
                String modifyUser = rs.getString(7);
                ProcessVO vo = new ProcessVO();
                vo.setProcessId(processId);
                vo.setProcessName(processName);
                vo.setVersion(version);
                vo.setProcessDescription(processDesc);
                vo.setCreateDate(createDate);
                vo.setModifyDate(modifyDate==null?createDate:modifyDate);
                vo.setModifyingUser(modifyUser);
                vo.setInRuleSet(procTypeId.equals(ProcessVO.PROCESS_TYPE_ALIAS));
                processList.add(vo);
            }
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to load process", e);
        } finally {
            db.closeConnection();
        }
        return processList;
    }

    /**
     * Uses 40% of progress monitor.  Deep is ignored since it has new meaning now.
     */
    public List<PackageVO> getPackageList(boolean deep, ProgressMonitor progressMonitor) throws DataAccessException {
        List<PackageVO> packageList = new ArrayList<PackageVO>();
        Map<Long,PackageVO> packageVOs = new HashMap<Long,PackageVO>();

        try {
            db.openConnection();
            // main package list
            if (progressMonitor != null)
                progressMonitor.subTask("Loading main package list");
            String query = null;
            if (db.isAnsiSQL()) {
                StringBuffer pkgQuery = new StringBuffer()
                        .append("select pkg.PACKAGE_ID, pkg.PACKAGE_NAME, pkg.SCHEMA_VERSION, pkg.DATA_VERSION, pkg.EXPORTED_IND, pkg.MOD_DT, pkg.group_name,")
                        .append("attr.ATTRIBUTE_ID, attr.ATTRIBUTE_NAME, attr.ATTRIBUTE_VALUE\n")
                        .append("from " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " pkg LEFT JOIN ATTRIBUTE attr\n")
                        .append("ON pkg.PACKAGE_ID = attr.ATTRIBUTE_OWNER_ID  and 'PACKAGE' = attr.ATTRIBUTE_OWNER ")
                        .append("order by PACKAGE_NAME, DATA_VERSION desc");
                query = pkgQuery.toString();
            }
            else {
                query = "select pkg.PACKAGE_ID, pkg.PACKAGE_NAME, pkg.SCHEMA_VERSION, pkg.DATA_VERSION, pkg.EXPORTED_IND, pkg.MOD_DT,"
                        + (DataAccess.isPackageLevelAuthorization ? " pkg.group_name," : "")
                        + "attr.ATTRIBUTE_ID, attr.ATTRIBUTE_NAME, attr.ATTRIBUTE_VALUE\n"
                        + "from " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " pkg, ATTRIBUTE attr\n"
                        + "where pkg.PACKAGE_ID = attr.ATTRIBUTE_OWNER_ID (+) and 'PACKAGE' = attr.ATTRIBUTE_OWNER (+)\n"
                        + "order by PACKAGE_NAME, DATA_VERSION desc";
            }

            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
                Long pkgId = rs.getLong(1);
                PackageVO packageVO = packageVOs.get(pkgId);
                if (packageVO == null) {
                    packageVO = new PackageVO();
                    packageVO.setPackageId(rs.getLong(1));
                    packageVO.setPackageName(rs.getString(2));
                    packageVO.setSchemaVersion(rs.getInt(3));
                    packageVO.setVersion(rs.getInt(4));
                    packageVO.setExported(rs.getInt(5) > 0);
                    packageVO.setModifyDate(rs.getTimestamp(6));
                    if (DataAccess.isPackageLevelAuthorization) {
                        packageVO.setGroup(rs.getString("GROUP_NAME"));
                    }
                    packageVO.setImplementors(new ArrayList<ActivityImplementorVO>());
                    packageVO.setExternalEvents(new ArrayList<ExternalEventVO>());
                    packageVO.setProcesses(new ArrayList<ProcessVO>());
                    packageVO.setRuleSets(new ArrayList<RuleSetVO>());
                    packageList.add(packageVO);
                    packageVOs.put(packageVO.getPackageId(), packageVO);
                }
                Long attrId = rs.getLong("ATTRIBUTE_ID");
                if (attrId != null && attrId != 0L) {
                    AttributeVO attrVO = new AttributeVO();
                    attrVO.setAttributeId(attrId);
                    attrVO.setAttributeName(rs.getString("ATTRIBUTE_NAME"));
                    attrVO.setAttributeValue(rs.getString("ATTRIBUTE_VALUE"));
                    if (packageVO.getAttributes() == null)
                        packageVO.setAttributes(new ArrayList<AttributeVO>());
                    packageVO.getAttributes().add(attrVO);
                }
            }
            if (progressMonitor != null)
                progressMonitor.progress(10);

            query =  "";
            if(getSupportedVersion() == DataAccess.schemaVersion52){
                query = "select pkg.PACKAGE_ID, rs.rule_set_details\n "
                        + "from " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " pkg, RULE_SET rs\n"
                        + "where pkg.PACKAGE_ID = rs.rule_set_id and rs.language='CONFIG'";
            }else if(getSupportedVersion() >= DataAccess.schemaVersion55){
                query = "select pkg.PACKAGE_ID, rs.rule_set_details "
                        + "from PACKAGE pkg, RULE_SET rs "
                        + "where rs.language='CONFIG' and rs.owner_type='PACKAGE' and rs.owner_id = pkg.package_id";
            }

            if(!StringHelper.isEmpty(query)){
                rs = db.runSelect(query, null);
                String voXMLStr = null;
                while (rs.next()) {
                    Long packageId = rs.getLong(1);
                    voXMLStr= rs.getString(2);
                    PackageVO packageVO = packageVOs.get(packageId);
                    packageVO.setMetaContent(voXMLStr);
                }
            }

            // activity implementors
            if (progressMonitor != null)
                progressMonitor.subTask("Loading package activity implementors");
            query = "select pai.PACKAGE_ID, ai.ACTIVITY_IMPLEMENTOR_ID, ai.IMPL_CLASS_NAME\n"
                + "from PACKAGE_ACTIVITY_IMPLEMENTORS pai, ACTIVITY_IMPLEMENTOR ai\n"
                + "where ai.ACTIVITY_IMPLEMENTOR_ID = pai.ACTIVITY_IMPLEMENTOR_ID\n";
            rs = db.runSelect(query, null);
            while (rs.next()) {
                Long packageId = rs.getLong(1);
                Long actImplId = rs.getLong(2);
                ActivityImplementorVO activityImpl = new ActivityImplementorVO(actImplId, rs.getString(3));
                PackageVO packageVO = packageVOs.get(packageId);
                packageVO.getImplementors().add(activityImpl);
            }
            if (progressMonitor != null)
                progressMonitor.progress(10);

            // external event handlers
            if (progressMonitor != null)
                progressMonitor.subTask("Loading package event handlers");
            query = "select pee.PACKAGE_ID, ee.EXTERNAL_EVENT_ID, ee.EVENT_NAME, ee.EVENT_HANDLER\n"
                + "from PACKAGE_EXTERNAL_EVENTS pee, EXTERNAL_EVENT ee\n"
                + "where ee.EXTERNAL_EVENT_ID = pee.EXTERNAL_EVENT_ID";
            rs = db.runSelect(query, null);
            while (rs.next()) {
                Long packageId = rs.getLong(1);
                Long extEventId = rs.getLong(2);
                ExternalEventVO externalEvent = new ExternalEventVO();
                externalEvent.setId(extEventId);
                externalEvent.setEventName(rs.getString(3));
                externalEvent.setEventHandler(rs.getString(4));
                PackageVO packageVO = packageVOs.get(packageId);
                packageVO.getExternalEvents().add(externalEvent);
            }
            if (progressMonitor != null)
                progressMonitor.progress(10);

            if (getSupportedVersion()<DataAccess.schemaVersion52) {

                // processes
                if (progressMonitor != null)
                    progressMonitor.subTask("Loading package processes");
                query = "select pp.PACKAGE_ID, p.PROCESS_ID, w.WORK_NAME, p.VERSION_NO, w.COMMENTS, p.PROCESS_TYPE_ID, w.CREATE_DT, w.MOD_DT, w.MOD_USR\n"
                    + "from PACKAGE_PROCESS pp, PROCESS p, WORK w\n"
                    + "where pp.PROCESS_ID = p.PROCESS_ID\n"
                    + "and p.PROCESS_ID = w.WORK_ID";
                rs = db.runSelect(query, null);
                while (rs.next()) {
                    Long packageId = rs.getLong(1);
                    Long processId = rs.getLong(2);
                    String processName = rs.getString(3);
                    int version = rs.getInt(4);
                    String comment = rs.getString(5);
                    ProcessVO procVO = new ProcessVO(processId, processName, comment, null);
                    procVO.setVersion(version);
                    procVO.setCreateDate(rs.getTimestamp(7));
                    procVO.setModifyDate(rs.getTimestamp(8));
                    procVO.setModifyingUser(rs.getString(9));
                    PackageVO packageVO = packageVOs.get(packageId);
                    packageVO.getProcesses().add(procVO);
                }
                if (progressMonitor != null)
                    progressMonitor.progress(10);

                // pools and lanes
                query = "SELECT p.PACKAGE_ID, p.POOL_ID, p.POOL_NAME, p.PROCESS_ID, l.LANE_ID, l.LANE_NAME\n"
                    + "from POOL p, LANE l\n"
                    + "where p.POOL_ID = l.POOL_ID (+)";
                Map<Long,PoolVO> poolVOs = new HashMap<Long,PoolVO>();
                rs = db.runSelect(query, null);
                while (rs.next()) {
                    Long packageId = rs.getLong(1);
                    Long poolId = rs.getLong(2);
                    PoolVO poolVO = poolVOs.get(poolId);
                    if (poolVO == null) {
                        poolVO = new PoolVO();
                        poolVO.setPoolId(poolId);
                        poolVO.setPoolName(rs.getString(3));
                        poolVO.setProcessId(rs.getLong(4));
                        poolVOs.put(poolId, poolVO);
                    }
                    Long laneId = rs.getLong(5);
                    if (laneId != null) {
                        LaneVO laneVO = new LaneVO();
                        laneVO.setLaneId(laneId);
                        laneVO.setLaneName(rs.getString(6));
                        laneVO.setPool(poolVO);
                        if (poolVO.getLanes() == null)
                            poolVO.setLanes(new ArrayList<LaneVO>());
                        poolVO.getLanes().add(laneVO);
                    }
                    PackageVO packageVO = packageVOs.get(packageId);
                    if (packageVO.getPools() == null)
                        packageVO.setPools(new ArrayList<PoolVO>());
                    packageVO.getPools().add(poolVO);
                }

            }
        }
        catch (SQLException e) {
            throw new DataAccessException(0, "failed to load package list", e);
        }
        finally {
            db.closeConnection();
        }

        return packageList;
    }

    public List<ActivityImplementorVO> getActivityImplementors()
            throws DataAccessException {
        try {
            db.openConnection();
            return getAllActivityImplementors0();
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to load activity implementors", e);
        } finally {
            db.closeConnection();
        }
    }

    private Hashtable<Long,ActivityImplementorVO> getImplementorAttributes()
        throws SQLException {
        String query = "select ATTRIBUTE_OWNER_ID, ATTRIBUTE_NAME, ATTRIBUTE_VALUE from ATTRIBUTE where ATTRIBUTE_OWNER=?";
        ResultSet rs = db.runSelect(query, OwnerType.ACTIVITY_IMPLEMENTOR);
        Hashtable<Long,ActivityImplementorVO> hash = new Hashtable<Long,ActivityImplementorVO>();
        ActivityImplementorVO vo;
        Long id;
        while (rs.next()) {
            id = new Long(rs.getLong(1));
            String attrname = rs.getString(2);
            String attrvalue = rs.getString(3);
            vo = hash.get(id);
            if (vo==null) {
                vo = new ActivityImplementorVO();
                vo.setImplementorId(id);
                hash.put(id, vo);
            }
            if (attrname.equals(ATTRIBUTE_ICONNAME))
                vo.setIconName(attrvalue);
            else if (attrname.equals(ATTRIBUTE_LABEL))
                vo.setLabel(attrvalue);
            else if (attrname.equals(ATTRIBUTE_ATTRDESC))
                vo.setAttributeDescription(attrvalue);
            else if (attrname.equals(ATTRIBUTE_BASECLASS))
                vo.setBaseClassName(attrvalue);
            else if (attrname.equals(ATTRIBUTE_MDW_VERSION))
                vo.setMdwVersion(attrvalue);
        }
        return hash;
    }

    private List<ActivityImplementorVO> getAllActivityImplementors0() throws SQLException {
        Hashtable<Long,ActivityImplementorVO> hash = getImplementorAttributes();
        ActivityImplementorVO vo;
        Long id;
        String query = "SELECT ACTIVITY_IMPLEMENTOR_ID, IMPL_CLASS_NAME FROM ACTIVITY_IMPLEMENTOR";
        ResultSet rs = db.runSelect(query, null);
        List<ActivityImplementorVO> retVOs = new ArrayList<ActivityImplementorVO>();
        while (rs.next()) {
            id = new Long(rs.getLong(1));
            vo = hash.get(id);
            if (vo==null) {
                vo = new ActivityImplementorVO();
                vo.setImplementorId(id);
            }
            vo.setImplementorClassName(rs.getString(2));
            vo.setShowInToolbox(vo.getAttributeDescription() != null &&
                    vo.getBaseClassName() != null && vo.getIconName() != null);
            retVOs.add(vo);
        }
        return retVOs;
    }

    public List<VariableTypeVO> getVariableTypes() throws DataAccessException {
        try {
            db.openConnection();
            String query = "select VARIABLE_TYPE_ID,VARIABLE_TYPE_NAME,TRANSLATOR_CLASS_NAME" +
                    " from variable_type order by VARIABLE_TYPE_ID";
            ResultSet rs = db.runSelect(query, null);
            List<VariableTypeVO> ret = new ArrayList<VariableTypeVO>();
            while (rs.next()) {
                Long id = new Long(rs.getLong(1));
                String name = rs.getString(2);
                String translator = rs.getString(3);
                VariableTypeVO vo = new VariableTypeVO(id,name,translator);
                ret.add(vo);
            }
            return ret;
        } catch (SQLException e) {
            throw new DataAccessException(0,"failed to load variable types", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<TaskCategory> getTaskCategories() throws DataAccessException {
        try {
            List<TaskCategory> taskCats = new ArrayList<TaskCategory>();
            db.openConnection();
            Map<Long,TaskCategory> categories = getCategoryId2CatMap();
            taskCats.addAll(categories.values());
            Collections.sort(taskCats);
            return taskCats;
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to load process variables", e);
        } finally {
            db.closeConnection();
        }
    }

    public void updateExternalEvent(ExternalEventVO event) throws DataAccessException {
        try {
            db.openConnection();
            String query = "update EXTERNAL_EVENT set EVENT_NAME=?,EVENT_HANDLER=?"
                + " where EXTERNAL_EVENT_ID=?";
            Object[] args = new Object[3];
            args[0] = event.getEventName();
            args[1] = event.getEventHandler();
            args[2] = event.getId();
            db.runUpdate(query, args);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to create exteranl event", e);
        } finally {
            db.closeConnection();
        }
    }

    public void createExternalEvent(ExternalEventVO event) throws DataAccessException {
        try {
            db.openConnection();
            this.createExternalEvent0(event);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to create external event", e);
        } finally {
            db.closeConnection();
        }
    }

    public void deleteExternalEvent(Long eventId) throws DataAccessException {
        try {
            db.openConnection();
            deleteReference("PACKAGE_EXTERNAL_EVENTS", "EXTERNAL_EVENT_ID", eventId);
            String query = "delete from EXTERNAL_EVENT where EXTERNAL_EVENT_ID=?";
            db.runUpdate(query, eventId);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to delete exteranl event", e);
        } finally {
            db.closeConnection();
        }
    }

    public void deleteActivitiesForImplementor(ActivityImplementorVO vo) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from ACTIVITY where ACTIVITY_IMPL_ID=?";
            db.runUpdate(query, vo.getImplementorId());
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to delete activities", e);
        } finally {
            db.closeConnection();
        }
    }

    public void deleteActivityImplementor(Long implementorId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from ACTIVITY_IMPLEMENTOR where ACTIVITY_IMPLEMENTOR_ID=?";
            db.runUpdate(query, implementorId);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to delete activity implementor", e);
        } finally {
            db.closeConnection();
        }
    }

    public Long createActivityImplementor(ActivityImplementorVO vo) throws DataAccessException {
        try {
            db.openConnection();
            Long id = this.createImplementor0(vo, null);
            db.commit();
            vo.setImplementorId(id);
            return id;
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to create activity implementor", e);
        } finally {
            db.closeConnection();
        }
    }

    private void updateImplementor0(ActivityImplementorVO vo, AttributeBatch batch) throws SQLException {
        String query = "update ACTIVITY_IMPLEMENTOR "
                + " set IMPL_CLASS_NAME=?"
                + " where ACTIVITY_IMPLEMENTOR_ID=?";
        Object[] args = new Object[2];
        args[0] = vo.getImplementorClassName();
        args[1] = vo.getImplementorId();
        db.runUpdate(query, args);
        if (vo.isLoaded()) {
            deleteAttributes(OwnerType.ACTIVITY_IMPLEMENTOR, vo.getImplementorId(), batch);
            List<AttributeVO> attrs = new ArrayList<AttributeVO>(5);
            attrs.add(new AttributeVO(ATTRIBUTE_ATTRDESC, vo.getAttributeDescription()));
            attrs.add(new AttributeVO(ATTRIBUTE_LABEL, vo.getLabel()));
            attrs.add(new AttributeVO(ATTRIBUTE_ICONNAME, vo.getIconName()));
            attrs.add(new AttributeVO(ATTRIBUTE_BASECLASS, vo.getBaseClassName()));
            addAttributes(attrs, vo.getImplementorId(), OwnerType.ACTIVITY_IMPLEMENTOR, batch);
        }
    }

    public void updateActivityImplementor(ActivityImplementorVO vo) throws DataAccessException {
        try {
            db.openConnection();
            updateImplementor0(vo, null);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to update activity implementor", e);
        } finally {
            db.closeConnection();
        }
    }

    public ProcessVO getProcessBase(String name, int version)
            throws DataAccessException {
        try {
            db.openConnection();
            return this.getProcessBase0(name, version);
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed load base process", e);
        } finally {
            db.closeConnection();
        }
    }

    public ProcessVO getProcessBase(Long processId)
            throws DataAccessException {
        try {
            db.openConnection();
            return this.getProcessBase0(processId);
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed load base process", e);
        } finally {
            db.closeConnection();
        }
    }

    ////////////////////////////// Loader portion

    /**
     * Returns a value in seconds.
     */
    protected final int getServiceLevelAgreement(String ownerType, Long ownerId)
            throws SQLException {
        String query = "SELECT SLA_HR from SLA sla"
            + " WHERE sla.SLA_OWNER_ID = ? and sla.SLA_OWNER = ? and"
            + " (sla.SLA_START_DT is null or sysdate > sla.SLA_START_DT) and"
            + " (sla.SLA_END_DT is null or sla.SLA_END_DT > sysdate )";
        String[] args = new String[2];
        args[0] = ownerId.toString();
        args[1] = ownerType;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) {
            return Math.round(rs.getFloat(1) * 3600);
        }
        else {
            return 0;
        }
    }

    public PackageVO loadPackage(Long packageId, boolean withProcesses)
        throws DataAccessException {
        PackageVO vo;
        try {
            db.openConnection();
            vo = loadPackage0(packageId, withProcesses);
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to load process", e);
        } catch (DataAccessException e) {
            throw e;
        } finally {
            db.closeConnection();
        }
        return vo;
    }

    protected PackageVO loadPackage0(Long packageId, boolean withProcesses)
            throws SQLException,DataAccessException {

        String query = "select PACKAGE_NAME,SCHEMA_VERSION,DATA_VERSION,EXPORTED_IND,MOD_DT" +
             (DataAccess.isPackageLevelAuthorization ? ",GROUP_NAME " : "") +
            " from " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " where PACKAGE_ID=?";
        ResultSet rs = db.runSelect(query, packageId);
        if (!rs.next()) throw new
            DataAccessException("Package with specified ID does not exist: "
                    + packageId);
        PackageVO packageVO = new PackageVO();
        packageVO.setPackageId(packageId);
        packageVO.setPackageName(rs.getString(1));
        packageVO.setSchemaVersion(rs.getInt(2));
        packageVO.setVersion(rs.getInt(3));
        packageVO.setExported(rs.getInt(4) > 0);
        packageVO.setModifyDate(rs.getTimestamp(5));
        if (DataAccess.isPackageLevelAuthorization)
            packageVO.setGroup(rs.getString("GROUP_NAME"));
        // load pools
        if (getSupportedVersion()<DataAccess.schemaVersion52)
            packageVO.setPools(loadPoolsForPackage(packageId));
        // load attributes
        packageVO.setAttributes(this.getAttributes0(OwnerType.PACKAGE, packageId));
        // load activity implementors
        if (withProcesses) this.loadExistingImplementors();
        query = "select pai.ACTIVITY_IMPLEMENTOR_ID, ai.IMPL_CLASS_NAME " +
                "from PACKAGE_ACTIVITY_IMPLEMENTORS pai, ACTIVITY_IMPLEMENTOR ai " +
                "where pai.PACKAGE_ID=? and pai.ACTIVITY_IMPLEMENTOR_ID=ai.ACTIVITY_IMPLEMENTOR_ID";
        rs = db.runSelect(query, packageId);
        List<ActivityImplementorVO> implementors = new ArrayList<ActivityImplementorVO>();
        while (rs.next()) {
            Long id = new Long(rs.getLong(1));
            ActivityImplementorVO vo;
            if (withProcesses) {
                vo = implementorId2Obj.get(id);
                if (vo==null) continue;
            } else {
                vo = new ActivityImplementorVO(id, rs.getString(2));
            }
            implementors.add(vo);
        }
        packageVO.setImplementors(implementors);
        // load external event handler
        query = "select pee.EXTERNAL_EVENT_ID, ee.EVENT_NAME, ee.EVENT_HANDLER " +
                "from PACKAGE_EXTERNAL_EVENTS pee, EXTERNAL_EVENT ee " +
                "where pee.PACKAGE_ID=? and pee.EXTERNAL_EVENT_ID=ee.EXTERNAL_EVENT_ID";
        rs = db.runSelect(query, packageId);
        List<ExternalEventVO> externalEvents = new ArrayList<ExternalEventVO>();
        while (rs.next()) {
            ExternalEventVO vo = new ExternalEventVO();
            vo.setId(new Long(rs.getLong(1)));
            vo.setEventName(rs.getString(2));
            vo.setEventHandler(rs.getString(3));
            externalEvents.add(vo);
        }
        packageVO.setExternalEvents(externalEvents);
        // load processes
        if (getSupportedVersion()<DataAccess.schemaVersion52) {
            query = "select PROCESS_ID from PACKAGE_PROCESS where PACKAGE_ID=?";
            rs = db.runSelect(query, packageId);
            List<Long> processIDs = new ArrayList<Long>();
            while (rs.next()) {
                processIDs.add(new Long(rs.getLong(1)));
            }
            List<ProcessVO> processVOs = new ArrayList<ProcessVO>(processIDs.size());
            for (Long processId: processIDs) {
                ProcessVO processVO;
                if (withProcesses) processVO = loadProcess0(processId, true, null);
                else processVO = getProcessBase0(processId);
                processVOs.add(processVO);
            }
            packageVO.setProcesses(processVOs);
        } else packageVO.setProcesses(new ArrayList<ProcessVO>());

        // load scripts
        loadScriptsForPackage(packageVO, withProcesses);

        return packageVO;
    }

    protected void loadScriptsForPackage(PackageVO packageVO, boolean withContent)
        throws SQLException, DataAccessException {
        // this function is for V5 only
    }

    public List<ActivityImplementorVO> getReferencedImplementors(PackageVO packageVO)
        throws DataAccessException {
        List<ActivityImplementorVO> ret = new ArrayList<ActivityImplementorVO>();
        try {
            db.openConnection();
            loadExistingImplementors();
            Map<String,ActivityImplementorVO> map = new HashMap<String,ActivityImplementorVO>();
            String query = "select PROCESS_ID from PACKAGE_PROCESS where PACKAGE_ID=?";
            ResultSet rs = db.runSelect(query, packageVO.getPackageId());
            List<Long> processIDs = new ArrayList<Long>();
            while (rs.next()) {
                processIDs.add(new Long(rs.getLong(1)));
            }
            query = "select wt.TO_WORK_ID from WORK_TRANSITION wt " +
                "where wt.FROM_WORK_ID in (select PROCESS_ID from PACKAGE_PROCESS where PACKAGE_ID=?) " +
                "  and event_type_id in ('3','4','5','8')";
            rs = db.runSelect(query, packageVO.getPackageId());
            while (rs.next()) {
                processIDs.add(new Long(rs.getLong(1)));
            }
            for (Long processId: processIDs) {
                List<ActivityImplementorVO> onelist
                    = this.getActivityImplementorVOsForProcess(processId);
                for (ActivityImplementorVO one : onelist) {
                    if (!map.containsKey(one.getImplementorClassName())) {
                        map.put(one.getImplementorClassName(), one);
                    }
                }
            }
            for (String key: map.keySet()) {
                ret.add(map.get(key));
            }
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to get referenced implementors", e);
        } finally {
            db.closeConnection();
        }
        return ret;
    }

    public ProcessVO loadProcess(Long processID, boolean withSubProcesses)
    throws DataAccessException {
        ProcessVO vo;
        try {
            db.openConnection();
            this.loadExistingImplementors();
            vo = loadProcess0(processID, withSubProcesses, null);
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to load process", e);
        } finally {
            db.closeConnection();
        }
        return vo;
    }

    public Map<String,String> getAttributes(String ownerType, Long ownerId) throws DataAccessException {
        try {
            db.openConnection();
            List<AttributeVO> attrs = getAttributes0(ownerType, ownerId);
            if (attrs == null)
                return null;
            Map<String,String> map = new HashMap<String,String>();
            for (AttributeVO attr : attrs)
                map.put(attr.getAttributeName(), attr.getAttributeValue());
            return map;
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to load process", e);
        } finally {
            db.closeConnection();
        }
    }

    private ProcessVO loadProcess0(Long processID, boolean withSubProcesses, ProcessVO mainProcess)
            throws SQLException,DataAccessException {
        boolean only_base_sub_process = false;

        if (getSupportedVersion() >= DataAccess.schemaVersion52) {
            ProcessVO processDefn = new ProcessVO();
            processDefn.setProcessId(processID);
            loadProcessFromRuleSet(processDefn);
            loadReferencedImplementors(processDefn);
            return processDefn;
        }

        ProcessVO processDefn = this.getProcessBase0(processID);     // was concPrId
        if (mainProcess!=null) {
            String mainProcName = mainProcess.getProcessName();
            String subProcName = processDefn.getProcessName();
            if (subProcName.startsWith(mainProcName) && subProcName.length() > mainProcName.length())
                processDefn.setProcessName(subProcName.substring(mainProcName.length()+1));
        }
        processDefn.setProcessId(processID);
        processDefn.setAttributes(getAttributes0(OwnerType.PROCESS, processID));
        if (mainProcess==null && !processDefn.isEmbeddedProcess()) mainProcess = processDefn;

        if (processDefn.isInRuleSet()) {
            loadProcessFromRuleSet(processDefn);
            loadReferencedImplementors(processDefn);
            return processDefn;
        }

        processDefn.setVariables(getVariablesForOwner(OwnerType.PROCESS, processID));
        processDefn.setTransitions(getWorkTransitionVOsForProcess(processID));
        processDefn.setActivities(getActivityVOsForProcess(processID, mainProcess));
        processDefn.setExternalEvents(loadExternalEvents(processID));
        this.loadSynchronizations(processID, processDefn);

        if (withSubProcesses) {
            List<Long> subProcessIds = this.getAllDirectSubProcessForProcess(processID);
            if (subProcessIds!=null) {
                List<ProcessVO> subProcesses = new ArrayList<ProcessVO>();
                for (Long pid : subProcessIds) {
                    ProcessVO subProc;
                    if (only_base_sub_process) {
                        subProc = this.getProcessBase0(pid);
                        subProc.setAttributes(getAttributes0(OwnerType.PROCESS, pid));
                    } else {
                        subProc = this.loadProcess0(pid, false, processDefn);
                        loadReferencedImplementors(subProc);
                    }
                    subProcesses.add(subProc);
                }
                processDefn.setSubProcesses(subProcesses);
            }
            loadReferencedImplementors(processDefn);
        }
        return processDefn;
    }

    private void loadReferencedImplementors(ProcessVO processVO) throws SQLException {
        processVO.setImplementors(getActivityImplementorVOsForProcess(processVO.getProcessId()));
        if (processVO.getSubProcesses()!=null) {
            for (ProcessVO subproc: processVO.getSubProcesses()) {
                subproc.setImplementors(getActivityImplementorVOsForProcess(subproc.getProcessId()));
            }
        }
    }

    public List<ExternalEventVO> loadExternalEvents() throws DataAccessException {
        try {
            String query = "select EXTERNAL_EVENT_ID,EVENT_NAME,EVENT_HANDLER" +
                    " from EXTERNAL_EVENT";
            db.openConnection();
            ResultSet rs = db.runSelect(query, null);
            List<ExternalEventVO> list = new ArrayList<ExternalEventVO>();
            while (rs.next()) {
                ExternalEventVO event = new ExternalEventVO();
                event.setEventHandler(rs.getString(3));
                event.setEventName(rs.getString(2));
                event.setId(new Long(rs.getString(1)));
                list.add(event);
            }
            return list;
        } catch (Exception e) {
            throw new DataAccessException(0,"fail to load external events", e);
        } finally {
            db.closeConnection();
        }
    }

    protected List<ExternalEventVO> loadExternalEvents(Long processId)
        throws SQLException {
        String query = "SELECT ee.EXTERNAL_EVENT_ID,ee.EVENT_NAME,ee.EVENT_HANDLER "
            + " from EXT_EVENT_PROCESS_MAPP eepm, EXTERNAL_EVENT ee"
            + " WHERE eepm.PROCESS_ID = ? and ee.EXTERNAL_EVENT_ID = eepm.EXT_EVENT_ID"
            + " and (eepm.START_DT is null or sysdate > eepm.START_DT)"
            + " and (eepm.END_DT is null or eepm.END_DT > sysdate)";
        ResultSet rs = db.runSelect(query, processId);
        List<ExternalEventVO> extEvents = null;
        while (rs.next()) {
            if (extEvents==null) extEvents = new ArrayList<ExternalEventVO>();
            ExternalEventVO vo = new ExternalEventVO();
            vo.setId(new Long(rs.getLong(1)));
            vo.setEventName(rs.getString(2));
            vo.setEventHandler(rs.getString(3));
            extEvents.add(vo);
        }
        return extEvents;
    }

    protected void loadSynchronizations(Long processId, ProcessVO pProcessVO)
            throws SQLException {
        String query = "SELECT ws.ACTIVITY_ID,ws.WORK_ID FROM work_synchronization ws, WORK_TRANSITION wt"
            + " WHERE wt.process_id = ? and wt.EFF_END_DT is NULL and wt.TO_WORK_ID = ws.ACTIVITY_ID";
        ResultSet rs = db.runSelect(query, processId);
        while (rs.next()) {
            String activityId = rs.getString(1);
            String workId = rs.getString(2);
            ActivityVO act = pProcessVO.getActivityVO(new Long(activityId));
            if (act != null)
                act.addSynchronizationId(new Long(workId));
        }
    }

    protected List<VariableVO> getVariablesForOwner(String ownerType, Long ownerId)
        throws SQLException {
        String query = "select distinct v.VARIABLE_ID, v.VARIABLE_NAME, vt.VARIABLE_TYPE_NAME,"
            + "    vm.VAR_REFERRED_AS, "
            + "    vm.VARIABLE_DATA_OPT_IND, vm.DISPLAY_SEQ, vm.VARIABLE_DATA_SOURCE, vm.COMMENTS "
            + "from VARIABLE v, VARIABLE_MAPPING vm, VARIABLE_TYPE vt "
            + "where vm.MAPPING_OWNER = '" + ownerType + "' and"
            + "    vm.MAPPING_OWNER_ID = ?  and"
            + "    vm.VARIABLE_ID = v.VARIABLE_ID and"
            + "    v.VARIABLE_TYPE_ID = vt.VARIABLE_TYPE_ID";
        ResultSet rs = db.runSelect(query, ownerId);
        List<VariableVO> variables = new ArrayList<VariableVO>();
        while (rs.next()) {
            Long id = new Long(rs.getLong(1));
            String name = rs.getString(2);
            String type = rs.getString(3);
            String refAs = rs.getString(4);
            Integer opt = new Integer(rs.getInt(5));
            Integer seq = new Integer(rs.getInt(6));
            String dataSource = rs.getString(7);
            String description = rs.getString(8);
            if (OwnerType.TASK.equals(ownerType) && VariableVO.DATA_SOURCE_READONLY.equals(dataSource)) opt = VariableVO.DATA_READONLY;
            VariableVO variable = new VariableVO(id, name, type, refAs, opt, seq);
            variable.setDescription(description);
            variables.add(variable);
        }
        return variables;
    }

    private List<WorkTransitionVO> getWorkTransitionVOsForProcess(Long pProcessId)
            throws SQLException {
        List<WorkTransitionVO> retVO = new ArrayList<WorkTransitionVO>();
        String query ="SELECT WORK_TRANS_ID,FROM_WORK_ID,TO_WORK_ID,EVENT_TYPE_ID,WORK_COMP_CD"
            + " FROM work_transition wt"
            + " WHERE wt.PROCESS_ID = ? and wt.EFF_END_DT is null";
        ResultSet rs = db.runSelect(query, pProcessId);
        List<AttributeVO> attrs = null;
        String validClassName = null;
        while (rs.next()) {
            WorkTransitionVO vo = new WorkTransitionVO(new Long(rs.getLong(1)),
                    new Long(rs.getLong(2)), new Long(rs.getLong(3)),
                    new Integer(rs.getInt(4)),
                    rs.getString(5), validClassName, attrs);
            retVO.add(vo);
        }
        StringBuffer sb = new StringBuffer();
        boolean batch = true;
        if (batch) {
            for (WorkTransitionVO vo : retVO) {
                if (sb.length()>0) sb.append(',');
                sb.append(vo.getWorkTransitionId().toString());
            }
            Map<Long,List<AttributeVO>> attrss = getAttributesBatch(OwnerType.WORK_TRANSITION, sb.toString());
            for (WorkTransitionVO vo : retVO) {
                attrs = attrss.get(vo.getWorkTransitionId());
                vo.setAttributes(attrs);
            }
        } else {
            for (WorkTransitionVO vo : retVO) {
                attrs = this.getAttributes0(OwnerType.WORK_TRANSITION, vo.getWorkTransitionId());
                vo.setAttributes(attrs);
            }
        }
        return retVO;
    }

    /**
     * Load basic information of process - including name, version, description and type
     * @param pProcessId
     * @return
     * @throws SQLException
     * @throws DataAccessException
     */
    protected ProcessVO getProcessBase0(Long pProcessId)
            throws SQLException,DataAccessException {
        String query;
        boolean isInRuleSet;
        if (getSupportedVersion()<DataAccess.schemaVersion52) {
            query = "select w.WORK_NAME, p.VERSION_NO, w.COMMENTS, w.CREATE_DT, w.MOD_DT, w.MOD_USR, p.PROCESS_TYPE_ID " +
                "from PROCESS p, WORK w " +
                "where p.PROCESS_ID=w.WORK_ID and p.PROCESS_ID=?";
            isInRuleSet = false;
        } else {
            query = "select RULE_SET_NAME, VERSION_NO, COMMENTS, CREATE_DT, MOD_DT, MOD_USR " +
                "from RULE_SET where RULE_SET_ID=?";
            isInRuleSet = true;
        }
        ResultSet rs = db.runSelect(query, pProcessId);
        String processName, processComment;
        int version;
        if (rs.next()) {
            processName = rs.getString(1);
            version = rs.getInt(2);
            processComment = rs.getString(3);
            if (!isInRuleSet) isInRuleSet = ProcessVO.PROCESS_TYPE_ALIAS.intValue()==rs.getInt(7);
        } else throw new DataAccessException("Process does not exist; ID=" + pProcessId);
        ProcessVO retVO= new ProcessVO(pProcessId, processName, processComment, null);  // external events - load later
        retVO.setVersion(version);
        retVO.setInRuleSet(isInRuleSet);
        Date createDate = rs.getTimestamp(4);
        Date modifyDate = rs.getTimestamp(5);
        retVO.setModifyDate(modifyDate==null?createDate:modifyDate);
        retVO.setModifyingUser(rs.getString(6));
        return retVO;
    }

    protected List<ActivityVO> getActivityVOsForProcess(Long pProcessId, ProcessVO mainProcess) throws SQLException {
        List<ActivityVO> retVO = new ArrayList<ActivityVO>();
        String query = "SELECT distinct act.ACTIVITY_ID, w.WORK_NAME, w.COMMENTS, act.ACTIVITY_IMPL_ID"
                + " FROM work_transition wt, ACTIVITY act, WORK w"
                + " WHERE wt.PROCESS_ID = ? and"
                + " ( wt.TO_WORK_ID = w.WORK_ID  or wt.FROM_WORK_ID = w.WORK_ID ) and"
                + " w.WORK_TYPE = 2 and"
                + " wt.EFF_END_DT is NULL and"
                + " w.WORK_ID = act.ACTIVITY_ID";
        ResultSet rs = db.runSelect(query, pProcessId);
        List<AttributeVO> attrs = null;
        while (rs.next()) {
            Long actId = new Long(rs.getString(1));
            String actName = rs.getString(2);
            String actDesc = rs.getString(3);
            Long actImplId = rs.getLong(4);
            String actImplClassName = implementorId2Obj.get(actImplId).getImplementorClassName();
            ActivityVO actVO = new ActivityVO(actId, actName, actDesc, actImplClassName, attrs);
            retVO.add(actVO);
        }

        boolean batch = true;
        if (batch) {
            StringBuffer sb = new StringBuffer();
            for (ActivityVO actVO  : retVO) {
                if (sb.length()>0) sb.append(',');
                sb.append(actVO.getActivityId().toString());
            }
            Map<Long,List<AttributeVO>> attrss = getAttributesBatch(OwnerType.ACTIVITY, sb.toString());
            query = "select RULE_SET_DETAILS from RULE_SET where RULE_SET_ID=?";
            for (ActivityVO actVO : retVO) {
                attrs = attrss.get(actVO.getActivityId());
                if (attrs!=null) {
                    for (AttributeVO attr : attrs) {
                        String v = attr.getAttributeValue();
                        if (v!=null && v.startsWith(ATTRIBUTE_OVERFLOW_PREFIX)) {
                            Long rulesetId = new Long(v.substring(ATTRIBUTE_OVERFLOW_PREFIX.length()));
                            rs = db.runSelect(query, rulesetId);
                            if (rs.next()) {
                                attr.setAttributeValue(rs.getString(1));
                            }
                        }
                    }
                    actVO.setAttributes(attrs);
                }
                ActivityImplementorVO activityImpl = getActivityImpl(actVO);
                // load task mapping for task activity when load process entry point is not an embedded process
                if (mainProcess!=null && (activityImpl != null && activityImpl.isManualTask()))
                    loadTaskDefinition(actVO, mainProcess);
            }
            // need to be after above, so that attributes won't be overriden
            loadActivitySlas(retVO);
        } else {
            for (ActivityVO actVO : retVO) {
                attrs = this.getAttributes0(OwnerType.ACTIVITY, actVO.getActivityId());
                actVO.setAttributes(attrs);
                if (actVO.getAttribute(WorkAttributeConstant.SLA)==null) {
                    actVO.setSlaSeconds(getServiceLevelAgreement(OwnerType.ACTIVITY, actVO.getActivityId()));
                }
                ActivityImplementorVO activityImpl = getActivityImpl(actVO);
                // load task mapping for task activity when load process entry point is not an embedded process
                if (mainProcess!=null && (activityImpl != null && activityImpl.isManualTask()))
                    loadTaskDefinition(actVO, mainProcess);
            }
        }
        return retVO;
    }

    protected void loadActivitySlas(List<ActivityVO> actVOs) throws SQLException {
        StringBuffer sb = new StringBuffer();
        for (ActivityVO actVO : actVOs) {
            if (sb.length()>0) sb.append(',');
            sb.append(actVO.getActivityId().toString());
        }

        String query = "SELECT SLA_HR, SLA_OWNER_ID from SLA sla"
            + " WHERE sla.SLA_OWNER_ID in (" + sb.toString() + ") and sla.SLA_OWNER = ? and"
            + " (sla.SLA_START_DT is null or sysdate > sla.SLA_START_DT) and"
            + " (sla.SLA_END_DT is null or sla.SLA_END_DT > sysdate )";
        ResultSet rs = db.runSelect(query, OwnerType.ACTIVITY);
        Map<Long,Integer> slas = new HashMap<Long,Integer>();
        while (rs.next()) {
            Integer sla = new Integer(Math.round(rs.getFloat(1) * 3600));
            Long ownerId = rs.getLong(2);
            slas.put(ownerId, sla);
        }

        for (ActivityVO actVO : actVOs) {
            Integer sla = slas.get(actVO.getActivityId());
            if (sla!=null && sla.intValue()>0) actVO.setSlaSeconds(sla);
        }
    }

    protected void loadTaskDefinitionSub(Long taskId, ActivityVO actVO, ProcessVO mainProcess)
            throws SQLException {
        Map<Long,TaskCategory> categories = getCategoryId2CatMap();
        TaskVO task = this.getTask(taskId, categories);
        actVO.setAttribute(TaskActivity.ATTRIBUTE_TASK_NAME, task.getTaskName());
        actVO.setAttribute(TaskActivity.ATTRIBUTE_TASK_CATEGORY, task.getTaskCategory());
        actVO.setAttribute(TaskActivity.ATTRIBUTE_TASK_DESC, task.getComment());
        if (task.getSlaSeconds()!=0) {
            String slaUnits = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_SLA_UNITS);
            if (StringHelper.isEmpty(slaUnits)) slaUnits = ServiceLevelAgreement.INTERVAL_HOURS;
            actVO.setAttribute(TaskActivity.ATTRIBUTE_TASK_SLA,
                    ServiceLevelAgreement.secondsToUnits(task.getSlaSeconds(), slaUnits));
        }
        String alertIntervalAttr = task.getAttribute(TaskAttributeConstant.ALERT_INTERVAL);
        if (alertIntervalAttr != null) {
            int alertIntervalSeconds = Integer.parseInt(alertIntervalAttr);
            String alertIntervalUnits = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_ALERT_INTERVAL_UNITS);
            if (StringHelper.isEmpty(alertIntervalUnits)) alertIntervalUnits = ServiceLevelAgreement.INTERVAL_MINUTES;
            actVO.setAttribute(TaskActivity.ATTRIBUTE_TASK_ALERT_INTERVAL,
                    ServiceLevelAgreement.secondsToUnits(alertIntervalSeconds, alertIntervalUnits));
        }
        actVO.setAttribute(TaskActivity.ATTRIBUTE_TASK_GROUPS, task.getUserGroupsAsString());
        actVO.setAttribute(TaskActivity.ATTRIBUTE_TASK_OBSERVER,
                AttributeVO.findAttribute(task.getAttributes(),TaskAttributeConstant.OBSERVER_NAME));
        actVO.setAttribute(TaskActivity.ATTRIBUTE_TASK_VARIABLES,
                task.getVariablesAsString(mainProcess.getVariables()));
//      actVO.setAttribute(TaskActivity.ATTRIBUTE_TASK_ID, taskId.toString());
        if (task.getCustomPage()!=null) {
            // for MDW 5.1 or newer only
            if (actVO.getAttribute(TaskActivity.ATTRIBUTE_FORM_NAME)!=null)
                actVO.setAttribute(TaskActivity.ATTRIBUTE_FORM_NAME,task.getCustomPage());
            else if (actVO.getAttribute(TaskActivity.ATTRIBUTE_CUSTOM_PAGE)!=null)
                actVO.setAttribute(TaskActivity.ATTRIBUTE_CUSTOM_PAGE,task.getCustomPage());
            // TODO handling form version
        }
    }

    protected void loadTaskDefinition(ActivityVO actVO, ProcessVO mainProcess) {
        String varCategory = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_CATEGORY);
        if (varCategory!=null) return;  // already converted from MDW 3 style
        String task_name = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_NAME);
        if (task_name==null) return;
        String query = "select TASK_ID from TASK where TASK_NAME=?" +
                " order by CREATE_DT desc";
        try {
            ResultSet rs = db.runSelect(query, task_name);
            if (rs.next()) loadTaskDefinitionSub(rs.getLong(1), actVO, mainProcess);
        } catch (Exception e) {
        }
    }

    protected void saveTaskDefinitionSub(ActivityVO actVO, Long taskId, boolean existing, ProcessVO mainProcess,
                AttributeBatch batch)
            throws SQLException,DataAccessException {
        PersistType persistType = existing?PersistType.UPDATE:PersistType.CREATE;
        TaskVO task = new TaskVO();
        task.setTaskId(taskId);
        String task_name = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_NAME);
        if (task_name == null || task_name.trim().length() == 0) task_name = "My Task";
        task.setTaskName(task_name);
        String varCategory = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_CATEGORY);
        if (varCategory == null || varCategory.trim().length() == 0)
            varCategory = "ORD";
        task.setTaskCategory(varCategory);
        task.setComment(actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_DESC));
        if (getDatabaseVersion()>=DataAccess.schemaVersion51) {
            task.setLogicalId(actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_LOGICAL_ID));
            task.setAttribute(TaskAttributeConstant.INDICES, actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_INDICES));
        }
        if (getDatabaseVersion()>=DataAccess.schemaVersion52) {
            task.setAttribute(TaskAttributeConstant.SERVICE_PROCESSES, actVO.getAttribute(TaskActivity.ATTRIBUTE_SERVICE_PROCESSES));
        }
        task.setAttribute(TaskAttributeConstant.OBSERVER_NAME, actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_OBSERVER));
        task.setAttribute(TaskAttributeConstant.NOTICES, actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_NOTICES));
        task.setAttribute(TaskAttributeConstant.NOTICE_GROUPS, actVO.getAttribute(TaskActivity.ATTRIBUTE_NOTICE_GROUPS));
        task.setAttribute(TaskAttributeConstant.RECIPIENT_EMAILS, actVO.getAttribute(TaskActivity.ATTRIBUTE_RECIPIENT_EMAILS));
        task.setAttribute(TaskAttributeConstant.CC_GROUPS, actVO.getAttribute(TaskActivity.ATTRIBUTE_CC_GROUPS));
        task.setAttribute(TaskAttributeConstant.CC_EMAILS, actVO.getAttribute(TaskActivity.ATTRIBUTE_CC_EMAILS));
        task.setAttribute(TaskAttributeConstant.AUTO_ASSIGN, actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_AUTOASSIGN));
        task.setAttribute(TaskAttributeConstant.ROUTING_STRATEGY, actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_ROUTING));
        task.setAttribute(TaskAttributeConstant.ROUTING_RULES, actVO.getAttribute(TaskActivity.ATTRIBUTE_ROUTING_RULES));
        task.setAttribute(TaskAttributeConstant.SUBTASK_STRATEGY, actVO.getAttribute(TaskActivity.ATTRIBUTE_SUBTASK_STRATEGY));
        task.setAttribute(TaskAttributeConstant.SUBTASK_RULES, actVO.getAttribute(TaskActivity.ATTRIBUTE_SUBTASK_RULES));
        task.setAttribute(TaskAttributeConstant.INDEX_PROVIDER, actVO.getAttribute(TaskActivity.ATTRIBUTE_INDEX_PROVIDER));
        task.setAttribute(TaskAttributeConstant.ASSIGNEE_VAR, actVO.getAttribute(TaskActivity.ATTRIBUTE_ASSIGNEE_VAR));
        task.setAttribute(TaskAttributeConstant.FORM_NAME, actVO.getAttribute(TaskActivity.ATTRIBUTE_FORM_NAME));
        task.setAttribute(TaskAttributeConstant.PRIORITY_STRATEGY, actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_PRIORITIZATION));
        task.setAttribute(TaskAttributeConstant.PRIORITY, actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_PRIORITY));
        task.setAttribute(TaskAttributeConstant.PRIORITIZATION_RULES, actVO.getAttribute(TaskActivity.ATTRIBUTE_PRIORITIZATION_RULES));
            // TODO handling form version
        task.setAttribute(TaskAttributeConstant.CUSTOM_PAGE, actVO.getAttribute(TaskActivity.ATTRIBUTE_CUSTOM_PAGE));
        task.setAttribute(TaskAttributeConstant.RENDERING_ENGINE, actVO.getAttribute(TaskActivity.ATTRIBUTE_RENDERING));
        //added for Auto Assignment Rules enhancement
        task.setAttribute(TaskAttributeConstant.AUTO_ASSIGN_RULES, actVO.getAttribute(TaskActivity.ATTRIBUTE_AUTO_ASSIGN_RULES));

        String sla = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_SLA);
        String slaUnits = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_SLA_UNITS);
        if (StringHelper.isEmpty(slaUnits)) slaUnits = "Hours";
        if (sla!=null && sla.trim().length()>0) task.setSlaSeconds(ServiceLevelAgreement.unitsToSeconds(sla, slaUnits));
        else task.setSlaSeconds(0);
        String alertInterval = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_ALERT_INTERVAL);
        int alertSecs;
        if (alertInterval!=null && alertInterval.trim().length()>0) {
            String alertIntervalUnits = actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_ALERT_INTERVAL_UNITS);
            if (StringHelper.isEmpty(alertIntervalUnits)) alertIntervalUnits = ServiceLevelAgreement.INTERVAL_MINUTES;
            alertSecs = ServiceLevelAgreement.unitsToSeconds(alertInterval, alertIntervalUnits);
        } else alertSecs = 0;
        task.setAttribute(TaskAttributeConstant.ALERT_INTERVAL,alertSecs == 0 ? null : String.valueOf(alertSecs));
        task.setVariablesFromString(actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_VARIABLES), mainProcess.getVariables());
        task.setUserGroupsFromString(actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_GROUPS));
        task.setTaskTypeId(getDatabaseVersion()>=DataAccess.schemaVersion51?TaskType.TASK_TYPE_TEMPLATE:TaskType.TASK_TYPE_WORKFLOW);
        if (getDatabaseVersion()>=DataAccess.schemaVersion51 && task.isAutoformTask()) {
            task.setAttribute(TaskAttributeConstant.VARIABLES, actVO.getAttribute(TaskActivity.ATTRIBUTE_TASK_VARIABLES));
        }
        persistTask(task, persistType, getCategoryCode2IdMap(), batch);
    }

    protected void saveTaskDefinition(ActivityVO actVO, Long actId, ProcessVO mainProcess, AttributeBatch batch)
            throws SQLException,DataAccessException {
        TaskVO task = this.getTask(actId, getCategoryId2CatMap());
        saveTaskDefinitionSub(actVO, actId, task!=null, mainProcess, batch);
    }

    protected List<ActivityImplementorVO> getActivityImplementorVOsForProcess(Long pProcId)
            throws SQLException {
        String query = "select distinct act.ACTIVITY_IMPL_ID"
            + " from WORK_TRANSITION wt, ACTIVITY act"
            + " where wt.PROCESS_ID = ?  and"
            + " ( wt.TO_WORK_ID = act.ACTIVITY_ID or wt.FROM_WORK_ID = act.ACTIVITY_ID ) and"
            + " wt.EFF_END_DT is NULL";
        ResultSet rs = db.runSelect(query, pProcId);
        List<ActivityImplementorVO> retVOs = new ArrayList<ActivityImplementorVO>();
        while (rs.next()) {
            ActivityImplementorVO vo = implementorId2Obj.get(new Long(rs.getLong(1)));
            if (vo != null && !retVOs.contains(vo))
                retVOs.add(vo);
        }
        return retVOs;
    }

    private List<Long> getAllDirectSubProcessForProcess(Long pProcessId) throws SQLException {
        List<Long> processList = null;
        String query = "SELECT distinct wr.WORK_ID"
            + " FROM WORK wr, PROCESS pr, WORK_TRANSITION wt"
            + " WHERE wt.PROCESS_ID = ? AND wt.TO_WORK_ID = pr.PROCESS_ID"
            + " AND wr.work_id = pr.process_id AND wt.EFF_END_DT is NULL";
        ResultSet rs = db.runSelect(query, pProcessId);
        while (rs.next()) {
            if (processList==null) processList = new ArrayList<Long>();
            processList.add(new Long(rs.getLong(1)));
        }
        return processList;
    }


    private List<PoolVO> loadPoolsForPackage(Long packageId)
            throws SQLException {
        String query = "SELECT POOL_ID, POOL_NAME, PROCESS_ID"
            + " FROM POOL where PACKAGE_ID=?";
        ResultSet rs = db.runSelect(query, packageId);
        List<PoolVO> list = new ArrayList<PoolVO>();
        while (rs.next()) {
            PoolVO poolVO = new PoolVO();
            poolVO.setPoolId(new Long(rs.getLong(1)));
            poolVO.setPoolName(rs.getString(2));
            poolVO.setProcessId(new Long(rs.getLong(3)));
            poolVO.setPackageVO(null);
            list.add(poolVO);
        }
        for (PoolVO poolVO : list) {
            poolVO.setLanes(loadLanesForPool(poolVO.getPoolId()));
        }
        return list;
    }

    private List<LaneVO> loadLanesForPool(Long poolId)
            throws SQLException {
        String query = "SELECT LANE_ID, LANE_NAME"
            + " FROM LANE where POOL_ID=?";
        ResultSet rs = db.runSelect(query, poolId);
        List<LaneVO> listVO = new ArrayList<LaneVO>();
        while (rs.next()) {
            LaneVO laneVO = new LaneVO();
            laneVO.setLaneId(new Long(rs.getLong(1)));
            laneVO.setLaneName(rs.getString(2));
            laneVO.setPool(null);
            listVO.add(laneVO);
        }
        for (LaneVO lane : listVO) {
            List<AttributeVO> attrs = this.getAttributes0(OwnerType.LANE, lane.getLaneId());
            lane.setAttributes(attrs);
        }
        return listVO;
    }

    ///////////////////// Persister Portion


    /**
         * Gets the work instances for a particular work id This method shall be used by the BPM
         * Designer
         * @param packageVO
         * @param overwrite when false, check if any process/package version
         *      already exists; when true, overwrite existing versions
         * @return Collection of Work Instances
         * @throws DataAccessException
         *
         */
    public Long persistPackage(PackageVO packageVO, PersistType persistType)
        throws DataAccessException {
        // persist activity implementors
        try {
            db.openConnection();
            if (persistType==PersistType.IMPORT) {
                checkVersions(packageVO);
            }

            // persist package itself and pools/lanes/attributes
            if (packageVO.getVersion()>0 || persistType!=PersistType.IMPORT) {
                Long package_id = persistPackageProper(packageVO, persistType);
                packageVO.setPackageId(package_id);
                persistAttributes(OwnerType.PACKAGE, package_id,
                        packageVO.getAttributes(), null);
            }

            // persist activity implementor, external events, participants (MDW5), scripts (MDW5)
            // and their mappings to package
            loadExistingImplementors();
            persistImplementors(packageVO, persistType);
            persistExternalEvents(packageVO, persistType);
            persistParticipants(packageVO, persistType);
            persistScripts(packageVO, persistType);

            if (persistType==PersistType.IMPORT) {

                // persist processes
                for (ProcessVO processVO : packageVO.getProcesses()) {
                    if (!processVO.isLoaded()) continue;    // existing version but need to include in package
                    AttributeBatch batch = new AttributeBatch();
                    Long dbProcessID = persistProcess0(processVO, PersistType.NEW_VERSION, processVO, batch);
                    processNametoProcessId.put(processVO.getProcessName(), dbProcessID);
                    batch.execute(db, dbProcessID);
                }

                persistCustomAttributes(packageVO);
            }

            // persist package process mapping
            if (packageVO.getVersion()>0 || persistType!=PersistType.IMPORT) {
                persistPackageProcessMapping(packageVO, persistType);
            }

            if (persistType==PersistType.IMPORT &&
                    getSupportedVersion()<DataAccess.schemaVersion52) {
                // check for calls to independent processes
                // this is after persisting attributes w/o using batch
                // Note: this is no longer needed at/after 4.4.08 - kept
                // for importing processes into ealier version of engine
                for(ProcessVO processVO: packageVO.getProcesses()) {
                    if (processVO.getActivities()==null) continue;  // existing process
                    if (processVO.isInRuleSet()) continue;
                    getIndependentSubProcessActivities(processVO);
                }
            }

            db.commit();
            return packageVO.getPackageId();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to persist the package", e);
        } finally {
            db.closeConnection();
        }
    }

    public long renamePackage(Long packageId, String newName, int newVersion) throws DataAccessException {
        try {
            db.openConnection();
            String query = "update " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " set PACKAGE_NAME = ?, DATA_VERSION = ?\n"
                         + "where PACKAGE_ID = ?";
            db.runUpdate(query, new String[] {newName, String.valueOf(newVersion), packageId.toString()});
            db.commit();
            return packageId;
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to rename the package", e);
        }
        finally {
            db.closeConnection();
        }
    }

    public long addProcessToPackage(Long processId, Long packageId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "insert into PACKAGE_PROCESS (PROCESS_ID, PACKAGE_ID) values (?, ?)\n";
            db.runUpdate(query, new String[] {processId.toString(), packageId.toString()});
            db.commit();
            return processId;
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to add process: " + processId + " to package: " + packageId, e);
        }
        finally {
            db.closeConnection();
        }
    }

    public void removeProcessFromPackage(Long processId, Long packageId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from PACKAGE_PROCESS where PROCESS_ID = ? AND PACKAGE_ID = ?\n";
            db.runUpdate(query, new String[] {processId.toString(), packageId.toString()});
            db.commit();
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to remove process: " + processId + " from package: " + packageId, e);
        }
        finally {
            db.closeConnection();
        }
    }

    public long addExternalEventToPackage(Long externalEventId, Long packageId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "insert into PACKAGE_EXTERNAL_EVENTS (EXTERNAL_EVENT_ID, PACKAGE_ID) values (?, ?)\n";
            db.runUpdate(query, new String[] {externalEventId.toString(), packageId.toString()});
            db.commit();
            return externalEventId;
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to add external event: " + externalEventId + " to package: " + packageId, e);
        }
        finally {
            db.closeConnection();
        }
     }

    public void removeExternalEventFromPackage(Long externalEventId, Long packageId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from PACKAGE_EXTERNAL_EVENTS where EXTERNAL_EVENT_ID = ? AND PACKAGE_ID = ?\n";
            db.runUpdate(query, new String[] {externalEventId.toString(), packageId.toString()});
            db.commit();
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to remove external event: " + externalEventId + " from package: " + packageId, e);
        }
        finally {
            db.closeConnection();
        }
    }

    public long addActivityImplToPackage(Long activityImplId, Long packageId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "insert into PACKAGE_ACTIVITY_IMPLEMENTORS (ACTIVITY_IMPLEMENTOR_ID, PACKAGE_ID) values (?, ?)\n";
            db.runUpdate(query, new String[] {activityImplId.toString(), packageId.toString()});
            db.commit();
            return activityImplId;
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to add activity impl: " + activityImplId + " to package: " + packageId, e);
        }
        finally {
            db.closeConnection();
        }
     }

    public void removeActivityImplFromPackage(Long activityImplId, Long packageId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from PACKAGE_ACTIVITY_IMPLEMENTORS where ACTIVITY_IMPLEMENTOR_ID = ? AND PACKAGE_ID = ?\n";
            db.runUpdate(query, new String[] {activityImplId.toString(), packageId.toString()});
            db.commit();
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to remove activity impl: " + activityImplId + " from package: " + packageId, e);
        }
        finally {
            db.closeConnection();
        }
    }

    public long addRuleSetToPackage(Long ruleSetId, Long packageId) throws DataAccessException {
      try {
            db.openConnection();
            String query = "insert into PACKAGE_RULESETS (RULE_SET_ID, PACKAGE_ID) values (?, ?)\n";
            db.runUpdate(query, new String[] {ruleSetId.toString(), packageId.toString()});
            db.commit();
            return ruleSetId;
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to add ruleset: " + ruleSetId + " to package: " + packageId, e);
        }
        finally {
            db.closeConnection();
        }
   }

    public void removeRuleSetFromPackage(Long ruleSetId, Long packageId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from PACKAGE_RULESETS where RULE_SET_ID = ? AND PACKAGE_ID = ?\n";
            db.runUpdate(query, new String[] {ruleSetId.toString(), packageId.toString()});
            db.commit();
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to remove ruleset: " + ruleSetId + " from package: " + packageId, e);
        }
        finally {
            db.closeConnection();
        }
    }

  /**
     * Connects Independent SubProcess Activities (1 .. N) to sub process.
     * Depends on an attribute "processname" to find the processId for the process name
     * If referred sub-prrocess is in same package, then looks local map object
     * else looks up database . If an attribute "processversion is passed along the tries to get
     * processId for that version else gets the latest
     * @param processVO
     * @return
     * @throws DataAccessException
     *
     */

    private void getIndependentSubProcessActivities(ProcessVO processVO) throws SQLException {
        for (ActivityVO activityVO : processVO.getActivities()) {
            setProcessIdForInvokeProcessActivity(activityVO);
        }
        if (processVO.getSubProcesses()!=null) {
            for (ProcessVO subproc: processVO.getSubProcesses()) {
                for (ActivityVO activityVO : subproc.getActivities()) {
                    setProcessIdForInvokeProcessActivity(activityVO);
                }
            }
        }
    }

    private void setProcessIdForInvokeProcessActivity(ActivityVO activityVO) throws SQLException {
        String subProcessName = activityVO.getAttribute(WorkAttributeConstant.PROCESS_NAME);
        String subProcessVersion = activityVO.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
        if (subProcessName!=null && subProcessVersion!=null) {
            // used to instantiate the class - that does not work with non-server mode
            Long potentialSubProcessId =  processNametoProcessId.get(subProcessName);
            if (potentialSubProcessId == null) {
                int subProcVersion = Integer.parseInt(subProcessVersion);
                if (subProcVersion > 0)
                    potentialSubProcessId = this.processVersionExist(subProcessName, subProcVersion);
            }
            activityVO.setAttribute("processid", (potentialSubProcessId!=null)?potentialSubProcessId.toString():null);
            activityVO.setAttribute(WorkAttributeConstant.ALIAS_PROCESS_ID, null);
            this.persistAttributes(OwnerType.ACTIVITY,
                    workNameRef.get(activityVO.getActivityId()),
                    activityVO.getAttributes(), null);
        }
    }

    protected Long persistPackageProper(PackageVO packageVO, PersistType persistType)
            throws SQLException, DataAccessException, XmlException {
        if (packageVO.getSchemaVersion() == 0)
            packageVO.setSchemaVersion(DataAccess.currentSchemaVersion);
        if (persistType==PersistType.NEW_VERSION) {
            int version = this.getCurrentVersionForPackage(packageVO.getPackageName());
            packageVO.setVersion(version+1);
            packageVO.setExported(false);
        }
        String query = null;
        Long packageId = packageVO.getPackageId();
        String groupName = packageVO.getGroup();
        if (groupName != null) {
            if (groupName.equals("") || groupName.equals(UserGroupVO.COMMON_GROUP)) groupName = null;
        }
        if (groupName != null) {
            groupName = "'"+groupName+"'";
        }
        if (persistType==PersistType.UPDATE) {
            query = "update " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " "
              + "set PACKAGE_ID = ?, "
              + "PACKAGE_NAME = ?, "
              + "SCHEMA_VERSION = ?, "
              + "DATA_VERSION = ?, "
              + "MOD_DT = " + now() + ", "
              + (DataAccess.isPackageLevelAuthorization ?   "GROUP_NAME = "+groupName+ ", " : "")
              + "EXPORTED_IND = ? "
              + "WHERE PACKAGE_ID = " + packageId;
            Object[] args = new Object[5];
            args[0] = packageId;
            args[1] = packageVO.getPackageName();
            args[2] = new Integer(packageVO.getSchemaVersion());
            args[3] = new Integer(packageVO.getVersion());
            args[4] = packageVO.isExported() ? new Integer(1) : new Integer(0);
            db.runUpdate(query, args);
        } else {
            packageId = db.isMySQL()?null:this.getNextId("MDW_COMMON_ID_SEQ");
            packageVO.setPackageId(packageId);
            query = "insert into " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " "
                  + "(PACKAGE_ID, PACKAGE_NAME, SCHEMA_VERSION, DATA_VERSION, MOD_DT, "
                  +  (DataAccess.isPackageLevelAuthorization ?   "GROUP_NAME, " : "")
                  + "EXPORTED_IND) "
                  + "values (?, ?, ?, ?, " + now() + (DataAccess.isPackageLevelAuthorization ? ","+groupName : "") +", ?)";
            Object[] args = new Object[5];
            args[0] = packageId;
            args[1] = packageVO.getPackageName();
            args[2] = new Integer(packageVO.getSchemaVersion());
            args[3] = new Integer(packageVO.getVersion());
            args[4] = packageVO.isExported() ? new Integer(1) : new Integer(0);
            if (db.isMySQL()) packageId = db.runInsertReturnId(query, args);
            else db.runUpdate(query, args);
        }

        if (getSupportedVersion()<DataAccess.schemaVersion52)
            persistPools(packageVO);
        return packageId;
    }

    protected void persistPools(PackageVO packageVO) throws SQLException {
        // not supported in V4
    }

    public Long persistProcess(ProcessVO processVO, PersistType persistType) throws DataAccessException {
        try {
            AttributeBatch batch = new AttributeBatch();
            db.openConnection();
            if (persistType.equals(PersistType.CREATE)) {
                String sql;
                if (getSupportedVersion() >= DataAccess.schemaVersion52 || processVO.isInRuleSet()) {
                    sql = "select rule_set_id from rule_set where rule_set_name = '" + processVO.getName() + "' and language = '" + RuleSetVO.PROCESS + "'";
                }
                else {
                    sql = "select * from work where work_name = '" + processVO.getName() + "' and work_type = " + WorkType.WORK_TYPE_PROCESS;
                }
                ResultSet rs = db.runSelect(sql, null);
                if (rs.next())
                    throw new DataAccessException("Process already exists: '" + processVO.getName() + "'");
            }
            loadExistingImplementors();
            Long procId = persistProcess0(processVO, persistType, processVO, batch);
            batch.execute(db, procId);
            db.commit();
            return procId;
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to persist the process", e);
        } finally {
            db.closeConnection();
        }
    }

    public long renameProcess(Long processId, String newName, int newVersion) throws DataAccessException {
        try {
            db.openConnection();
            String query = "update WORK set WORK_NAME = ? where WORK_ID = ?";
            db.runUpdate(query, new String[] {newName, processId.toString()});
            query = "update PROCESS set VERSION_NO = ? where PROCESS_ID = ?";
            db.runUpdate(query, new String[] {String.valueOf(newVersion), processId.toString()});
            db.commit();
            return processId;
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to rename the process", e);
        }
        finally {
            db.closeConnection();
        }
    }

    /**
     * When version number is 0:
     *   - CREATE: automatically create a version number
     *   - UPDATE: ignore version - update based on process ID
     *   - NEW_VERSION: increment the latest version number.
     * When version number is greater than 0:
     *   - CREATE: create the given version
     *   - UPDATE: ignore version - update based on process ID
     *   - NEW_VERSION: try to use the version; return exception
     *          if the newest version number is greater than it.
     * @param processVO
     * @param persistType
     * @return
     * @throws DataAccessException
     */
    protected Long persistProcess0(ProcessVO processVO, PersistType persistType,
                ProcessVO mainProcess, AttributeBatch batch)
            throws SQLException,DataAccessException,XmlException {
        int version = processVO.getVersion();
        int actualVersion;
        switch (persistType) {
        case CREATE:
            if (version==0) actualVersion = 1;
            else actualVersion = version;
            break;
        case UPDATE:
        case SAVE:
            actualVersion = version;
            break;
        case NEW_VERSION:
            if (version==0) {
                actualVersion = getCurrentVersionForProcess(processVO.getProcessName());
                actualVersion = actualVersion+1;
            } else {
                actualVersion = version;
            }
            break;
        default: actualVersion = version;  // should never reach here
            break;
        }
//        System.out.println("Persist starts ... ");
        if (this.implementorName2Obj==null) loadExistingImplementors();
        Long process_id;
        if (getSupportedVersion()>=DataAccess.schemaVersion52) {
            saveProcessInRuleSet(processVO, processVO.getProcessId(), persistType, actualVersion, batch);
            process_id = processVO.getProcessId();
            this.workNameRef.put(process_id, process_id);   // needed for package-process mapping
            return process_id;
        } else {
            process_id = persistProcessBase(processVO, persistType, actualVersion,
                    processVO==mainProcess?null:mainProcess.getProcessName());
            if (processVO.isInRuleSet())  {
                processVO.setProcessId(process_id);
                saveProcessInRuleSet(processVO, process_id, persistType, actualVersion, batch);
                this.workNameRef.put(process_id, process_id);   // needed for package-process mapping
                return process_id;
            }
        }
        this.workNameRef.put(processVO.getProcessId(), process_id);
        if (persistType==PersistType.UPDATE || persistType==PersistType.SAVE) {
            updateVariables(processVO.getVariables(), OwnerType.PROCESS, process_id);
        } else {
            addVariables(processVO.getVariables(), process_id, OwnerType.PROCESS);
        }
        if (persistType==PersistType.UPDATE || persistType==PersistType.SAVE) {
            this.updateSubProcesses(processVO.getSubProcesses(), actualVersion, processVO, batch);
        } else {
            this.createSubProcesses(processVO.getSubProcesses(),
                    persistType,version,actualVersion, processVO, batch);
        }
//        System.out.println("Persist activities starts ... ");
        for (ActivityVO node : processVO.getActivities()) {
            Long activity_id = node.getActivityId();
            if ((persistType==PersistType.UPDATE || persistType==PersistType.SAVE)
                    && activity_id.longValue()>=0) {
                // existing activity - update name, implementor, attributes
                this.updateActivity(node, mainProcess, batch);
            } else {
                this.createActivity(node, persistType, mainProcess, batch);
            }
        }
//        System.out.println("Persist activites done .... ");
        if (persistType!=PersistType.UPDATE && persistType!=PersistType.SAVE) {  // TODO include this in isUpdate from designer
            this.createSynchronizations(processVO.getActivities());
            this.addAttributes(processVO.getAttributes(), process_id, OwnerType.PROCESS, batch);
        } else {
            this.updateSynchronizations(processVO.getActivities(), process_id);
            persistAttributes(OwnerType.PROCESS, processVO.getProcessId(),
                    processVO.getAttributes(), batch);
        }
        for(WorkTransitionVO connector : processVO.getTransitions()) {
            if ((persistType==PersistType.UPDATE || persistType==PersistType.SAVE)
                && connector.getWorkTransitionId()>0) { // existing link
                updateTransition(connector, batch);
            } else {
                createTransition(connector, process_id, batch);
            }
        }
        if (persistType!=PersistType.UPDATE && persistType!=PersistType.SAVE) {
            this.createExternalEventMapping(processVO.getExternalEvents(), process_id);
        }
        if (persistType==PersistType.UPDATE || persistType==PersistType.SAVE) {
            clearDeletedNodeAndLinks(null, processVO.getDeletedTransitions());
        }
//        System.out.println("Persist complete!!! ");
        return process_id;
    }

    private void clearDeletedNodeAndLinks(
            List<Long> deletedNodes, List<Long> deletedLinks)
            throws SQLException {
        if (deletedLinks!=null) {
            String query = "delete from WORK_TRANSITION where WORK_TRANS_ID=?";
            db.prepareStatement(query);
            for (Long id : deletedLinks) {
                db.runUpdateWithPreparedStatement(id);
            }
        }
        if (deletedNodes!=null) {
            String query = "delete from WORK where WORK_ID=?";
            db.prepareStatement(query);
            for (Long id : deletedNodes) {
                db.runUpdateWithPreparedStatement(id);
            }
            query = "delete from ACTIVITY where ACTIVITY_ID=?";
            db.prepareStatement(query);
            for (Long id : deletedNodes) {
                db.runUpdateWithPreparedStatement(id);
            }
        }
    }

    private Long createImplementor0(ActivityImplementorVO ai, AttributeBatch batch)
            throws SQLException {
        String implClass = ai.getImplementorClassName();
        String query = "insert into ACTIVITY_IMPLEMENTOR"
            + " (ACTIVITY_IMPLEMENTOR_ID,IMPL_CLASS_NAME) values (?,?)";
        Long id = db.isMySQL()?null:this.getNextId("MDW_COMMON_ID_SEQ");
        Object[] args = new Object[2];
        args[0] = id;
        args[1] = implClass;
        if (db.isMySQL()) id = db.runInsertReturnId(query, args);
        else db.runUpdate(query, args);
        ai.setImplementorId(id);
        if (implementorName2Obj == null)
          loadExistingImplementors();
        this.implementorName2Obj.put(implClass, ai);
        if (ai.getAttributeDescription()!=null) {
            List<AttributeVO> attrs = new ArrayList<AttributeVO>(5);
            attrs.add(new AttributeVO(ATTRIBUTE_ATTRDESC, ai.getAttributeDescription()));
            attrs.add(new AttributeVO(ATTRIBUTE_LABEL, ai.getLabel()));
            attrs.add(new AttributeVO(ATTRIBUTE_ICONNAME, ai.getIconName()));
            attrs.add(new AttributeVO(ATTRIBUTE_BASECLASS, ai.getBaseClassName()));
            this.addAttributes(attrs, id, OwnerType.ACTIVITY_IMPLEMENTOR, batch);
        }
        return id;
    }

    /**
     * This can be called by persistPackage or by persistProcess.
     */
    protected void loadExistingImplementors() throws SQLException {
        implementorName2Obj = new HashMap<String,ActivityImplementorVO>();
        implementorId2Obj = new HashMap<Long,ActivityImplementorVO>();
        List<ActivityImplementorVO> retVOs = getAllActivityImplementors0();
        for (ActivityImplementorVO aImplementor : retVOs) {
            implementorName2Obj.put(aImplementor.getImplementorClassName(), aImplementor);
            implementorId2Obj.put(aImplementor.getImplementorId(), aImplementor);
        }
    }

    protected ActivityImplementorVO getActivityImpl(ActivityVO activity) throws SQLException {
        if (this.implementorName2Obj == null)
            loadExistingImplementors();
        return implementorName2Obj.get(activity.getImplementorClassName());
    }

    private void deleteExternalEventMapping(String eventName) throws SQLException {
        String query = "delete from EXT_EVENT_PROCESS_MAPP where EXT_EVENT_ID" +
                " in (select EXTERNAL_EVENT_ID from EXTERNAL_EVENT where EVENT_NAME=?)";
        db.runUpdate(query, eventName);
    }

    /**
         * Method that maps the passed in external event to the process
         * @param pEvents
         * @param pProcId
         * @throws DataAccessException
         */
    private void createExternalEventMapping(List<ExternalEventVO> pEvents, Long pProcId)
        throws SQLException {
        if (pEvents == null) return;
        String query = "insert into EXT_EVENT_PROCESS_MAPP"
            + " (EXT_EVENT_PROCESS_MAPP_ID,EXT_EVENT_ID,PROCESS_ID,START_DT)"
            + " values (MDW_COMMON_ID_SEQ.NEXTVAL,?,?,SYSDATE)";
        Object[] args = new Object[2];
        args[1] = pProcId;
        for (ExternalEventVO vo : pEvents) {
            deleteExternalEventMapping(vo.getEventName());
            Long extEventId = createExternalEvent0(vo);
            args[0] = extEventId;
            db.runUpdate(query, args);
        }
    }

    private ExternalEventVO getExternalEvent(String eventPattern) throws SQLException {
        String query = "select EXTERNAL_EVENT_ID, EVENT_NAME, EVENT_HANDLER from EXTERNAL_EVENT where EVENT_NAME=?";
        ResultSet rs = db.runSelect(query, eventPattern);
        if (rs.next()) {
            ExternalEventVO eeVO = new ExternalEventVO();
            eeVO.setId(new Long(rs.getLong(1)));
            eeVO.setEventName(rs.getString(2));
            eeVO.setEventHandler(rs.getString(3));
            return eeVO;
        }
        else return null;
    }

    private Long createExternalEvent0(ExternalEventVO vo) throws SQLException {
        ExternalEventVO eeVO = getExternalEvent(vo.getEventName());
        if (eeVO != null) {
            if (!eeVO.getEventHandler().equals(vo.getEventHandler())) {
                // update the handler value
                String query = "update EXTERNAL_EVENT set EVENT_HANDLER = ? where EXTERNAL_EVENT_ID = ?";
                Object[] args = new Object[] {vo.getEventHandler(), eeVO.getId()};
                db.runUpdate(query, args);
            }
            return eeVO.getId();
        }
        Long id = db.isMySQL()?null:this.getNextId("MDW_COMMON_ID_SEQ");
        String query = "insert into EXTERNAL_EVENT " +
                "(EXTERNAL_EVENT_ID,EVENT_NAME,EVENT_HANDLER,CREATE_DT,CREATE_USR)" +
                " values (?,?,?,"+now()+",'ProcessLoader')";
        Object[] args = new Object[3];
        args[0] = id;
        args[1] = vo.getEventName();
        args[2] = vo.getEventHandler();
        if (db.isMySQL()) id = db.runInsertReturnId(query, args);
        else db.runUpdate(query, args);
        vo.setId(id);
        return id;
    }

    /**
         * Method that checks and creates the activity impls
         * @param pActImplArr
         */
    private void createTransition(WorkTransitionVO vo, Long pProcId, AttributeBatch batch)
        throws SQLException {

        String query = "insert into WORK_TRANSITION (WORK_TRANS_ID,PROCESS_ID,FROM_WORK_ID,"
            + " TO_WORK_ID,EVENT_TYPE_ID,WORK_COMP_CD,EFF_START_DT,EFF_END_DT)"
            + " values (?,?,?,?,?,?,SYSDATE,'')";
        Long id = this.getNextId("MDW_COMMON_ID_SEQ");
        Object[] args = new Object[6];
        args[0] = id;
        args[1] = pProcId;
        args[2] = this.workNameRef.get(vo.getFromWorkId());
        args[3] = this.workNameRef.get(vo.getToWorkId());
        args[4] = vo.getEventType().toString();
        args[5] = vo.getCompletionCode();
        db.runUpdate(query, args);
//        this.workTransRef.put(vo.getWorkTransitionId(), id);
        if (vo.getAttributes()!=null)
            this.addAttributes(vo.getAttributes(), id, OwnerType.WORK_TRANSITION, batch);
    }

    private void updateTransition(WorkTransitionVO vo, AttributeBatch batch)
                throws SQLException {
        Long wt_id = vo.getWorkTransitionId();
        String query = "update WORK_TRANSITION set FROM_WORK_ID=?,"
            + " TO_WORK_ID=?,EVENT_TYPE_ID=?,WORK_COMP_CD=?"
            + " where WORK_TRANS_ID=?";
        Object[] args = new Object[5];
        args[0] = this.workNameRef.get(vo.getFromWorkId());
        args[1] = this.workNameRef.get(vo.getToWorkId());
        args[2] = vo.getEventType().toString();
        args[3] = vo.getCompletionCode();
        args[4] = wt_id;
        db.runUpdate(query, args);
        persistAttributes(OwnerType.WORK_TRANSITION,
                wt_id, vo.getAttributes(), batch);
//        this.workTransRef.put(wt_id, wt_id);
    }

    /**
         * Method that creates the activities
         * @param mDWActivity arr
         *
         */
    protected void createActivity(ActivityVO a, PersistType persistType, ProcessVO mainProcess, AttributeBatch batch)
            throws SQLException,DataAccessException {
        ActivityImplementorVO impl = this.implementorName2Obj.get(a.getImplementorClassName());
        String actName = a.getActivityName();
        if (impl == null) {
            String msg = "Failed to locate the Impl Class Id for Activity:" + actName
            + " ImplClass:" + a.getImplementorClassName();
//            logger.warn(msg);
            throw new DataAccessException(msg);
        }
        Long id = this.getNextId("MDW_COMMON_ID_SEQ");
        String query = "insert into WORK (WORK_ID,WORK_NAME,WORK_TYPE,COMMENTS)" +
                " values (?,?,?,?)";
        Object[] args = new Object[4];
        args[0] = id;
        args[1] = actName;
        args[2] = WorkType.WORK_TYPE_ACTIVITY;
        args[3] = a.getActivityDescription();
        db.runUpdate(query, args);

        query = "insert into ACTIVITY (ACTIVITY_ID, ACTIVITY_IMPL_ID) values (?,?)";
        Object[] args2 = new Object[2];
        args2[0] = id;
        args2[1] = impl.getImplementorId();
        db.runUpdate(query, args2);
        Long actId = new Long(id);
        this.workNameRef.put(a.getActivityId(), actId);
//      super.addServiceLevelAgreement(a.getSla(), act.getId(), OwnerType.ACTIVITY);
        persistSla(actId, OwnerType.ACTIVITY, a.getSlaSeconds(), persistType);
        if (impl.isManualTask())
            saveTaskDefinition(a, actId, mainProcess, batch);
        this.addAttributes(a.getAttributes(), actId, OwnerType.ACTIVITY, batch);
    }

    protected void updateActivity(ActivityVO node, ProcessVO mainProcess, AttributeBatch batch)
            throws SQLException,DataAccessException {
        // existing activity/process - update name, implementor
        ActivityImplementorVO implementor = this.implementorName2Obj.get(node.getImplementorClassName());
        if (implementor==null) {
            throw new DataAccessException("Cannot find implementor "+node.getImplementorClassName());
        }
        Long activity_id = node.getActivityId();
        String query = "update WORK set WORK_NAME=?, COMMENTS=? where WORK_ID=?";
        Object[] args = new Object[3];
        args[0] = node.getActivityName();
        args[1] = node.getActivityDescription();
        args[2] = activity_id;
        db.runUpdate(query, args);
        query = "update ACTIVITY set ACTIVITY_IMPL_ID=? where ACTIVITY_ID=?";
        Object[] args2 = new Object[2];
        args2[0] = implementor.getImplementorId();
        args2[1] = activity_id;
        db.runUpdate(query, args2);
        workNameRef.put(activity_id, activity_id);
        persistSla(activity_id, OwnerType.ACTIVITY, node.getSlaSeconds(), PersistType.UPDATE);
        if (implementor.isManualTask())
            saveTaskDefinition(node, activity_id, mainProcess, batch);
        persistAttributes(OwnerType.ACTIVITY, activity_id, node.getAttributes(), batch);
    }

    private Long createProcessBase(String procName, int version, String desc, boolean isInRuleSet)
        throws SQLException {
        Long id = this.getNextId("MDW_COMMON_ID_SEQ");
        Long process_id = new Long(id);
        String query = "insert into WORK (WORK_ID,WORK_NAME,WORK_TYPE,COMMENTS)" +
                " values (?,?,?,?)";
        Object[] args = new Object[4];
        args[0] = id;
        args[1] = procName;
        args[2] = WorkType.WORK_TYPE_PROCESS;
        args[3] = desc;
        db.runUpdate(query, args);

        query = "insert into PROCESS (PROCESS_ID, PROCESS_TYPE_ID, VERSION_NO) values (?,?,?)";
        Object[] args2 = new Object[3];
        args2[0] = id;
        args2[1] = isInRuleSet?ProcessVO.PROCESS_TYPE_ALIAS:ProcessVO.PROCESS_TYPE_CONCRETE;
        args2[2] = Integer.toString(version);
        db.runUpdate(query, args2);
        return process_id;
    }

    private String getProcessFullName(ProcessVO embeddedProc,
            String mainProcessName) {
        String processName = embeddedProc.getProcessName();
        if (mainProcessName!=null) {
            if (!processName.startsWith(mainProcessName))
                processName = mainProcessName + " " + processName;
        }
        return processName;
    }

    private Long persistProcessBase(ProcessVO processVO,
                PersistType persistType, int newVersion, String mainProcessName)
            throws SQLException {
        Long process_id = processVO.getProcessId();
        String processName = getProcessFullName(processVO, mainProcessName);
        if (persistType==PersistType.UPDATE || persistType==PersistType.SAVE) {
            String query = "update WORK set WORK_NAME=?, COMMENTS=?, MOD_DT=" + now() +
                    (persistType==PersistType.UPDATE?", MOD_USR='' ":"") +
                    " where WORK_ID=?";
            String[] args = new String[3];
            args[0] = processName;
            args[1] = processVO.getProcessDescription();
            args[2] = process_id.toString();
            db.runUpdate(query, args);
        } else {
            process_id = createProcessBase(processName, newVersion,
                    processVO.getProcessDescription(), processVO.isInRuleSet());
        }
//        this.processSet.add(pr);          // TODO
        return process_id;
    }

    private void createSla(String pOwnerType, Long pOwnerId, float slaHours) throws SQLException {
        String query = "insert into SLA (SLA_ID,SLA_OWNER,SLA_OWNER_ID,"
            + "SLA_HR,SLA_START_DT,SLA_END_DT)"
            + " values (MDW_COMMON_ID_SEQ.NEXTVAL,?,?,?,SYSDATE,'')";
        String[] args = new String[3];
        args[0] = pOwnerType;
        args[1] = pOwnerId.toString();
        args[2] = String.valueOf(slaHours);
        db.runUpdate(query, args);
    }

    protected void persistSla(Long pOwnerId, String pOwnerType,
            int newSlaSeconds, PersistType persistType)
        throws SQLException
    {
        float newSlaHours = (float)newSlaSeconds / 3600;
        int sla;
        String query;
        String[] args;
        if (persistType==PersistType.UPDATE || persistType==PersistType.SAVE) {
            sla = getServiceLevelAgreement(pOwnerType, pOwnerId);
            if (sla!=0) {
                if (sla!=newSlaHours) {
                    if (newSlaHours>0) {
                        query = "update SLA set SLA_HR=? where SLA_OWNER='"
                            + pOwnerType +"' and SLA_OWNER_ID=?";
                        args = new String[2];
                        args[0] = String.valueOf(newSlaHours);
                        args[1] = pOwnerId.toString();
                        db.runUpdate(query, args);
                    } else {
                        query = "delete SLA where SLA_OWNER=? and SLA_OWNER_ID=?";
                        args = new String[2];
                        args[0] = pOwnerType;
                        args[1] = pOwnerId.toString();
                        db.runUpdate(query, args);
                    }
                }
            } else {
                if (newSlaHours>0) createSla(pOwnerType, pOwnerId, newSlaHours);
            }
        } else {
            if (newSlaHours>0) createSla(pOwnerType, pOwnerId, newSlaHours);
        }
    }

    /**
         * Method that creates the synchronizations
         * @param mDWActivity arr
         *
         */

    protected void createSynchronizations(List<ActivityVO> pActArr)
            throws SQLException,DataAccessException {
        String query = "insert into WORK_SYNCHRONIZATION" +
                " (WORK_SYNCHRONIZATION_ID,ACTIVITY_ID,WORK_ID)" +
                " values (MDW_COMMON_ID_SEQ.NEXTVAL,?,?)";
        db.prepareStatement(query);
        Object[] args = new Object[2];
        for (ActivityVO a : pActArr) {
            Long[] expWorkIds = a.getSynchronzingIds();
            if (expWorkIds == null || expWorkIds.length == 0) {
                continue;
            }
            Long actId = (Long) this.workNameRef.get(a.getActivityId());
            args[0] = actId;
            if (actId == null) {
                String msg = "Failed to locate the DB Id for Synch Activity:"
                    + a.getActivityName();
//                logger.warn(msg);
                throw new DataAccessException(msg);
            }

            for (int j = 0; j < expWorkIds.length; j++) {
                Long workId = this.workNameRef.get(expWorkIds[j]);
                if (workId == null) {
//                    String msg = "Failed to locate the DB Id for Synching Activity:"
//                        + expWorkIds[j];
//                    logger.warn(msg);
                    // throw new DataAccessException(msg);
                    continue;
                }
                args[1] = workId;
                db.runUpdateWithPreparedStatement(args);
            }
        }
    }

    protected void updateSynchronizations(List<ActivityVO> actList, Long processId)
            throws DataAccessException,SQLException {
        String activityQuery = "select unique(ACTIVITY_ID) from ACTIVITY w, WORK_TRANSITION t " +
            "where (w.ACTIVITY_ID=t.FROM_WORK_ID or w.ACTIVITY_ID=t.TO_WORK_ID) and t.PROCESS_ID=?";
        String query = "delete from WORK_SYNCHRONIZATION where ACTIVITY_ID in (" + activityQuery + ")";
        db.runUpdate(query, processId);
        this.createSynchronizations(actList);
    }

    /**
         * Method that creates all the work's including processes, activities, sub processes and
         * aliases
         */
    private void createSubProcesses(List<ProcessVO> pSubProcs, PersistType persistType,
            int version, int actualVersion, ProcessVO mainProcess, AttributeBatch batch) throws SQLException,DataAccessException,XmlException {
        if (pSubProcs==null) return;
        for (ProcessVO p : pSubProcs) {
            String visibility = p.getAttribute(WorkAttributeConstant.PROCESS_VISIBILITY);
            if (visibility!=null && visibility.equals(ProcessVisibilityConstant.EMBEDDED))
                p.setVersion(actualVersion);
            else p.setVersion(version);
            this.persistProcess0(p, persistType, mainProcess, batch);
        }
    }

    private void updateSubProcesses(List<ProcessVO> pSubProcs, int actualVersion, ProcessVO mainProcess, AttributeBatch batch)
        throws DataAccessException,SQLException,XmlException {
        if (pSubProcs==null) return;
        for (ProcessVO p : pSubProcs) {
            String visibility = p.getAttribute(WorkAttributeConstant.PROCESS_VISIBILITY);
            if (visibility!=null && visibility.equals(ProcessVisibilityConstant.EMBEDDED)) {
                if (p.getProcessId().longValue()>0) {
                    this.persistProcess0(p, PersistType.UPDATE, mainProcess, batch);
                } else {
                    p.setVersion(actualVersion);
                    this.persistProcess0(p, PersistType.CREATE, mainProcess, batch);
                }
            } else {
                this.workNameRef.put(p.getProcessId(), p.getProcessId());
                persistAttributes(OwnerType.PROCESS, p.getProcessId(), p.getAttributes(), batch);
            }
        }
    }

    private void deleteAttributes(String ownerType, Long ownerId, AttributeBatch batch)
            throws SQLException {
        if (batch!=null) {
            Object[] args = new Object[2];
            args[0] = ownerType;
            args[1] = ownerId;
            batch.addDelete(args);
        } else {
            super.deleteAttributes0(ownerType, ownerId);
            super.deleteOverflowAttributes(ATTRIBUTE_OVERFLOW_PREFIX + ownerId + "_%");
        }
    }

    protected void persistAttributes(String ownerType, Long ownerId,
            List<AttributeVO> attributes, AttributeBatch batch)
            throws SQLException {
        deleteAttributes(ownerType, ownerId, batch);
        if (attributes!=null)
            this.addAttributes(attributes, ownerId, ownerType, batch);
    }

    private Long createVariable(String name, Long typeId)
        throws SQLException {
        String query = "insert into VARIABLE" +
                " (VARIABLE_ID,VARIABLE_NAME,VARIABLE_TYPE_ID)" +
                " values (?,?,?)";
        Object[] args = new Object[3];
        Long id = this.getNextId("MDW_COMMON_ID_SEQ");
        args[0] = id;
        args[1] = name;
        args[2] = typeId;
        db.runUpdate(query, args);
        return new Long(id);
    }

    protected void createVariableMapping(Long ownerId, String ownerType, Long variableId, VariableVO vo)
            throws SQLException {
        String query = "insert into VARIABLE_MAPPING" +
                " (VARIABLE_MAPPING_ID,MAPPING_OWNER,MAPPING_OWNER_ID,VARIABLE_ID," +
                " VAR_REFERRED_AS,VARIABLE_DATA_SOURCE,VARIABLE_DATA_OPT_IND,DISPLAY_SEQ,COMMENTS)" +
                " values (MDW_COMMON_ID_SEQ.NEXTVAL,?,?,?,?,?,?,?,?)";
        Object[] args = new Object[8];
        args[0] = ownerType;
        args[1] = ownerId;
        args[2] = variableId;
        args[3] = vo.getVariableReferredAs();
        args[4] = VariableVO.DATA_SOURCE_OTHERS;
        args[5] = vo.getDisplayMode();
        args[6] = vo.getDisplaySequence();
        args[7] = vo.getDescription();
        db.runUpdate(query, args);
    }

    protected void updateVariableMapping(String ownerType, Long ownerId,
            Long oldVarId, VariableVO var) throws SQLException {
        String query = "update VARIABLE_MAPPING" +
                " set VAR_REFERRED_AS=?, VARIABLE_DATA_OPT_IND=?," +
                " VARIABLE_DATA_SOURCE=?, DISPLAY_SEQ=?, VARIABLE_ID=?, COMMENTS=? " +
                " where MAPPING_OWNER=? and MAPPING_OWNER_ID=? and VARIABLE_ID=?";
        Object[] args = new Object[9];
        args[0] = var.getVariableReferredAs();
        args[1] = var.getDisplayMode();
        args[2] = VariableVO.DATA_SOURCE_OTHERS;
        args[3] = var.getDisplaySequence();
        args[4] = var.getVariableId();
        args[5] = var.getDescription();
        args[6] = ownerType;
        args[7] = ownerId;
        args[8] = oldVarId;
        db.runUpdate(query, args);
    }

    /**
     *
     * @param variables
     * @param pOwnerID   Currently only process ID
     * @param pOwnerType Currently always "PROCESS"
     * @throws DataAccessException
     */
    private void updateVariables(List<VariableVO> variables, String ownerType, Long ownerId)
        throws SQLException {
        List<VariableVO> oldVariables = this.getVariablesForOwner(ownerType, ownerId);
        Map<Long,VariableVO> existing = new HashMap<Long,VariableVO>();
        for (VariableVO oldVar : oldVariables) {
            boolean found = false;
            for (VariableVO var : variables) {
                if (var.getVariableId()==null) continue;
                if (oldVar.getVariableId().equals(var.getVariableId())) {
                    found = true;
                    existing.put(oldVar.getVariableId(), oldVar);
                    break;
                }
            }
            if (!found) deleteVariable(oldVar, ownerType, ownerId);
        }
        for (VariableVO var : variables) {
            if (var.getVariableId()==null) addVariable(var, ownerType, ownerId);
            else if (existing.containsKey(var.getVariableId()))
                updateVariable(var, existing.get(var.getVariableId()), ownerType, ownerId);
            else addVariable(var, ownerType, ownerId);
        }
    }

    private Long processVersionExist(String name, int version)
            throws SQLException {
        String query;
        if (getSupportedVersion()>=DataAccess.schemaVersion52) {
            query = "select RULE_SET_ID from RULE_SET"
                + " where VERSION_NO=? and RULE_SET_NAME=?";
        } else {
            query = "select p.PROCESS_ID from PROCESS p, WORK w"
                + " where p.PROCESS_ID=w.WORK_ID and p.VERSION_NO=? and w.WORK_NAME=?";
        }
        Object[] args = new Object[2];
        args[0] = new Integer(version);
        args[1] = name;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) return new Long(rs.getLong(1));
        else return null;
    }

    private Long packageVersionExist(String name, int version)
            throws SQLException {
        String query = "select PACKAGE_ID from " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " "
            + " where PACKAGE_NAME=? and DATA_VERSION=?";
        Object[] args = new Object[2];
        args[0] = name;
        args[1] = new Integer(version);
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) return new Long(rs.getLong(1));
        else return null;
    }

    private void checkVersionSub(ProcessVO processVO, StringBuffer sb, String mainProcessName)
        throws SQLException
    {
        int version = processVO.getVersion();
        Long existingProcessId;
        if (version==0) existingProcessId = null;
        else {
            String processName = getProcessFullName(processVO, mainProcessName);
            existingProcessId = processVersionExist(processName, version);
        }
        if (existingProcessId!=null && !processVO.isLoaded()) {
            // existing version to be included in package mapping, so do not check
            this.workNameRef.put(processVO.getProcessId(), existingProcessId);
        } else if (existingProcessId!=null) {
            if (sb.length()==0) {
                sb.append("The following process version(s) cannot be imported because the same or later version already exists:\n");
            }
            sb.append(processVO.getProcessName());
            sb.append(": version ");
            sb.append(createVersionString(version));
            sb.append("\n");
        }
        if (processVO.getSubProcesses()!=null && processVO.getActivities()!=null) {
            for (ProcessVO subprocVO : processVO.getSubProcesses()) {
                checkVersionSub(subprocVO, sb, processVO.getProcessName());
            }
        }
    }

    private void checkVersions(PackageVO packageVO)
        throws SQLException,DataAccessException
    {
        StringBuffer sb = new StringBuffer();
        int version = packageVO.getVersion();
        boolean exist;
        if (version==0) exist = false;
        else {
            Long procId = packageVersionExist(packageVO.getPackageName(), version);
            exist = procId!=null;
        }
        for (ProcessVO processVO : packageVO.getProcesses()) {
            checkVersionSub(processVO, sb, null);
        }
        if (exist) {
            sb.append("The package version " + createVersionString(version) + " already exist\n");
        }
        if (sb.length()>0) {
            throw new DataAccessException(sb.toString());
        }
    }

    private String createVersionString(int version) {
        return "" + (version/1000) + "." + (version%1000);
    }

    /**
     * Add the attributes for a specific process/ process alias.
     * @param pAttributes the Attribute Array
     * @param pName process name/ process alias name
     */
    protected void addAttributes(List<AttributeVO> pAttributes, Long pOwnerId, String pOwner, AttributeBatch batch)
        throws SQLException {
        if (pAttributes != null) {
            if (batch!=null) {
                Object[] args;
                for (AttributeVO vo : pAttributes) {
                    String v = vo.getAttributeValue();
                    if (v==null||v.length()==0) continue;
                    args = new Object[4];
                    args[0] = pOwner;
                    args[1] = pOwnerId;
                    args[2] = vo.getAttributeName();
                    args[3] = v;
                    batch.addInsert(args);
                }
            } else {
                super.addAttributes0(pOwner, pOwnerId, pAttributes);
            }
        }
    }

    private VariableTypeVO getVariableType(String typeName) throws SQLException {
//        String query = "select VARIABLE_TYPE_ID, TRANSLATOR_CLASS_NAME from VARIABLE_TYPE"
//            + " where VARIABLE_TYPE_NAME=?";
//        ResultSet rs = db.runSelect(query, typeName);
//        if (rs.next()) {
//          return new VariableTypeVO(rs.getLong(1), typeName, rs.getString(2));
//        } else return null;
        return VariableTypeCache.getVariableTypeVO(typeName);
    }

//    private Long createVariableType(String typeName, String translator)
//            throws SQLException {
//        String query = "insert into VARIABLE_TYPE "
//            + " (VARIABLE_TYPE_ID,VARIABLE_TYPE_NAME,TRANSLATOR_CLASS_NAME)"
//            + " values (?,?,?)";
//        Long id = this.getNextId("MDW_COMMON_ID_SEQ");
//        Object[] args = new Object[3];
//        args[0] = id;
//        args[1] = typeName;
//        args[2] = translator;
//        db.runUpdate(query, args);
//        return new Long(id);
//    }

    private void updateVariableType(Long variableId, String typeName) throws SQLException
    {
        VariableTypeVO typeVO = this.getVariableType(typeName);
        String query = "update VARIABLE" +
            " set VARIABLE_TYPE_ID=?" +
            " where VARIABLE_ID=?";
        Object[] args = new Object[2];
        args[0] = typeVO.getVariableTypeId();
        args[1] = variableId;
        db.runUpdate(query, args);
    }

    private void updateVariableName(Long variableId, String varName) throws SQLException
    {
        String query = "update VARIABLE" +
            " set VARIABLE_NAME=?" +
            " where VARIABLE_ID=?";
        Object[] args = new Object[2];
        args[0] = varName;
        args[1] = variableId;
        db.runUpdate(query, args);
    }

    private boolean variableMappingExistForOthers(Long variableId, Long ownerId, String ownerType) throws SQLException {
        String query = "select VARIABLE_MAPPING_ID from VARIABLE_MAPPING"
            + " where VARIABLE_ID=? and (MAPPING_OWNER!=? or MAPPING_OWNER_ID!=?)";
        Object[] args = new Object[3];
        args[0] = variableId;
        args[1] = ownerType;
        args[2] = ownerId;
        ResultSet rs = db.runSelect(query, args);
        return rs.next();
    }

    private void deleteVariable(VariableVO var, String ownerType, Long ownerId)
            throws SQLException {
        String query = "delete from VARIABLE_MAPPING where " +
            "MAPPING_OWNER=? and MAPPING_OWNER_ID=? and VARIABLE_ID=?";
        Object[] args = new Object[3];
        args[0] = ownerType;
        args[1] = ownerId;
        args[2] = var.getVariableId();
        db.runUpdate(query, args);
        query = "select VARIABLE_MAPPING_ID from VARIABLE_MAPPING where" +
                " VARIABLE_ID=?";
        ResultSet rs = db.runSelect(query, var.getVariableId());
        if (!rs.next()) {
            // shall we delete variable instances?
            query = "delete from VARIABLE where VARIABLE_ID=?";
            db.runUpdate(query, var.getVariableId());
        }
    }

    private void determineSomeVariableAttributes(VariableVO vo, String pOwner)
    {
        if (vo.getDisplaySequence()==null) vo.setDisplaySequence(0);
        if (OwnerType.TASK.equals(pOwner)) {
            if (vo.getDisplayMode()==null) vo.setDisplayMode(VariableVO.DATA_REQUIRED);
        } else {
            if (vo.getVariableCategory()==null) vo.setVariableCategory(VariableVO.CAT_LOCAL);
        }
    }

    private void addVariable(VariableVO vo, String pOwner, Long pOwnerId)
            throws SQLException {
        determineSomeVariableAttributes(vo, pOwner);
        VariableTypeVO varType = this.getVariableType(vo.getVariableType());
        Long varTypeId;
        if (varType == null) {
            varTypeId = 1L;     // supposed to be String
//          varTypeId = createVariableType(vo.getVariableType(), vo.getTranslatorClass());
        } else varTypeId = varType.getVariableTypeId();
        Long varId = vo.getVariableId();
        if (varId!=null&&varId.longValue()>0) {
            String query = "select VARIABLE_NAME, VARIABLE_TYPE_ID"
                + " FROM VARIABLE where VARIABLE_ID = ?";
            ResultSet rs = db.runSelect(query, varId);
            if (rs.next()) {
                String vn = rs.getString(1);
                Long vt = rs.getLong(2);
                if (!vn.equals(vo.getVariableName()) || !vt.equals(varTypeId)) varId = null;
            } else varId = null;
        }
        if (varId==null) {
            varId = createVariable(vo.getVariableName(), varTypeId);
            vo.setVariableId(varId);
        }
        createVariableMapping(pOwnerId, pOwner, varId, vo);
    }

    private void updateVariable(VariableVO vo, VariableVO oldVar, String pOwner, Long pOwnerId)
        throws SQLException {
        determineSomeVariableAttributes(vo, pOwner);
        Long varId = vo.getVariableId();
        Long oldVarId = oldVar.getVariableId();
        if (!oldVar.getVariableType().equals(vo.getVariableType())) {
            // may need to create a new var if the existing var is used else where
            if (variableMappingExistForOthers(varId, pOwnerId, pOwner)) {
                VariableTypeVO varType = this.getVariableType(vo.getVariableType());
                Long varTypeId;
                if (varType == null) {
                    varTypeId = 1L;     // supposed to be String
//                  varTypeId = createVariableType(vo.getVariableType(), vo.getTranslatorClass());
                } else varTypeId = varType.getVariableTypeId();
                varId = createVariable(vo.getVariableName(), varTypeId);
                vo.setVariableId(varId);
            } else {
                this.updateVariableType(oldVarId, vo.getVariableType());
            }
        }
        if (!oldVar.getVariableName().equals(vo.getVariableName())) {
            if (variableMappingExistForOthers(varId, pOwnerId, pOwner)) {
                varId = createVariable(vo.getVariableName(), getVariableType(vo.getVariableType()).getVariableTypeId());
                vo.setVariableId(varId);
            } else {
                this.updateVariableName(oldVarId, vo.getVariableName());
            }
        }
        updateVariableMapping(pOwner, pOwnerId, oldVarId, vo);
    }

    private void addVariables(List<VariableVO> pVariables, Long pOwnerId, String pOwner)
        throws SQLException {
        if (pVariables==null) return;
        for (VariableVO vo : pVariables) {
            addVariable(vo, pOwner, pOwnerId);
        }
    }

    private void persistImplementors(PackageVO packageVO, PersistType persistType)
            throws SQLException,DataAccessException {
        if (packageVO.getImplementors()==null) return;
        AttributeBatch batch = new AttributeBatch();
        for (ActivityImplementorVO ai : packageVO.getImplementors()) {
            String implClass = ai.getImplementorClassName();
            ActivityImplementorVO old = this.implementorName2Obj.get(implClass);
            if (old!=null) {
                Long id = old.getImplementorId();
                if (persistType==PersistType.IMPORT) {
                    ai.setImplementorId(id);
                    updateImplementor0(ai, batch);
                }
            } else {
                if (persistType!=PersistType.IMPORT)
                    throw new DataAccessException("Implementor " + implClass + " does not exist");
                createImplementor0(ai, batch);
            }
        }
        if (packageVO.getVersion()>0 || persistType!=PersistType.IMPORT) {
            // save package-implementor mapping
            String query = "delete from PACKAGE_ACTIVITY_IMPLEMENTORS "
                + " where PACKAGE_ID=?";
            db.runUpdate(query, packageVO.getPackageId());
            query = "insert into PACKAGE_ACTIVITY_IMPLEMENTORS "
                + "(PACKAGE_ID, ACTIVITY_IMPLEMENTOR_ID) values "
                + "(?, ?)";
            Object[] args = new Object[2];
            args[0] = packageVO.getPackageId();
            for (ActivityImplementorVO ai : packageVO.getImplementors()) {
                args[1] = ai.getImplementorId();
                db.runUpdate(query, args);
            }
        }
        batch.execute(db, null);
    }

    private void persistExternalEvents(PackageVO packageVO, PersistType persistType) throws SQLException {
        if (packageVO.getExternalEvents()==null) return;
        for (ExternalEventVO ei : packageVO.getExternalEvents()) {
            ei.setId(createExternalEvent0(ei));
        }
        if (packageVO.getVersion()>0 || persistType!=PersistType.IMPORT) {
            String query = "delete from PACKAGE_EXTERNAL_EVENTS "
            + " where PACKAGE_ID=?";
            db.runUpdate(query, packageVO.getPackageId());
            query = "insert into PACKAGE_EXTERNAL_EVENTS "
                + "(PACKAGE_ID, EXTERNAL_EVENT_ID) values "
                + "(?, ?)";
            Object[] args = new Object[2];
            args[0] = packageVO.getPackageId();
            for (ExternalEventVO ei : packageVO.getExternalEvents()) {
                args[1] = ei.getId();
                db.runUpdate(query, args);
            }
        }
    }

    protected void persistParticipants(PackageVO packageVO, PersistType persistType)
    throws SQLException,DataAccessException {
        return; // this is MDW 5 feature and will be overriden in ProcessLoaderPersisterV5
    }

    protected void persistScripts(PackageVO packageVO, PersistType persistType)
    throws SQLException,DataAccessException {
        return; // this is MDW 5 feature and will be overriden in ProcessLoaderPersisterV5
    }

    protected void persistCustomAttributes(PackageVO packageVO)
    throws SQLException,DataAccessException {
        return;  // MDW 5 only
    }

    protected void persistPackageProcessMapping(PackageVO packageVO, PersistType persistType) throws SQLException {
        String query = "delete from PACKAGE_PROCESS "
            + " where PACKAGE_ID=?";
        db.runUpdate(query, packageVO.getPackageId());
        if (packageVO.getProcesses()!=null) {
            query = "insert into PACKAGE_PROCESS "
                + "(PACKAGE_ID, PROCESS_ID) values "
                + "(?, ?)";
            Object[] args = new Object[2];
            args[0] = packageVO.getPackageId();
            for (ProcessVO vo : packageVO.getProcesses()) {
                if (persistType.equals(PersistType.IMPORT) && vo.isLoaded())
                   args[1] = workNameRef.get(vo.getProcessId());
                else
                    args[1] = vo.getProcessId();
                db.runUpdate(query, args);
            }
        }
    }

    protected TaskVO getTask(Long taskId, Map<Long,TaskCategory> categories)
            throws SQLException {
        String query = "select TASK_NAME, TASK_TYPE_ID, COMMENTS, TASK_CATEGORY_ID" +
            " from TASK where TASK_ID=?";
        ResultSet rs = db.runSelect(query, taskId);
        if (!rs.next()) return null;
        TaskVO task = new TaskVO();
        Long categoryId = rs.getLong(4);
        task.setTaskId(taskId);
        Integer taskType = rs.getInt(2);
        task.setTaskTypeId(taskType);
        task.setTaskCategory(categories.get(categoryId).getCode());
        task.setTaskName(rs.getString(1));
        task.setComment(rs.getString(3));
        task.setAttributes(getAttributes1(OwnerType.TASK, taskId));
        task.setVariables(getVariablesForOwner(OwnerType.TASK, taskId));
        if (task.getAttribute(TaskActivity.ATTRIBUTE_TASK_SLA)==null) {
            // for MDW 3 and early versions of MDW 4 tasks
            task.setSlaSeconds(getServiceLevelAgreement(OwnerType.TASK, taskId));
        }
        task.setUserGroups(getGroupsForTask(taskId));
        return task;
    }

    protected final List<String> getGroupsForTask(Long taskId) throws SQLException {
        String query = "select g.GROUP_NAME " +
            "from TASK_USR_GRP_MAPP m, USER_GROUP g " +
            "where m.USER_GROUP_ID = g.USER_GROUP_ID " +
            "and m.TASK_ID=?";
        ResultSet rs = db.runSelect(query, taskId);
        List<String> groups = null;
        while (rs.next()) {
            if (groups==null) groups = new ArrayList<String>();
            groups.add(rs.getString(1));
        }
        return groups;
    }

    protected void createTask(Long taskId, String taskName, Integer taskTypeId, Long categoryId, String comment) throws SQLException {
        String query = "insert into TASK" +
            " (TASK_ID,TASK_NAME,TASK_TYPE_ID,TASK_CATEGORY_ID,COMMENTS)" +
            " values (?,?,?,?,?)";
        Object[] args = new Object[5];
        args[0] = taskId;
        args[1] = taskName;
        args[2] = taskTypeId;
        args[3] = categoryId;
        args[4] = comment;
        db.runUpdate(query, args);
    }

    protected void updateTask(Long id, String taskName, Integer taskType, Long category, String comments) throws SQLException
    {
        String query = "update TASK" +
            " set TASK_NAME=?, TASK_TYPE_ID=?, TASK_CATEGORY_ID=?, COMMENTS=?" +
            " where TASK_ID=?";
        Object[] args = new Object[5];
        args[0] = taskName;
        args[1] = taskType;
        args[2] = category;
        args[3] = comments;
        args[4] = id;
        db.runUpdate(query, args);
    }

    protected final Long createCategory(String category) throws SQLException {
        Long categoryId = db.isMySQL()?null:this.getNextId("MDW_COMMON_ID_SEQ");
        String query = "insert into TASK_CATEGORY" +
            " (TASK_CATEGORY_ID, TASK_CATEGORY_CD, TASK_CATEGORY_DESC)" +
            " values (?, ?, '')";
        Object[] args = new Object[2];
        args[0] = categoryId;
        args[1] = category;
        if (db.isMySQL()) categoryId = db.runInsertReturnId(query, args);
        else db.runUpdate(query, args);
        return categoryId;
    }

    private Map<String,Long> getCategoryCode2IdMap() throws SQLException
    {
        if (this.categoryCode2Id==null) {
            categoryCode2Id = new HashMap<String,Long>();
            String query = "select TASK_CATEGORY_ID,TASK_CATEGORY_CD from TASK_CATEGORY";
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) categoryCode2Id.put(rs.getString(2), rs.getLong(1));
        }
        return categoryCode2Id;
    }

    private Map<Long,TaskCategory> getCategoryId2CatMap() throws SQLException
    {
        if (this.categoryId2Cat == null) {
            categoryId2Cat = new HashMap<Long,TaskCategory>();
            String query = "select TASK_CATEGORY_ID,TASK_CATEGORY_CD,TASK_CATEGORY_DESC from TASK_CATEGORY";
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
                TaskCategory cat = new TaskCategory(rs.getLong(1), rs.getString(2), rs.getString(3));
                categoryId2Cat.put(cat.getId(), cat);
            }
        }
        return categoryId2Cat;
    }

    protected final void persistTaskGroupsAndVariables(TaskVO task, PersistType persistType)
    throws SQLException, DataAccessException {
        Long taskId = task.getTaskId();
        List<String> groups = task.getUserGroups();
        if (persistType==PersistType.UPDATE || persistType==PersistType.SAVE) {
            updateVariables(task.getVariables(), OwnerType.TASK, taskId);
            List<String> oldGroups = this.getGroupsForTask(taskId);
            if (oldGroups!=null) {
                if (groups!=null) {
                    for (String grp : oldGroups) {
                        if (!groups.contains(grp)) deleteGroupForTask(taskId, grp);
                    }
                    for (String grp : groups) {
                        if (!oldGroups.contains(grp)) addGroupForTask(taskId, grp);
                    }
                } else {
                    for (String grp : oldGroups) {
                        deleteGroupForTask(taskId, grp);
                    }
                }
            } else {
                if (groups!=null) {
                    for (String grp : groups) {
                        addGroupForTask(taskId, grp);
                    }
                }
            }
        } else {
            this.addVariables(task.getVariables(), taskId, OwnerType.TASK);
            if (groups!=null) {
                for (String grp : groups) {
                    addGroupForTask(taskId, grp);
                }
            }
        }
    }

    protected void persistTask(TaskVO task, PersistType persistType, Map<String,Long> categories,
            AttributeBatch batch)
            throws SQLException, DataAccessException {
        Long categoryId = categories.get(task.getTaskCategory());
        if (categoryId==null) {
            try {
                categoryId = createCategory(task.getTaskCategory());
                categories.put(task.getTaskCategory(), categoryId);
            }
            catch (SQLException ex){
                throw new DataAccessException(0,"Please check if TASK_CATEGORY_ID in TASK_CATEGORY and TASK table is set to NUMBER(20)", ex);
            }

        }
        Long taskId = task.getTaskId();
        if (persistType==PersistType.CREATE) {
            this.createTask(taskId, task.getTaskName(), task.getTaskTypeId(), categoryId, task.getComment());
        } else {
            updateTask(taskId, task.getTaskName(), task.getTaskTypeId(), categoryId, task.getComment());
        }
        persistAttributes(OwnerType.TASK, taskId, task.getAttributes(), batch);
        // for MDW 3 and earlier versions of MDW 4
        persistSla(taskId, OwnerType.TASK, task.getSlaSeconds(), PersistType.UPDATE);
        persistTaskGroupsAndVariables(task, persistType);
    }

    private Long getGroupId(String groupName) throws SQLException {
        String query = "select USER_GROUP_ID " +
            "from USER_GROUP " +
            "where GROUP_NAME = ?";
        ResultSet rs = db.runSelect(query, groupName);
        if (rs.next()) return rs.getLong(1);
        else return null;
    }

    private void deleteGroupForTask(Long taskId, String groupName) throws SQLException {
        String query = "delete from TASK_USR_GRP_MAPP " +
            "where TASK_ID=? and USER_GROUP_ID in " +
            "(select USER_GROUP_ID from USER_GROUP where GROUP_NAME=?)";
        Object[] args = new Object[2];
        args[0] = taskId;
        args[1] = groupName;
        db.runUpdate(query, args);
    }

    private void addGroupForTask(Long taskId, String groupName)
            throws SQLException, DataAccessException {
        String query = "insert into TASK_USR_GRP_MAPP " +
            "(TASK_USR_GRP_MAPP_ID, TASK_ID, USER_GROUP_ID) " +
            "values (MDW_COMMON_ID_SEQ.NEXTVAL, ?, ?)";
        Object[] args = new Object[2];
        args[0] = taskId;
        Long grpId = getGroupId(groupName);
        if (grpId==null) {
            grpId = createGroup(groupName, "");
        }

        args[1] = grpId;
        db.runUpdate(query, args);
    }

    public List<RuleSetVO> getRuleSets() throws DataAccessException {
        try {
            db.openConnection();
            String query = "select RULE_SET_ID,RULE_SET_NAME,MOD_USR,MOD_DT,COMMENTS" +
                    " from RULE_SET " +
                    " where COMMENTS not like '" + RuleSetVO.ATTRIBUTE_OVERFLOW + "%'" +
                    " order by RULE_SET_NAME";
            ResultSet rs = db.runSelect(query, null);
            List<RuleSetVO> ret = new ArrayList<RuleSetVO>();
            while (rs.next()) {
                RuleSetVO vo = new RuleSetVO();
                vo.setId(rs.getLong(1));
                vo.setName(rs.getString(2));
                vo.setModifyingUser(rs.getString(3));
                vo.setModifyDate(rs.getTimestamp(4));
                vo.setLanguage(rs.getString(5));
                vo.setRuleSet(null);
                ret.add(vo);
            }
            return ret;
        } catch (SQLException e) {
            throw new DataAccessException(0,"failed to load resources", e);
        } finally {
            db.closeConnection();
        }
    }

    @Override
    public RuleSetVO getRuleSet(Long id) throws DataAccessException {
        try {
            db.openConnection();
            return this.getRuleSet0(id);
        } catch (SQLException e) {
            throw new DataAccessException(0,"failed to load resource", e);
        } finally {
            db.closeConnection();
        }
    }

    protected RuleSetVO getRuleSet0(Long id) throws SQLException {
        db.openConnection();
        String query = "select RULE_SET_NAME,RULE_SET_DETAILS,MOD_USR,MOD_DT,COMMENTS" +
            " from RULE_SET " +
            " where RULE_SET_ID=?";
        ResultSet rs = db.runSelect(query, id.toString());
        if (rs.next()) {
            RuleSetVO ruleset = new RuleSetVO();
            ruleset.setId(id);
            ruleset.setName(rs.getString(1));
            ruleset.setRuleSet(rs.getString(2));
            ruleset.setModifyingUser(rs.getString(3));
            ruleset.setModifyDate(rs.getTimestamp(4));
            ruleset.setLanguage(rs.getString(5));
            ruleset.setLoadDate(new Date());
            return ruleset;
        }
        else {
            return null;
        }
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

    /**
     * Not relevant for V4.
     */
    public RuleSetVO getRuleSet(Long packageId, String name) throws DataAccessException {
        return null;
    }


    public RuleSetVO getRuleSetForOwner(String ownerType, Long ownerId) throws DataAccessException {
        return null;
    }

    protected RuleSetVO getRuleSet0(String name, String language, int version) throws SQLException {
        String query;
        Object[] args;
        if (language==null) {
            query = "select RULE_SET_ID,RULE_SET_DETAILS,MOD_USR,MOD_DT,COMMENTS "+
                " from RULE_SET " +
                " where RULE_SET_NAME=?";
            args = new Object[1];
            args[0] = name;
        } else {
            query = "select RULE_SET_ID,RULE_SET_DETAILS,COMMENTS,MOD_USR,MOD_DT " +
                " from RULE_SET " +
                " where RULE_SET_NAME=? and COMMENTS=?";
            args = new Object[2];
            args[0] = name;
            args[1] = language;
        }
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) {
            RuleSetVO ruleset = new RuleSetVO();
            ruleset.setId(rs.getLong(1));
            ruleset.setName(name);
            ruleset.setRuleSet(rs.getString(2));
            ruleset.setModifyingUser(rs.getString(3));
            ruleset.setModifyDate(rs.getTimestamp(4));
            ruleset.setLanguage(rs.getString(5));
            ruleset.setLoadDate(new Date());
            if (rs.next()) {
                throw new SQLException("There are more than one ruleset with the name " + name);
            }
            return ruleset;
        }
        else {
            return null;
        }
    }

    public Long createRuleSet(RuleSetVO ruleset) throws DataAccessException {
        try {
            db.openConnection();
            Long id = createRuleSet0(ruleset);
            db.commit();
            return id;
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to create resource", e);
        }
        finally {
            db.closeConnection();
        }
    }

    protected Long createRuleSet0(RuleSetVO ruleset) throws SQLException {
        String rules = ruleset.getRuleSet();
        Long id = ruleset.getId();
        String language = ruleset.getLanguage();
        if (id<=0) id = this.getNextId("MDW_COMMON_ID_SEQ");
        String query = "insert into RULE_SET " +
            " (RULE_SET_ID,RULE_SET_NAME,RULE_SET_DETAILS,COMMENTS)" +
            " values (?,?,?,?)";
        db.runUpdate(query, new Object[]{id, ruleset.getName(), rules, language});
        return id;
    }

    public void updateRuleSet(RuleSetVO ruleset) throws DataAccessException {
        try {
            db.openConnection();
            updateRuleSet0(ruleset);
            db.commit();
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "Failed to save report", e);
        }
        finally {
            db.closeConnection();
        }
    }

    public void renameRuleSet(RuleSetVO ruleset, String newName) throws DataAccessException {
        ruleset.setName(newName);
        updateRuleSet(ruleset);
    }

    protected void updateRuleSet0(RuleSetVO ruleset) throws SQLException {
        String rules = ruleset.getRuleSet();
        Long id = ruleset.getId();
        String language = ruleset.getLanguage();
        String query = "update RULE_SET set RULE_SET_NAME=?, RULE_SET_DETAILS=?, COMMENTS=?"
                + " where RULE_SET_ID=?";
        db.runUpdate(query, new Object[]{ruleset.getName(), rules, language, id});
    }

    public void deleteRuleSet(Long id)
            throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from RULE_SET where RULE_SET_ID=?";
            db.runUpdate(query, id);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to delete the rule set", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<ProcessVO> findCallingProcesses(ProcessVO subproc) throws DataAccessException {
        throw new UnsupportedOperationException("Not supported for MDW 4.x");
    }

    public List<ProcessVO> findCalledProcesses(ProcessVO mainproc) throws DataAccessException {
        throw new UnsupportedOperationException("Not supported for MDW 4.x");
    }

    private void lockInOracle(String tableName, String idField, Long rowId,
            String[] locking) throws SQLException {
        String query = "select MOD_USR, MOD_DT from " + tableName +
            " where " + idField + "=? for update";
        ResultSet rs = db.runSelect(query, rowId);
        if (rs.next()) {
            locking[0] = rs.getString(1);
            Date dt = rs.getTimestamp(2);
            locking[1] = dt==null?null:dt.toString();
        } else throw new SQLException("Row does not exist: " + rowId);
    }

    protected String lockUnlockObject(String tableName, String idField, Long rowId,
                String cuid, String objectKind, boolean lock)
            throws DataAccessException {
        try {
            String error_message;
            Object[] args = new Object[2];
            String[] lockingInfo = new String[2];
            db.openConnection();
            lockInOracle(tableName, idField, rowId, lockingInfo);
            if (lock) {
                if (lockingInfo[0]==null) {
                    String query = "update " + tableName +
                        " set MOD_USR=?, MOD_DT=" + now() + " where " + idField + "=?";
                    args[0] = cuid;
                    args[1] = rowId;
                    db.runUpdate(query, args);
                    error_message = null;
                } else if (!lockingInfo[0].equals(cuid)) {
                    error_message = "The " + objectKind + " is locked by " +
                        lockingInfo[0] + " on " + lockingInfo[1];
                } else error_message = null;
            } else {
                if (lockingInfo[0]==null || !lockingInfo[0].equals(cuid)) {
                    error_message = "The " + objectKind + " is not locked by " + cuid;
                } else {
                    String query = "update " + tableName +
                        " set MOD_USR=" + (db.isMySQL()?"null":"''") + " where " + idField + "=?";
                    db.runUpdate(query, rowId);
                    error_message = null;
                }
            }
            db.commit();
            return error_message;
        } catch (SQLException e) {
            db.rollback();
            throw new DataAccessException(0, "failed to lock the " + objectKind, e);
        } finally {
            db.closeConnection();
        }
    }

    public String lockUnlockProcess(Long processId, String cuid, boolean lock)
            throws DataAccessException {
        if (getSupportedVersion()>=DataAccess.schemaVersion52)
            return lockUnlockObject("RULE_SET", "RULE_SET_ID", processId, cuid, "process", lock);
        else return lockUnlockObject("WORK", "WORK_ID", processId, cuid, "process", lock);
    }

    public String lockUnlockRuleSet(Long ruleSetId, String cuid, boolean lock)
            throws DataAccessException {
        return lockUnlockObject("RULE_SET", "RULE_SET_ID", ruleSetId, cuid, "resource", lock);
    }

    private boolean setAttributeSpecial(String ownerType, Long ownerId,
            String attrname, String attrvalue) throws SQLException {
        if (ownerType.equals(OwnerType.PACKAGE)) {
            if (attrname.equalsIgnoreCase("EXPORTED_IND")) {
                String query = "update PACKAGE set EXPORTED_IND=? where PACKAGE_ID=?";
                Object[] args = new Object[2];
                args[0] = attrvalue;
                args[1] = ownerId;
                db.runUpdate(query, args);
                return true;
            }
        }
        return false;
    }

    public Long setAttribute(String ownerType, Long ownerId, String attrname, String attrvalue)
            throws DataAccessException {

        try {
            db.openConnection();
            Long existingId = null;
            if (!setAttributeSpecial(ownerType, ownerId, attrname, attrvalue)) {
                AttributeVO existingAttr = getAttribute0(ownerType, ownerId, attrname);
                if (existingAttr != null && existingAttr.getAttributeValue() != null
                        && existingAttr.getAttributeValue().startsWith(ATTRIBUTE_OVERFLOW_PREFIX)) {
                    // delete overflow rule_set
                    Long ruleSetId = new Long(existingAttr.getAttributeValue().substring(ATTRIBUTE_OVERFLOW_PREFIX.length()));
                    String query = "delete from RULE_SET where RULE_SET_ID=?";
                    db.runUpdate(query, ruleSetId);
                }
                if (attrvalue != null && attrvalue.length() > 3960) {
                    // insert ruleSet
                    RuleSetVO ruleSet = new RuleSetVO();
                    ruleSet.setId(new Long(-1));
                    ruleSet.setLanguage(RuleSetVO.ATTRIBUTE_OVERFLOW);
                    ruleSet.setRuleSet(attrvalue);
                    ruleSet.setName(ATTRIBUTE_OVERFLOW_PREFIX + ownerType + "_" + ownerId
                            + "_" + attrname);
                    Long rsId = createRuleSet0(ruleSet);
                    attrvalue = ATTRIBUTE_OVERFLOW_PREFIX + rsId;
                }
                existingId = setAttribute0(ownerType, ownerId, attrname, attrvalue);
            }
            db.commit();
            return existingId;
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to set attribute", e);
        } finally {
            db.closeConnection();
        }
    }

    public CustomAttributeVO getCustomAttribute(String ownerType, String categorizer)
    throws DataAccessException {
        CustomAttributeVO vo = new CustomAttributeVO(ownerType, categorizer);
        try {
            db.openConnection();
            AttributeVO defAttr = getAttribute0(vo.getDefinitionAttrOwner(), vo.getOwnerId(), CustomAttributeVO.DEFINITION);
            if (defAttr != null)
                vo.setDefinition(defAttr.getAttributeValue());
            AttributeVO rolesAttr = getAttribute0(vo.getRolesAttrOwner(), vo.getOwnerId(), CustomAttributeVO.ROLES);
            if (rolesAttr != null) {
                List<String> roles = new ArrayList<String>();
                String rolesStr = rolesAttr.getAttributeValue();
                if (rolesStr != null) {
                    for (String role : rolesStr.split(","))
                        roles.add(role);
                    vo.setRoles(roles);
                }
            }
            if (vo.isEmpty())
                return null;

            return vo;
        }
        catch (SQLException ex) {
            throw new DataAccessException(-1, "failed to load custom attribute definition", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Long setCustomAttribute(CustomAttributeVO customAttrVO)
    throws DataAccessException {
        try {
            db.openConnection();
            Long ownerId = customAttrVO.getOwnerId();
            setAttribute0(customAttrVO.getDefinitionAttrOwner(), ownerId, CustomAttributeVO.DEFINITION, customAttrVO.getDefinition());
            String rolesStr = null;
            if (customAttrVO.getRoles() != null) {
                rolesStr = "";
                for (int i = 0; i < customAttrVO.getRoles().size(); i++) {
                    rolesStr += customAttrVO.getRoles().get(i);
                    if (i < customAttrVO.getRoles().size() - 1)
                        rolesStr += ",";
                }
            }
            return setAttribute0(customAttrVO.getRolesAttrOwner(), ownerId, CustomAttributeVO.ROLES, rolesStr);
        }
        catch (SQLException ex) {
            throw new DataAccessException(-1, "failed to save custom attribute definition", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void setAttributes(String owner, Long ownerId, Map<String,String> attributes)
    throws DataAccessException {
        if (attributes == null) {
            setAttributes(owner, ownerId, (List<AttributeVO>)null);
        }
        else {
            List<AttributeVO> attrs = new ArrayList<AttributeVO>();
            for (String name : attributes.keySet())
                attrs.add(new AttributeVO(name, attributes.get(name)));
            setAttributes(owner, ownerId, attrs);
        }
    }

    protected void setAttributes(String owner, Long ownerId, List<AttributeVO> attrs)
    throws DataAccessException {
        try {
            db.openConnection();
            AttributeBatch batch = new AttributeBatch();
            persistAttributes(owner, ownerId, attrs, batch);
            batch.execute(db, null);
        }
        catch (SQLException ex) {
            throw new DataAccessException(-1, "failed to save attributes", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Long createGroup(String groupName, String groupDescription) throws SQLException {
        String query = "insert into USER_GROUP "
            + " (USER_GROUP_ID, GROUP_NAME, COMMENTS, CREATE_DT, CREATE_USR)"
            + " values (?,?,?," + now() + ",'ProcessLoader')";
        Long id = db.isMySQL()?null:this.getNextId("MDW_COMMON_ID_SEQ");
        Object[] args = new Object[3];
        args[0] = id;
        args[1] = groupName;
        args[2] = groupDescription;
        if (db.isMySQL()) id = db.runInsertReturnId(query, args);
        else db.runUpdate(query, args);
        return id;
    }

    private Map<Long, List<AttributeVO>> getAttributesBatch(String ownerType, String ownerIdList)
    throws SQLException {
        String query = "select ATTRIBUTE_ID, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, ATTRIBUTE_OWNER_ID from ATTRIBUTE "
                + "where ATTRIBUTE_OWNER=? and ATTRIBUTE_OWNER_ID in (" + ownerIdList + ")";
        ResultSet rs = db.runSelect(query, ownerType);
        Map<Long, List<AttributeVO>> attrss = new HashMap<Long, List<AttributeVO>>();
        List<AttributeVO> attrs;
        while (rs.next()) {
            AttributeVO vo = new AttributeVO(rs.getString(2), rs.getString(3));
            vo.setAttributeId(new Long(rs.getLong(1)));
            Long ownerId = rs.getLong(4);
            attrs = attrss.get(ownerId);
            if (attrs == null) {
                attrs = new ArrayList<AttributeVO>();
                attrss.put(ownerId, attrs);
            }
            attrs.add(vo);
        }
        return attrss;
    }

    protected String getLanguageColumnName() {
        return "COMMENTS";
    }

    protected class AttributeBatch {
        private List<Object[]> deletes;
        private List<Object[]> inserts;
        AttributeBatch() {
            deletes = new ArrayList<Object[]>();
            inserts = new ArrayList<Object[]>();
        }
        private void addDelete(Object[] one) {
            deletes.add(one);
        }
        private void addInsert(Object[] one) {
            inserts.add(one);
        }
        private void execute(DatabaseAccess db, Long processId) throws SQLException {
            String query;
            // delete entries in ATTRIBUTE table
            if (deletes.size()>0) {
                query = "delete from ATTRIBUTE where ATTRIBUTE_OWNER=? and ATTRIBUTE_OWNER_ID=?";
                db.prepareStatement(query);
                for (Object[] oneset : deletes) {
                    db.addToBatch(oneset);
                }
                db.runBatchUpdate();
            }
            // delete overflow attributes
            if (processId!=null) {
                query = "delete from RULE_SET where " + getLanguageColumnName() + " like ?";
                db.runUpdate(query, ATTRIBUTE_OVERFLOW_PREFIX + processId + "_%");
            }

            if (inserts.size()==0) return;

            // collect overflow attributes
            if (db.isMySQL()) {
                query = "insert into RULE_SET " +
                    " (RULE_SET_NAME,RULE_SET_DETAILS," + getLanguageColumnName() + ",CREATE_DT,CREATE_USR)" +
                    " values (?,?,?,now(),'ProcessPersister')";
            } else {
                query = "insert into RULE_SET " +
                " (RULE_SET_ID,RULE_SET_NAME,RULE_SET_DETAILS," + getLanguageColumnName() + ")" +
                " values (?,?,?,?)";
            }
            if (processId!=null) {
                for (Object[] oneset : inserts) {
                    String v = (String)oneset[3];
                    if (v!=null && v.length()>3960) {
                        if (db.isMySQL()) {
                            String rulesetName = ATTRIBUTE_OVERFLOW_PREFIX + processId + "_" +
                                oneset[1] + "_" + oneset[2];
                            // owner id + attribute name above is just to make ruleset name unique
                            String language = RuleSetVO.ATTRIBUTE_OVERFLOW;
                            Long rulesetId = db.runInsertReturnId(query, new Object[]{rulesetName, v, language});
                            oneset[3] =  ATTRIBUTE_OVERFLOW_PREFIX + rulesetId;
                        } else {
                            Long rulesetId = getNextId("MDW_COMMON_ID_SEQ");
                            String rulesetName = ATTRIBUTE_OVERFLOW_PREFIX + processId + "_" + rulesetId;
                            // ruleset id above is just to make ruleset name unique
                            String language = RuleSetVO.ATTRIBUTE_OVERFLOW;
                            db.runUpdate(query, new Object[]{rulesetId, rulesetName, v, language});
                            oneset[3] =  ATTRIBUTE_OVERFLOW_PREFIX + rulesetId;
                        }
                    }
                }
            }
            // insert overflow attributes
            if (db.isMySQL()) {
                query = "insert into ATTRIBUTE"
                    + " (attribute_owner,attribute_owner_id,attribute_name,attribute_value,create_dt,create_usr)"
                    + " values (?,?,?,?,now(),'ProcessPersister')";
            } else {
                query = "insert into ATTRIBUTE"
                    + " (attribute_id,attribute_owner,attribute_owner_id,attribute_name,attribute_value)"
                    + " values (MDW_COMMON_ID_SEQ.NEXTVAL,?,?,?,?)";
            }
            db.prepareStatement(query);
            for (Object[] oneset : inserts) {
                db.addToBatch(oneset);
            }
            db.runBatchUpdate();
        }
    }

    public List<ProcessVO> getProcessListForImplementor(Long implementorId, String implementorClass)
        throws DataAccessException {
        List<ProcessVO> processList = new ArrayList<ProcessVO>();
        try {
            db.openConnection();
            String query = "select wr.WORK_ID, wr.WORK_NAME, wr.COMMENTS,"
                + "pr.VERSION_NO, wr.CREATE_DT, wr.MOD_DT, wr.MOD_USR"
                + " from WORK wr, PROCESS pr"
                + " where pr.PROCESS_ID in (select distinct t.PROCESS_ID "
                + "             from ACTIVITY a, WORK_TRANSITION t"
                + "             where a.activity_impl_id=?"
                + "               and (t.from_work_id=a.activity_id or t.to_work_id=a.activity_id)) "
                + "    and wr.WORK_TYPE = 1 AND wr.WORK_ID = pr.PROCESS_ID "
                + " order by upper(wr.WORK_NAME), pr.VERSION_NO";
            ResultSet rs = db.runSelect(query, implementorId);
            while (rs.next()) {
                Long processId = new Long(rs.getLong(1));
                String processName = rs.getString(2);
                String processDesc = rs.getString(3);
                int version = rs.getInt(4);
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
                if (!processList.contains(vo))
                    processList.add(vo);
            }
        } catch (SQLException e) {
            throw new DataAccessException(0, "failed to load process", e);
        } finally {
            db.closeConnection();
        }
        return processList;
    }

    protected void saveProcessInRuleSet(ProcessVO processVO, Long process_id,
            PersistType persistType, int actualVersion, AttributeBatch batch) throws SQLException, DataAccessException, XmlException {
        // this is only possible for MDW 5.2 or later
    }

    protected void loadProcessFromRuleSet(ProcessVO processVO)
    throws SQLException, DataAccessException {
        // this is only possible for MDW 5.2 or later
    }

    /**
     * Copied from old method getPackage(name, version).  Only called with version = 0.
     */
    public PackageVO getPackage(String name) throws DataAccessException {
        try {
            int version = 0;
            db.openConnection();
            String query = "select PACKAGE_ID, PACKAGE_NAME, SCHEMA_VERSION, DATA_VERSION, EXPORTED_IND, MOD_DT\n"
                + (DataAccess.isPackageLevelAuthorization ? " , GROUP_NAME\n" : " ")
                + "from " + DBMappingUtil.tagSchemaOwner("PACKAGE") + " where PACKAGE_NAME=? "
                + "order by DATA_VERSION desc";
            ResultSet rs = db.runSelect(query, name);
            while (rs.next()) {
                int ver = rs.getInt(4);
                if (version!=ver && version!=0) continue;
                PackageVO packageVO = new PackageVO();
                packageVO.setPackageId(rs.getLong(1));
                packageVO.setPackageName(rs.getString(2));
                packageVO.setSchemaVersion(rs.getInt(3));
                packageVO.setVersion(rs.getInt(4));
                packageVO.setExported(rs.getInt(5) > 0);
                packageVO.setModifyDate(rs.getTimestamp(6));
                if (DataAccess.isPackageLevelAuthorization)
                    packageVO.setGroup(rs.getString("GROUP_NAME"));
                packageVO.setAttributes(this.getAttributes0(OwnerType.PACKAGE, packageVO.getPackageId()));
                return packageVO;
            }
            throw new DataAccessException("Package does not exist: " + name);
        }
        catch (SQLException e) {
            throw new DataAccessException(0, "failed to load package", e);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<TaskVO> getTaskTemplates() throws DataAccessException {
        try {
            List<TaskVO> list = new ArrayList<TaskVO>();
            if (getSupportedVersion() >= DataAccess.schemaVersion52) {
                String query = "select TASK_ID, TASK_NAME, TASK_CATEGORY_ID, LOGICAL_ID, COMMENTS\n"
                        + "from TASK where TASK_TYPE_ID = " + TaskType.TASK_TYPE_TEMPLATE;
                db.openConnection();
                Map<Long,TaskCategory> categories = getCategoryId2CatMap();
                ResultSet rs = db.runSelect(query, null);
                while (rs.next()) {
                    TaskVO task = new TaskVO();
                    task.setTaskId(rs.getLong("TASK_ID"));
                    task.setTaskName(rs.getString("TASK_NAME"));
                    task.setTaskTypeId(TaskType.TASK_TYPE_TEMPLATE);
                    TaskCategory cat = categories.get(rs.getLong("TASK_CATEGORY_ID"));
                    if (cat != null)
                      task.setTaskCategory(cat.getCode());
                    task.setLogicalId(rs.getString("LOGICAL_ID"));
                    task.setComment(rs.getString("COMMENTS"));
                    list.add(task);
                }
            }
            return list;
        } catch (Exception e) {
            throw new DataAccessException(0, "Failed to load task templates", e);
        } finally {
            db.closeConnection();
        }
    }

    public void createTaskTemplate(TaskVO taskTemplate) throws DataAccessException {
        // not for v4
    }
    public void deleteTaskTemplate(Long taskId) throws DataAccessException {
        // not for v4
    }
    public void updateTaskTemplate(TaskVO taskTemplate) throws DataAccessException {
        // not for v4
    }
    public long addTaskTemplateToPackage(Long taskId, Long packageId) throws DataAccessException {
        return 0L;
        // not for v4
    }
    public void removeTaskTemplateFromPackage(Long taskId, Long packageId) throws DataAccessException {
        // not for v4
    }
}

