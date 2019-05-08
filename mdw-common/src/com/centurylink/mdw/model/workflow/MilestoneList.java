package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Jsonable;

import java.util.List;

public class MilestoneList implements Jsonable {

    public MilestoneList(List<Milestone> milestones, long total) {
        this.milestones = milestones;
        this.total = total;
    }

    private List<Milestone> milestones;
    public List<Milestone> getMilestones() { return milestones; }
    public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }

    private long total = -1;
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

}
