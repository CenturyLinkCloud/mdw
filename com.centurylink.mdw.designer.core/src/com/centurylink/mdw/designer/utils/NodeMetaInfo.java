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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;

public class NodeMetaInfo implements Serializable {

    public static String PSEUDO_PROCESS_ACTIVITY = "PseudoProcessActivity";

    public String DEFAULT_ACTIVITY_IMPLEMENTOR = "com.centurylink.mdw.workflow.activity.DefaultActivityImpl";
    public String DEFAULT_ACTIVITY_IMPLEMENTOR_OLD = "com.qwest.mdw.workflow.activity.impl.ControlledActivityImpl";;
    public String DEFAULT_TASK_ACTIVITY = "com.centurylink.mdw.workflow.activity.task.CustomManualTaskActivity";
    public String DEFAULT_TASK_ACTIVITY_OLD = "com.qwest.mdw.workflow.activity.impl.task.ManualTaskAndEventWaitActivity";
    public String DEFAULT_START = "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity";
    public String DEFAULT_START_OLD = "com.qwest.mdw.workflow.activity.impl.process.ProcessStartControlledActivity";
    public String DEFAULT_STOP = "com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity";
    public String DEFAULT_STOP_OLD = "com.qwest.mdw.workflow.activity.impl.process.ProcessFinishControlledActivity";

    public ActivityImplementorVO getDefaultActivity() {
        return find(DEFAULT_ACTIVITY_IMPLEMENTOR);
    }

    public ActivityImplementorVO getStartActivity() {
        return find(DEFAULT_START);
    }

    public ActivityImplementorVO getStopActivity() {
        return find(DEFAULT_STOP);
    }

    public ActivityImplementorVO getTaskActivity() {
        return find(DEFAULT_TASK_ACTIVITY);
    }

    public ActivityImplementorVO getPseudoActivity() {
        return find(PSEUDO_PROCESS_ACTIVITY);
    }

	private List<ActivityImplementorVO> visibleImpls = null;
	private List<ActivityImplementorVO> hiddenImpls = null;

	public void init(List<ActivityImplementorVO> implementors, int dbschema_version) {
	    init(implementors, false, dbschema_version);
	}

	public void init(List<ActivityImplementorVO> implementors,
			boolean sortAtoZ, int dbschema_version) {
		if (dbschema_version < DataAccess.schemaVersion55) {
		    DEFAULT_ACTIVITY_IMPLEMENTOR = DEFAULT_ACTIVITY_IMPLEMENTOR_OLD;
			DEFAULT_TASK_ACTIVITY = DEFAULT_TASK_ACTIVITY_OLD;
			DEFAULT_START = DEFAULT_START_OLD;
			DEFAULT_STOP = DEFAULT_STOP_OLD;
		}
	    List<ActivityImplementorVO> unsortedList = new ArrayList<ActivityImplementorVO>();
	    for (ActivityImplementorVO activityImpl : implementors) {
            if (activityImpl.getLabel()==null) {
                String label = activityImpl.getImplementorClassName();
                int k = label.lastIndexOf('.');
                if (k>0) label = label.substring(k+1);
                activityImpl.setLabel(label);
            }
            unsortedList.add(activityImpl);
	    }
	    ActivityImplementorVO defaultActivity = null;
	    for (ActivityImplementorVO ai : unsortedList) {
	        if (ai.getImplementorClassName().equals(DEFAULT_ACTIVITY_IMPLEMENTOR)) {
	            defaultActivity = ai;
	            break;
	        }
	    }
	    String attrdesc;
	    if (defaultActivity==null) {
	        attrdesc = "<PAGELET>\n" +
	            "<NOTE LABEL='ERROR' LX='12' LY='25' VX='120' VY='25' VH='120'>\n" +
	            " The implementor is not configured in database, or not completely configured.\n" +
	            "</NOTE>\n" +
	            "<HYPERLINK URL='/MDWWeb/doc/implementor.html'>\n" +
	            "Click here for documentation on implementing activity implementor.</HYPERLINK>\n" +
	            "</PAGELET>\n";
	        defaultActivity = new ActivityImplementorVO("Dummy Activity",
	                DEFAULT_ACTIVITY_IMPLEMENTOR, "shape:activity", attrdesc);
	        defaultActivity.setShowInToolbox(true);
            unsortedList.add(defaultActivity);
        }
	    // add basic activity implementor if not there. This is for prior version of MDW database
	    // and for off-line designer

	    //the first three are reserved for embedded process, start and stop meta info
	    attrdesc = "<PAGELET>"
	        + "<TEXT NAME='" + WorkAttributeConstant.PROCESS_VISIBILITY + "' LABEL='Process Type' READONLY='TRUE'/>"
            + "<TEXT NAME='" + WorkAttributeConstant.EMBEDDED_PROCESS_TYPE + "' LABEL='Sub Type' READONLY='TRUE'/>"
            + "<TEXT NAME='" + WorkAttributeConstant.ENTRY_CODE + "' LABEL='Entry Code' VW='200'/>"
            + "</PAGELET>";
	    ActivityImplementorVO pseudoMainProcess = new ActivityImplementorVO("Embedded Process", PSEUDO_PROCESS_ACTIVITY, "shape:subproc", attrdesc);
	    visibleImpls = new ArrayList<ActivityImplementorVO>();
	    hiddenImpls = new ArrayList<ActivityImplementorVO>();
        visibleImpls.add(pseudoMainProcess);          // process itself
        for (ActivityImplementorVO implVO : unsortedList) {
            if (implVO.isShowInToolbox())
                visibleImpls.add(implVO);
            else
                hiddenImpls.add(implVO);
        }
	    if (sortAtoZ) {
	        // alpha sort by label
	        Collections.sort(visibleImpls);
	    }
	    else {
  	        // default sorting -- grouped by icon (with Start and Stop appearing first)
	        Collections.sort(visibleImpls, new Comparator<ActivityImplementorVO>() {
                public int compare(ActivityImplementorVO impl1, ActivityImplementorVO impl2) {
                    if (PSEUDO_PROCESS_ACTIVITY.equals(impl1.getImplementorClassName())) {
                        return PSEUDO_PROCESS_ACTIVITY.equals(impl2.getImplementorClassName()) ? 0 : -1;
                    }
                    else if (PSEUDO_PROCESS_ACTIVITY.equals(impl2.getImplementorClassName())) {
                        return 1;
                    }
                    else {
                        if (impl1.isStart()) {
                            return impl2.isStart() ? impl1.getIconName().compareTo(impl2.getIconName()) : -1;
                        }
                        else if (impl2.isStart()) {
                            return 1;
                        }
                        else {
                            if (impl1.isFinish()) {
                                return impl2.isFinish() ? impl1.getIconName().compareTo(impl2.getIconName()) : -1;
                            }
                            else if (impl2.isFinish()) {
                                return 1;
                            }
                            else {
                                if (impl1.getIconName() == null) {
                                    return impl2.getIconName() == null ? 0 : -1;
                                }
                                else if (impl2.getIconName() == null) {
                                    return 1;
                                }
                                return impl1.getIconName().compareTo(impl2.getIconName());
                            }
                        }
                    }
                }
	        });
	    }
	}

    public ActivityImplementorVO findVisible(String className) {
        // check visibleImpls
        for (ActivityImplementorVO visibleImpl : visibleImpls) {
            if (visibleImpl.getImplementorClassName().equals(className))
                return visibleImpl;
        }
        return null;
    }

	public ActivityImplementorVO find(String className) {
	    ActivityImplementorVO visibleImpl = findVisible(className);
	    if (visibleImpl != null)
	        return visibleImpl;
        // check hiddenImpls
        for (ActivityImplementorVO hiddenImpl : hiddenImpls) {
            if (hiddenImpl.getImplementorClassName().equals(className))
                return hiddenImpl;
        }
		return null;
    }

	/**
	 * @param i
	 * @return
	 */
	public ActivityImplementorVO get(int i) {
		return visibleImpls.get(i);
	}

	/**
	 * @return
	 */
	public int count() {
		return visibleImpls.size();
	}

	public void complement(ActivityImplementorVO vo) {
		ActivityImplementorVO vo0 = findVisible(vo.getImplementorClassName());
		if (vo0==null) {
			if (vo.isLoaded()) visibleImpls.add(vo);
		} else {
			if (vo0.getAttributeDescription()==null && vo.getAttributeDescription()!=null)
				vo0.setAttributeDescription(vo.getAttributeDescription());
			if (vo0.getBaseClassName()==null && vo.getBaseClassName()!=null)
				vo0.setBaseClassName(vo.getBaseClassName());
			if (vo0.getLabel()==null && vo.getLabel()!=null)
				vo0.setLabel(vo.getLabel());
			if (vo0.getIconName()==null && vo.getIconName()!=null)
				vo0.setIconName(vo.getIconName());
		}
	}

}
