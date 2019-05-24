package com.centurylink.mdw.service.data.process;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.impl.PackageCache;
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

    private static final String MILESTONES_PACKAGE = "com.centurylink.mdw.milestones";

    private static volatile Map<Long,List<Linked<Process>>> hierarchies = new HashMap<>();
    private static volatile Map<Long,Linked<Milestone>> milestones = new HashMap<>();
    private static volatile Map<Long,Linked<Activity>> endToEndActivities = new HashMap<>();
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
                    hierarchy = loadHierarchy(processId, true);
                    // null values are stored to avoid repeated processing
                    hierarchies.put(processId, hierarchy);
                }
            }
        }
        return hierarchy;
    }

    private static List<Linked<Process>> loadHierarchy(Long processId, boolean downward) {
        try {
            return ServiceLocator.getDesignServices().getProcessHierarchy(processId, downward);
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
        if (PackageCache.getPackage(MILESTONES_PACKAGE) == null)
            return null; // don't bother and save some time
        Process process = ProcessCache.getProcess(processId);
        if (process != null) {
            MilestoneFactory milestoneFactory = new MilestoneFactory(process);
            Linked<Milestone> start = milestoneFactory.start();
            addMilestones(start, getEndToEndActivities(processId));
            if (!start.getChildren().isEmpty())
                return start;
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

    public static Linked<Activity> getEndToEndActivities(Long processId) {
        Linked<Activity> endToEnd;
        Map<Long,Linked<Activity>> endToEndMap = endToEndActivities;
        if (endToEndMap.containsKey(processId)) {
            endToEnd = endToEndMap.get(processId);
        } else {
            synchronized (HierarchyCache.class) {
                endToEndMap = endToEndActivities;
                if (endToEndMap.containsKey(processId)) {
                    endToEnd = endToEndMap.get(processId);
                } else {
                    endToEnd = loadEndToEndActivities(processId);
                    endToEndActivities.put(processId, endToEnd);
                }
            }
        }
        return endToEnd;
    }

    private static Linked<Activity> loadEndToEndActivities(Long processId) {
        if (PackageCache.getPackage(MILESTONES_PACKAGE) == null)
            return null; // don't bother and save some time
        Process process = ProcessCache.getProcess(processId);
        if (process != null) {
            Linked<Process> hierarchy = getHierarchy(processId).get(0);
            try {
                Linked<Activity> endToEndActivities = process.getLinkedActivities();
                addSubprocActivities(endToEndActivities, hierarchy, null);
                return endToEndActivities;
            }
            catch (DataAccessException ex) {
                throw new CachingException(ex.getMessage(), ex);
            }
        }
        return null;
    }

    /**
     * Hierarchy is passed to avoid process invocation circularity.
     */
    private static void addSubprocActivities(Linked<Activity> start, Linked<Process> hierarchy,
            List<Linked<Activity>> downstreams) throws DataAccessException {

        List<Linked<Activity>> furtherDown = downstreams;

        for (Linked<Activity> child : start.getChildren()) {
            Activity activity = child.get();
            List<Linked<Process>> subhierarchies = findInvoked(activity, hierarchy);
            if (!subhierarchies.isEmpty()) {
                // link downstream children
                if (downstreams != null) {
                    for (Linked<Activity> downstreamChild : child.getChildren()) {
                        for (Linked<Activity> end : downstreamChild.getEnds()) {
                            if (end.get().isStop()) {
                                end.setChildren(furtherDown);
                            }
                        }
                    }
                }
                if (activity.isSynchronous()) {
                    furtherDown = child.getChildren();
                    child.setChildren(new ArrayList<>());
                }
                for (Linked<Process> subhierarchy : subhierarchies) {
                    Process loadedSub = ProcessCache.getProcess(subhierarchy.get().getId());
                    Linked<Activity> subprocActivities = loadedSub.getLinkedActivities();
                    child.getChildren().add(subprocActivities);
                    subprocActivities.setParent(child);
                    addSubprocActivities(subprocActivities, subhierarchy, furtherDown);
                }
            }
            else {
                // non-invoker
                if (child.getChildren().isEmpty() && child.get().isStop()) {
                    if (furtherDown != null) {
                        child.setChildren(furtherDown);
                        furtherDown = null;
                    }
                }
                addSubprocActivities(child, hierarchy, furtherDown);
            }
        }
    }

    /**
     * Omits invoked subflows that would cause circularity by consulting process hierarchy.
     */
    private static List<Linked<Process>> findInvoked(Activity activity, Linked<Process> hierarchy)
            throws DataAccessException {
        List<Linked<Process>> invoked = new ArrayList<>();
        for (Process subprocess : activity.findInvoked(ProcessCache.getAllProcesses())) {
            Linked<Process> found = hierarchy.find(subprocess);
            if (found != null)
                invoked.add(found);
        }
        return invoked;
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
            if (PackageCache.getPackage(MILESTONES_PACKAGE) != null) {
                for (Process process : ProcessCache.getAllProcesses()) {
                    List<Linked<Process>> hierarchyList = getHierarchy(process.getId());
                    if (!hierarchyList.isEmpty()) {
                        if (hasMilestones(hierarchyList.get(0))) {
                            milestoned.add(process.getId());
                        }
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
        endToEndActivities = new HashMap<>();
        milestoned = null;
    }

    @Override
    public void refreshCache() {
        // hierarchies are lazily loaded
        clearCache();
    }
}
