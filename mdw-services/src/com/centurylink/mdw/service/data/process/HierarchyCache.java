package com.centurylink.mdw.service.data.process;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.services.ServiceLocator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HierarchyCache implements CacheService {

    private static volatile Map<Long,List<Linked<Process>>> hierarchies = new HashMap<>();
    private static volatile Map<Long,Linked<Milestone>> milestones = new HashMap<>();
    private static volatile List<Long> milestoned = null;

    public static List<Linked<Process>> getHierarchy(Long processId) {
        List<Linked<Process>> hierarchy;
        Map<Long,List<Linked<Process>>> hierarchyMap = hierarchies;
        if (hierarchyMap.containsKey(processId)){
            hierarchy = hierarchyMap.get(processId);
        } else {
            synchronized(HierarchyCache.class) {
                hierarchyMap = hierarchies;
                if (hierarchyMap.containsKey(processId)) {
                    hierarchy = hierarchyMap.get(processId);
                } else {
                    hierarchy = loadHierarchy(processId);
                    // null values are stored to avoid repeated processing
                    hierarchies.put(processId, hierarchy);
                }
            }
        }
        return hierarchy;
    }

    private static List<Linked<Process>> loadHierarchy(Long processId) {
        try {
            return ServiceLocator.getDesignServices().getProcessHierarchy(processId);
        } catch (ServiceException ex) {
            throw new CachingException("Failed to load hierarchy for: " + processId, ex);
        }
    }

    /**
     * Descending milestones starting with the specified process.
     */
    public static Linked<Milestone> getMilestones(Long processId) {
        Linked<Milestone> processMilestones;
        Map<Long,Linked<Milestone>> milestoneMap = milestones;
        if (milestoneMap.containsKey(processId)) {
            processMilestones = milestoneMap.get(processId);
        } else {
            synchronized (HierarchyCache.class) {
                milestoneMap = milestones;
                if (milestoneMap.containsKey(processId)) {
                    processMilestones = milestoneMap.get(processId);
                } else {
                    processMilestones = loadMilestones(processId);
                    // null values are stored to avoid repeated processing
                    milestones.put(processId, processMilestones);
                }
            }
        }
        return processMilestones;
    }

    private static Linked<Milestone> loadMilestones(Long processId) {
        Process process = ProcessCache.getProcess(processId);
        if (process != null) {
            MilestoneFactory milestoneFactory = new MilestoneFactory(process);
            Linked<Milestone> start = milestoneFactory.start();
            try {
                Linked<Activity> endToEndActivities = process.getLinkedActivities();
                addSubprocActivities(endToEndActivities);
                addMilestones(start, endToEndActivities);
                if (!start.getChildren().isEmpty())
                    return start;
            }
            catch (DataAccessException ex) {
                throw new CachingException(ex.getMessage(), ex);
            }

        }
        return null;
    }

    private static void addMilestones(Linked<Milestone> head, Linked<Activity> start) {
        Activity activity = start.get();
        Process process = ProcessCache.getProcess(activity.getProcessId());
        Milestone milestone = new MilestoneFactory(process).getMilestone(activity);
        if (milestone != null) {
            Linked<Milestone> linkedMilestone = new Linked<>(milestone);
            linkedMilestone.setParent(head);
            head.getChildren().add(linkedMilestone);
            for (Linked<Activity> child : start.getChildren()) {
                addMilestones(linkedMilestone, child);
            }
        }
        else {
            for (Linked<Activity> child : start.getChildren()) {
                addMilestones(head, child);
            }
        }
    }

    private static void addSubprocActivities(Linked<Activity> start) throws DataAccessException {
        for (Linked<Activity> child : start.getChildren()) {
            Activity activity = child.get();
            List<Process> subprocesses = activity.findInvoked(ProcessCache.getAllProcesses());
            for (Process subprocess : subprocesses) {
                addSubprocActivities(child, ProcessCache.getProcess(subprocess.getId()));
            }
            if (!child.checkCircular()) {
                addSubprocActivities(child);
            }
        }
    }

    /**
     * Build an end-to-end list of linked activities.
     */
    private static void addSubprocActivities(Linked<Activity> subprocInvoke, Process process) throws DataAccessException {
        Linked<Activity> processActivities = process.getLinkedActivities();
        subprocInvoke.getChildren().add(processActivities);
        processActivities.setParent(subprocInvoke);
        if (!processActivities.checkCircular()) {
            for (Linked<Activity> child : subprocInvoke.getChildren()) {
                Activity activity = child.get();
                List<Process> subprocesses = activity.findInvoked(ProcessCache.getAllProcesses());
                for (Process subprocess : subprocesses) {
                    addSubprocActivities(child, ProcessCache.getProcess(subprocess.getId()));
                }
            }
        }
    }

    /**
     * Processes with milestones in their hierarchies.
     */
    public static List<Long> getMilestoned() {
        List<Long> milestonedTemp = milestoned;
        if (milestonedTemp == null)
            synchronized(HierarchyCache.class) {
                milestonedTemp = milestoned;
                if (milestonedTemp == null)
                    milestoned = milestonedTemp = loadMilestoned();
            }
        return milestonedTemp;
    }

    private static List<Long> loadMilestoned() {
        List<Long> milestoned = new ArrayList<>();
        try {
            for (Process process : ProcessCache.getAllProcesses()) {
                List<Linked<Process>> hierarchyList = getHierarchy(process.getId());
                if (!hierarchyList.isEmpty()) {
                    if (hasMilestones(hierarchyList.get(0))) {
                        milestoned.add(process.getId());
                    }
                }
            }
            return milestoned;
        } catch (DataAccessException ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }

    private static boolean hasMilestones(Linked<Process> hierarchy) {

        Process process = hierarchy.get();
        Linked<Activity> start = process.getLinkedActivities();

        if (hasMilestones(new MilestoneFactory(process), start))
            return true;

        for (Linked<Process> child : hierarchy.getChildren()) {
            if (hasMilestones(child))
                return true;
        }

        return false;
    }

    private static boolean hasMilestones(MilestoneFactory factory, Linked<Activity> start) {
        if (factory.getMilestone(start.get()) != null)
            return true;
        for (Linked<Activity> child : start.getChildren()) {
            if (hasMilestones(factory, child))
                return true;
        }
        return false;
    }

    @Override
    public void clearCache() {
        hierarchies = new HashMap<>();
        milestones = new HashMap<>();
        milestoned = null;
    }

    @Override
    public void refreshCache() {
        // hierarchies are lazily loaded
        clearCache();
    }
}
