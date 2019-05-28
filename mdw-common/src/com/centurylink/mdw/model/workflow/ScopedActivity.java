package com.centurylink.mdw.model.workflow;

import java.util.ArrayList;
import java.util.List;


/**
 * Wraps a linked activity to include its place in the process hierarchy.
 */
public class ScopedActivity extends Linked<Activity> {

    private Linked<Process> processHierarchy;
    public Linked<Process> getProcessHierarchy() { return processHierarchy; }

    private Linked<Activity> linkedActivity;

    public ScopedActivity(Linked<Process> processHierarchy, Linked<Activity> linkedActivity) {
        super(linkedActivity.get());
        this.processHierarchy = processHierarchy;
        this.linkedActivity = linkedActivity;
    }

    @Override
    public List<Linked<Activity>> getChildren() {
        return linkedActivity.getChildren();
    }

    @Override
    public void setChildren(List<Linked<Activity>> children) {
        linkedActivity.setChildren(children);
    }

    @Override
    public Linked<Activity> getParent() {
        return linkedActivity.getParent();
    }

    @Override
    public void setParent(Linked<Activity> parent) {
        linkedActivity.setParent(parent);
    }

    public List<ScopedActivity> getScopedChildren() {
        List<ScopedActivity> scopedChildren = new ArrayList<>();
        for (Linked<Activity> child : getChildren()) {
            if (child instanceof ScopedActivity)
                scopedChildren.add((ScopedActivity)child);
            else
                scopedChildren.add(new ScopedActivity(processHierarchy, child));
        }
        return scopedChildren;
    }

    /**
     * Subprocesses invoked by this activity within the context of my process hierarchy.
     */
    public List<Linked<Process>> findInvoked(List<Process> processes) {
        List<Linked<Process>> invoked = new ArrayList<>();
        for (Process subprocess : get().findInvoked(processes)) {
            Linked<Process> found = null;
            for (Linked<Process> child : processHierarchy.getChildren()) {
                if (child.get().getId().equals(subprocess.getId())) {
                    found = child;
                    break;
                }
            }
            if (found != null)
                invoked.add(found);
        }
        return invoked;
    }

    @Override
    public String toString() {
        return processHierarchy.get().getName() + " " + super.toString();
    }
}
