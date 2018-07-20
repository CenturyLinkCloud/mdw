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
package com.centurylink.mdw.dataaccess;

import java.util.Map;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.event.ExternalEvent;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;

/**
 * common interface for all process persistence
 */
public interface ProcessPersister {

    public static enum PersistType {
        CREATE, CREATE_JSON, UPDATE, NEW_VERSION, IMPORT, IMPORT_JSON, SAVE
    }

    int getDatabaseVersion();

    Long persistPackage(Package packageVO, PersistType persistType)
    throws DataAccessException;

    int deletePackage(Long packageId)
    throws DataAccessException;

    void deleteProcess(Process process)
    throws DataAccessException;

    Long persistProcess(Process process, PersistType persistType)
    throws DataAccessException, XmlException;

    void createExternalEvent(ExternalEvent eventHandler)
    throws DataAccessException;

    void deleteExternalEvent(Long handlerId)
    throws DataAccessException;

    void updateExternalEvent(ExternalEvent eventHandler)
    throws DataAccessException;

    void createTaskTemplate(TaskTemplate taskTemplate)
    throws DataAccessException;

    void deleteTaskTemplate(Long taskId)
    throws DataAccessException;

    void updateTaskTemplate(TaskTemplate taskTemplate)
    throws DataAccessException;

    Long createActivityImplementor(ActivityImplementor implementor)
    throws DataAccessException;

    void deleteActivityImplementor(Long implementorId)
    throws DataAccessException;

    void updateActivityImplementor(ActivityImplementor vo)
    throws DataAccessException;

    long renameProcess(Long processId, String newName, int newVersion)
    throws DataAccessException;

    long renamePackage(Long packageId, String newName, int newVersion)
    throws DataAccessException;

    /**
     * TODO: The signatures for renameProcess() and renamePackage() should be similar.
     */
    void renameAsset(Asset asset, String newName)
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

    long addAssetToPackage(Long assetId, Long packageId)
    throws DataAccessException;

    void removeAssetFromPackage(Long assetId, Long packageId)
    throws DataAccessException;

    Long createAsset(Asset asset)
    throws DataAccessException;

    void updateAsset(Asset asset)
    throws DataAccessException;

    void deleteAsset(Long assetId)
    throws DataAccessException;

    Long setAttribute(String ownerType, Long ownerId, String attrname, String attrvalue)
    throws DataAccessException;

    void setAttributes(String ownerType, Long ownerId, Map<String,String> attributes)
    throws DataAccessException;
}
