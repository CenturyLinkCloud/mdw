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
package com.centurylink.mdw.services.project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.workflow.Solution;
import com.centurylink.mdw.model.workflow.Solution.MemberType;
import com.centurylink.mdw.model.workflow.SolutionList;
import com.centurylink.mdw.service.data.SolutionsDataAccess;
import com.centurylink.mdw.services.SolutionServices;

public class SolutionServicesImpl implements SolutionServices {

    private SolutionsDataAccess getDAO() {
        return new SolutionsDataAccess();
    }

    public SolutionList getSolutions() throws ServiceException {
            Query query = new Query();
            return this.getSolutions(query);  //get all solutions SDWF
    }

    public SolutionList getSolutions(Query query) throws ServiceException {
        try {
            String solutionId = query.getFind();
            List<Solution> solutions = getDAO().getSolutions(solutionId);
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
    public Map<MemberType,List<Jsonable>> getMembers(Long solutionId) throws ServiceException {
        try {
            return getDAO().getMembers(solutionId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public List<Jsonable> getMembers(Long solutionId, MemberType memberType) throws ServiceException {
        try {
            return getDAO().getMembers(solutionId, memberType);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public List<Solution> getSolutions(MemberType memberType, String memberId) throws ServiceException {
        try {
            return getDAO().getSolutions(memberType, memberId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
}
