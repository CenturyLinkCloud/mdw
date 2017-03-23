/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;

/**
 * Assigns geographical sequence numbers to activities and subprocesses for a process.
 */
public class ActivityNodeSequencer {

    private Process process;

    public ActivityNodeSequencer(Process process) {
        this.process = process;
    }

    public void assignNodeSequenceIds() {

        // activities
        int currentActivitySeq = assignNodeSequenceIds(this.process, 1);
        // subprocesses
        if (process.getSubProcesses() != null && !process.getSubProcesses().isEmpty()) {
            List<Process> subprocesses = new ArrayList<Process>(); // create a copy to avoid side effects
            subprocesses.addAll(process.getSubProcesses());
            Collections.sort(subprocesses, new Comparator<Process>() {
                public int compare(Process sp1, Process sp2) {
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
                Process subprocess = subprocesses.get(i);
                subprocess.setSequenceId(i + 1);
                currentActivitySeq = assignNodeSequenceIds(subprocess, ++currentActivitySeq);
            }
        }
    }

    private int currentSeq;
    private int assignNodeSequenceIds(Process process, int sequenceStart) {
        for (Activity activity : process.getActivities())
            activity.setSequenceId(0);  // clear all
        currentSeq = sequenceStart;
        Activity start = process.getStartActivity();
        start.setSequenceId(currentSeq);
        setDownstreamNodeSequenceIds(process, start);
        return currentSeq;
    }

    private void setDownstreamNodeSequenceIds(Process process, Activity start) {
        List<Activity> downstreamNodes = new ArrayList<Activity>(); // create a copy to avoid side effects
        for (Activity activity : process.getDownstreamActivities(start))
            downstreamNodes.add(process.getActivityById(activity.getLogicalId()));
        Collections.sort(downstreamNodes, new Comparator<Activity>() {
            public int compare(Activity a1, Activity a2) {
                DisplayInfo d1 = getDisplayInfo(a1.getLogicalId(), a1.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO));
                DisplayInfo d2 = getDisplayInfo(a2.getLogicalId(), a2.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO));
                // TODO: something better
                if (Math.abs(d1.y - d2.y) > 100)
                    return d1.y - d2.y;
                // otherwise closest to top-left of canvas
                return (int)(Math.sqrt(Math.pow(d1.x,2) + Math.pow(d1.y,2)) - Math.sqrt(Math.pow(d2.x,2) + Math.pow(d2.y,2)));
            }
        });
        for (Activity downstreamNode : downstreamNodes) {
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
