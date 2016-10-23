/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.data.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.centurylink.mdw.cache.CacheEnabled;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.provider.CacheService;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Lazily loads the Processes for use by the RuntimeEngine.
 */
public class ProcessCache implements CacheEnabled, CacheService {

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
        return getSingleton().getProcess0(procname, version);
    }

    private void putInCache(Process process) {
        putInCache(process, false);
    }

    private void putInCache(Process process, boolean versionZero) {
        processMap.put(process.getProcessId(), process);
        List<Process> vl = procNameMap.get(process.getProcessName());
        if (vl == null) {
            vl = new ArrayList<Process>();
            procNameMap.put(process.getProcessName(), vl);
        }
        vl.add(process);
        if (versionZero)
            procNameLatest.put(process.getProcessName(), process);
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
                if (spec.getName().equals(process.getName())) {
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

    private Process getProcess0(String procname, int version) {
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
                procdef = loadProcess(procname, version);
                if (procdef != null) {
                    procdef.removeDeletedTransitions();
                    putInCache(procdef, version == 0);
                }
            }
        }
        return procdef;
    }

    private Process loadProcess(Long processId) {
        try {
            EventManager eventMgr = ServiceLocator.getEventManager();
            return eventMgr.getProcess(processId);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    private Process loadProcess(String procname, int version) {
        try {
            EventManager eventMgr = ServiceLocator.getEventManager();
            Long procid = eventMgr.findProcessId(procname, version);
            if (procid == null)
                throw new Exception("Process not found " + procname + " v" + version);
            return eventMgr.getProcess(procid);
        }
        catch (Exception ex) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }
}
