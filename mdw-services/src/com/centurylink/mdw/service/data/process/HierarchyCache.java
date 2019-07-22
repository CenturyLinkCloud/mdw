package com.centurylink.mdw.service.data.process;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyGroup;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.services.ServiceLocator;

import java.util.*;

public class HierarchyCache implements CacheService {

    private static final String MILESTONES_PACKAGE = "com.centurylink.mdw.milestones";

    private static volatile Map<Long,List<Linked<Process>>> hierarchies = new HashMap<>();
    private static volatile Map<Long,Linked<Milestone>> milestones = new HashMap<>();
    private static volatile Map<Long,Linked<Activity>> activityHierarchies = new HashMap<>();
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
            Linked<Activity> activityHierarchy = getActivityHierarchy(processId);
            if (activityHierarchy != null)
                addMilestones(start, activityHierarchy);
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

    public static Linked<Activity> getActivityHierarchy(Long processId) {
        Linked<Activity> hierarchy;
        Map<Long,Linked<Activity>> endToEndMap = activityHierarchies;
        if (endToEndMap.containsKey(processId)) {
            hierarchy = endToEndMap.get(processId);
        } else {
            synchronized (HierarchyCache.class) {
                endToEndMap = activityHierarchies;
                if (endToEndMap.containsKey(processId)) {
                    hierarchy = endToEndMap.get(processId);
                } else {
                    hierarchy = loadActivityHierarchy(processId);
                    activityHierarchies.put(processId, hierarchy);
                }
            }
        }
        return hierarchy;
    }

    private static Linked<Activity> loadActivityHierarchy(Long processId) {
        if (PackageCache.getPackage(MILESTONES_PACKAGE) == null)
            return null; // don't bother and save some time
        Process process = ProcessCache.getProcess(processId);
        if (process != null) {
            Linked<Process> hierarchy = getHierarchy(processId).get(0);
            try {
                Linked<Activity> endToEndActivities = process.getLinkedActivities();
                ScopedActivity scoped = new ScopedActivity(hierarchy, endToEndActivities);
                addSubprocessActivities(scoped, null);
                return endToEndActivities;
            }
            catch (DataAccessException ex) {
                throw new CachingException(ex.getMessage(), ex);
            }
        }
        return null;
    }

    private static int MAX_DEPTH = 5000;

    /**
     * Activities are scoped to avoid process invocation circularity.
     */
    private static void addSubprocessActivities(ScopedActivity start, List<ScopedActivity> downstreams)
            throws DataAccessException {

        // we particularly care about these processes
        MAX_DEPTH += getImportance(start);

        if (start.depth() > MAX_DEPTH)
            return;

        List<ScopedActivity> furtherDowns = downstreams;

        for (ScopedActivity scopedChild : start.getScopedChildren()) {
            Activity activity = scopedChild.get();
            List<Linked<Process>> subhierarchies = getInvoked(scopedChild);
            if (!subhierarchies.isEmpty()) {
                // link downstream children
                if (furtherDowns != null) {
                    for (Linked<Activity> downstreamChild : scopedChild.getChildren()) {
                        for (Linked<Activity> end : downstreamChild.getEnds()) {
                            if (end.get().isStop()) {
                                end.setChildren(new ArrayList<>(furtherDowns));
                            }
                        }
                    }
                }
                if (activity.isSynchronous()) {
                    furtherDowns = scopedChild.getScopedChildren();
                    scopedChild.setChildren(new ArrayList<>());
                }
                for (Linked<Process> subhierarchy : subhierarchies) {
                    Process loadedSub = ProcessCache.getProcess(subhierarchy.get().getId());
                    Linked<Activity> subprocActivities = loadedSub.getLinkedActivities();
                    scopedChild.getChildren().add(subprocActivities);
                    subprocActivities.setParent(scopedChild);
                    ScopedActivity subprocScoped = new ScopedActivity(subhierarchy, subprocActivities);
                    addSubprocessActivities(subprocScoped, furtherDowns);
                }
                furtherDowns = downstreams;
            }
            else {
                // non-invoker
                if (scopedChild.get().isStop() && scopedChild.getChildren().isEmpty() ) {
                    if (furtherDowns != null) {
                        scopedChild.setChildren(new ArrayList<>(furtherDowns));
                        furtherDowns = null;
                    }
                }
                addSubprocessActivities(scopedChild, furtherDowns);
            }
        }
    }


    /**
     * Omits invoked subflows that would cause circularity by consulting relevant process hierarchy.
     */
    private static List<Linked<Process>> getInvoked(ScopedActivity scopedActivity)
            throws DataAccessException {
        List<Linked<Process>> invoked = new ArrayList<>();
        for (Linked<Process> subprocess : scopedActivity.findInvoked(ProcessCache.getAllProcesses())) {
            if (!isIgnored(subprocess.get()))
                invoked.add(subprocess);
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

    public static boolean hasMilestones(Long processId) {
        return getMilestoned().contains(processId);
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
        activityHierarchies = new HashMap<>();
        milestoned = null;
    }

    @Override
    public void refreshCache() {
        // hierarchies are lazily loaded
        clearCache();
    }

    /**
     * Only returns value the first time so depth boost is applied just once.
     */
    private static Map<String,Integer> importance;
    private static int getImportance(Linked<Activity> linkedActivity) {
        if (importance == null) {
            importance = new HashMap<>();
            Properties props = PropertyManager.getInstance().getProperties(PropertyNames.MDW_MILESTONE_IMPORTANCE);
            PropertyGroup importanceGroup = new PropertyGroup("milestone.groups", PropertyNames.MDW_MILESTONE_IMPORTANCE, props);
            for (String value : importanceGroup.getProperties().stringPropertyNames()) {
                String path = value;
                int slash = value.indexOf('/');
                if (slash > 0) {
                    path = path.substring(0,slash).replace('_', '.') + path.substring(slash);
                }
                if (path.endsWith("_proc"))
                    path = path.substring(0, path.length() - 5) + ".proc";
                importance.put(path, Integer.parseInt(importanceGroup.getProperties().getProperty(value)));
            }
        }
        String path = linkedActivity.get().getPackageName() + "/" + linkedActivity.get().getProcessName() + ".proc";
        if (importance.containsKey(path)) {
            int value = importance.remove(path);
            return value;
        }
        return 0;
    }

    private static boolean isIgnored(Process process) {
        List<String> ignores = PropertyManager.getListProperty(PropertyNames.MDW_MILESTONE_IGNORES);
        return ignores != null && ignores.contains(process.getPackageName() + "/" + process.getName() + ".proc");
    }


}
