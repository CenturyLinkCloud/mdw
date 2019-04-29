package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.StringHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: MicroserviceOrchestratorActivity
 */
public class ProcessHierarchy implements Jsonable {

    public static boolean activityInvokesProcess(Activity activity, Process subproc) {
        String procName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
        if (procName != null && (procName.equals(subproc.getName()) || procName.endsWith("/" + subproc.getName()))) {
            String verSpec = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
            if (subproc.meetsVersionSpec(verSpec))
                return true;
        }
        else {
            String procMap = activity.getAttribute(WorkAttributeConstant.PROCESS_MAP);
            if (procMap != null) {
                List<String[]> procmap = StringHelper.parseTable(procMap, ',', ';', 3);
                for (int i = 0; i < procmap.size(); i++) {
                    String nameSpec = procmap.get(i)[1];
                    if (nameSpec != null && (nameSpec.equals(subproc.getName()) || nameSpec.endsWith("/" + subproc.getName()))) {
                        String verSpec = procmap.get(i)[2];
                        if (subproc.meetsVersionSpec(verSpec))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public static List<Process> findInvoked(Process caller, List<Process> processes) {
        List<Process> called = new ArrayList<>();
        if (caller.getActivities() != null) {
            for (Activity activity : caller.getActivities()) {
                String procName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
                if (procName != null) {
                    String verSpec = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
                    if (verSpec != null) {
                        Process latestMatch = null;
                        for (Process process : processes) {
                            if ((procName.equals(process.getName()) || procName.endsWith("/" + process.getName()))
                                    && (process.meetsVersionSpec(verSpec) && (latestMatch == null || latestMatch.getVersion() < process.getVersion()))) {
                                latestMatch = process;
                            }
                        }
                        if (latestMatch != null && !called.contains(latestMatch))
                            called.add(latestMatch);
                    }
                }
                else {
                    String procMap = activity.getAttribute(WorkAttributeConstant.PROCESS_MAP);
                    if (procMap != null) {
                        List<String[]> procmap = StringHelper.parseTable(procMap, ',', ';', 3);
                        for (int i = 0; i < procmap.size(); i++) {
                            String nameSpec = procmap.get(i)[1];
                            if (nameSpec != null) {
                                String verSpec = procmap.get(i)[2];
                                if (verSpec != null) {
                                    Process latestMatch = null;
                                    for (Process process : processes) {
                                        if ((nameSpec.equals(process.getName()) || nameSpec.endsWith("/" + process.getName()))
                                                && (process.meetsVersionSpec(verSpec) && (latestMatch == null || latestMatch.getVersion() < process.getVersion()))) {
                                            latestMatch = process;
                                        }
                                    }
                                    if (latestMatch != null && !called.contains(latestMatch))
                                        called.add(latestMatch);
                                }
                            }
                        }
                    }
                }
            }
        }

        return called;
    }
}
