/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.util.Map;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;

/**
 * common interface for all process persistence
 */
public interface ProcessPersister {

    public static enum PersistType {
        CREATE, UPDATE, NEW_VERSION, IMPORT, SAVE
    }

    int getDatabaseVersion();

    Long persistPackage(PackageVO packageVO, PersistType persistType)
    throws DataAccessException;

    int deletePackage(Long packageId)
    throws DataAccessException;

    void deleteProcess(ProcessVO process)
    throws DataAccessException;

    Long persistProcess(ProcessVO process, PersistType persistType)
    throws DataAccessException, XmlException;

    void createExternalEvent(ExternalEventVO eventHandler)
    throws DataAccessException;

    void deleteExternalEvent(Long handlerId)
    throws DataAccessException;

    void updateExternalEvent(ExternalEventVO eventHandler)
    throws DataAccessException;

    void createTaskTemplate(TaskVO taskTemplate)
    throws DataAccessException;

    void deleteTaskTemplate(Long taskId)
    throws DataAccessException;

    void updateTaskTemplate(TaskVO taskTemplate)
    throws DataAccessException;

    Long createActivityImplementor(ActivityImplementorVO implementor)
    throws DataAccessException;

    void deleteActivityImplementor(Long implementorId)
    throws DataAccessException;

    void deleteActivitiesForImplementor(ActivityImplementorVO vo)
    throws DataAccessException;

    void updateActivityImplementor(ActivityImplementorVO vo)
    throws DataAccessException;

    long renameProcess(Long processId, String newName, int newVersion)
    throws DataAccessException;

    long renamePackage(Long packageId, String newName, int newVersion)
    throws DataAccessException;

    /**
     * TODO: The signatures for renameProcess() and renamePackage() should be similar.
     */
    void renameRuleSet(RuleSetVO ruleset, String newName)
    throws DataAccessException;

    long addProcessToPackage(Long processId, Long packageId)
    throws DataAccessException;

    void removeProcessFromPackage(Long processId, Long packageId)
    throws DataAccessException;

    long addExternalEventToPackage(Long externalEventId, Long packageId)
    throws DataAccessException;

    void removeExternalEventFromPackage(Long externalEventId, Long packageId)
    throws DataAccessException;

    long addTaskTemplateToPackage(Long taskId, Long packageId)
    throws DataAccessException;

    void removeTaskTemplateFromPackage(Long taskId, Long packageId)
    throws DataAccessException;

    long addActivityImplToPackage(Long activityImplId, Long packageId)
    throws DataAccessException;

    void removeActivityImplFromPackage(Long activityImplId, Long packageId)
    throws DataAccessException;

    long addRuleSetToPackage(Long ruleSetId, Long packageId)
    throws DataAccessException;

    void removeRuleSetFromPackage(Long ruleSetId, Long packageId)
    throws DataAccessException;

    Long createRuleSet(RuleSetVO ruleset)
    throws DataAccessException;

    void updateRuleSet(RuleSetVO ruleset)
    throws DataAccessException;

    void deleteRuleSet(Long ruleSetId)
    throws DataAccessException;

    String lockUnlockProcess(Long processId, String cuid, boolean lock)
	throws DataAccessException;

    String lockUnlockRuleSet(Long ruleSetId, String cuid, boolean lock)
	throws DataAccessException;

    Long setAttribute(String ownerType, Long ownerId, String attrname, String attrvalue)
    throws DataAccessException;

    void setAttributes(String ownerType, Long ownerId, Map<String,String> attributes)
    throws DataAccessException;

    Long setCustomAttribute(CustomAttributeVO customAttrVO)
    throws DataAccessException;
}
