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
package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.workflow.Solution;
import com.centurylink.mdw.model.workflow.SolutionList;
import com.centurylink.mdw.model.workflow.Solution.MemberType;

public interface SolutionServices {

    public SolutionList getSolutions(Query query) throws ServiceException;
    public Solution getSolution(String id) throws ServiceException;

    public void createSolution(Solution solution) throws ServiceException;
    public void updateSolution(Solution solution) throws ServiceException;
    public void addMemberToSolution(String id, MemberType memberType, String memberId) throws ServiceException;
    public void removeMemberFromSolution(String id, MemberType memberType, String memberId) throws ServiceException;
    public void deleteSolution(String id) throws ServiceException;

}
