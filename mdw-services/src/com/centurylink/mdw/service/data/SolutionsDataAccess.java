/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.Solution;
import com.centurylink.mdw.model.workflow.Solution.MemberType;
import com.centurylink.mdw.model.workflow.WorkStatuses;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class SolutionsDataAccess extends CommonDataAccess {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected static final String SOLUTION_COLS = "s.solution_id, s.id, s.name, s.owner_type, s.owner_id, s.create_dt, s.create_usr, s.mod_dt, s.mod_usr, s.comments";
    protected static final String SOLUTION_MAP_COLS = "sm.solution_id, sm.member_type, sm.member_id";

    protected static final String TASK_INST_COLS = "ti.task_instance_id, ti.task_id, ti.task_instance_status, ti.task_instance_owner, ti.task_instance_owner_id, " +
            "ti.task_inst_secondary_owner, task_inst_secondary_owner_id, ti.task_claim_user_id, ti.task_start_dt, ti.task_end_dt, ti.comments, ti.task_instance_state, " +
            "ti.due_date, ti.priority, ti.master_request_id, ti.task_instance_referred_as";

    public SolutionsDataAccess() {
        super(null, DataAccess.currentSchemaVersion, DataAccess.supportedSchemaVersion);
    }

    /**
     * TODO: pagination and filtering
     */
    public List<Solution> getSolutions() throws DataAccessException {

        try {
            List<Solution> solutions = new ArrayList<Solution>();
            db.openConnection();
            String sql = "select " + SOLUTION_COLS + " from solution s";
            ResultSet rs = db.runSelect(sql, null);
            while (rs.next())
                solutions.add(buildSolution(rs, false, false));
            return solutions;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve Solutions", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Solution getSolution(String id) throws DataAccessException {
        return getSolution(id, false);
    }

    public Solution getSolution(String id, boolean deep) throws DataAccessException {
        try {
            db.openConnection();
            String sql = "select " + SOLUTION_COLS + " from solution s where s.id = ?";
            ResultSet rs = db.runSelect(sql, id);
            if (rs.next())
                return buildSolution(rs, deep, deep);
            else
                return null;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve Solution: " + id, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Solution getSolution(Long solutionId) throws DataAccessException {

        try {
            db.openConnection();
            String sql = "select " + SOLUTION_COLS + " from solution s where s.solution_id = ?";
            ResultSet rs = db.runSelect(sql, solutionId);
            if (rs.next())
                return buildSolution(rs, true, true);
            else
                return null;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve Solution ID: " + solutionId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Map<MemberType,List<Jsonable>> getMembers(Long solutionId) throws DataAccessException {
        try {
            db.openConnection();
            return getMembers0(solutionId);
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to retrieve members for solution: " + solutionId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private Map<MemberType,List<Jsonable>> getMembers0(Long solutionId) throws SQLException {
        Map<MemberType,List<Jsonable>> members = new HashMap<MemberType,List<Jsonable>>();
        for (MemberType type : MemberType.values()) {
            List<Jsonable> membersForType = getMembers0(solutionId, type);
            if (membersForType != null)
                members.put(type,  membersForType);
        }
        return members;
    }

    private List<Jsonable> getMembers0(Long solutionId, MemberType type) throws SQLException {
        switch(type) {
            case MasterRequest:
              return getMasterRequestMembers0(solutionId);
            case TaskInstance:
              return getTaskInstanceMembers0(solutionId);
            case ProcessInstance:
              return getProcessInstanceMembers0(solutionId);
            case Solution:
              return getSolutionMembers0(solutionId, true);
            case Other:
                return null;
            default:
                throw new IllegalArgumentException("Unsupported solution MemberType: " + type);
        }
    }

    private List<Jsonable> getMasterRequestMembers0(Long solutionId) throws SQLException {
        String query = "select " + SOLUTION_MAP_COLS + ", " + PROC_INST_COLS +
                " from solution_map sm, process_instance pi" +
            "\n where sm.solution_id = ?" +
            "\n and sm.member_type = '"  + MemberType.MasterRequest + "'" +
            "\n and pi.master_request_id = sm.member_id" +
            "\n and pi.owner != '" + OwnerType.PROCESS_INSTANCE + "'" +
            "\n and pi.owner != '" + OwnerType.MAIN_PROCESS_INSTANCE + "'";

        List<Jsonable> members = null;
        ResultSet rs = db.runSelect(query, solutionId);
        while (rs.next()) {
            if (members == null)
                members = new ArrayList<Jsonable>();
            Request request = new Request(0l);
            request.setMasterRequestId(rs.getString("member_id"));
            ProcessInstance pi = buildProcessInstance(rs);
            request.setCreated(rs.getTimestamp("start_dt"));
            request.setMasterRequestId(pi.getMasterRequestId());
            request.setProcessInstanceId(pi.getId());
            request.setProcessId(pi.getProcessId());
            request.setProcessName(pi.getProcessName());
            request.setProcessVersion(pi.getProcessVersion());
            request.setPackageName(pi.getPackageName());
            request.setProcessStatus(WorkStatuses.getName(pi.getStatusCode()));
            request.setProcessStart(rs.getTimestamp("start_dt"));
            request.setProcessEnd(rs.getTimestamp("end_dt"));
            members.add(request);
        }
        return members;
    }

    private List<Jsonable> getTaskInstanceMembers0(Long solutionId) throws SQLException {
        String query = "select " + SOLUTION_MAP_COLS + ", " + TASK_INST_COLS +
                " from solution s, solution_map sm, task_instance ti" +
            "\n where s.solution_id = ?" +
            "\n and sm.solution_id = s.solution_id" +
            "\n and sm.member_type = '"  + MemberType.TaskInstance + "'" +
            "\n and ti.task_instance_id = sm.member_id";

        List<Jsonable> members = null;
        ResultSet rs = db.runSelect(query, solutionId);
        while (rs.next()) {
            if (members == null)
                members = new ArrayList<Jsonable>();
            members.add(buildTaskInstance(rs));
        }
        return members;
    }

    private List<Jsonable> getProcessInstanceMembers0(Long solutionId) throws SQLException {
        String query = "select " + SOLUTION_COLS + ", " + SOLUTION_MAP_COLS + ", " + PROC_INST_COLS +
                " from solution s, solution_map sm, process_instance pi" +
            "\n where s.solution_id = ?" +
            "\n and s.solution_id = sm.solution_id" +
            "\n and sm.member_type = '"  + MemberType.ProcessInstance + "'" +
            "\n and pi.process_instance_id = sm.member_id";

        List<Jsonable> members = null;
        ResultSet rs = db.runSelect(query, solutionId);
        while (rs.next()) {
            if (members == null)
                members = new ArrayList<Jsonable>();
            members.add(buildProcessInstance(rs));
        }
        return members;
    }

    private List<Jsonable> getSolutionMembers0(Long solutionId, boolean deep) throws SQLException {
        String query = "select " + SOLUTION_MAP_COLS +
                " from solution_map sm" +
            "\n where sm.solution_id = ?" +
            "\n and sm.member_type = '"  + MemberType.Solution + "'";

        List<String> memberIds = null;
        ResultSet rs = db.runSelect(query, solutionId);
        while (rs.next()) {
            if (memberIds == null)
                memberIds = new ArrayList<String>();
            memberIds.add(rs.getString("member_id"));
        }
        List<Jsonable> members = null;
        if (memberIds != null) {
            for (String memberId : memberIds) {
                if (members == null)
                    members = new ArrayList<Jsonable>();
                String sql = "select " + SOLUTION_COLS + " from solution s where s.id = ?";
                ResultSet rs2 = db.runSelect(sql, memberId);
                if (rs2.next())
                    members.add(buildSolution(rs2, deep, deep));
            }
        }
        return members;
    }

    public void saveSolution(Solution solution) throws DataAccessException {
        try {
            Long solutionId = solution.getSolutionId();
            db.openConnection();
            if (solutionId == null || solutionId.longValue() <= 0L) {
                solutionId = db.isMySQL() ? null : getNextId("MDW_COMMON_ID_SEQ");
                String query = "insert into solution" +
                  "\n(solution_id, id, name, owner_type, owner_id, create_dt, create_usr, comments)" +
                  "\nvalues (?, ?, ?, ?, ?, " + now() + ", ?, ?)";
                Object[] args = new Object[7];
                args[0] = solutionId;
                args[1] = solution.getId();
                args[2] = solution.getName();
                args[3] = solution.getOwnerType();
                args[4] = solution.getOwnerId();
                args[5] = solution.getCreatedBy() == null ? "MDW" : solution.getCreatedBy();
                args[6] = solution.getDescription();
                if (db.isMySQL())
                    solutionId = db.runInsertReturnId(query, args);
                else
                    db.runUpdate(query, args);
                solution.setSolutionId(solutionId);
            }
            else {
                String query = "update solution set" +
                  "\nname = ?, owner_type = ?, owner_id = ?, mod_dt = " + now() + ", mod_usr = ?, comments = ?" +
                   "\nwhere id = ?";
                Object[] args = new Object[6];
                args[0] = solution.getName();
                args[1] = solution.getOwnerType();
                args[2] = solution.getOwnerId();
                args[3] = solution.getModifiedBy() == null ? "MDW" : solution.getModifiedBy();
                args[4] = solution.getDescription();
                args[5] = solution.getId();
                db.runUpdate(query, args);
                setValues0(OwnerType.SOLUTION, solution.getId(), solution.getValues());
            }
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException(-1, "Failed to update solution: " + solution.getId(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void addMember(Long solutionId, MemberType memberType, String memberId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "insert into solution_map" +
              "\n (solution_id, member_type, member_id, create_dt, create_usr, comments)" +
              "\n values (?, ?, ?, " + now() + ", ?, ?)";
            Object[] args = new Object[5];
            args[0] = solutionId;
            args[1] = memberType.toString();
            args[2] = memberId;
            args[3] = "MDW";
            args[4] = "null";
            db.runUpdate(query, args);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException("Failed to add " + memberType + ": " + memberId + " to solution: " + solutionId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void removeMember(Long solutionId, MemberType memberType, String memberId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from solution_map" +
              "\n where solution_id = ?" +
              "\n and member_type = ?" +
              "\n and member_id = ?";
            Object[] args = new Object[3];
            args[0] = solutionId;
            args[1] = memberType.toString();
            args[2] = memberId;
            db.runUpdate(query, args);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException("Failed to remove " + memberType + ": " + memberId + " to solution: " + solutionId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void deleteSolution(Long solution_id, String id) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from solution_map " +
              "\n where solution_id = ?";
            db.runUpdate(query, solution_id);
            query = "delete from value " +
                    "\n where owner_type = ? " +
                    "\n and owner_id = ? ";
            Object[] args = new Object[2];
            args[0] = OwnerType.SOLUTION;
            args[1] = id;
            db.runUpdate(query, args);
            query = "delete from solution" +
                    "\n where solution_id = ?";
            db.runUpdate(query, solution_id);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException("Failed to delete solution: " + id, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<String> getValueNames() throws DataAccessException {
        return getValueNames(OwnerType.SOLUTION);
    }


    private Solution buildSolution(ResultSet rs, boolean includeValues, boolean includeMembers) throws SQLException {
        Long solutionId = rs.getLong("solution_id");
        String id = rs.getString("id");
        String name = rs.getString("name");
        String ownerType = rs.getString("owner_type");
        String ownerId = rs.getString("owner_id");
        Date createDate = rs.getTimestamp("create_dt");
        String createUser = rs.getString("create_usr");
        Solution solution = new Solution(solutionId, id, name, ownerType, ownerId, createDate, createUser);
        solution.setDescription(rs.getString("comments"));
        solution.setModified(rs.getTimestamp("mod_dt"));
        solution.setModifiedBy(rs.getString("mod_usr"));
        if (includeValues)
            solution.setValues(getValues0(OwnerType.SOLUTION, id));
        if (includeMembers) {
            solution.setMembers(getMembers0(solutionId));
        }
        return solution;
    }

    /**
     * Assumes ti.* table prefix.
     */
    protected TaskInstance buildTaskInstance(ResultSet rs) throws SQLException {
        TaskInstance task = new TaskInstance();
        task.setTaskInstanceId(rs.getLong("task_instance_id"));
        task.setTaskId(rs.getLong("task_id"));
        task.setStatusCode(rs.getInt("task_instance_status"));
        task.setOwnerType(rs.getString("task_instance_owner"));
        task.setOwnerId(rs.getLong("task_instance_owner_id"));
        task.setSecondaryOwnerType(rs.getString("task_inst_secondary_owner"));
        task.setSecondaryOwnerId(rs.getLong("task_inst_secondary_owner_id"));
        task.setTaskClaimUserId(rs.getLong("task_claim_user_id"));
        task.setStartDate(rs.getTimestamp("task_start_dt"));
        task.setEndDate(rs.getTimestamp("task_end_dt"));
        task.setComments(rs.getString("comments"));
        task.setStateCode(rs.getInt("task_instance_state"));
        task.setDueDate(rs.getTimestamp("due_date"));
        task.setPriority(rs.getInt("priority"));
        task.setMasterRequestId(rs.getString("master_request_id"));
        TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(task.getTaskId());
        if (taskVO == null) {
            String ref = rs.getString("task_instance_referred_as");
            logger.severe("ERROR: Task instance ID " + task.getTaskInstanceId() + " missing task definition (" + ref + ").");
            task.setTaskName(ref);
            task.setInvalid(true);
        }
        else {
            task.setCategoryCode(taskVO.getTaskCategory());
            task.setTaskName(taskVO.getTaskName());
            task.setDescription(taskVO.getComment());
        }
        try {
            if (task.getTaskClaimUserId() != 0L) {
                User user = UserGroupCache.getUser(task.getTaskClaimUserId());
                if (user == null)
                    logger.severe("ERROR: Cannot find user for id: " + task.getTaskClaimUserId());
                else
                    task.setTaskClaimUserCuid(user.getCuid());
            }
        }
        catch (CachingException ex) {
            logger.severeException(ex.getMessage(),  ex);
        }
        return task;
    }


}