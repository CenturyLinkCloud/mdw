package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.constant.OwnerType;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a linked ActivityInstance to include its place in the process hierarchy.
 */
public class ScopedActivityInstance extends Linked<ActivityInstance> {

    private Linked<ProcessInstance> instanceHierarchy;
    public Linked<ProcessInstance> getInstanceHierarchy() { return instanceHierarchy; }

    private Linked<ActivityInstance> linkedActivityInstance;

    public ScopedActivityInstance(Linked<ProcessInstance> instanceHierarchy,
            Linked<ActivityInstance> linkedActivityInstance) {
        super(linkedActivityInstance.get());
        this.instanceHierarchy = instanceHierarchy;
        this.linkedActivityInstance = linkedActivityInstance;
    }

    @Override
    public List<Linked<ActivityInstance>> getChildren() {
        return linkedActivityInstance.getChildren();
    }

    @Override
    public void setChildren(List<Linked<ActivityInstance>> children) {
        linkedActivityInstance.setChildren(children);
    }

    @Override
    public Linked<ActivityInstance> getParent() {
        return linkedActivityInstance.getParent();
    }

    @Override
    public void setParent(Linked<ActivityInstance> parent) {
        linkedActivityInstance.setParent(parent);
    }

    public List<ScopedActivityInstance> getScopedChildren() {
        List<ScopedActivityInstance> scopedChildren = new ArrayList<>();
        for (Linked<ActivityInstance> child : getChildren()) {
            if (child instanceof ScopedActivityInstance)
                scopedChildren.add((ScopedActivityInstance)child);
            else
                scopedChildren.add(new ScopedActivityInstance(instanceHierarchy, child));
        }
        return scopedChildren;
    }

    /**
     * Subprocesses invoked by this activity within the context of my process hierarchy.
     */
    public List<Linked<ProcessInstance>> findInvoked(Activity activity, List<Process> processes) {
        List<Linked<ProcessInstance>> subprocs = new ArrayList<>();
        List<Process> subprocesses = activity.findInvoked(processes);
        for (Linked<ProcessInstance> childProcess : instanceHierarchy.getChildren()) {
            for (Process subprocess : subprocesses) {
                ProcessInstance subInst = childProcess.get();
                if (subInst.getProcessId().equals(subprocess.getId()) &&
                        (subInst.getSecondaryOwner() == null || subInst.getSecondaryOwner().equals(OwnerType.ACTIVITY_INSTANCE)) &&
                        (subInst.getSecondaryOwnerId() == null || subInst.getSecondaryOwnerId().equals(get().getId()))) {
                    subprocs.add(childProcess);

                }
            }
        }
        return subprocs;
    }

    @Override
    public String toString() {
        return instanceHierarchy.get().getProcessName() + " " + super.toString();
    }
}

