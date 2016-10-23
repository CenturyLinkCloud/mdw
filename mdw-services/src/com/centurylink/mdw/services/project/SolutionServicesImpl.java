/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.workflow.Solution;
import com.centurylink.mdw.model.workflow.SolutionList;
import com.centurylink.mdw.model.workflow.Solution.MemberType;
import com.centurylink.mdw.service.data.SolutionsDataAccess;
import com.centurylink.mdw.services.SolutionServices;

public class SolutionServicesImpl implements SolutionServices {

    private SolutionsDataAccess getDAO() {
        return new SolutionsDataAccess();
    }

    public SolutionList getSolutions() throws ServiceException {
        try {
            List<Solution> solutions = getDAO().getSolutions();
            SolutionList solutionList = new SolutionList(solutions);
            solutionList.setRetrieveDate(DatabaseAccess.getDbDate());
            solutionList.setTotal(solutions.size());
            return solutionList;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public Solution getSolution(String id) throws ServiceException {
        try {
            Solution solution = getDAO().getSolution(id, true);
            if (solution != null) {
                Map<String,String> values = solution.getValues();
                for (String key : getDAO().getValueNames()) {
                    if (values == null) {
                        values = new HashMap<String,String>();
                        solution.setValues(values);
                    }
                    if (!values.containsKey(key))
                        values.put(key, null); // empty value
                }
            }
            return solution;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public void createSolution(Solution solution) throws ServiceException {
        try {
            Solution existing = getDAO().getSolution(solution.getId());
            if (existing != null)
                throw new ServiceException(409, "Solution ID: " + solution.getId() + " already exists"); //TODO Should we update solution instead of throwing ex
            getDAO().saveSolution(solution);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public void updateSolution(Solution solution) throws ServiceException {
        try {
            getDAO().saveSolution(solution);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public void addMemberToSolution(String id, MemberType memberType, String memberId) throws ServiceException {
        try {
            Solution solution = getDAO().getSolution(id);
            if (solution == null)
                throw new ServiceException(404, "Solution ID not found: " + id);
            getDAO().addMember(solution.getSolutionId(), memberType, memberId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, ex.getMessage(), ex);
        }
    }

    public void removeMemberFromSolution(String id, MemberType memberType, String memberId) throws ServiceException {
        try {
            Solution solution = getDAO().getSolution(id);
            if (solution == null)
                throw new ServiceException(404, "Solution ID not found: " + id);
            getDAO().removeMember(solution.getSolutionId(), memberType, memberId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, ex.getMessage(), ex);
        }
    }

    public void deleteSolution(String id) throws ServiceException {
        try {
            Solution solution = getDAO().getSolution(id);
            if (solution == null)
                throw new ServiceException(404, "Solution ID not found: " + id);
            getDAO().deleteSolution(solution.getSolutionId(), id); //Pass Friendly ID to be used for Value owner_id
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

}
