/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.workflow.Solution;
import com.centurylink.mdw.model.workflow.SolutionList;
import com.centurylink.mdw.model.workflow.Solution.MemberType;

public interface SolutionServices {

    public SolutionList getSolutions() throws ServiceException;
    public Solution getSolution(String id) throws ServiceException;

    public void createSolution(Solution solution) throws ServiceException;
    public void updateSolution(Solution solution) throws ServiceException;
    public void addMemberToSolution(String id, MemberType memberType, String memberId) throws ServiceException;
    public void removeMemberFromSolution(String id, MemberType memberType, String memberId) throws ServiceException;
    public void deleteSolution(String id) throws ServiceException;

}
