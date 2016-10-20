/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

/**
 * common interface for all process loading
 */
public interface ProcessLoader {

    PackageVO loadPackage(Long packageId, boolean deep)
    throws DataAccessException;

    PackageVO getPackage(String name)
    throws DataAccessException;

    List<PackageVO> getPackageList(boolean deep, ProgressMonitor progressMonitor)
    throws DataAccessException;

    List<ProcessVO> getProcessList()
    throws DataAccessException;

    ProcessVO loadProcess(Long processID, boolean withSubProcesses)
    throws DataAccessException;

    ProcessVO getProcessBase(Long processId)
    throws DataAccessException;

    ProcessVO getProcessBase(String name, int version)
    throws DataAccessException;

    List<ExternalEventVO> loadExternalEvents()
    throws DataAccessException;

    List<TaskVO> getTaskTemplates()
    throws DataAccessException;

    List<ActivityImplementorVO> getActivityImplementors()
    throws DataAccessException;

    List<VariableTypeVO> getVariableTypes()
    throws DataAccessException;

    List<TaskCategory> getTaskCategories()
    throws DataAccessException;

    List<ActivityImplementorVO> getReferencedImplementors(PackageVO packageVO)
    throws DataAccessException;

    List<ProcessVO> findCallingProcesses(ProcessVO subproc)
    throws DataAccessException;

    List<ProcessVO> findCalledProcesses(ProcessVO main)
    throws DataAccessException;

    List<RuleSetVO> getRuleSets()
    throws DataAccessException;

    RuleSetVO getRuleSet(Long ruleSetId)
    throws DataAccessException;

    RuleSetVO getRuleSet(String name, String language, int version)
    throws DataAccessException;

    RuleSetVO getRuleSet(Long packageId, String name)
    throws DataAccessException;

    RuleSetVO getRuleSetForOwner(String ownerType, Long ownerId)
    throws DataAccessException;

    public List<ProcessVO> getProcessListForImplementor(Long implementorId, String implementorClass)
    throws DataAccessException;

    public Map<String,String> getAttributes(String ownerType, Long ownerId)
    throws DataAccessException;
}
