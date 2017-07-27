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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.WorkflowDataAccess;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;

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
        processMap = new ConcurrentHashMap<Long,Process>();
        procNameMap = new ConcurrentHashMap<String,List<Process>>();
        procNameLatest = new ConcurrentHashMap<String,Process>();
    }

    synchronized private static ProcessCache getSingleton() {
        if (singleton==null) {
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
        processMap.put(process.getProcessId(), process);
        List<Process> vl = procNameMap.get(process.getProcessQualifiedName());
        if (vl == null) {
            vl = new ArrayList<Process>();
            procNameMap.put(process.getProcessQualifiedName(), vl);
        }
        vl.add(process);
        if (versionZero)
            procNameLatest.put(process.getProcessQualifiedName(), process);

        if (process.getPackageName() == null)
            logger.warn("Non-Package Qualified Process Names are DEPRECATED in MDW");
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
                    if (process!=null) {
                        process.removeDeletedTransitions();
                        putInCache(process);
                    }
                }
            }
        }
        return process;
    }

    private static List<Process> processList;
    public static synchronized List<Process> getAllProcesses() throws DataAccessException {
        if (processList == null)
            processList = DataAccess.getProcessLoader().getProcessList();
        return processList;
    }

    /**
     * Either a specific version number can be specified, or a Smart Version can be specified which designates an allowable range.
     * @see AssetVO.meetsVersionSpec().
     * TODO: spec.packageName is ignored
     */
    public static Process getProcessSmart(AssetVersionSpec spec) {
        try {
            Process match = null;
            for (Process process : getAllProcesses()) {
                if (spec.getQualifiedName().equals(spec.getName())) {   // Missing package name - Match using only process name
                    if (spec.getName().equals(process.getProcessName())) {
                        if (process.meetsVersionSpec(spec.getVersion()) && (match == null || process.getVersion() > match.getVersion()))
                            match = process;
                    }
                }
                else
                    if (spec.getQualifiedName().equals(process.getProcessQualifiedName())) {   // Match using fully qualified process name
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
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    private Process getProcess0(String procname, int version, boolean exceptionWhenNotFound) {
        if (procname.endsWith(".proc"))
            procname = procname.substring(0, procname.length() - 5);

        Process procdef = null;

        if (version == 0) {
            procdef = procNameLatest.get(procname);
        }
        else {
            List<Process> vl = procNameMap.get(procname);
            if (vl != null) {
                for (Process p : vl) {
                    if (p.getVersion() == version) {
                        procdef = p;
                        break;
                    }
                }
            }
        }

        if (procdef == null) {
            synchronized(processMap) {
                procdef = loadProcess(procname, version, exceptionWhenNotFound);
                if (procdef != null) {
                    procdef.removeDeletedTransitions();
                    putInCache(procdef, version == 0);
                }
            }
        }
        return procdef;
    }

    private Process loadProcess(Long id) {
        CodeTimer timer = new CodeTimer("ProcessCache.loadProcess()", true);
        try {
            Process proc = DataAccess.getProcessLoader().loadProcess(id, true);
            if (proc != null) {
                // all db attributes are override attributes
                Map<String,String> attributes = getWorkflowDao().getAttributes(OwnerType.PROCESS, id);
                if (attributes != null)
                    proc.applyOverrideAttributes(attributes);
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
            if (proc != null) {
                // all db attributes are override attributes
                Map<String,String> attributes = getWorkflowDao().getAttributes(OwnerType.PROCESS, proc.getProcessId());
                if (attributes != null)
                    proc.applyOverrideAttributes(attributes);
            }
            if (proc == null && exceptionWhenNotFound)
                throw new Exception("Process not found " + name + (version == 0 ? "" : " v" + version));
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
}
