package com.centurylink.mdw.service.data.process;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessHierarchy;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO complete this impl
 */
public class ProcessHierarchyCache {

    private List<Process> findCallingProcesses(Process subproc) throws DataAccessException {
        List<Process> callers = new ArrayList<>();
        for (Process process : ProcessCache.getAllProcesses()) {
            for (Activity activity : process.getActivities()) {
                if (ProcessHierarchy.activityInvokesProcess(activity, subproc) && !callers.contains(process))
                    callers.add(process);
            }
            for (Process embedded : process.getSubprocesses()) {
                for (Activity activity : embedded.getActivities()) {
                    if (ProcessHierarchy.activityInvokesProcess(activity, subproc) && !callers.contains(process))
                        callers.add(process);
                }
            }
        }
        return callers;
    }

    private List<Process> findCalledProcesses(Process mainproc) throws DataAccessException {
        return ProcessHierarchy.findInvoked(ProcessCache.getProcess(mainproc.getId()), ProcessCache.getAllProcesses());
    }
}
