package com.centurylink.mdw.services;

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.workflow.Solution;
import com.centurylink.mdw.model.workflow.Solution.MemberType;
import com.centurylink.mdw.model.workflow.SolutionList;

public interface SolutionServices {

    public SolutionList getSolutions(Query query) throws ServiceException;
    public SolutionList getSolutions() throws ServiceException;
    public Solution getSolution(String id) throws ServiceException;

    public void createSolution(Solution solution) throws ServiceException;
    public void updateSolution(Solution solution) throws ServiceException;
    public void addMemberToSolution(String id, MemberType memberType, String memberId) throws ServiceException;
    public void removeMemberFromSolution(String id, MemberType memberType, String memberId) throws ServiceException;
    public Map<MemberType,List<Jsonable>> getMembers(Long solutionId) throws ServiceException;
    public List<Jsonable> getMembers(Long solutionId, MemberType memberType) throws ServiceException;
    public void deleteSolution(String id) throws ServiceException;
    public List<Solution> getSolutions(MemberType memberType, String memberId) throws ServiceException ;

}
