/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.process.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.CacheEnabled;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.provider.CacheService;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;

/**
 * Lazily loads the ProcessVOs for use by the RuntimeEngine.
 */
public class ProcessVOCache implements CacheEnabled, CacheService {

	public static String name = "ProcessVOCache";
    private static ProcessVOCache singleton = null;
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private volatile Map<Long, ProcessVO> processVOMap;
    private volatile Map<String,List<ProcessVO>> procNameMap;
    private volatile Map<String,ProcessVO> procNameLatest;

    private ProcessVOCache() {
        processVOMap = new ConcurrentHashMap<Long, ProcessVO>();
        procNameMap = new ConcurrentHashMap<String, List<ProcessVO>>();
        procNameLatest = new ConcurrentHashMap<String,ProcessVO>();
    }

    synchronized private static ProcessVOCache getSingleton() {
    	if (singleton==null) {
    		singleton = new ProcessVOCache();
        	(new CacheRegistration()).registerCache(name, singleton);
    	}
    	return singleton;
    }

    public void clearCache() {
        synchronized(processVOMap) {
            processVOMap.clear();
            procNameMap.clear();
            procNameLatest.clear();
        }
    }

    public void refreshCache() {
        	clearCache();
    }

    public static ProcessVO getProcessVO(Long processId) {
    	return getSingleton().getProcessVO0(processId);
    }

    public static ProcessVO getProcessVO(String procname, int version) {
    	return getSingleton().getProcessVO0(procname, version);
    }

    private void putInCache(ProcessVO processVO) {
        putInCache(processVO, false);
    }

    private void putInCache(ProcessVO processVO, boolean versionZero) {
    	processVOMap.put(processVO.getProcessId(), processVO);
        List<ProcessVO> vl = procNameMap.get(processVO.getProcessName());
        if (vl == null) {
        	vl = new ArrayList<ProcessVO>();
        	procNameMap.put(processVO.getProcessName(), vl);
        }
        vl.add(processVO);
        if (versionZero)
            procNameLatest.put(processVO.getProcessName(), processVO);
    }

    private ProcessVO getProcessVO0(Long processId) {
        ProcessVO processVO = null;
        Map<Long, ProcessVO> processVOMapTemp = processVOMap;
        if (processVOMapTemp.containsKey(processId)){
            processVO = processVOMapTemp.get(processId);
        } else {
            synchronized(processVOMap) {
                processVOMapTemp = processVOMap;
                if (processVOMapTemp.containsKey(processId))
                    processVO = processVOMapTemp.get(processId);
                else {
                    processVO = loadProcessVO(processId);
                    if (processVO!=null) {
                        processVO.removeDeletedTransitions();
                        putInCache(processVO);
                    }
                }
            }
        }
        return processVO;
    }

    private static List<ProcessVO> processList;
    public static synchronized List<ProcessVO> getAllProcesses() throws DataAccessException {
        if (processList == null)
            processList = DataAccess.getProcessLoader().getProcessList();
        return processList;
    }

    /**
     * Either a specific version number can be specified, or a Smart Version can be specified which designates an allowable range.
     * @see RuleSetVO.meetsVersionSpec().
     * TODO: spec.packageName is ignored
     */
    public static ProcessVO getProcessVOSmart(AssetVersionSpec spec) {
        try {
            ProcessVO match = null;
            for (ProcessVO process : getAllProcesses()) {
                if (spec.getName().equals(process.getName())) {
                    if (process.meetsVersionSpec(spec.getVersion()) && (match == null || process.getVersion() > match.getVersion()))
                        match = process;
                }
            }
            if (match == null) {
                if (ApplicationContext.isFileBasedAssetPersist()) {
                    // try falling back to non-smart VO from
                    return getProcessVO(spec.getName(), RuleSetVO.parseVersion(spec.getVersion()));
                }
                return null;
            }
            else {
                return getProcessVO(match.getId());
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    private ProcessVO getProcessVO0(String procname, int version) {
        ProcessVO procdef = null;

        if (version == 0) {
            procdef = procNameLatest.get(procname);
        }
        else {
            List<ProcessVO> vl = procNameMap.get(procname);
            if (vl != null) {
                for (ProcessVO p : vl) {
                    if (p.getVersion() == version) {
                        procdef = p;
                        break;
                    }
                }
            }
        }

        if (procdef == null) {
            synchronized(processVOMap) {
                procdef = loadProcessVO(procname, version);
                if (procdef != null) {
                    procdef.removeDeletedTransitions();
                    putInCache(procdef, version == 0);
                }
            }
        }
        return procdef;
    }

    private ProcessVO loadProcessVO(Long processId) {
        try {
			EventManager eventMgr = ServiceLocator.getEventManager();
            return eventMgr.getProcessVO(processId);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    private ProcessVO loadProcessVO(String procname, int version) {
        try {
        	EventManager eventMgr = ServiceLocator.getEventManager();
        	Long procid = eventMgr.findProcessId(procname, version);
            if (procid == null)
                throw new Exception("Process not found " + procname + " v" + version);
            return eventMgr.getProcessVO(procid);
        }
        catch (Exception ex) {
        	StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }
}
