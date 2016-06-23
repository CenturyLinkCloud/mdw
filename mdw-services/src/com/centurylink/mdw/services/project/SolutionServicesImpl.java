/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.project;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.value.project.Solution;
import com.centurylink.mdw.model.value.project.Solution.MemberType;
import com.centurylink.mdw.model.value.project.SolutionList;
import com.centurylink.mdw.services.SolutionServices;
import com.centurylink.mdw.services.dao.SolutionsDAO;

public class SolutionServicesImpl implements SolutionServices {

    private SolutionsDAO getDAO() {
        return new SolutionsDAO();
    }

    public SolutionList getSolutions() throws ServiceException {
        try {
            List<Solution> solutions = getDAO().getSolutions();
            SolutionList solutionList = new SolutionList(solutions);
            solutionList.setRetrieveDate(new Date()); // TODO db date
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
            throw new ServiceException(ex.getMessage(), ex);
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
            throw new ServiceException(ex.getMessage(), ex);
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
