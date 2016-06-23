/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.value.project.Solution;
import com.centurylink.mdw.model.value.project.SolutionList;
import com.centurylink.mdw.model.value.project.Solution.MemberType;

public interface SolutionServices {

    public SolutionList getSolutions() throws ServiceException;
    public Solution getSolution(String id) throws ServiceException;

    public void createSolution(Solution solution) throws ServiceException;
    public void updateSolution(Solution solution) throws ServiceException;
    public void addMemberToSolution(String id, MemberType memberType, String memberId) throws ServiceException;
    public void removeMemberFromSolution(String id, MemberType memberType, String memberId) throws ServiceException;
    public void deleteSolution(String id) throws ServiceException;

}
