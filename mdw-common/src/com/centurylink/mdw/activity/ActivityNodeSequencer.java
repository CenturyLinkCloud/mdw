/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

/**
 * Assigns geographical sequence numbers to activities and subprocesses for a process.
 */
public class ActivityNodeSequencer {

    private ProcessVO process;

    public ActivityNodeSequencer(ProcessVO process) {
        this.process = process;
    }

    public void assignNodeSequenceIds() {

        // activities
        int currentActivitySeq = assignNodeSequenceIds(this.process, 1);
        // subprocesses
        if (process.getSubProcesses() != null && !process.getSubProcesses().isEmpty()) {
            List<ProcessVO> subprocesses = new ArrayList<ProcessVO>(); // create a copy to avoid side effects
            subprocesses.addAll(process.getSubProcesses());
            Collections.sort(subprocesses, new Comparator<ProcessVO>() {
                public int compare(ProcessVO sp1, ProcessVO sp2) {
                    DisplayInfo d1 = getDisplayInfo("T" + sp1.getId(), sp1.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO));
                    DisplayInfo d2 = getDisplayInfo("T" + sp2.getName(), sp2.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO));
                    // TODO: something better
                    if (Math.abs(d1.y - d2.y) > 100)
                        return d1.y - d2.y;
                    // otherwise closest to top-left of canvas
                    return (int)(Math.sqrt(Math.pow(d1.x,2) + Math.pow(d1.y,2)) - Math.sqrt(Math.pow(d2.x,2) + Math.pow(d2.y,2)));
                }
            });
            for (int i = 0; i < subprocesses.size(); i++) {
                ProcessVO subprocess = subprocesses.get(i);
                subprocess.setSequenceId(i + 1);
                currentActivitySeq = assignNodeSequenceIds(subprocess, ++currentActivitySeq);
            }
        }
    }

    private int currentSeq;
    private int assignNodeSequenceIds(ProcessVO process, int sequenceStart) {
        for (ActivityVO activity : process.getActivities())
            activity.setSequenceId(0);  // clear all
        currentSeq = sequenceStart;
        ActivityVO start = process.getStartActivity();
        start.setSequenceId(currentSeq);
        setDownstreamNodeSequenceIds(process, start);
        return currentSeq;
    }

    private void setDownstreamNodeSequenceIds(ProcessVO process, ActivityVO start) {
        List<ActivityVO> downstreamNodes = new ArrayList<ActivityVO>(); // create a copy to avoid side effects
        for (ActivityVO activity : process.getDownstreamActivities(start))
            downstreamNodes.add(process.getActivityById(activity.getLogicalId()));
        Collections.sort(downstreamNodes, new Comparator<ActivityVO>() {
            public int compare(ActivityVO a1, ActivityVO a2) {
                DisplayInfo d1 = getDisplayInfo(a1.getLogicalId(), a1.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO));
                DisplayInfo d2 = getDisplayInfo(a2.getLogicalId(), a2.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO));
                // TODO: something better
                if (Math.abs(d1.y - d2.y) > 100)
                    return d1.y - d2.y;
                // otherwise closest to top-left of canvas
                return (int)(Math.sqrt(Math.pow(d1.x,2) + Math.pow(d1.y,2)) - Math.sqrt(Math.pow(d2.x,2) + Math.pow(d2.y,2)));
            }
        });
        for (ActivityVO downstreamNode : downstreamNodes) {
            // may have been already set due to converging paths
            if (downstreamNode.getSequenceId() == 0) {
                downstreamNode.setSequenceId(++currentSeq);
                setDownstreamNodeSequenceIds(process, downstreamNode);
            }
        }
    }

    private Map<String,DisplayInfo> nodeDisplayInfo = new HashMap<String,DisplayInfo>();
    private DisplayInfo getDisplayInfo(String logicalId, String attr) {
        DisplayInfo displayInfo = nodeDisplayInfo.get(logicalId);
        if (displayInfo == null) {
            // parse from attribute
            displayInfo = new DisplayInfo();
            if (attr != null && !attr.isEmpty()) {
                String [] tmps = attr.split(",");
                int k;
                String an, av;
                for (int i = 0; i < tmps.length; i++) {
                    k = tmps[i].indexOf('=');
                    if (k <= 0)
                        continue;
                    an = tmps[i].substring(0, k);
                    av = tmps[i].substring(k + 1);
                    if (an.equals("x"))
                        displayInfo.x = Integer.parseInt(av);
                    else if (an.equals("y"))
                        displayInfo.y = Integer.parseInt(av);
                }
            }
        }
        return displayInfo;
    }

    /**
     * We only use x and y (not w and h).
     */
    private class DisplayInfo {
        int x;
        int y;
    }
}
