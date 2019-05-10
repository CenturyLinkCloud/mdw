package com.centurylink.mdw.services.workflow;

import com.centurylink.mdw.cli.Hierarchy;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.service.data.activity.ImplementorCache;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.DesignServices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DesignServicesImpl implements DesignServices {

    @Override
    public Process getProcessDefinition(String assetPath, Query query) throws ServiceException {
        int lastSlash = assetPath.lastIndexOf('/');
        if (lastSlash <= 0)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad asset path: " + assetPath);
        String processName = assetPath; //.substring(lastSlash + 1);
        if (assetPath.endsWith(".proc"))
            processName = processName.substring(0, processName.length() - ".proc".length());

        int version = query == null ? 0 : query.getIntFilter("version");
        if (version < 0)
            version = 0;
        boolean forUpdate = query == null ? false : query.getBooleanFilter("forUpdate");
        Process process = ProcessCache.getProcess(processName, version);
        if (forUpdate) {
            // load from file
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(process.getRawFile().getAbsolutePath()));
                process = new Process(new JsonObject(new String(bytes)));
                process.setName(processName.substring(lastSlash + 1));
                process.setPackageName(processName.substring(0, lastSlash));
            }
            catch (Exception ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error reading process: " + process.getRawFile());
            }
        }
        if (process == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process definition not found: " + assetPath);

        return process;
    }

    @Override
    public List<Process> getProcessDefinitions(Query query) throws ServiceException {
        try {
            String find = query.getFind();
            if (find == null) {
                return ProcessCache.getAllProcesses();
            }
            else {
                List<Process> found = new ArrayList<>();
                for (Process processVO : ProcessCache.getAllProcesses()) {
                    if (processVO.getName() != null && processVO.getName().startsWith(find))
                        found.add(processVO);
                    else if (find.indexOf(".") > 0 && processVO.getPackageName() != null && processVO.getPackageName().startsWith(find))
                        found.add(processVO);
                }
                return found;
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, ex.getMessage(), ex);
        }
    }

    @Override
    public Process getProcessDefinition(Long id) {
        return ProcessCache.getProcess(id);
    }

    @Override
    public ActivityList getActivityDefinitions(Query query) throws ServiceException {
        try {
            String find = query.getFind();
            List<ActivityInstance> activityInstanceList = new ArrayList<>();
            ActivityList found = new ActivityList(ActivityList.ACTIVITY_INSTANCES, activityInstanceList);

            if (find == null) {
                List<Process> processes = ProcessCache.getAllProcesses();
                for (Process process : processes) {
                    process = ProcessCache.getProcess(process.getId());
                    List<Activity> activities = process.getActivities();
                    for (Activity activityVO : activities) {
                        if (activityVO.getName() != null && activityVO.getName().startsWith(find)) {
                            ActivityInstance ai = new ActivityInstance();
                            ai.setId(activityVO.getId());
                            ai.setName(activityVO.getName());
                            ai.setDefinitionId(activityVO.getLogicalId());
                            ai.setProcessId(process.getId());
                            ai.setProcessName(process.getName());
                            ai.setProcessVersion(process.getVersionString());
                            activityInstanceList.add(ai);
                        }
                    }
                }
            } else {
                for (Process process : ProcessCache.getAllProcesses()) {
                    process = ProcessCache.getProcess(process.getId());
                    List<Activity> activities = process.getActivities();
                    for (Activity activityVO : activities) {
                        if (activityVO.getName() != null && activityVO.getName().startsWith(find)) {
                            ActivityInstance ai = new ActivityInstance();
                            ai.setId(activityVO.getId());
                            ai.setName(activityVO.getName());
                            ai.setDefinitionId(activityVO.getLogicalId());
                            ai.setProcessId(process.getId());
                            ai.setProcessName(process.getName());
                            ai.setProcessVersion(process.getVersionString());
                            activityInstanceList.add(ai);
                        }
                    }
                }
            }
            found.setRetrieveDate(DatabaseAccess.getDbDate());
            found.setCount(activityInstanceList.size());
            found.setTotal(activityInstanceList.size());
            return found;
        } catch (DataAccessException ex) {
            throw new ServiceException(500, ex.getMessage(), ex);
        }
    }

    @Override
    public List<ActivityImplementor> getImplementors() {
        return new ArrayList<>(ImplementorCache.getImplementors().values());
    }

    @Override
    public ActivityImplementor getImplementor(String className) throws ServiceException {
        ActivityImplementor implementor =  ImplementorCache.get(className);
        if (implementor.getPagelet() == null) {
            try {
                for (ActivityImplementor impl : DataAccess.getProcessLoader().getActivityImplementors()) {
                    if (impl.getImplementorClass().equals(implementor.getImplementorClass()))
                        return impl; // loaded from .impl file
                }
            } catch (DataAccessException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
            }
        }
        return implementor; // loaded from annotation or not found
    }

    @Override
    public List<Linked<Process>> getProcessHierarchy(Long processId) throws ServiceException {
        Process process = ProcessCache.getProcess(processId);
        if (process == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process not found: " + processId);

        try {
            // all must be loaded
            List<Process> processes = new ArrayList<>();
            for (Process proc : ProcessCache.getAllProcesses()) {
                Process loaded = ProcessCache.getProcess(proc.getId());
                if (loaded != null)
                    processes.add(loaded);
            }
            Hierarchy hierarchy = new Hierarchy(process, processes);
            hierarchy.run();
            return hierarchy.getTopLevelCallers();
        }
        catch (DataAccessException | IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Hierarchy error for " + processId, ex);
        }
    }
}
