/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.constant.WorkTransitionAttributeConstant;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;

public class ProcessValidator
{
    private PackageVO packageVO;
    private ProcessVO process;

    private List<String> errors = new ArrayList<String>();

    public ProcessValidator(PackageVO packageVO) {
        this.packageVO = packageVO;
        this.process = null;
    }

    public ProcessValidator(ProcessVO process) {
        this.packageVO = null;
        this.process = process;
    }

    public void validate(NodeMetaInfo implInfo) throws ValidationException {
    	validate(false, implInfo);
    }

    public void validate(boolean checkImplementors, NodeMetaInfo implInfo) throws ValidationException {
        if (packageVO!=null) {
            // check if processes are up to date
            for (ProcessVO proc : packageVO.getProcesses()) {
                if (proc.getNextVersion()!=null) errors.add("Process " + proc.getProcessName() + " has newer versions");
            }
            // check implementors
            if (checkImplementors) {
	            Map<String,ActivityImplementorVO> has = new HashMap<String,ActivityImplementorVO>();
	            Map<String,String> hasnot = new HashMap<String,String>();
            	for (ActivityImplementorVO ai : packageVO.getImplementors()) {
	            	has.put(ai.getImplementorClassName(), ai);
	            }
            	for (ProcessVO proc : packageVO.getProcesses()) {
            		for (ActivityVO act : proc.getActivities()) {
            			String v = act.getImplementorClassName();
            			if (!has.containsKey(v)) hasnot.put(v, v);
            		}
            		if (proc.getSubProcesses()!=null) {
	            		for (ProcessVO subproc: proc.getSubProcesses()) {
	            			for (ActivityVO act : subproc.getActivities()) {
	                			String v = act.getImplementorClassName();
	                			if (!has.containsKey(v)) hasnot.put(v, v);
	                		}
	            		}
            		}
            	}
            	for (String v : hasnot.keySet()) {
            		errors.add("Implementor " + v + " is used but not included");
            	}
            }
            // validate each process
            for (ProcessVO proc : packageVO.getProcesses()) {
                process = proc;
                validateProcess(implInfo);
            }
        } else {
            validateProcess(implInfo);
        }

        if (errors.size() > 0)
            throw new ValidationException(errors);
    }

    private void validateProcess(NodeMetaInfo implInfo) {
        //AK..Validate Nodes, Links, etc. before saving; continue to allow Saving graph, if user still wants to Save
        //Check if any "floating" Nodes exist
        checkForFloatingNodes();

        //Check if any links(transitions) are visually overlapping
        checkForOverlappingLinks();

        // check for attributes of individual activities
        for (ActivityVO act : process.getActivities()) {
            validateActivity(act, implInfo);
        }
        if (process.getSubProcesses()!=null) {
            for (ProcessVO subproc : process.getSubProcesses()) {
                if (subproc.getActivities()==null) continue;
                for (ActivityVO act : subproc.getActivities()) {
                    validateActivity(act, implInfo);
                }
            }
        }
    }

  /*
   *
   * @param graph
   * @return
   */ //AK..Added 06/10/2008
  private void checkForFloatingNodes()
  {
      //boolean flag = false;
      HashSet<Long> linked_node_set = new HashSet<Long>();
      for (WorkTransitionVO link : process.getTransitions())
      {
          linked_node_set.add(link.getFromWorkId());
          linked_node_set.add(link.getToWorkId());
      } //for..outer loop

      //Now loop through all Nodes for each link to determine if any of the nodes from link-loop (outer) matches node from inner loop....
      for (ActivityVO node : process.getActivities())
      {
          if (!linked_node_set.contains(node.getActivityId())) {
              errors.add("Activity '" + node.getActivityName()
                      + "' has no transitions.");
          }
      } //Inner while..loop
  }

      /*
       *
       * @param graph
       * @return
       */ //AK..Added 06/10/2008
  private void checkForOverlappingLinks()
  {
      Map<Long,String> actnames = new HashMap<Long,String>();
      for (ActivityVO node : process.getActivities()) {
          actnames.put(node.getActivityId(), node.getActivityName());
      }

      HashSet<String> dispinfo_set = new HashSet<String>();
      for (WorkTransitionVO link : process.getTransitions())
      {
          String dispinfo = AttributeVO.findAttribute(link.getAttributes(),
              WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO);
          if (dispinfo==null) continue;
          if( dispinfo_set.contains(dispinfo))
          {
              errors.add("There are overlap transitions between '"
                      + actnames.get(link.getFromWorkId()) + "' and '"
                      + actnames.get(link.getToWorkId()) + "'");
          }
          else
          {
              dispinfo_set.add(dispinfo);
          }
      } //for..outer loop
  }

    private void validateAttributeNotNull(ActivityVO act, String attrname, String errmsg) {
        String av = act.getAttribute(attrname);
        if (av==null || av.trim().length()==0) recordError(act, errmsg);
    }

    private void recordError(ActivityVO act, String errmsg) {
        errors.add("[" + process.getProcessName() + " - "
                + act.getActivityName() + "] "+ errmsg);
    }

    private void validateActivity(ActivityVO act, NodeMetaInfo implInfo) {
    	ActivityImplementorVO impl = implInfo.find(act.getImplementorClassName());
        if (impl.isManualTask()) {
            validateAttributeNotNull(act, TaskActivity.ATTRIBUTE_TASK_NAME,
                "task name is not defined");
            validateAttributeNotNull(act, TaskActivity.ATTRIBUTE_TASK_GROUPS,
                "task has not assigned to any groups");
        } else if (impl.isSubProcessInvoke() && !impl.isHeterogeneousSubProcInvoke()) {
            validateAttributeNotNull(act, WorkAttributeConstant.PROCESS_NAME, "process name not defined for subprocess call");

            if (packageVO!=null) {
                String subproc = act.getAttribute(WorkAttributeConstant.PROCESS_NAME);
                String v = act.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
                int version = v==null?0:Integer.parseInt(v);
                boolean found = false;
                for (ProcessVO proc : packageVO.getProcesses()) {
                    if (proc.getProcessName().equals(subproc)) {
                        found = true;
                        if (proc.getVersion()!=version) {
                            recordError(act, "subprocess version does not match");
                        }
                        break;
                    }
                }
                if (!found) {
                    recordError(act, "invoked subprocess not in the package");
                }
            }
        }
    }

}
