/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.Solution;
import com.centurylink.mdw.model.workflow.Solution.MemberType;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.SolutionServices;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Solutions")
@Api("Solutions entities")
public class Solutions extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Solution;
    }

    /**
     * Retrieve a solution or the solutions list.
     */
    @Override
    @Path("/{solutionId}")
    @ApiOperation(value="Retrieve a solution or all solutions",
        notes="If {solutionId} is not present, returns all solutions.",
        response=Solution.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        SolutionServices solutionServices = ServiceLocator.getSolutionServices();
        String id = getSegment(path, 1);
        if (id != null) {
            Solution solution = solutionServices.getSolution(id);
            if (solution == null)
                throw new ServiceException(404, "Solution not found: " + id);
            else
                return solution.getJson();
        }
        else {
            return solutionServices.getSolutions().getJson();
        }
    }

    /**
     * For create (creating a new solution, or creating a new solution/member).
     */
    @Override
    @Path("/{solutionId}/memberType/{memberId}")
    @ApiOperation(value="Create a solution or add a member to an existing solution",
        notes="Supported memberTypes: requests, tasks, processes, solutions.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Solution", paramType="body", dataType="com.centurylink.mdw.model.value.project.Solution")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String id = getSegment(path, 1);
        String rel = getSegment(path, 2);

        SolutionServices solutionServices = ServiceLocator.getSolutionServices();
        if (rel == null) {
            Solution existing = solutionServices.getSolution(id);
            if (existing != null)
                throw new ServiceException(HTTP_409_CONFLICT, "Solution ID already exists: " + id);
            Solution solution = new Solution(content);
            solution.setOwnerType(OwnerType.USER);
            String userId = headers.get(Listener.AUTHENTICATED_USER_HEADER);
            solution.setOwnerId(userId);
            solution.setCreatedBy(userId);
            solutionServices.createSolution(solution);
        }
        else if (rel.equals("requests")) {
            String requestId = getSegment(path, 3);
            solutionServices.addMemberToSolution(id, MemberType.MasterRequest, requestId);
        }
        else if (rel.equals("tasks")) {
            String taskInstanceId = getSegment(path, 3);
            solutionServices.addMemberToSolution(id, MemberType.TaskInstance, taskInstanceId);
        }
        else if (rel.equals("processes")) {
            String processInstanceId = getSegment(path, 3);
            solutionServices.addMemberToSolution(id, MemberType.ProcessInstance, processInstanceId);
        }
        else if (rel.equals("solutions")) {
            String memebrSolId = getSegment(path, 3);
            solutionServices.addMemberToSolution(id, MemberType.Solution, memebrSolId);
        }
        else {
            String msg = "Unsupported relationship for solution " + id + ": " + rel;
            throw new ServiceException(HTTP_400_BAD_REQUEST, msg);
        }
        return null;
    }

    /**
     * For update.
     */
    @Override
    @Path("/{solutionId}")
    @ApiOperation(value="Update a solution", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Solution", paramType="body", required=true, dataType="com.centurylink.mdw.model.value.project.Solution")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String id = getSegment(path, 1);
        if (id == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing segment: {id}");
        SolutionServices solutionServices = ServiceLocator.getSolutionServices();
        Solution solution = new Solution(content);
        Solution existing = solutionServices.getSolution(id);
        if (existing == null)
            throw new ServiceException(HTTP_404_NOT_FOUND, "Solution ID not found: " + id);
        // update
        solution.setSolutionId(existing.getSolutionId());
        solutionServices.updateSolution(solution);
        return null;
    }

    /**
     * Delete a user or a user/group, user/role relationship.
     */
    @Path("/{solutionId}/memberType/{memberId}")
    @ApiOperation(value="Delete a solution or remove a member from a solution",
        notes="Supported memberTypes: requests, tasks, processes, solutions.", response=StatusMessage.class)
    public JSONObject delete(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String id = getSegment(path, 1);
        String rel = getSegment(path, 2);

        SolutionServices solutionServices = ServiceLocator.getSolutionServices();
        if (rel == null) {
            solutionServices.deleteSolution(id);
        }
        else if (rel.equals("requests")) {
            String requestId = getSegment(path, 3);
            solutionServices.removeMemberFromSolution(id, MemberType.MasterRequest, requestId);
        }
        else if (rel.equals("tasks")) {
            String taskInstanceId = getSegment(path, 3);
            solutionServices.removeMemberFromSolution(id, MemberType.TaskInstance, taskInstanceId);
        }
        else if (rel.equals("processes")) {
            String processInstanceId = getSegment(path, 3);
            solutionServices.removeMemberFromSolution(id, MemberType.ProcessInstance, processInstanceId);
        }
        else if (rel.equals("solutions")) {
            String taskInstanceId = getSegment(path, 3);
            solutionServices.removeMemberFromSolution(id, MemberType.Solution, taskInstanceId);
        }
        return null;
    }
}