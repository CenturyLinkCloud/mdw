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
package com.centurylink.mdw.designer.utils;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;

public class ProcessWorker {

    private void moveStartActivityToFirst(ProcessVO processVO, NodeMetaInfo metainfo) {
    	for (int i=0; i<processVO.getActivities().size(); i++) {
    		ActivityVO act = processVO.getActivities().get(i);
            ActivityImplementorVO nmi = metainfo.find(act.getImplementorClassName());
            if (nmi==null) continue;
            if (nmi.isStart()) {
            	if (i>0) {
            		processVO.getActivities().remove(i);
            		processVO.getActivities().add(0,act);
            	}
            	return;
            }
        }
    }

    private Long findStartActivityId(ProcessVO processVO, NodeMetaInfo metainfo) {
        for (ActivityVO act : processVO.getActivities()) {
            ActivityImplementorVO nmi = metainfo.find(act.getImplementorClassName());
            if (nmi==null) continue;
            if (nmi.isStart())
                return act.getActivityId();
        }
        // for backward compatibility only
        for (ActivityVO act : processVO.getActivities()) {
            ActivityImplementorVO nmi = metainfo.find(act.getImplementorClassName());
            if (nmi==null) continue;
            if (nmi.getIconName().equals("shape:start")) return act.getActivityId();
        }
        return null;
    }

    // add hiddlen links for main and embedded subprocesses
    public void convert_from_designer(ProcessVO processVO, NodeMetaInfo metainfo) {
    	if (processVO.isInRuleSet()) {
    		moveStartActivityToFirst(processVO, metainfo);
	        List<ProcessVO> subprocs = processVO.getSubProcesses();
	        if (subprocs!=null) {
	            for (ProcessVO subproc : subprocs) {
	        		moveStartActivityToFirst(subproc, metainfo);
	            }
	        }
    	} else {
    		Long startNodeId = findStartActivityId(processVO, metainfo);
	        int hiddenLinkLogicalId = 1000;
	        String startTransId = processVO.getAttribute(WorkAttributeConstant.START_TRANSITION_ID);
	        createHiddenTransition(processVO, startNodeId, EventType.START, null, startTransId,
	        		hiddenLinkLogicalId++, processVO.isInRuleSet());
	        List<ProcessVO> subprocs = processVO.getSubProcesses();
	        if (subprocs!=null) {
	            for (ProcessVO subproc : subprocs) {
	                Integer eventType = subproc.getEventType();
	                String entryTransId = subproc.getAttribute(WorkAttributeConstant.ENTRY_TRANSITION_ID);
	                String entryCode = subproc.getAttribute(WorkAttributeConstant.ENTRY_CODE);
	                createHiddenTransition(processVO, subproc.getProcessId(), eventType,
	                		entryCode, entryTransId, hiddenLinkLogicalId++, processVO.isInRuleSet());
	                startNodeId = findStartActivityId(subproc, metainfo);
	                startTransId = subproc.getAttribute(WorkAttributeConstant.START_TRANSITION_ID);
	                createHiddenTransition(subproc, startNodeId, EventType.START, null, startTransId,
	                		hiddenLinkLogicalId++, processVO.isInRuleSet());
	            }
	        }
    	}
    }

    private void createHiddenTransition(ProcessVO owner, Long to,
    		Integer type, String compcode, String existingId, int logicalId, boolean isInRuleSet)
    {
        Long transId = isInRuleSet?logicalId:existingId==null?new Long(0):new Long(existingId);
        WorkTransitionVO trans = new WorkTransitionVO();
        trans.setEventType(type);
        trans.setProcessId(owner.getProcessId());
        trans.setWorkTransitionId(transId);
        trans.setFromWorkId(isInRuleSet?0L:owner.getProcessId());
        trans.setToWorkId(to);
        trans.setCompletionCode(compcode);
        trans.setAttribute(WorkAttributeConstant.LOGICAL_ID, "H"+logicalId);
        owner.getTransitions().add(trans);
    }

	// remember and then delete hidden links
    public void convert_to_designer(ProcessVO processVO) {
    	if (processVO.isInRuleSet()) return;
    	WorkTransitionVO startTrans = null;
    	List<WorkTransitionVO> hiddenLinks = new ArrayList<WorkTransitionVO>();
    	List<WorkTransitionVO> invalidHiddenLinks = new ArrayList<WorkTransitionVO>();
    	for (WorkTransitionVO w : processVO.getTransitions()) {
    		if (w.getFromWorkId().equals(processVO.getProcessId())) {
    			hiddenLinks.add(w);
    			if (w.getEventType().equals(EventType.START)) startTrans = w;
    		}
    		else if (w.isHidden()) {
    		    // somehow in 4.5 invalid hidden transitions are being added
    		    invalidHiddenLinks.add(w);
    		}
    	}
    	processVO.setAttribute(WorkAttributeConstant.START_TRANSITION_ID,
    			startTrans==null?null:startTrans.getWorkTransitionId().toString());
    	for (WorkTransitionVO w : hiddenLinks) {
    		processVO.getTransitions().remove(w);
    	}
    	for (WorkTransitionVO w : invalidHiddenLinks) {
    	    processVO.getTransitions().remove(w);
    	}
    	List<ProcessVO> subProcesses = processVO.getSubProcesses();
    	if (subProcesses!=null) {
    		for (ProcessVO subProcessVO : subProcesses) {
    			WorkTransitionVO entryTrans = null;
    			for (WorkTransitionVO w : hiddenLinks) {
    				if (!w.getEventType().equals(EventType.START)
    						&& w.getToWorkId().equals(subProcessVO.getProcessId())) {
    					entryTrans = w;
    					break;
    				}
    			}
    			startTrans = null;
    			for (WorkTransitionVO w : subProcessVO.getTransitions()) {
    				if (w.getEventType().equals(EventType.START)) {
    					startTrans = w;
    					break;
    				}
    			}
    			if (startTrans!=null) subProcessVO.getTransitions().remove(startTrans);
    			subProcessVO.setAttribute(WorkAttributeConstant.ENTRY_TRANSITION_ID,
    					entryTrans==null?null:entryTrans.getWorkTransitionId().toString());
    			subProcessVO.setAttribute(WorkAttributeConstant.START_TRANSITION_ID,
    					startTrans==null?null:startTrans.getWorkTransitionId().toString());
    		}
    	}
    }

}