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
import com.centurylink.mdw.cache.impl.AssetHistory;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.WorkflowDataAccess;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;

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

    public static String name = "ProcessCache";
    private static ProcessCache singleton = null;
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private volatile Map<Long,Process> processMap;
    private volatile Map<String,List<Process>> procNameMap;
    private volatile Map<String,Process> procNameLatest;

    private ProcessCache() {
        processMap = new ConcurrentHashMap<>();
        procNameMap = new ConcurrentHashMap<>();
        procNameLatest = new ConcurrentHashMap<>();
    }

    synchronized private static ProcessCache getSingleton() {
        if (singleton == null) {
            singleton = new ProcessCache();
            (new CacheRegistration()).registerCache(name, singleton);
        }
        return singleton;
    }

    public void clearCache() {
        synchronized(processMap) {
            processList = null;
            processMap.clear();
            procNameMap.clear();
            procNameLatest.clear();
        }
    }

    public void refreshCache() {
            clearCache();
    }

    public static Process getProcess(Long processId) {
        return getSingleton().getProcess0(processId);
    }

    public static Process getProcessInstanceDefiniton(Long processId, Long processInstDefId) {
        return getSingleton().getProcessInstanceDefinition0(processId, processInstDefId);
    }

    public static Process getProcess(String procname, int version) {
        return getSingleton().getProcess0(procname, version, true);
    }

    /**
     * Returns null when not found rather than throwing an exception.
     */
    public static Process getProcess(String name) {
        return getSingleton().getProcess0(name, 0, false);
    }

    private void putInCache(Process process) {
        putInCache(process, false);
    }

    private void putInCache(Process process, boolean versionZero) {
        processMap.put(process.getId(), process);
        List<Process> vl = procNameMap.get(process.getQualifiedName());
        if (vl == null) {
            vl = new ArrayList<>();
            procNameMap.put(process.getQualifiedName(), vl);
        }
        vl.add(process);
        if (versionZero)
            procNameLatest.put(process.getQualifiedName(), process);
    }

    private Process getProcessInstanceDefinition0(Long processId, Long processInstDefId) {
        Process procdef = getProcess0(processId);
        if (procdef != null) {
            try {
                Document instanceDoc = getWorkflowDao().getDocument(processInstDefId);
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
            } catch (DataAccessException ex) {
                logger.severeException("Error retrieving instance document: " + processInstDefId, ex);
            }
        }
        return null;
    }

    private Process getProcess0(Long processId) {
        Process process = null;
        Map<Long,Process> processMapTemp = processMap;
        if (processMapTemp.containsKey(processId)){
            process = processMapTemp.get(processId);
        } else {
            synchronized(processMap) {
                processMapTemp = processMap;
                if (processMapTemp.containsKey(processId))
                    process = processMapTemp.get(processId);
                else {
                    process = loadProcess(processId);
                    if (process != null) {
                        putInCache(process);
                    }
                }
            }
        }
        return process;
    }

    /**
     * @deprecated use {@link #getProcesses(boolean)}
     */
    @Deprecated
    public static synchronized List<Process> getAllProcesses() throws DataAccessException {
        return getProcesses(true);
    }

    private static List<Process> processList;
    private static List<Process> latestProcesses;
    public static synchronized List<Process> getProcesses(boolean withArchived) throws DataAccessException {
        if (withArchived) {
            if (processList == null)
                processList = loadProcesses(true);
            return processList;
        }
        else {
            if (latestProcesses == null)
                latestProcesses = loadProcesses(false);
            return latestProcesses;
        }
    }

    private static List<Process> loadProcesses(boolean withArchived) throws DataAccessException {
        List<Process> processes = DataAccess.getProcessLoader().getProcessList();
        if (withArchived) {
            if (PropertyManager.getProperty(PropertyNames.MDW_GIT_USER) != null) {
                for (Process process : AssetHistory.getProcesses()) {
                    if (!processes.contains(process))
                        processes.add(process);
                }
            }
        }
        Collections.sort(processes);
        return processes;
    }


    /**
     * Either a specific version number can be specified, or a Smart Version can be specified which designates an allowable range.
     * @see com.centurylink.mdw.model.asset.AssetVersion#meetsSpec(AssetVersionSpec)
     */
    public static Process getProcessSmart(AssetVersionSpec spec) throws DataAccessException {
        if (spec.getPackageName() == null)
            throw new DataAccessException("Spec must be package-qualified: " + spec);
        Process match = null;
        String specQualifiedName = spec.getQualifiedName();
        if (specQualifiedName.endsWith(".proc"))
            specQualifiedName = specQualifiedName.substring(0, specQualifiedName.length() - 5);
        for (Process process : getProcesses(true)) {
            if (specQualifiedName.equals(process.getQualifiedName())) {
                if (process.meetsVersionSpec(spec.getVersion()) && (match == null || process.getVersion() > match.getVersion()))
                    match = process;
            }
        }
        if (match == null) {
            return null;
        }
        else {
            return getProcess(match.getId());
        }
    }

    /**
     * Find all definitions matching the specified version spec.  Returns shallow processes.
     */
    public static List<Process> getProcessesSmart(AssetVersionSpec spec) throws DataAccessException {
        if (spec.getPackageName() == null)
            throw new DataAccessException("Spec must be package-qualified: " + spec);
        List<Process> matches = new ArrayList<>();
        for (Process process : getProcesses(true)) {
            if (spec.getQualifiedName().equals(process.getQualifiedName())) {
                if (process.meetsVersionSpec(spec.getVersion()))
                    matches.add(process);
            }
        }
        return matches;
    }

    private Process getProcess0(String procname, int version, boolean exceptionWhenNotFound) {
        if (procname.indexOf("/") < 0) {
            String msg = procname + " not retrieved. Process names must be package-qualified.";
            logger.severeException(msg, new DataAccessException(msg));
            return null;
        }

        if (procname.endsWith(".proc"))
            procname = procname.substring(0, procname.length() - 5);

        Process procdef = getProcess0(procname, version);

        if (procdef == null) {
            synchronized(processMap) {
                // Try to see if it got loaded while waiting to enter
                procdef = getProcess0(procname, version);
                if (procdef == null)
                    procdef = loadProcess(procname, version, exceptionWhenNotFound);
                if (procdef != null) {
                    putInCache(procdef, version == 0);
                }
            }
        }
        return procdef;
    }

    private Process getProcess0(String procname, int version) {
        Process procdef = null;

        if (version == 0) {
            Map<String,Process> procNameLatestTemp = procNameLatest;
            procdef = procNameLatestTemp.get(procname);
        }
        else {
            Map<String,List<Process>> procNameMapTemp = procNameMap;
            List<Process> vl = procNameMapTemp.get(procname);
            if (vl != null) {
                for (Process p : vl) {
                    if (p.getVersion() == version) {
                        procdef = p;
                        break;
                    }
                }
            }
        }
        return procdef;
    }

    private Process loadProcess(Long id) {
        CodeTimer timer = new CodeTimer("ProcessCache.loadProcess()", true);
        try {
            Process proc = DataAccess.getProcessLoader().loadProcess(id, true);
            // If proc == null, check history
            if (proc == null) {
                proc = AssetHistory.getProcess(id);
                if (proc != null)
                    applyMonitors(proc);
            }
            return proc;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }

    private Process loadProcess(String name, int version, boolean exceptionWhenNotFound) {
        CodeTimer timer = new CodeTimer("ProcessCache.loadProcess()", true);
        try {
            Process proc = DataAccess.getProcessLoader().getProcessBase(name, version);
            if (proc == null && version != 0) {
                // search history when not looking for "latest"
                String spec = name;
                if (!spec.endsWith(".proc"))
                    spec += ".proc";
                spec += " v" + AssetVersion.formatVersion(version);
                proc = AssetHistory.getProcess(AssetVersionSpec.parse(spec));
                if (proc != null)
                    applyMonitors(proc);
            }
            if (proc == null && exceptionWhenNotFound)
                throw new Exception("Process not found: " + name + (version == 0 ? "" : " v" + AssetVersion.formatVersion(version)));
            return proc;
        }
        catch (Exception ex) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }

    private WorkflowDataAccess getWorkflowDao() {
        return new WorkflowDataAccess();
    }

    /**
     * Monitors for archived processes are taken from latest process definition.
     * If process has been deleted, monitors are removed from archived.
     */
    private void applyMonitors(Process process) {
        Process latestProcess = getProcess(process.getQualifiedName());
        if (latestProcess == null || latestProcess.getVersion() != process.getVersion()) {
            // apply monitors from latest process/activities
            process.setAttribute(MONITORS, latestProcess == null ? null : latestProcess.getAttribute(MONITORS));
            for (Activity activity : process.getActivities()) {
                Activity latestActivity = latestProcess == null ? null : latestProcess.getActivity(activity.getId());
                activity.setAttribute(MONITORS, latestActivity == null ? null : latestActivity.getAttribute(MONITORS));
            }
        }
    }
}
