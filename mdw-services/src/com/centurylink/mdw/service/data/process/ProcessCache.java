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
package com.centurylink.mdw.service.data.process;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.WorkflowDataAccess;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.centurylink.mdw.constant.WorkAttributeConstant.MONITORS;

/**
 * Lazily loads the Processes for use by the RuntimeEngine.
 */
public class ProcessCache implements CacheService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static List<Process> allProcesses;
    private static final Map<Long,Process> allById = new ConcurrentHashMap<>();
    private static List<Process> latestProcesses;
    private static final Map<String,Process> latestByPath = new ConcurrentHashMap<>();

    private ProcessCache() {
    }

    static {
        new CacheRegistration().registerCache("ProcessCache", new ProcessCache());
    }

    public void clearCache() {
        synchronized(ProcessCache.class) {
            allProcesses = null;
            allById.clear();
            latestProcesses = null;
            latestByPath.clear();
        }
    }

    public void refreshCache() {
        clearCache();
    }

    public static synchronized List<Process> getProcesses(boolean withArchived) {
        if (withArchived) {
            if (allProcesses == null)
                allProcesses = loadProcesses(true);
            return allProcesses;
        }
        else {
            if (latestProcesses == null)
                latestProcesses = loadProcesses(false);
            return latestProcesses;
        }
    }

    public static Process getProcess(Long id) throws IOException {
        Process process = allById.get(id);
        if (process == null) {
            synchronized (ProcessCache.class) {
                process = allById.get(id);
                if (process == null) {
                    for (Process proc : getProcesses(true)) {
                        if (proc.getId().equals(id)) {
                            process = proc;
                            if (!process.isLoaded()) {
                                process = loadProcess(process);
                                Process latest = getProcess(process.getPath());
                                // latest is null if process has been deleted
                                if (latest != null && latest.getVersion() != process.getVersion()) {
                                    // old processes use latest monitor config
                                    applyMonitors(process);
                                }
                            }
                            allById.put(process.getId(), process);
                            break;
                        }
                    }
                }
            }
        }
        return process;
    }

    public static Process getProcess(String path) {
        if (!path.endsWith(".proc"))
            path += ".proc";
        Process process = latestByPath.get(path);
        if (process == null) {
            synchronized (ProcessCache.class) {
                process = latestByPath.get(path);
                if (process == null) {
                    for (Process proc : getProcesses(false)) {
                        if (proc.getPath().equals(path)) {
                            process = proc;
                            latestByPath.put(process.getPath(), process);
                            break;
                        }
                    }
                }
            }
        }
        return process;
    }

    public static Process getProcessSmart(AssetVersionSpec spec) throws IOException {
        if (!spec.getPath().endsWith(".proc"))
            spec = new AssetVersionSpec(spec.getPath() + ".proc", spec.getVersion());
        Asset asset = AssetCache.getAsset(spec);
        return asset == null ? null : getProcess(asset.getId());
    }

    public static Process getProcess(String path, int version) throws IOException {
        if (!path.endsWith(".proc"))
            path += ".proc";
        Asset asset = AssetCache.getAsset(path, version);
        return asset == null ? null : getProcess(asset.getId());
    }

    /**
     * Archived are unloaded processes.
     */
    private static List<Process> loadProcesses(boolean withArchived) {
        List<Process> processes = new ArrayList<>();
        for (Asset asset : AssetCache.getAssets("proc", withArchived)) {
            String name = asset.getName().substring(0, asset.getName().length() - 5);
            Process process = new Process(asset.getId(), name, asset.getPackageName(), asset.getVersion());
            if (!processes.contains(process)) {
                if (!withArchived && !process.isLoaded()) {
                    // latest processes are always loaded (and their monitors are verbatim)
                    try {
                        process = loadProcess(process);
                    }
                    catch (Exception ex) {
                        logger.error("Cannot load process: " + process.getLabel(), ex);
                    }
                }
                processes.add(process);
            }
        }
        Collections.sort(processes);
        return processes;
    }

    private static Process loadProcess(Asset asset) throws IOException {
        Asset loadedAsset = AssetCache.getAsset(asset.getId());
        Process process = Process.fromString(loadedAsset.getText());
        process.setId(loadedAsset.getId());
        process.setName(loadedAsset.getName().substring(0, loadedAsset.getName().length() - 5));
        process.setVersion(loadedAsset.getVersion());
        process.setPackageName(loadedAsset.getPackageName());
        process.setFile(loadedAsset.getFile());
        process.setArchived(loadedAsset.isArchived());
        return process;
    }

    /**
     * Monitors for archived processes are taken from latest process definition.
     * If process has been deleted, monitors are removed from archived.
     */
    private static void applyMonitors(Process process) {
        Process latestProcess = getProcess(process.getPath());
        if (latestProcess == null || latestProcess.getVersion() != process.getVersion()) {
            // apply monitors from latest process/activities
            process.setAttribute(MONITORS, latestProcess == null ? null : latestProcess.getAttribute(MONITORS));
            for (Activity activity : process.getActivities()) {
                Activity latestActivity = latestProcess == null ? null : latestProcess.getActivity(activity.getId());
                activity.setAttribute(MONITORS, latestActivity == null ? null : latestActivity.getAttribute(MONITORS));
            }
        }
    }

    public static Process getInstanceDefinition(Long processId, Long instanceDefinitionId) {
        try {
            Process procdef = getProcess(processId);
            if (procdef != null) {
                Document instanceDoc = new WorkflowDataAccess().getDocument(instanceDefinitionId);
                String content = instanceDoc.getContent(null);
                Process process = null;
                if (Jsonable.class.getName().equals(instanceDoc.getDocumentType())) {
                    // compatibility for previously-saved instances
                    JsonObject json = new JsonObject(content);
                    for (String key : json.keySet()) {
                        if (!"_type".equals(key)) {
                            process = new Process(json.getJSONObject(key));
                            break;
                        }
                    }
                }
                else {
                    process = Process.fromString(content);
                }
                if (process != null) {
                    process.setName(procdef.getName());
                    process.setPackageName(procdef.getPackageName());
                }
                return process;
            }
        }
        catch (DataAccessException | IOException ex) {
            logger.error("Error retrieving instance document: " + instanceDefinitionId, ex);
        }
        return null;
    }
}
